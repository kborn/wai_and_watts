package nz.waiwatts.explanations.parser;

import nz.waiwatts.explanations.dto.ExplanationRequest;

/**
 * Interface for different intent parsing strategies.
 * 
 * Allows swapping between rule-based parsing (current) and LLM-based parsing (future).
 * All implementations must return the same structured ExplanationRequest format.
 */
public interface IntentParser {
    
    /**
     * Parses a natural language question into a structured ExplanationRequest.
     * 
     * @param question natural language question from user
     * @return ExplanationRequest or null if unable to parse
     */
    ExplanationRequest parseQuestion(String question);
}