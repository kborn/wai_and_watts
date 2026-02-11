package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.parser.IntentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of IntentParserService using injected IntentParser strategy.
 * 
 * Follows Phase 12 NL intent parsing contract:
 * - Maps NL to structured ExplanationRequest via injected parser strategy
 * - Returns deterministic refusals for ambiguous/unsupported inputs
 * - Never generates facts or accesses database
 */
@Service
public class IntentParserServiceImpl implements IntentParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntentParserServiceImpl.class);
    
    private final IntentParser intentParser;
    
    public IntentParserServiceImpl(IntentParser intentParser) {
        this.intentParser = intentParser;
    }
    
    @Override
    public IntentParseResponse parseQuestion(String question) {
        logger.info("Parsing natural language question: {}", question);
        
        try {
            // Delegate to injected parser strategy (currently StubIntentParser)
            ExplanationRequest request = intentParser.parseQuestion(question);
            
            if (request == null) {
                logger.warn("Unable to parse question: {}", question);
                return IntentParseResponse.refusal("AMBIGUOUS_INTENT", 
                    "I can't confidently map this question to a supported explanation type. Try specifying the dataset and a time range.");
            }
            
            logger.info("Successfully parsed question to request: questionType={}, datasetSource={}, filters={}", 
                request.getQuestionType(), request.getDatasetSource(), request.getFilters());
            
            return IntentParseResponse.success(request);
            
        } catch (Exception e) {
            logger.error("Error parsing question: {}", question, e);
            return IntentParseResponse.refusal("AMBIGUOUS_INTENT", 
                "I encountered an error while parsing your question. Please try rephrasing it.");
        }
    }
}