package nz.waiwatts.explanations.api;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.service.ExplanationService;
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
@RequestMapping("/api/explanations")
public class ExplanationController {

    private final ExplanationService explanationService;

    public ExplanationController(ExplanationService explanationService) {
        this.explanationService = explanationService;
    }

    /**
     * Generate an explanation for a supported question type.
     * 
     * @param request structured request with question_type and filters
     * @return explanation with citations or refusal
     */
    @PostMapping
    public ResponseEntity<Explanation> generateExplanation(@RequestBody ExplanationRequest request) {
        Explanation explanation = explanationService.generateExplanation(request);
        return ResponseEntity.ok(explanation);
    }

    /**
     * Get supported question types and required filter structure.
     * 
     * @return supported and unsupported question classes with filter requirements
     */
    @GetMapping("/question-types")
    public ResponseEntity<Map<String, Object>> getSupportedQuestionTypes() {
        Map<String, Object> supportedTypes = Map.of(
            "supportedQuestionTypes", Map.of(
                "renewable_generation_trend", "Explain renewable generation trends between years",
                "hydro_generation_trend", "Explain hydro generation trends between years", 
                "fuel_type_comparison", "Compare wind vs hydro generation"
            ),
            "unsupportedQuestionTypes", Map.of(
                "forecasting", "Predicting future values",
                "causation", "Claiming cause-and-effect relationships",
                "policy_recommendation", "Recommending policies",
                "hypothetical", "What-if scenarios or counterfactuals"
            ),
            "requiredFilters", Map.of(
                "datasetSource", "Must specify the data source (e.g., 'mbie.generation.annual')"
            ),
            "filterStructure", Map.of(
                "datasetSource", "string (required)",
                "fuelType", "string (optional)",
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
}