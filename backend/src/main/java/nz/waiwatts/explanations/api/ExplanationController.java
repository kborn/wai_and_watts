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
}