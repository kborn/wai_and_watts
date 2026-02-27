package nz.waiwatts.explanations.api;

import io.micrometer.core.instrument.Metrics;
import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.dto.AskResult;
import nz.waiwatts.explanations.dto.Citation;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.service.CapabilitiesService;
import nz.waiwatts.explanations.service.CitationMapper;
import nz.waiwatts.explanations.service.DatasetSelectionService;
import nz.waiwatts.explanations.service.ExplanationService;
import nz.waiwatts.explanations.service.IntentParserService;
import nz.waiwatts.explanations.service.RequestValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * REST API controller for generating explanations from environmental data.
 * <p>
 * Versioned public API controller under /api/v1.
 * <p>
 * Enforces structured question typing and prevents freeform chat prompts.
 * Only accepts question_type from supported classes and structured filters.
 */
@RestController
@RequestMapping("/api/v1/explanations")
public class ExplanationController {

    private static final Logger logger = LoggerFactory.getLogger(ExplanationController.class);
    private static final String LEGACY_API_SUNSET = "Wed, 31 Dec 2026 23:59:59 GMT";
    
    private final ExplanationService explanationService;
    private final IntentParserService intentParserService;
    private final RequestValidationService validationService;
    private final DatasetSelectionService datasetSelectionService;
    private final CapabilitiesService capabilitiesService;
    private final CapabilityRegistry capabilityRegistry;
    private final CitationMapper citationMapper = new CitationMapper();

    public ExplanationController(
        ExplanationService explanationService,
        IntentParserService intentParserService,
        RequestValidationService validationService,
        DatasetSelectionService datasetSelectionService,
        CapabilitiesService capabilitiesService,
        CapabilityRegistry capabilityRegistry
    ) {
        this.explanationService = explanationService;
        this.intentParserService = intentParserService;
        this.validationService = validationService;
        this.datasetSelectionService = datasetSelectionService;
        this.capabilitiesService = capabilitiesService;
        this.capabilityRegistry = capabilityRegistry;
    }

    private String summarizeFilters(ExplanationRequest request) {
        if (request.getFilters() == null || request.getFilters().isEmpty()) {
            return "none";
        }
        
        StringBuilder summary = new StringBuilder();
        request.getFilters().forEach((key, value) -> {
            if (!summary.isEmpty()) summary.append(", ");
            summary.append(key).append("=").append(value);
        });
        return summary.toString();
    }

    /**
     * Generate an explanation for a supported question type.
     * 
     * @param request structured request with questionType, datasetSource, and filters
     * @return explanation with citations or refusal
     */
    @PostMapping
    public ResponseEntity<Explanation> generateExplanation(@RequestBody ExplanationRequest request) {
        Explanation explanation = explanationService.generateExplanation(request);
        return ResponseEntity.ok(explanation);
    }

    /**
     * Process a natural language question and generate an explanation.
     * <p>
     * Phase 12 endpoint: Parses natural language → validates → generates explanation.
     * Fact pack builders pin data to one canonical dataset_release per request.
     * Follows same refusal behavior as structured endpoint.
     * 
     * @param body request containing natural language question
     * @return explanation with citations or refusal
     */
    @PostMapping("/ask")
    public ResponseEntity<AskResult> askQuestion(@RequestBody Map<String, String> body) {
        logger.info("Received /ask request body: {}", body);
        
        String question = body.get("question");
        if (question == null || question.trim().isEmpty()) {
            logger.warn("Question is null or empty in request body: {}", body);
            AskResult result = refusalResult(
                "VALIDATION_FAILED",
                "Question is required",
                null,
                null,
                new AskResult.Debug(null, null, null, "REQUEST")
            );
            return ResponseEntity.badRequest().body(result);
        }

        logger.info("Processing natural language question (length: {})", question.length());

        IntentParseResponse parseResponse = null;
        ExplanationRequest parsedRequest = null;
        DatasetSelectionService.DatasetSelectionResult selectionResult = null;
        Long parseDurationMs = null;

        try {
            long parseStart = System.nanoTime();
            // Parse natural language to structured request
            parseResponse = intentParserService.parseQuestion(question);
            parseDurationMs = (System.nanoTime() - parseStart) / 1_000_000;
            recordAskStageLatency("parse", parseDurationMs);
        
            if (!parseResponse.isOk()) {
                logger.info("Intent parse result: refusal - category: {}, message: {}", 
                    parseResponse.getRefusal().getCategory(), 
                    parseResponse.getRefusal().getMessage());

                AskResult.Debug debug = new AskResult.Debug(
                    parseResponse.getParserUsed(),
                    parseDurationMs,
                    null,
                    "PARSE"
                );
                AskResult result = refusalResult(
                    mapParserCode(parseResponse.getRefusal().getCategory()),
                    parseResponse.getRefusal().getMessage(),
                    null,
                    null,
                    debug
                );
                incrementAskRefusalCounter("parse", result.getRefusal().getCode());
                return ResponseEntity.ok(result);
            }

            // Log successful intent parse
            parsedRequest = parseResponse.getRequest();
            logger.info("Intent parse result: ok - questionType: {}, datasetSource: {}, filters: {}", 
                parsedRequest.getQuestionType(),
                parsedRequest.getDatasetSource(),
                summarizeFilters(parsedRequest));

            // Select dataset if missing or verify explicit dataset
            long selectionStart = System.nanoTime();
            selectionResult = datasetSelectionService.selectDataset(question, parsedRequest);
            long selectionDurationMs = (System.nanoTime() - selectionStart) / 1_000_000;
            recordAskStageLatency("selection", selectionDurationMs);

            if (!selectionResult.isSelected()) {
                logger.info("Dataset selection refusal: category={}, message={}",
                    selectionResult.getRefusalCategory(), selectionResult.getRefusalMessage());

                AskResult.Debug debug = new AskResult.Debug(
                    parseResponse.getParserUsed(),
                    parseDurationMs,
                    selectionResult.getCandidates(),
                    "SELECTION"
                );
                AskResult result = refusalResult(
                    selectionResult.getRefusalCategory(),
                    selectionResult.getRefusalMessage(),
                    parsedRequest,
                    selectionResult,
                    debug
                );
                incrementAskRefusalCounter("selection", result.getRefusal().getCode());
                return ResponseEntity.ok(result);
            }

            parsedRequest.setDatasetSource(selectionResult.getDatasetSource());

            // Validate the parsed request
            long validationStart = System.nanoTime();
            RequestValidationService.ValidationResult validationResult = validationService.validateRequest(parsedRequest);
            long validationDurationMs = (System.nanoTime() - validationStart) / 1_000_000;
            recordAskStageLatency("validation", validationDurationMs);
            
            if (!validationResult.isValid()) {
                logger.info("Validation result: refusal - category: {}, message: {}", 
                    validationResult.getRefusalCategory(), 
                    validationResult.getRefusalMessage());

                AskResult.Debug debug = new AskResult.Debug(
                    parseResponse.getParserUsed(),
                    parseDurationMs,
                    selectionResult.getCandidates(),
                    "VALIDATION"
                );
                AskResult result = refusalResult(
                    mapValidationCode(validationResult.getRefusalCategory()),
                    validationResult.getRefusalMessage(),
                    parsedRequest,
                    selectionResult,
                    debug
                );
                incrementAskRefusalCounter("validation", result.getRefusal().getCode());
                return ResponseEntity.ok(result);
            }

            logger.info("Validation result: ok - request valid");

            // Generate explanation using existing pipeline
            long explanationStart = System.nanoTime();
            Explanation explanation = explanationService.generateExplanation(parsedRequest);
            long explanationDurationMs = (System.nanoTime() - explanationStart) / 1_000_000;
            recordAskStageLatency("explanation", explanationDurationMs);
            
            if (explanation.isRefusal()) {
                String code = mapExplanationRefusalCode(explanation.getRefusalReason());
                String message = explanation.getRefusalReason();
                if ("NO_DATA".equals(code)) {
                    message = "Valid request, but no rows matched.";
                }
                logger.info("Final result: refusal - category: {}, reason: {}", 
                    code, message);

                AskResult.Debug debug = new AskResult.Debug(
                    parseResponse.getParserUsed(),
                    parseDurationMs,
                    selectionResult.getCandidates(),
                    "EXPLANATION"
                );
                AskResult result = refusalResult(
                    code,
                    message,
                    parsedRequest,
                    selectionResult,
                    debug
                );
                incrementAskRefusalCounter("explanation", result.getRefusal().getCode());
                return ResponseEntity.ok(result);
            } else {
                logger.info("Final result: ok - explanation generated");
            }
            
            List<Citation> citations = citationMapper.map(
                explanation.getCitations(),
                selectionResult.getDatasetSource()
            );

            AskResult result = AskResult.success(
                parsedRequest,
                selectionResult.getDatasetSource(),
                new AskResult.DatasetSelection(
                    selectionResult.getStrategy().name(),
                    selectionResult.getReason()
                ),
                explanation.getExplanationText(),
                citations,
                new AskResult.Debug(
                    parseResponse.getParserUsed(),
                    parseDurationMs,
                    selectionResult.getCandidates(),
                    null
                )
            );
            Metrics.globalRegistry.counter("waiwatts.ask.success.count").increment();
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Exception in /ask endpoint: {}", e.getMessage(), e);
            AskResult.Debug debug = new AskResult.Debug(
                parseResponse != null ? parseResponse.getParserUsed() : null,
                parseDurationMs,
                selectionResult != null ? selectionResult.getCandidates() : null,
                "EXCEPTION"
            );
            AskResult result = refusalResult(
                "INTERNAL_ERROR",
                "An internal error occurred while processing your request. Please try again.",
                parsedRequest,
                selectionResult,
                debug
            );
            incrementAskRefusalCounter("internal", result.getRefusal().getCode());
            return ResponseEntity.ok(result);
        }
    }

    private void recordAskStageLatency(String stage, Long durationMs) {
        if (durationMs == null) {
            return;
        }
        Metrics.globalRegistry
            .timer("waiwatts.ask.stage.duration", "stage", stage)
            .record(Math.max(durationMs, 0L), TimeUnit.MILLISECONDS);
    }

    private void incrementAskRefusalCounter(String stage, String code) {
        String refusalCode = (code == null || code.isBlank()) ? "UNKNOWN" : code;
        Metrics.globalRegistry
            .counter("waiwatts.ask.refusal.count", "stage", stage, "code", refusalCode)
            .increment();
    }

    private AskResult refusalResult(
        String code,
        String message,
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
            datasetSelection = new AskResult.DatasetSelection(
                selectionResult.getStrategy().name(),
                reason
            );
        } else {
            datasetSelection = new AskResult.DatasetSelection(
                DatasetSelectionService.DatasetSelectionStrategy.NONE.name(),
                "No dataset selection performed."
            );
        }

        return AskResult.refusal(
            code,
            message,
            refusalDetails(code, parsedRequest, selectionResult),
            parsedRequest,
            selectedDatasetSource,
            datasetSelection,
            debug
        );
    }

    private Map<String, Object> refusalDetails(
        String code,
        ExplanationRequest parsedRequest,
        DatasetSelectionService.DatasetSelectionResult selectionResult
    ) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("category", code);

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
        if (parsedRequest != null) {
            return parsedRequest.getDatasetSource();
        }
        return null;
    }

    private String mapValidationCode(String refusalCategory) {
        if (refusalCategory == null) {
            return "VALIDATION_FAILED";
        }
        return switch (refusalCategory) {
            case "UNSUPPORTED_QUESTION_TYPE", "UNSUPPORTED_CAPABILITY" -> "UNSUPPORTED_CAPABILITY";
            case "MISSING_REQUIRED_FILTERS" -> "MISSING_REQUIRED_FILTERS";
            case "DATASET_MISMATCH" -> "DATASET_MISMATCH";
            default -> "VALIDATION_FAILED";
        };
    }

    private String mapParserCode(String refusalCategory) {
        if (refusalCategory == null || refusalCategory.isBlank()) {
            return "UNABLE_TO_PARSE";
        }
        return switch (refusalCategory) {
            case "UNSUPPORTED_INTENT", "UNSUPPORTED_QUESTION_TYPE" -> "UNSUPPORTED_CAPABILITY";
            default -> refusalCategory;
        };
    }

    private String mapExplanationRefusalCode(String reason) {
        if (reason == null || reason.isBlank()) {
            return "INTERNAL_ERROR";
        }
        if (reason.startsWith("Invalid request:")) {
            return "VALIDATION_FAILED";
        }
        if (reason.startsWith("No facts available")) {
            return "NO_DATA";
        }
        if (reason.startsWith("No data source available")) {
            return "UNSUPPORTED_CAPABILITY";
        }
        if (reason.startsWith("Unable to build FactPack")) {
            return "UNSUPPORTED_CAPABILITY";
        }
        if (reason.startsWith("FactPack missing")) {
            return "INTERNAL_ERROR";
        }
        if (reason.startsWith("Generated explanation missing required citations")) {
            return "INTERNAL_ERROR";
        }
        if (reason.startsWith("Explanation provider failed")) {
            return "INTERNAL_ERROR";
        }
        return "UNSUPPORTED_CAPABILITY";
    }

    /**
     * Get supported question types and required filter structure.
     * 
     * @return supported and unsupported question classes with filter requirements
     */
    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> getSupportedQuestionTypes() {
        return ResponseEntity.ok()
            .header("Sunset", LEGACY_API_SUNSET)
            .header(HttpHeaders.LINK, "</api/v1/capabilities>; rel=\"successor-version\"")
            .header(HttpHeaders.WARNING, "299 - \"Deprecated API; use /api/v1/capabilities\"")
            .body(capabilitiesService.buildCapabilitiesResponse());
    }

    /**
     * Health check for explanation service.
     * 
     * @return service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok()
            .header("Sunset", LEGACY_API_SUNSET)
            .header(HttpHeaders.LINK, "</api/v1/health>; rel=\"successor-version\"")
            .header(HttpHeaders.WARNING, "299 - \"Deprecated API; use /api/v1/health\"")
            .body(Map.of(
                "status", "healthy",
                "service", "explanation-api",
                "phase", "11"
            ));
    }

    /**
     * Debug endpoint to build and return full FactPack for verification.
     * 
     * @param request same request structure as /api/v1/explanations
     * @return full FactPack JSON for debugging
     */
    @PostMapping("/fact-pack")
    public ResponseEntity<Object> buildFactPack(@RequestBody ExplanationRequest request) {
        Object factPack = explanationService.buildFactPack(request);
        return ResponseEntity.ok(factPack);
    }
}
