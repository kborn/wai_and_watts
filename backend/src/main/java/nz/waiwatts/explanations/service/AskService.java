package nz.waiwatts.explanations.service;

import io.micrometer.core.instrument.Metrics;
import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.dto.AskResult;
import nz.waiwatts.explanations.dto.Citation;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AskService {

    private static final Logger logger = LoggerFactory.getLogger(AskService.class);

    private final IntentParserService intentParserService;
    private final RequestValidationService validationService;
    private final DatasetSelectionService datasetSelectionService;
    private final ExplanationService explanationService;
    private final CapabilityRegistry capabilityRegistry;
    private final CitationMapper citationMapper;
    private final AskRefusalMapper refusalMapper;

    public AskService(
        IntentParserService intentParserService,
        RequestValidationService validationService,
        DatasetSelectionService datasetSelectionService,
        ExplanationService explanationService,
        CapabilityRegistry capabilityRegistry,
        CitationMapper citationMapper,
        AskRefusalMapper refusalMapper
    ) {
        this.intentParserService = intentParserService;
        this.validationService = validationService;
        this.datasetSelectionService = datasetSelectionService;
        this.explanationService = explanationService;
        this.capabilityRegistry = capabilityRegistry;
        this.citationMapper = citationMapper;
        this.refusalMapper = refusalMapper;
    }

    public AskResponse ask(String question) {
        if (question == null || question.trim().isEmpty()) {
            AskRefusalMapper.AskRefusal refusal = refusalMapper.refusalForMissingQuestion();
            return response(refusal.code().httpStatus(), refusalResult(refusal, null, null, debug(null, null, null, refusal.code().debugTrigger())));
        }

        logger.info("Processing natural language question (length: {})", question.length());

        IntentParseResponse parseResponse = null;
        ExplanationRequest parsedRequest = null;
        DatasetSelectionService.DatasetSelectionResult selectionResult = null;
        Long parseDurationMs = null;

        try {
            long parseStart = System.nanoTime();
            parseResponse = intentParserService.parseQuestion(question);
            parseDurationMs = (System.nanoTime() - parseStart) / 1_000_000;
            recordAskStageMetrics("parse", parseDurationMs);

            if (!parseResponse.isOk()) {
                AskRefusalMapper.AskRefusal refusal = refusalMapper.fromParserRefusal(
                    parseResponse.getRefusal().getCategory(),
                    parseResponse.getRefusal().getMessage()
                );
                AskResult result = refusalResult(
                    refusal,
                    null,
                    null,
                    debug(parseResponse.getParserUsed(), parseDurationMs, null, "PARSE")
                );
                incrementAskRefusalCounter("parse", refusal.code().code());
                return response(refusal.code().httpStatus(), result);
            }

            parsedRequest = parseResponse.getRequest();
            logger.info("Intent parse result: ok - questionType: {}, datasetSource: {}, filters: {}",
                parsedRequest.getQuestionType(),
                parsedRequest.getDatasetSource(),
                summarizeFilters(parsedRequest));

            long selectionStart = System.nanoTime();
            selectionResult = datasetSelectionService.selectDataset(question, parsedRequest);
            long selectionDurationMs = (System.nanoTime() - selectionStart) / 1_000_000;
            recordAskStageMetrics("selection", selectionDurationMs);

            if (!selectionResult.isSelected()) {
                AskRefusalMapper.AskRefusal refusal = refusalMapper.fromSelectionRefusal(
                    selectionResult.getRefusalCategory(),
                    selectionResult.getRefusalMessage()
                );
                AskResult result = refusalResult(
                    refusal,
                    parsedRequest,
                    selectionResult,
                    debug(parseResponse.getParserUsed(), parseDurationMs, selectionResult.getCandidates(), "SELECTION")
                );
                incrementAskRefusalCounter("selection", refusal.code().code());
                return response(refusal.code().httpStatus(), result);
            }

            parsedRequest.setDatasetSource(selectionResult.getDatasetSource());

            long validationStart = System.nanoTime();
            RequestValidationService.ValidationResult validationResult = validationService.validateRequest(parsedRequest);
            long validationDurationMs = (System.nanoTime() - validationStart) / 1_000_000;
            recordAskStageMetrics("validation", validationDurationMs);

            if (!validationResult.isValid()) {
                AskRefusalMapper.AskRefusal refusal = refusalMapper.fromValidationRefusal(
                    validationResult.getRefusalCategory(),
                    validationResult.getRefusalMessage()
                );
                AskResult result = refusalResult(
                    refusal,
                    parsedRequest,
                    selectionResult,
                    debug(parseResponse.getParserUsed(), parseDurationMs, selectionResult.getCandidates(), "VALIDATION")
                );
                incrementAskRefusalCounter("validation", refusal.code().code());
                return response(refusal.code().httpStatus(), result);
            }

            long explanationStart = System.nanoTime();
            Explanation explanation = explanationService.generateExplanation(parsedRequest);
            long explanationDurationMs = (System.nanoTime() - explanationStart) / 1_000_000;
            recordAskStageMetrics("explanation", explanationDurationMs);

            if (explanation.isRefusal()) {
                AskRefusalMapper.AskRefusal refusal = refusalMapper.fromExplanationRefusal(explanation.getRefusalReason());
                AskResult result = refusalResult(
                    refusal,
                    parsedRequest,
                    selectionResult,
                    debug(parseResponse.getParserUsed(), parseDurationMs, selectionResult.getCandidates(), "EXPLANATION")
                );
                incrementAskRefusalCounter("explanation", refusal.code().code());
                return response(refusal.code().httpStatus(), result);
            }

            List<Citation> citations = citationMapper.map(explanation.getCitations(), selectionResult.getDatasetSource());
            Metrics.globalRegistry.counter("waiwatts.ask.success.count").increment();
            return response(
                HttpStatus.OK,
                AskResult.success(
                    parsedRequest,
                    selectionResult.getDatasetSource(),
                    new AskResult.DatasetSelection(selectionResult.getStrategy().name(), selectionResult.getReason()),
                    explanation.getExplanationText(),
                    citations,
                    debug(parseResponse.getParserUsed(), parseDurationMs, selectionResult.getCandidates(), null)
                )
            );
        } catch (Exception e) {
            logger.error("Exception in /ask pipeline: {}", e.getMessage(), e);
            AskRefusalMapper.AskRefusal refusal = refusalMapper.internalError();
            AskResult result = refusalResult(
                refusal,
                parsedRequest,
                selectionResult,
                debug(parseResponse != null ? parseResponse.getParserUsed() : null, parseDurationMs, selectionResult != null ? selectionResult.getCandidates() : null, "EXCEPTION")
            );
            incrementAskRefusalCounter("internal", refusal.code().code());
            return response(refusal.code().httpStatus(), result);
        }
    }

    private String summarizeFilters(ExplanationRequest request) {
        if (request.getFilters() == null || request.getFilters().isEmpty()) {
            return "none";
        }
        StringBuilder summary = new StringBuilder();
        request.getFilters().forEach((key, value) -> {
            if (!summary.isEmpty()) {
                summary.append(", ");
            }
            summary.append(key).append("=").append(value);
        });
        return summary.toString();
    }

    private AskResult.Debug debug(String parserUsed, Long parseDurationMs, List<String> selectionCandidates, String refusalTrigger) {
        return new AskResult.Debug(parserUsed, parseDurationMs, selectionCandidates, refusalTrigger);
    }

    private AskResult refusalResult(
        AskRefusalMapper.AskRefusal refusal,
        ExplanationRequest parsedRequest,
        DatasetSelectionService.DatasetSelectionResult selectionResult,
        AskResult.Debug debug
    ) {
        String selectedDatasetSource = resolveSelectedDatasetSource(parsedRequest, selectionResult);
        AskResult.DatasetSelection datasetSelection;
        if (selectionResult != null) {
            String reason = selectionResult.getReason();
            if (reason == null || reason.isBlank()) {
                reason = selectionResult.getRefusalMessage();
            }
            datasetSelection = new AskResult.DatasetSelection(selectionResult.getStrategy().name(), reason);
        } else {
            datasetSelection = new AskResult.DatasetSelection(
                DatasetSelectionService.DatasetSelectionStrategy.NONE.name(),
                "No dataset selection performed."
            );
        }
        return AskResult.refusal(
            refusal.code().code(),
            refusal.message(),
            refusalDetails(refusal.code(), parsedRequest, selectionResult),
            parsedRequest,
            selectedDatasetSource,
            datasetSelection,
            debug
        );
    }

    private Map<String, Object> refusalDetails(
        AskRefusalCode code,
        ExplanationRequest parsedRequest,
        DatasetSelectionService.DatasetSelectionResult selectionResult
    ) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("category", code.code());

        String datasetSource = resolveSelectedDatasetSource(parsedRequest, selectionResult);
        String questionType = parsedRequest != null ? parsedRequest.getQuestionType() : null;

        List<String> suggestedQuestionTypes = capabilityRegistry.suggestedQuestionTypes(questionType, datasetSource);
        if (!suggestedQuestionTypes.isEmpty()) {
            details.put("supportedQuestionTypes", suggestedQuestionTypes);
        } else {
            details.put("supportedQuestionTypes", capabilityRegistry.getSupportedQuestionTypes().stream().sorted().toList());
        }

        List<String> examples = suggestedQuestionTypes.stream()
            .flatMap(qt -> capabilityRegistry.examplesForQuestion(qt).stream())
            .distinct()
            .limit(3)
            .toList();
        if (examples.isEmpty()) {
            examples = capabilityRegistry.getSupportedQuestionTypes().stream()
                .sorted()
                .limit(3)
                .flatMap(qt -> capabilityRegistry.examplesForQuestion(qt).stream())
                .distinct()
                .limit(3)
                .toList();
        }
        details.put("examples", examples);

        if (questionType != null && !questionType.isBlank()) {
            List<String> metricTypes = capabilityRegistry.supportedMetricTypesForQuestion(questionType).stream()
                .sorted()
                .toList();
            if (!metricTypes.isEmpty()) {
                details.put("supportedMetricTypes", metricTypes);
                capabilityRegistry.defaultMetricTypeForQuestion(questionType)
                    .ifPresent(defaultMetric -> details.put("defaultMetricType", defaultMetric));
            }
        }
        if (datasetSource != null && !datasetSource.isBlank()) {
            details.put("datasetSource", datasetSource);
        }
        return details;
    }

    private String resolveSelectedDatasetSource(
        ExplanationRequest parsedRequest,
        DatasetSelectionService.DatasetSelectionResult selectionResult
    ) {
        if (selectionResult != null && selectionResult.getDatasetSource() != null) {
            return selectionResult.getDatasetSource();
        }
        return parsedRequest != null ? parsedRequest.getDatasetSource() : null;
    }

    private void recordAskStageMetrics(String stage, Long durationMs) {
        Metrics.globalRegistry.counter("waiwatts.ask.stage.count", "stage", stage).increment();
        if (durationMs != null) {
            Metrics.globalRegistry
                .timer("waiwatts.ask.stage.duration", "stage", stage)
                .record(Math.max(durationMs, 0L), TimeUnit.MILLISECONDS);
        }
    }

    private void incrementAskRefusalCounter(String stage, String code) {
        String refusalCode = (code == null || code.isBlank()) ? "UNKNOWN" : code;
        Metrics.globalRegistry.counter("waiwatts.ask.refusal.count", "stage", stage, "code", refusalCode).increment();
    }

    private AskResponse response(HttpStatus status, AskResult result) {
        return new AskResponse(status, result);
    }

    public record AskResponse(HttpStatus httpStatus, AskResult result) {}
}
