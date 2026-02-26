package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;

/**
 * Service for generating explanations from environmental data.
 * <p>
 * The Explanation Service:
 * - Accepts explanation requests with structured question typing
 * - Selects appropriate Fact Pack Builder
 * - Generates Fact Pack
 * - Calls LLM Provider Adapter
 * - Validates citation presence
 * <p>
 * Must not query DB directly or contain dataset-specific logic.
 * Enforces refusal behavior for unsupported question types.
 */
public interface ExplanationService {
    
    /**
     * Generates an explanation for the given request.
     * 
     * @param request the explanation request with question_type and filters
     * @return an Explanation with citations or a refusal
     */
    Explanation generateExplanation(ExplanationRequest request);

    /**
     * Builds and returns the full FactPack for debugging purposes.
     * 
     * @param request the explanation request with question_type and filters
     * @return the full FactPack JSON for verification/debugging
     */
    Object buildFactPack(ExplanationRequest request);
}