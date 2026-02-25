package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for validating ExplanationRequest against capability contracts.
 * 
 * Enforces:
 * - Supported question types
 * - Supported dataset sources  
 * - Filter schema validation
 * - Cross-field and metric-type constraint validation
 */
@Service
public class RequestValidationService {

    private final CapabilityRegistry capabilityRegistry;

    public RequestValidationService(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    // Test-only fallback constructor.
    RequestValidationService() {
        this(new CapabilityRegistry(new DatasetCatalog()));
    }
    
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
        if (!capabilityRegistry.isSupportedQuestionType(request.getQuestionType())) {
            return ValidationResult.failure("UNSUPPORTED_QUESTION_TYPE", 
                "Unsupported question type: " + request.getQuestionType());
        }
        
        // Validate dataset source
        if (!capabilityRegistry.isSupportedDatasetSource(request.getDatasetSource())) {
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
    
    private ValidationResult validateFilters(ExplanationRequest request) {
        Map<String, Object> filters = request.getFilters();

        if (filters == null) {
            return ValidationResult.success();
        }

        String questionType = request.getQuestionType();

        // Check for unknown filter keys
        for (String key : filters.keySet()) {
            if (!capabilityRegistry.getAllowedFilterKeys().contains(key)) {
                return ValidationResult.failure("VALIDATION_FAILED",
                    "Unknown filter key: " + key);
            }
            if (!capabilityRegistry.isFilterSupportedForQuestion(questionType, key)) {
                return ValidationResult.failure("UNSUPPORTED_CAPABILITY",
                    "Question type " + questionType + " does not support filter: " + key);
            }
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
            } else if ("fuelType".equals(key) || "fuelTypeB".equals(key) || "indicator".equals(key)
                    || "region".equals(key) || "trend".equals(key) || "stateCategory".equals(key)) {
                if (!(value instanceof String)) {
                    return ValidationResult.failure("VALIDATION_FAILED",
                        key + " must be a string");
                }
            } else if ("metricType".equals(key)) {
                if (!(value instanceof String)) {
                    return ValidationResult.failure("VALIDATION_FAILED",
                        "metricType must be a string");
                }
            }
        }

        // Validate year filters after type checks to avoid ClassCastException
        Integer startYear = (Integer) filters.get("startYear");
        Integer endYear = (Integer) filters.get("endYear");
        if (startYear != null && endYear != null && startYear > endYear) {
            return ValidationResult.failure("VALIDATION_FAILED",
                "startYear must be less than or equal to endYear");
        }

        String metricType = filters.get("metricType") instanceof String s ? s.trim() : null;
        if (metricType != null && !metricType.isBlank()
            && !capabilityRegistry.isMetricTypeSupportedForQuestionAndDataset(
                questionType,
                request.getDatasetSource(),
                metricType
            )) {
            return ValidationResult.failure(
                "UNSUPPORTED_CAPABILITY",
                "Question type " + questionType
                    + " with dataset " + request.getDatasetSource()
                    + " does not support metricType: " + metricType
            );
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

        if ("fuel_generation_trend".equals(request.getQuestionType())) {
            String fuelType = filters.get("fuelType") instanceof String s ? s.trim() : null;
            if (fuelType == null || fuelType.isEmpty()) {
                return ValidationResult.failure("MISSING_REQUIRED_FILTERS",
                    "fuel_generation_trend requires fuelType");
            }
        }

        if ("water_quality_state_sites_trend".equals(request.getQuestionType())) {
            String stateCategory = filters.get("stateCategory") instanceof String s ? s.trim() : null;
            if (stateCategory == null || stateCategory.isEmpty()) {
                return ValidationResult.failure("MISSING_REQUIRED_FILTERS",
                    "water_quality_state_sites_trend requires stateCategory");
            }
        }

        return ValidationResult.success();
    }

    private ValidationResult validateCompatibility(ExplanationRequest request) {
        String questionType = request.getQuestionType();
        String datasetSource = request.getDatasetSource();

        if (!capabilityRegistry.isDatasetSupportedForQuestion(questionType, datasetSource)) {
            var supportedSources = capabilityRegistry.supportedDatasetSourcesForQuestion(questionType);
            if (questionType != null && questionType.startsWith("water_quality_")
                    && datasetSource != null && datasetSource.startsWith("mbie.generation.")) {
                return ValidationResult.failure("DATASET_MISMATCH",
                        "Parsed a LAWA water quality question, but selected an MBIE dataset.");
            }
            if (questionType != null
                    && (questionType.contains("generation") || questionType.contains("fuel_type"))
                    && datasetSource != null && datasetSource.startsWith("lawa.water_quality.")) {
                return ValidationResult.failure("DATASET_MISMATCH",
                        "Parsed an MBIE generation question, but selected a LAWA dataset.");
            }
            if (!supportedSources.isEmpty()
                    && supportedSources.stream().allMatch(source -> source.contains(".state."))
                    && datasetSource != null && datasetSource.contains(".trend.")) {
                return ValidationResult.failure("DATASET_MISMATCH",
                        "Parsed a LAWA state question, but selected a trend dataset.");
            }
            if (!supportedSources.isEmpty()
                    && supportedSources.stream().allMatch(source -> source.contains(".trend."))
                    && datasetSource != null && datasetSource.contains(".state.")) {
                return ValidationResult.failure("DATASET_MISMATCH",
                        "Parsed a LAWA trend question, but selected a state dataset.");
            }
            return ValidationResult.failure("DATASET_MISMATCH",
                    "Question type and dataset source are not a supported combination.");
        }

        return ValidationResult.success();
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
