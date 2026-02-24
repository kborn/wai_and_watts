package nz.waiwatts.explanations.api;

import nz.waiwatts.explanations.dto.AskResult;
import nz.waiwatts.explanations.dto.CapabilitiesResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for generating explanations from environmental data.
 * 
 * Versioned public API controller under /api/v1.
 *
 * Enforces structured question typing and prevents freeform chat prompts.
 * Only accepts question_type from supported classes and structured filters.
 */
@RestController
@RequestMapping("/api/v1/explanations")
public class ExplanationController {

    private static final Logger logger = LoggerFactory.getLogger(ExplanationController.class);
    
    private final ExplanationService explanationService;
    private final IntentParserService intentParserService;
    private final RequestValidationService validationService;
    private final DatasetSelectionService datasetSelectionService;
    private final CapabilitiesService capabilitiesService;
    private final CitationMapper citationMapper = new CitationMapper();

    public ExplanationController(
        ExplanationService explanationService,
        IntentParserService intentParserService,
        RequestValidationService validationService,
        DatasetSelectionService datasetSelectionService,
        CapabilitiesService capabilitiesService
    ) {
        this.explanationService = explanationService;
        this.intentParserService = intentParserService;
        this.validationService = validationService;
        this.datasetSelectionService = datasetSelectionService;
        this.capabilitiesService = capabilitiesService;
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
     * 
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
                return ResponseEntity.ok(result);
            }

            // Log successful intent parse
            parsedRequest = parseResponse.getRequest();
            logger.info("Intent parse result: ok - questionType: {}, datasetSource: {}, filters: {}", 
                parsedRequest.getQuestionType(),
                parsedRequest.getDatasetSource(),
                summarizeFilters(parsedRequest));

            // Select dataset if missing or verify explicit dataset
            selectionResult = datasetSelectionService.selectDataset(question, parsedRequest);

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
                return ResponseEntity.ok(result);
            }

            parsedRequest.setDatasetSource(selectionResult.getDatasetSource());

            // Validate the parsed request
            RequestValidationService.ValidationResult validationResult = validationService.validateRequest(parsedRequest);
            
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
                return ResponseEntity.ok(result);
            }

            logger.info("Validation result: ok - request valid");

            // Generate explanation using existing pipeline
            Explanation explanation = explanationService.generateExplanation(parsedRequest);
            
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
                return ResponseEntity.ok(result);
            } else {
                logger.info("Final result: ok - explanation generated");
            }
            
            List<Citation> citations = citationMapper.map(
                explanation.getCitations(),
                selectionResult.getDatasetSource()
            );

            AskResult result = new AskResult();
            result.setRefusal(false);
            result.setRefusal(new AskResult.Refusal(null, null, null));
            result.setParsedRequest(parsedRequest);
            result.setSelectedDatasetSource(selectionResult.getDatasetSource());
            result.setDatasetSelection(new AskResult.DatasetSelection(
                selectionResult.getStrategy().name(),
                selectionResult.getReason()
            ));
            result.setExplanation(explanation.getExplanationText());
            result.setCitations(citations != null ? citations : List.of());
            result.setDebug(new AskResult.Debug(
                parseResponse.getParserUsed(),
                parseDurationMs,
                selectionResult.getCandidates(),
                null
            ));
            
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
            return ResponseEntity.ok(result);
        }
    }

    private AskResult refusalResult(
        String code,
        String message,
        ExplanationRequest parsedRequest,
        DatasetSelectionService.DatasetSelectionResult selectionResult,
        AskResult.Debug debug
    ) {
        AskResult result = new AskResult();
        result.setRefusal(true);
        result.setRefusal(new AskResult.Refusal(code, message, null));
        result.setParsedRequest(parsedRequest);
        result.setSelectedDatasetSource(resolveSelectedDatasetSource(parsedRequest, selectionResult));
        if (selectionResult != null) {
            String reason = selectionResult.getReason();
            if (reason == null || reason.isBlank()) {
                reason = selectionResult.getRefusalMessage();
            }
            result.setDatasetSelection(new AskResult.DatasetSelection(
                selectionResult.getStrategy().name(),
                reason
            ));
        } else {
            result.setDatasetSelection(new AskResult.DatasetSelection(
                DatasetSelectionService.DatasetSelectionStrategy.NONE.name(),
                "No dataset selection performed."
            ));
        }
        result.setExplanation("");
        result.setCitations(List.of());
        result.setDebug(debug);
        return result;
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
            case "UNSUPPORTED_QUESTION_TYPE" -> "UNSUPPORTED_CAPABILITY";
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
    public ResponseEntity<CapabilitiesResponse> getSupportedQuestionTypes() {
        return ResponseEntity.ok(capabilitiesService.buildCapabilitiesResponse());
    }

    /**
     * Health check for explanation service.
     * 
     * @return service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
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
