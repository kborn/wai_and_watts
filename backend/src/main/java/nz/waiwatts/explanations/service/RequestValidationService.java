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
        "generation_mix_overview",
        
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
        "fuelType", "fuelTypeB", "indicator", "region", "trend", "startYear", "endYear"
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
            return ValidationResult.failure("VALIDATION_FAILED", "questionType is required");
        }
        
        if (request.getDatasetSource() == null || request.getDatasetSource().trim().isEmpty()) {
            return ValidationResult.failure("VALIDATION_FAILED", "datasetSource is required");
        }
        
        // Validate question type
        if (!SUPPORTED_QUESTION_TYPES.contains(request.getQuestionType())) {
            return ValidationResult.failure("UNSUPPORTED_QUESTION_TYPE", 
                "Unsupported question type: " + request.getQuestionType());
        }
        
        // Validate dataset source
        if (!SUPPORTED_DATASET_SOURCES.contains(request.getDatasetSource())) {
            return ValidationResult.failure("VALIDATION_FAILED", 
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
            return ValidationResult.failure("DATASET_MISMATCH", 
                "Parsed an MBIE generation question, but selected a LAWA dataset.");
        }
        
        // LAWA question types must use LAWA dataset sources
        if (isLawaQuestionType(questionType) && !datasetSource.startsWith("lawa.water_quality.")) {
            return ValidationResult.failure("DATASET_MISMATCH", 
                "Parsed a LAWA water quality question, but selected an MBIE dataset.");
        }
        
        // LAWA state question types must use state dataset source
        if (isLawaStateQuestionType(questionType) && !datasetSource.contains(".state.")) {
            return ValidationResult.failure("DATASET_MISMATCH", 
                "Parsed a LAWA state question, but selected a trend dataset.");
        }
        
        // LAWA trend question types must use trend dataset source
        if (isLawaTrendQuestionType(questionType) && !datasetSource.contains(".trend.")) {
            return ValidationResult.failure("DATASET_MISMATCH", 
                "Parsed a LAWA trend question, but selected a state dataset.");
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
                return ValidationResult.failure("VALIDATION_FAILED", 
                    "Unknown filter key: " + key);
            }
        }
        
        // Validate year filters
        Integer startYear = (Integer) filters.get("startYear");
        Integer endYear = (Integer) filters.get("endYear");
        
        if (startYear != null && endYear != null && startYear > endYear) {
            return ValidationResult.failure("VALIDATION_FAILED", 
                "startYear must be less than or equal to endYear");
        }
        
        // Validate filter types
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if ("startYear".equals(key) || "endYear".equals(key)) {
                if (!(value instanceof Integer)) {
                    return ValidationResult.failure("VALIDATION_FAILED", 
                        key + " must be an integer");
                }
            } else if ("fuelType".equals(key) || "fuelTypeB".equals(key) || "indicator".equals(key) || 
                      "region".equals(key) || "trend".equals(key)) {
                if (!(value instanceof String)) {
                    return ValidationResult.failure("VALIDATION_FAILED", 
                        key + " must be a string");
                }
            }
        }

        // Question-type specific filter requirements
        if ("fuel_type_comparison".equals(request.getQuestionType())) {
            String fuelA = filters.get("fuelType") instanceof String s ? s.trim() : null;
            String fuelB = filters.get("fuelTypeB") instanceof String s ? s.trim() : null;
            if (fuelA == null || fuelA.isEmpty() || fuelB == null || fuelB.isEmpty()) {
                return ValidationResult.failure("MISSING_REQUIRED_FILTERS",
                    "fuel_type_comparison requires fuelType and fuelTypeB");
            }
            if (fuelA.equalsIgnoreCase(fuelB)) {
                return ValidationResult.failure("VALIDATION_FAILED",
                    "fuelType and fuelTypeB must be different");
            }
        }
        
        return ValidationResult.success();
    }
    
    private boolean isMbieQuestionType(String questionType) {
        return questionType.equals("renewable_generation_trend") ||
               questionType.equals("hydro_generation_trend") ||
               questionType.equals("fuel_type_comparison") ||
               questionType.equals("generation_mix_overview");
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
