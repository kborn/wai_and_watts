package nz.waiwatts.explanations.api;

import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dataset.DatasetDescriptor;
import nz.waiwatts.explanations.dto.AskResult;
import nz.waiwatts.explanations.dto.Citation;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final DatasetCatalog datasetCatalog;
    private final CitationMapper citationMapper = new CitationMapper();

    public ExplanationController(
        ExplanationService explanationService,
        IntentParserService intentParserService,
        RequestValidationService validationService,
        DatasetSelectionService datasetSelectionService,
        DatasetCatalog datasetCatalog
    ) {
        this.explanationService = explanationService;
        this.intentParserService = intentParserService;
        this.validationService = validationService;
        this.datasetSelectionService = datasetSelectionService;
        this.datasetCatalog = datasetCatalog;
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

        try {
            long parseStart = System.nanoTime();
            // Parse natural language to structured request
            IntentParseResponse parseResponse = intentParserService.parseQuestion(question);
            long parseDurationMs = (System.nanoTime() - parseStart) / 1_000_000;
        
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
                    parseResponse.getRefusal().getCategory(),
                    parseResponse.getRefusal().getMessage(),
                    null,
                    null,
                    debug
                );
                return ResponseEntity.ok(result);
            }

            // Log successful intent parse
            ExplanationRequest parsedRequest = parseResponse.getRequest();
            logger.info("Intent parse result: ok - questionType: {}, datasetSource: {}, filters: {}", 
                parsedRequest.getQuestionType(),
                parsedRequest.getDatasetSource(),
                summarizeFilters(parsedRequest));

            // Select dataset if missing or verify explicit dataset
            DatasetSelectionService.DatasetSelectionResult selectionResult =
                datasetSelectionService.selectDataset(question, parsedRequest);

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
            throw e;
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
        Map<String, Object> supportedTypes = new LinkedHashMap<>();
        supportedTypes.put("supportedQuestionTypes", Map.of(
            // MBIE Generation Question Types
            "renewable_generation_trend", "Explain renewable generation trends between years",
            "hydro_generation_trend", "Explain hydro generation trends between years",
            "fuel_type_comparison", "Compare two fuel types (e.g., hydro vs geothermal)",
            "generation_mix_overview", "Summarize main sources of electricity generation",

            // LAWA Water Quality State Question Types
            "water_quality_overview", "Provide overview of water quality state distribution",
            "excellent_sites_trend", "Explain trends in excellent water quality sites",
            "regional_water_quality", "Compare water quality across regions",

            // LAWA Water Quality Trend Question Types
            "water_quality_trends", "Explain overall water quality trend distribution",
            "improving_sites_trend", "Explain trends in improving water quality sites",
            "regional_trend_comparison", "Compare water quality trends across regions"
        ));
        supportedTypes.put("unsupportedQuestionTypes", Map.of(
            "forecasting", "Predicting future values",
            "causation", "Claiming cause-and-effect relationships",
            "policy_recommendation", "Recommending policies",
            "site_specific_advice", "Providing site-specific water quality advice",
            "hypothetical", "What-if scenarios or counterfactuals"
        ));
        supportedTypes.put("supportedDatasetSources", Map.of(
            "mbie.generation.annual", "Annual electricity generation data (MBIE)",
            "mbie.generation.quarterly", "Quarterly electricity generation data (MBIE)",
            "lawa.water_quality.state.multi_year", "Water quality state assessments (LAWA)",
            "lawa.water_quality.trend.multi_year", "Water quality trend analyses (LAWA)"
        ));
        supportedTypes.put("requiredFilters", Map.of(
            "datasetSource", "Must specify the data source (e.g., 'mbie.generation.annual', 'lawa.water_quality.state.multi_year')"
        ));
        supportedTypes.put("filterStructure", Map.of(
            "datasetSource", "string (required)",
            "fuelType", "string (optional, for MBIE data)",
            "fuelTypeB", "string (optional second fuel for MBIE comparisons)",
            "indicator", "string (optional, for LAWA data)",
            "region", "string (optional, for LAWA data)",
            "trend", "string (optional, for LAWA trend data)",
            "startYear", "integer (optional)",
            "endYear", "integer (optional)"
        ));

        List<Map<String, Object>> datasets = new ArrayList<>();
        for (DatasetDescriptor descriptor : datasetCatalog.getDatasets()) {
            datasets.add(Map.of(
                "datasetSource", descriptor.datasetSource(),
                "displayName", descriptor.displayName(),
                "description", descriptor.displayName(),
                "supportedQuestionTypes", descriptor.supportedQuestionTypes(),
                "supportedFilters", descriptor.supportedFilters()
            ));
        }
        supportedTypes.put("datasets", datasets);
        
        return ResponseEntity.ok(supportedTypes);
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
