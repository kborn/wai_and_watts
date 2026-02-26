package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.config.LlmProvider;
import nz.waiwatts.explanations.config.LlmProperties;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.parser.IntentParser;
import nz.waiwatts.explanations.parser.HardcodedDemoIntentParser;
import nz.waiwatts.explanations.parser.UnsupportedIntentDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
    private static final Set<String> NULLISH_TOKENS = Set.of("unknown", "null");
    private static final Set<String> NULLABLE_CATEGORICAL_FILTERS = Set.of(
        FilterKey.FUEL_TYPE.wireValue(),
        FilterKey.FUEL_TYPE_B.wireValue(),
        FilterKey.INDICATOR.wireValue(),
        FilterKey.STATE_CATEGORY.wireValue(),
        FilterKey.REGION.wireValue(),
        FilterKey.TREND.wireValue(),
        FilterKey.METRIC_TYPE.wireValue()
    );
    
    private final IntentParser intentParser;
    private final LlmProperties llmProperties;
    private final UnsupportedIntentDetector unsupportedIntentDetector;
    private final HardcodedDemoIntentParser demoIntentParser = new HardcodedDemoIntentParser();
    
    public IntentParserServiceImpl(
        IntentParser intentParser,
        LlmProperties llmProperties,
        UnsupportedIntentDetector unsupportedIntentDetector
    ) {
        this.intentParser = intentParser;
        this.llmProperties = llmProperties;
        this.unsupportedIntentDetector = unsupportedIntentDetector;
    }
    
    @Override
    public IntentParseResponse parseQuestion(String question) {
        logger.info("Parsing natural language question: {}", question);


        if (!isLlmEnabled()) {
            ExplanationRequest request = demoIntentParser.parseQuestion(question);
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

            request = normalizeParsedRequest(request);

            logger.info("Successfully parsed question to request: questionType={}, datasetSource={}, filters={}",
                request.getQuestionType(), request.getDatasetSource(), request.getFilters());

            IntentParseResponse response = IntentParseResponse.success(request);
            response.setParserUsed("LLM");
            return response;
        } catch (Exception e) {
            logger.warn("LLM intent parser failed; attempting demo fallback", e);
            ExplanationRequest request = demoIntentParser.parseQuestion(question);
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


    /**
     * Deterministic normalization layer to keep parsed requests consistent with builders.
     *
     * Key rule: if the parser produces two fuels (fuelType + fuelTypeB), the request MUST be
     * a fuel_type_comparison so the fact pack builder includes both time series.
     *
     * This prevents "missing geothermal in fact pack" refusals when the LLM chooses a single-fuel
     * questionType but still extracts fuelTypeB.
     */
    private ExplanationRequest normalizeParsedRequest(ExplanationRequest request) {
        if (request == null) return null;
        // Fix LAWA state/trend dataset mismatches deterministically.
        String qt = request.getQuestionType();
        String ds = request.getDatasetSource();
        if (qt != null && ds != null) {
            boolean isLawaStateQt = qt.equals(QuestionType.WATER_QUALITY_OVERVIEW.wireValue())
                    || qt.equals(QuestionType.WATER_QUALITY_STATE_SITES_TREND.wireValue())
                    || qt.equals(QuestionType.REGIONAL_WATER_QUALITY.wireValue());
            boolean isLawaTrendQt = qt.equals(QuestionType.WATER_QUALITY_TRENDS.wireValue())
                    || qt.equals(QuestionType.IMPROVING_SITES_TREND.wireValue())
                    || qt.equals(QuestionType.REGIONAL_TREND_COMPARISON.wireValue());

            if (isLawaStateQt && ds.equals(DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue())) {
                return new ExplanationRequest(qt, DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue(), request.getFilters());
            }
            if (isLawaTrendQt && ds.equals(DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue())) {
                return new ExplanationRequest(qt, DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue(), request.getFilters());
            }
        }
        Map<String, Object> filters = request.getFilters();
        if (filters == null) return request;

        removeNullishCategoricalFilters(filters);

        Object ftA = filters.get(FilterKey.FUEL_TYPE.wireValue());
        Object ftB = filters.get(FilterKey.FUEL_TYPE_B.wireValue());
        boolean hasA = ftA instanceof String s && !s.isBlank() && !"null".equalsIgnoreCase(s);
        boolean hasB = ftB instanceof String s && !s.isBlank() && !"null".equalsIgnoreCase(s);

        if (hasA && hasB && !QuestionType.FUEL_TYPE_COMPARISON.wireValue().equals(request.getQuestionType())) {
            logger.info("Normalizing parsed request: fuelTypeB present -> forcing questionType=fuel_type_comparison (was {})",
                    request.getQuestionType());
            return new ExplanationRequest(
                    QuestionType.FUEL_TYPE_COMPARISON.wireValue(),
                    request.getDatasetSource(),
                    filters
            );
        }

        return request;
    }

    private void removeNullishCategoricalFilters(Map<String, Object> filters) {
        Set<String> keysToRemove = new LinkedHashSet<>();
        for (String filterKey : NULLABLE_CATEGORICAL_FILTERS) {
            Object value = filters.get(filterKey);
            if (value instanceof String s && NULLISH_TOKENS.contains(s.trim().toLowerCase())) {
                keysToRemove.add(filterKey);
            }
        }
        keysToRemove.forEach(filters::remove);
    }
}
