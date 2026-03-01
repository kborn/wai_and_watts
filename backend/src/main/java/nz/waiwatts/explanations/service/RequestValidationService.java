package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import org.springframework.stereotype.Service;

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

    private final ContractValidator contractValidator;

    public RequestValidationService(ContractValidator contractValidator) {
        this.contractValidator = contractValidator;
    }

    // Test-only fallback constructor.
    RequestValidationService() {
        this(new ContractValidator(new nz.waiwatts.explanations.capabilities.CapabilityRegistry(new DatasetCatalog())));
    }
    
    /**
     * Validates an ExplanationRequest according to Phase 12 contract.
     * 
     * @param request the request to validate
     * @return ValidationResult with either success or refusal details
     */
    public ValidationResult validateRequest(ExplanationRequest request) {
        ContractValidator.Result result = contractValidator.validate(request);
        if (result.valid()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(result.refusalCategory(), result.refusalMessage());
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
