package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dto.IntentParseResponse;

/**
 * Service for parsing natural language questions into structured ExplanationRequests.
 * 
 * Follows Phase 12 contract:
 * - Converts NL → validated ExplanationRequest
 * - Returns deterministic refusals for ambiguous/unsupported inputs
 * - Never generates facts or accesses database
 */
public interface IntentParserService {
    
    /**
     * Parses a natural language question into a structured ExplanationRequest.
     * 
     * @param question natural language question from user
     * @return IntentParseResponse with either parsed request or refusal
     */
    IntentParseResponse parseQuestion(String question);
}