package nz.waiwatts.explanations.api;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.service.ExplanationService;
import nz.waiwatts.explanations.service.IntentParserService;
import nz.waiwatts.explanations.service.RequestValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for generating explanations from environmental data.
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

    public ExplanationController(
        ExplanationService explanationService,
        IntentParserService intentParserService,
        RequestValidationService validationService
    ) {
        this.explanationService = explanationService;
        this.intentParserService = intentParserService;
        this.validationService = validationService;
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
     * Follows same refusal behavior as structured endpoint.
     * 
     * @param body request containing natural language question
     * @return explanation with citations or refusal
     */
    @PostMapping("/ask")
    public ResponseEntity<Explanation> askQuestion(@RequestBody Map<String, String> body) {
        logger.info("Received /ask request body: {}", body);
        
        String question = body.get("question");
        if (question == null || question.trim().isEmpty()) {
            logger.warn("Question is null or empty in request body: {}", body);
            Explanation explanation = Explanation.refusal("INVALID_FILTERS");
            explanation.setRefusalReason("Question is required");
            return ResponseEntity.badRequest().body(explanation);
        }

        logger.info("Processing natural language question (length: {})", question.length());

        try {
            // Parse natural language to structured request
            IntentParseResponse parseResponse = intentParserService.parseQuestion(question);
        
        if (!parseResponse.isOk()) {
            logger.info("Intent parse result: refusal - category: {}, message: {}", 
                parseResponse.getRefusal().getCategory(), 
                parseResponse.getRefusal().getMessage());
            
            Explanation explanation = Explanation.refusal(parseResponse.getRefusal().getCategory());
            explanation.setRefusalReason(parseResponse.getRefusal().getMessage());
            return ResponseEntity.ok(explanation);
        }

        // Log successful intent parse
        ExplanationRequest parsedRequest = parseResponse.getRequest();
        logger.info("Intent parse result: ok - questionType: {}, datasetSource: {}, filters: {}", 
            parsedRequest.getQuestionType(),
            parsedRequest.getDatasetSource(),
            summarizeFilters(parsedRequest));

        // Validate the parsed request
        RequestValidationService.ValidationResult validationResult = validationService.validateRequest(parsedRequest);
        
        if (!validationResult.isValid()) {
            logger.info("Validation result: refusal - category: {}, message: {}", 
                validationResult.getRefusalCategory(), 
                validationResult.getRefusalMessage());
            
            Explanation explanation = Explanation.refusal(validationResult.getRefusalCategory());
            explanation.setRefusalReason(validationResult.getRefusalMessage());
            return ResponseEntity.ok(explanation);
        }

        logger.info("Validation result: ok - request valid");

        // Generate explanation using existing pipeline
        Explanation explanation = explanationService.generateExplanation(parsedRequest);
        
        if (explanation.isRefusal()) {
            logger.info("Final result: refusal - category: {}, reason: {}", 
                "EXPLANATION_FAILED", explanation.getRefusalReason());
        } else {
            logger.info("Final result: ok - explanation generated");
        }
        
        return ResponseEntity.ok(explanation);
        } catch (Exception e) {
            logger.error("Exception in /ask endpoint: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get supported question types and required filter structure.
     * 
     * @return supported and unsupported question classes with filter requirements
     */
    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> getSupportedQuestionTypes() {
        Map<String, Object> supportedTypes = Map.of(
            "supportedQuestionTypes", Map.of(
                // MBIE Generation Question Types
                "renewable_generation_trend", "Explain renewable generation trends between years",
                "hydro_generation_trend", "Explain hydro generation trends between years", 
                "fuel_type_comparison", "Compare wind vs hydro generation",
                
                // LAWA Water Quality State Question Types
                "water_quality_overview", "Provide overview of water quality state distribution",
                "excellent_sites_trend", "Explain trends in excellent water quality sites",
                "regional_water_quality", "Compare water quality across regions",
                
                // LAWA Water Quality Trend Question Types
                "water_quality_trends", "Explain overall water quality trend distribution",
                "improving_sites_trend", "Explain trends in improving water quality sites",
                "regional_trend_comparison", "Compare water quality trends across regions"
            ),
            "unsupportedQuestionTypes", Map.of(
                "forecasting", "Predicting future values",
                "causation", "Claiming cause-and-effect relationships",
                "policy_recommendation", "Recommending policies",
                "site_specific_advice", "Providing site-specific water quality advice",
                "hypothetical", "What-if scenarios or counterfactuals"
            ),
            "supportedDatasetSources", Map.of(
                "mbie.generation.annual", "Annual electricity generation data (MBIE)",
                "mbie.generation.quarterly", "Quarterly electricity generation data (MBIE)",
                "lawa.water_quality.state.multi_year", "Water quality state assessments (LAWA)",
                "lawa.water_quality.trend.multi_year", "Water quality trend analyses (LAWA)"
            ),
            "requiredFilters", Map.of(
                "datasetSource", "Must specify the data source (e.g., 'mbie.generation.annual', 'lawa.water_quality.state.multi_year')"
            ),
            "filterStructure", Map.of(
                "datasetSource", "string (required)",
                "fuelType", "string (optional, for MBIE data)",
                "indicator", "string (optional, for LAWA data)",
                "region", "string (optional, for LAWA data)",
                "trend", "string (optional, for LAWA trend data)",
                "startYear", "integer (optional)",
                "endYear", "integer (optional)"
            )
        );
        
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