package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dataset.DatasetDescriptor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.LinkedHashSet;

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
    private final DatasetCatalog datasetCatalog;
    private final QuestionTypeCatalog questionTypeCatalog;
    private final Set<String> allowedFilterKeys;

    public RequestValidationService(DatasetCatalog datasetCatalog, QuestionTypeCatalog questionTypeCatalog) {
        this.datasetCatalog = datasetCatalog;
        this.questionTypeCatalog = questionTypeCatalog;
        this.allowedFilterKeys = datasetCatalog.getDatasets().stream()
            .flatMap(ds -> ds.supportedFilters().stream())
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
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
        if (!questionTypeCatalog.isSupported(request.getQuestionType())) {
            return ValidationResult.failure("UNSUPPORTED_QUESTION_TYPE", 
                "Unsupported question type: " + request.getQuestionType());
        }
        
        // Validate dataset source
        Optional<DatasetDescriptor> descriptor = datasetCatalog.findBySource(request.getDatasetSource());
        if (descriptor.isEmpty()) {
            return ValidationResult.failure("VALIDATION_FAILED", 
                "Unsupported dataset source: " + request.getDatasetSource());
        }
        
        // Validate question type and dataset source compatibility
        ValidationResult compatibilityResult = validateCompatibility(request, descriptor.get());
        if (!compatibilityResult.isValid()) {
            return compatibilityResult;
        }
        
        // Validate filters
        return validateFilters(request);
    }
    
    private ValidationResult validateCompatibility(ExplanationRequest request, DatasetDescriptor descriptor) {
        String questionType = request.getQuestionType();
        String datasetSource = descriptor.datasetSource();
        QuestionTypeCatalog.QuestionTypeGroup group = questionTypeCatalog.groupFor(questionType);
        
        // MBIE question types must use MBIE dataset sources
        if (group == QuestionTypeCatalog.QuestionTypeGroup.MBIE && !datasetSource.startsWith("mbie.generation.")) {
            return ValidationResult.failure("DATASET_MISMATCH", 
                "Parsed an MBIE generation question, but selected a LAWA dataset.");
        }
        
        // LAWA question types must use LAWA dataset sources
        if ((group == QuestionTypeCatalog.QuestionTypeGroup.LAWA_STATE || group == QuestionTypeCatalog.QuestionTypeGroup.LAWA_TREND)
            && !datasetSource.startsWith("lawa.water_quality.")) {
            return ValidationResult.failure("DATASET_MISMATCH", 
                "Parsed a LAWA water quality question, but selected an MBIE dataset.");
        }
        
        // LAWA state question types must use state dataset source
        if (group == QuestionTypeCatalog.QuestionTypeGroup.LAWA_STATE && !datasetSource.contains(".state.")) {
            return ValidationResult.failure("DATASET_MISMATCH", 
                "Parsed a LAWA state question, but selected a trend dataset.");
        }
        
        // LAWA trend question types must use trend dataset source
        if (group == QuestionTypeCatalog.QuestionTypeGroup.LAWA_TREND && !datasetSource.contains(".trend.")) {
            return ValidationResult.failure("DATASET_MISMATCH", 
                "Parsed a LAWA trend question, but selected a state dataset.");
        }

        if (!descriptor.supportedQuestionTypes().contains(questionType)) {
            return ValidationResult.failure("DATASET_MISMATCH",
                "Dataset " + datasetSource + " does not support question type: " + questionType);
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
            if (!allowedFilterKeys.contains(key)) {
                return ValidationResult.failure("VALIDATION_FAILED", 
                    "Unknown filter key: " + key);
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
            } else if ("fuelType".equals(key) || "fuelTypeB".equals(key) || "indicator".equals(key) || 
                      "region".equals(key) || "trend".equals(key)) {
                if (!(value instanceof String)) {
                    return ValidationResult.failure("VALIDATION_FAILED", 
                        key + " must be a string");
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
