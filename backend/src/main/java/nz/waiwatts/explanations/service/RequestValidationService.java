package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dto.ExplanationRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for validating ExplanationRequest against Phase 12 contracts.
 * 
 * Enforces:
 * - Supported question types
 * - Supported dataset sources  
 * - Filter schema validation
 * - Cross-field constraint validation
 */
@Service
public class RequestValidationService {
    
    // Supported question types from contract
    private static final List<String> SUPPORTED_QUESTION_TYPES = List.of(
        // MBIE Generation Question Types
        "renewable_generation_trend",
        "hydro_generation_trend", 
        "fuel_type_comparison",
        
        // LAWA Water Quality State Question Types
        "water_quality_overview",
        "excellent_sites_trend",
        "regional_water_quality",
        
        // LAWA Water Quality Trend Question Types
        "water_quality_trends",
        "improving_sites_trend",
        "regional_trend_comparison"
    );
    
    // Supported dataset sources from contract
    private static final List<String> SUPPORTED_DATASET_SOURCES = List.of(
        "mbie.generation.annual",
        "mbie.generation.quarterly", 
        "lawa.water_quality.state.multi_year",
        "lawa.water_quality.trend.multi_year"
    );
    
    // Allowed filter keys
    private static final Set<String> ALLOWED_FILTER_KEYS = Set.of(
        "fuelType", "indicator", "region", "trend", "startYear", "endYear"
    );
    
    /**
     * Validates an ExplanationRequest according to Phase 12 contract.
     * 
     * @param request the request to validate
     * @return ValidationResult with either success or refusal details
     */
    public ValidationResult validateRequest(ExplanationRequest request) {
        // Check required fields
        if (request.getQuestionType() == null || request.getQuestionType().trim().isEmpty()) {
            return ValidationResult.failure("INVALID_FILTERS", "questionType is required");
        }
        
        if (request.getDatasetSource() == null || request.getDatasetSource().trim().isEmpty()) {
            return ValidationResult.failure("INVALID_FILTERS", "datasetSource is required");
        }
        
        // Validate question type
        if (!SUPPORTED_QUESTION_TYPES.contains(request.getQuestionType())) {
            return ValidationResult.failure("UNSUPPORTED_QUESTION_TYPE", 
                "Unsupported question type: " + request.getQuestionType());
        }
        
        // Validate dataset source
        if (!SUPPORTED_DATASET_SOURCES.contains(request.getDatasetSource())) {
            return ValidationResult.failure("INVALID_FILTERS", 
                "Unsupported dataset source: " + request.getDatasetSource());
        }
        
        // Validate question type and dataset source compatibility
        ValidationResult compatibilityResult = validateCompatibility(request);
        if (!compatibilityResult.isValid()) {
            return compatibilityResult;
        }
        
        // Validate filters
        return validateFilters(request);
    }
    
    private ValidationResult validateCompatibility(ExplanationRequest request) {
        String questionType = request.getQuestionType();
        String datasetSource = request.getDatasetSource();
        
        // MBIE question types must use MBIE dataset sources
        if (isMbieQuestionType(questionType) && !datasetSource.startsWith("mbie.generation.")) {
            return ValidationResult.failure("INVALID_FILTERS", 
                "MBIE question types require MBIE dataset sources");
        }
        
        // LAWA question types must use LAWA dataset sources
        if (isLawaQuestionType(questionType) && !datasetSource.startsWith("lawa.water_quality.")) {
            return ValidationResult.failure("INVALID_FILTERS", 
                "LAWA question types require LAWA dataset sources");
        }
        
        // LAWA state question types must use state dataset source
        if (isLawaStateQuestionType(questionType) && !datasetSource.contains(".state.")) {
            return ValidationResult.failure("INVALID_FILTERS", 
                "LAWA state question types require state dataset source");
        }
        
        // LAWA trend question types must use trend dataset source
        if (isLawaTrendQuestionType(questionType) && !datasetSource.contains(".trend.")) {
            return ValidationResult.failure("INVALID_FILTERS", 
                "LAWA trend question types require trend dataset source");
        }
        
        return ValidationResult.success();
    }
    
    private ValidationResult validateFilters(ExplanationRequest request) {
        Map<String, Object> filters = request.getFilters();
        
        if (filters == null) {
            return ValidationResult.success();
        }
        
        // Check for unknown filter keys
        for (String key : filters.keySet()) {
            if (!ALLOWED_FILTER_KEYS.contains(key)) {
                return ValidationResult.failure("INVALID_FILTERS", 
                    "Unknown filter key: " + key);
            }
        }
        
        // Validate year filters
        Integer startYear = (Integer) filters.get("startYear");
        Integer endYear = (Integer) filters.get("endYear");
        
        if (startYear != null && endYear != null && startYear > endYear) {
            return ValidationResult.failure("INVALID_FILTERS", 
                "startYear must be less than or equal to endYear");
        }
        
        // Validate filter types
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if ("startYear".equals(key) || "endYear".equals(key)) {
                if (!(value instanceof Integer)) {
                    return ValidationResult.failure("INVALID_FILTERS", 
                        key + " must be an integer");
                }
            } else if ("fuelType".equals(key) || "indicator".equals(key) || 
                      "region".equals(key) || "trend".equals(key)) {
                if (!(value instanceof String)) {
                    return ValidationResult.failure("INVALID_FILTERS", 
                        key + " must be a string");
                }
            }
        }
        
        return ValidationResult.success();
    }
    
    private boolean isMbieQuestionType(String questionType) {
        return questionType.equals("renewable_generation_trend") ||
               questionType.equals("hydro_generation_trend") ||
               questionType.equals("fuel_type_comparison");
    }
    
    private boolean isLawaQuestionType(String questionType) {
        return questionType.equals("water_quality_overview") ||
               questionType.equals("excellent_sites_trend") ||
               questionType.equals("regional_water_quality") ||
               questionType.equals("water_quality_trends") ||
               questionType.equals("improving_sites_trend") ||
               questionType.equals("regional_trend_comparison");
    }
    
    private boolean isLawaStateQuestionType(String questionType) {
        return questionType.equals("water_quality_overview") ||
               questionType.equals("excellent_sites_trend") ||
               questionType.equals("regional_water_quality");
    }
    
    private boolean isLawaTrendQuestionType(String questionType) {
        return questionType.equals("water_quality_trends") ||
               questionType.equals("improving_sites_trend") ||
               questionType.equals("regional_trend_comparison");
    }
    
    /**
     * Result of validation with optional refusal details.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String refusalCategory;
        private final String refusalMessage;
        
        private ValidationResult(boolean valid, String refusalCategory, String refusalMessage) {
            this.valid = valid;
            this.refusalCategory = refusalCategory;
            this.refusalMessage = refusalMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }
        
        public static ValidationResult failure(String category, String message) {
            return new ValidationResult(false, category, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getRefusalCategory() {
            return refusalCategory;
        }
        
        public String getRefusalMessage() {
            return refusalMessage;
        }
    }
}