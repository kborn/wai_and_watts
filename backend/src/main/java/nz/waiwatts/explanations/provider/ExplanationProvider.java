package nz.waiwatts.explanations.provider;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;

/**
 * Interface for LLM providers that generate explanations from Fact Packs.
 * 
 * LLM Provider Adapters are responsible for:
 * - Serializing Fact Pack + instructions to provider format
 * - Calling the provider
 * - Returning structured response
 * 
 * Must not query DB or modify facts.
 * Validates citation presence for grounding enforcement.
 */
public interface ExplanationProvider {
    
    /**
     * Generates an explanation from a Fact Pack.
     * 
     * @param question the user's question (derived from question_type)
     * @param factPack the Fact Pack containing relevant facts
     * @return an Explanation with citations or a refusal
     */
    Explanation generateExplanation(String question, FactPack factPack);
    
    /**
     * Validates that the explanation includes required citations.
     * 
     * @param explanation the generated explanation
     * @param factPack the original Fact Pack
     * @return true if required citations are present
     */
    boolean validateCitations(Explanation explanation, FactPack factPack);
}