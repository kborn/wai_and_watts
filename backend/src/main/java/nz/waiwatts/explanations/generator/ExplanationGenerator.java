package nz.waiwatts.explanations.generator;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;

/**
 * Interface for components that generate explanations from Fact Packs.
 * <p>
 * Explanation generators are responsible for:
 * - Serializing Fact Pack + instructions to generator-specific format
 * - Calling an external LLM/API when needed
 * - Returning structured response
 * <p>
 * Must not query DB or modify facts.
 * Citation validation must follow the shared citation-validation layer rules
 * so stub and live generators cannot diverge.
 */
public interface ExplanationGenerator {
    
    /**
     * Generates an explanation from a Fact Pack.
     * <p>
     * Routing must be based on structured context (e.g., questionType/promptKey), not freeform text.
     *
     * @param questionType the structured question type (derived from request.questionType)
     * @param factPack the Fact Pack containing relevant facts
     * @return an Explanation with citations or a refusal
     */
    Explanation generateExplanation(String questionType, FactPack factPack);
    
    /**
     * Validates that the explanation includes required citations.
     * 
     * @param explanation the generated explanation
     * @param factPack the original Fact Pack
     * @return true if required citations are present
     */
    boolean validateCitations(Explanation explanation, FactPack factPack);
}
