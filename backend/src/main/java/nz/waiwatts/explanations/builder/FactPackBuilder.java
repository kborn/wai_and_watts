package nz.waiwatts.explanations.builder;

import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.dto.ExplanationRequest;

/**
 * Interface for building Fact Packs from database data.
 * 
 * Fact Pack Builders are dataset-specific, deterministic, and must only query the database.
 * They must not call LLMs or contain explanation prose.
 * 
 * Ensures stable ordering and deterministic outputs for testing.
 */
public interface FactPackBuilder {
    
    /**
     * Builds a Fact Pack for the given request context.
     * 
     * @param request the explanation request
     * @return a deterministic Fact Pack containing the relevant facts
     */
    FactPack buildFactPack(ExplanationRequest request);
    
    /**
     * Determines if this builder can handle the given request.
     * 
     * @param request the explanation request
     * @return true if this builder supports the request
     */
    boolean canHandle(ExplanationRequest request);
    
    /**
     * Gets the dataset source code this builder supports.
     * 
     * @return the dataset source code (e.g., "mbie.generation.annual")
     */
    String getSupportedDatasetSourceCode();
}