package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.config.LlmProvider;
import nz.waiwatts.explanations.config.LlmProperties;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.parser.IntentParser;
import nz.waiwatts.explanations.parser.DemoIntentParser;
import nz.waiwatts.explanations.parser.UnsupportedIntentDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of IntentParserService using injected IntentParser strategy.
 * <p>
 * Follows Phase 12 NL intent parsing contract:
 * - Maps NL to structured ExplanationRequest via injected parser strategy
 * - Returns deterministic refusals for ambiguous/unsupported inputs
 * - Never generates facts or accesses database
 */
@Service
public class IntentParserServiceImpl implements IntentParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntentParserServiceImpl.class);
    
    private final IntentParser intentParser;
    private final LlmProperties llmProperties;
    private final UnsupportedIntentDetector unsupportedIntentDetector;
    private final ExplanationRequestNormalizer requestNormalizer;
    private final DemoIntentParser demoIntentParser = new DemoIntentParser();
    
    public IntentParserServiceImpl(
        IntentParser intentParser,
        LlmProperties llmProperties,
        UnsupportedIntentDetector unsupportedIntentDetector,
        ExplanationRequestNormalizer requestNormalizer
    ) {
        this.intentParser = intentParser;
        this.llmProperties = llmProperties;
        this.unsupportedIntentDetector = unsupportedIntentDetector;
        this.requestNormalizer = requestNormalizer;
    }
    
    @Override
    public IntentParseResponse parseQuestion(String question) {
        logger.info("Parsing natural language question: {}", question);


        if (!isLlmEnabled()) {
            ExplanationRequest request = requestNormalizer.normalize(demoIntentParser.parseQuestion(question));
            if (request == null) {
                IntentParseResponse response = IntentParseResponse.refusal(
                    "LLM_REQUIRED",
                    "Configure LLM API key to enable natural language questions."
                );
                response.setParserUsed("DEMO");
                return response;
            }
            logger.info("Demo intent parser matched sample question: questionType={}, datasetSource={}, filters={}",
                request.getQuestionType(), request.getDatasetSource(), request.getFilters());
            IntentParseResponse response = IntentParseResponse.success(request);
            response.setParserUsed("DEMO");
            return response;
        }

        if (unsupportedIntentDetector.isDerivedAnalyticsUnsupported(question)) {
            IntentParseResponse response = IntentParseResponse.refusal(
                "UNSUPPORTED_CAPABILITY",
                "That requires derived analytics (ranking/argmax/share thresholds), which is not supported in this phase."
            );
            response.setParserUsed("LLM");
            return response;
        }

        if (unsupportedIntentDetector.isUnsupported(question)) {
            IntentParseResponse response = IntentParseResponse.refusal(
                "UNSUPPORTED_INTENT",
                "Wai & Watts explains what published data shows, not why or predictions."
            );
            response.setParserUsed("LLM");
            return response;
        }

        try {
            ExplanationRequest request = intentParser.parseQuestion(question);
            if (request == null) {
                logger.warn("LLM intent parser returned null for question: {}", question);
                IntentParseResponse response = IntentParseResponse.refusal(
                    "UNABLE_TO_PARSE",
                    "I can't confidently map this question to a supported explanation type."
                );
                response.setParserUsed("LLM");
                return response;
            }

            request = requestNormalizer.normalize(request);

            logger.info("Successfully parsed question to request: questionType={}, datasetSource={}, filters={}",
                request.getQuestionType(), request.getDatasetSource(), request.getFilters());

            IntentParseResponse response = IntentParseResponse.success(request);
            response.setParserUsed("LLM");
            return response;
        } catch (Exception e) {
            logger.warn("LLM intent parser failed; attempting demo fallback", e);
            ExplanationRequest request = requestNormalizer.normalize(demoIntentParser.parseQuestion(question));
            if (request == null) {
                IntentParseResponse response = IntentParseResponse.refusal(
                    "UNABLE_TO_PARSE",
                    "LLM was unavailable and the demo parser only supports the sample questions."
                );
                response.setParserUsed("LLM_FALLBACK_DEMO");
                return response;
            }

            IntentParseResponse response = IntentParseResponse.success(request);
            response.setParserUsed("LLM_FALLBACK_DEMO");
            return response;
        }
    }

    private boolean isLlmEnabled() {
        return llmProperties != null
            && llmProperties.isConfigured()
            && llmProperties.getProvider() == LlmProvider.OPENAI;
    }

}
