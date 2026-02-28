package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for validating ExplanationRequest against capability contracts.
 * <p>
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

            if (FilterKey.START_YEAR.wireValue().equals(key) || FilterKey.END_YEAR.wireValue().equals(key)) {
                if (!(value instanceof Integer)) {
                    return ValidationResult.failure("VALIDATION_FAILED",
                        key + " must be an integer");
                }
            } else if (FilterKey.FUEL_TYPE.wireValue().equals(key)
                    || FilterKey.FUEL_TYPE_B.wireValue().equals(key)
                    || FilterKey.INDICATOR.wireValue().equals(key)
                    || FilterKey.REGION.wireValue().equals(key)
                    || FilterKey.TREND.wireValue().equals(key)
                    || FilterKey.STATE_CATEGORY.wireValue().equals(key)) {
                if (!(value instanceof String)) {
                    return ValidationResult.failure("VALIDATION_FAILED",
                        key + " must be a string");
                }
            } else if (FilterKey.METRIC_TYPE.wireValue().equals(key)) {
                if (!(value instanceof String)) {
                    return ValidationResult.failure("VALIDATION_FAILED",
                        "metricType must be a string");
                }
            }
        }

        // Validate year filters after type checks to avoid ClassCastException
        Integer startYear = (Integer) filters.get(FilterKey.START_YEAR.wireValue());
        Integer endYear = (Integer) filters.get(FilterKey.END_YEAR.wireValue());
        if (startYear != null && endYear != null && startYear > endYear) {
            return ValidationResult.failure("VALIDATION_FAILED",
                "startYear must be less than or equal to endYear");
        }

        String metricType = filters.get(FilterKey.METRIC_TYPE.wireValue()) instanceof String s ? s.trim() : null;
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
        if (QuestionType.FUEL_TYPE_COMPARISON.wireValue().equals(request.getQuestionType())) {
            String fuelA = filters.get(FilterKey.FUEL_TYPE.wireValue()) instanceof String s ? s.trim() : null;
            String fuelB = filters.get(FilterKey.FUEL_TYPE_B.wireValue()) instanceof String s ? s.trim() : null;
            if (fuelA == null || fuelA.isEmpty() || fuelB == null || fuelB.isEmpty()) {
                return ValidationResult.failure("MISSING_REQUIRED_FILTERS",
                    "fuel_type_comparison requires fuelType and fuelTypeB");
            }
            if (fuelA.equalsIgnoreCase(fuelB)) {
                return ValidationResult.failure("VALIDATION_FAILED",
                    "fuelType and fuelTypeB must be different");
            }
        }

        if (QuestionType.FUEL_GENERATION_TREND.wireValue().equals(request.getQuestionType())) {
            String fuelType = filters.get(FilterKey.FUEL_TYPE.wireValue()) instanceof String s ? s.trim() : null;
            if (fuelType == null || fuelType.isEmpty()) {
                return ValidationResult.failure("MISSING_REQUIRED_FILTERS",
                    "fuel_generation_trend requires fuelType");
            }
        }

        if (QuestionType.WATER_QUALITY_STATE_SITES_TREND.wireValue().equals(request.getQuestionType())) {
            String stateCategory = filters.get(FilterKey.STATE_CATEGORY.wireValue()) instanceof String s ? s.trim() : null;
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
        QuestionType parsedQuestionType = QuestionType.fromWireValue(questionType).orElse(null);

        if (!capabilityRegistry.isDatasetSupportedForQuestion(questionType, datasetSource)) {
            var supportedSources = capabilityRegistry.supportedDatasetSourcesForQuestion(questionType);
            if (isLawaQuestionType(parsedQuestionType)
                    && isMbieDataset(datasetSource)) {
                return ValidationResult.failure("DATASET_MISMATCH",
                        "Parsed a LAWA water quality question, but selected an MBIE dataset.");
            }
            if (isMbieQuestionType(parsedQuestionType)
                    && isLawaDataset(datasetSource)) {
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

    private boolean isMbieQuestionType(QuestionType questionType) {
        return questionType == QuestionType.RENEWABLE_GENERATION_TREND
            || questionType == QuestionType.FUEL_GENERATION_TREND
            || questionType == QuestionType.FUEL_TYPE_COMPARISON
            || questionType == QuestionType.GENERATION_MIX_OVERVIEW;
    }

    private boolean isLawaQuestionType(QuestionType questionType) {
        return questionType == QuestionType.WATER_QUALITY_OVERVIEW
            || questionType == QuestionType.WATER_QUALITY_STATE_SITES_TREND
            || questionType == QuestionType.REGIONAL_WATER_QUALITY
            || questionType == QuestionType.WATER_QUALITY_TRENDS
            || questionType == QuestionType.IMPROVING_SITES_TREND
            || questionType == QuestionType.REGIONAL_TREND_COMPARISON;
    }

    private boolean isMbieDataset(String datasetSource) {
        return datasetSource != null
            && (datasetSource.equals(DatasetSource.MBIE_GENERATION_ANNUAL.wireValue())
            || datasetSource.equals(DatasetSource.MBIE_GENERATION_QUARTERLY.wireValue()));
    }

    private boolean isLawaDataset(String datasetSource) {
        return datasetSource != null
            && (datasetSource.equals(DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue())
            || datasetSource.equals(DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue()));
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
