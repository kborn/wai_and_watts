package nz.waiwatts.explanations.builder;

import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.dto.ExplanationRequest;

/**
 * Interface for building Fact Packs from database data.
 * <p>
 * Fact Pack Builders are dataset-specific, deterministic, and must only query the database.
 * They must not call LLMs or contain explanation prose.
 * For ask flows, builders must pin records to one canonical dataset_release before
 * constructing facts/citations to keep provenance and outputs deterministic.
 * <p>
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
     * Gets the dataset source code this builder supports.
     * 
     * @return the dataset source code (e.g., "mbie.generation.annual")
     */
    String getSupportedDatasetSourceCode();
}
