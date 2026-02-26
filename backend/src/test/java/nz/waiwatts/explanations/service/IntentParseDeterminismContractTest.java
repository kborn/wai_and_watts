package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.config.LlmProperties;
import nz.waiwatts.explanations.config.LlmProvider;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.parser.IntentParser;
import nz.waiwatts.explanations.parser.UnsupportedIntentDetector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("contract")
class IntentParseDeterminismContractTest {

    private static final int ITERATIONS_PER_PROMPT = 3;

    private static final List<String> FIXED_PROMPT_CORPUS = List.of(
        "How has geothermal generation changed since 2005?",
        "How has geothermal generation changed since 2005??",
        "How has wind generation changed since 2010?",
        "How has solar generation changed since 2019?",
        "How has hydro generation changed since 2000?",
        "Compare hydro and wind generation trends",
        "Compare hydro and geothermal generation patterns",
        "What is the generation mix in New Zealand?",
        "Explain renewable generation trends between 2020 and 2023",
        "Show renewable generation trend from 2015 to 2024",
        "Is water quality improving in Auckland?",
        "Compare water quality trends across regions",
        "Show water quality state in Canterbury",
        "Summarize trend direction for E. coli in Auckland",
        "Tell me something random",
        "Predict generation for next year",
        "Why did hydro generation fall?",
        "What should I invest in?",
        "   Explain hydro generation trends between 2018 and 2023   ",
        "EXPLAIN RENEWABLE GENERATION TRENDS BETWEEN 2020 AND 2023"
    );

    @Test
    void fixedCorpus_parseNormalizeAndValidationSignature_remainsStableAcrossRepeatedRuns() {
        IntentParserServiceImpl service = buildServiceWithDeterministicParser();
        RequestValidationService validationService = new RequestValidationService(
            new CapabilityRegistry(new DatasetCatalog())
        );

        for (String prompt : FIXED_PROMPT_CORPUS) {
            String expected = null;
            for (int i = 0; i < ITERATIONS_PER_PROMPT; i++) {
                IntentParseResponse parsed = service.parseQuestion(prompt);
                String signature = signatureFor(parsed, validationService);
                if (expected == null) {
                    expected = signature;
                } else {
                    assertEquals(expected, signature, "Determinism drift for prompt: " + prompt);
                }
            }
        }
    }

    private IntentParserServiceImpl buildServiceWithDeterministicParser() {
        IntentParser llmParser = mock(IntentParser.class);
        when(llmParser.parseQuestion(anyString()))
            .thenAnswer(invocation -> deterministicParsedRequest(invocation.getArgument(0, String.class)));

        LlmProperties props = new LlmProperties();
        props.setProvider(LlmProvider.OPENAI);
        props.setModel("gpt-4.1-mini");
        props.setApiKey("test-key");
        props.setBaseUrl("https://api.openai.com");

        return new IntentParserServiceImpl(llmParser, props, new UnsupportedIntentDetector());
    }

    private ExplanationRequest deterministicParsedRequest(String question) {
        String q = question == null ? "" : question.trim().toLowerCase();

        if (q.contains("geothermal generation changed since 2005")) {
            // Exercise normalization: metricType=unknown must be removed.
            return request(
                QuestionType.FUEL_GENERATION_TREND,
                DatasetSource.MBIE_GENERATION_ANNUAL,
                mapOf(
                    FilterKey.FUEL_TYPE, "geothermal",
                    FilterKey.START_YEAR, 2005,
                    FilterKey.METRIC_TYPE, "unknown"
                )
            );
        }
        if (q.contains("compare hydro and wind generation trends")) {
            // Exercise normalization: fuelTypeB present should force comparison questionType.
            return request(
                QuestionType.FUEL_GENERATION_TREND,
                DatasetSource.MBIE_GENERATION_ANNUAL,
                mapOf(
                    FilterKey.FUEL_TYPE, "hydro",
                    FilterKey.FUEL_TYPE_B, "wind"
                )
            );
        }
        if (q.contains("compare hydro and geothermal generation patterns")) {
            return request(
                QuestionType.FUEL_TYPE_COMPARISON,
                DatasetSource.MBIE_GENERATION_ANNUAL,
                mapOf(
                    FilterKey.FUEL_TYPE, "hydro",
                    FilterKey.FUEL_TYPE_B, "geothermal"
                )
            );
        }
        if (q.contains("generation mix")) {
            return request(
                QuestionType.GENERATION_MIX_OVERVIEW,
                DatasetSource.MBIE_GENERATION_ANNUAL,
                null
            );
        }
        if (q.contains("renewable generation")) {
            return request(
                QuestionType.RENEWABLE_GENERATION_TREND,
                DatasetSource.MBIE_GENERATION_ANNUAL,
                mapOf(
                    FilterKey.START_YEAR, 2020,
                    FilterKey.END_YEAR, 2023
                )
            );
        }
        if (q.contains("hydro generation trends between 2018 and 2023")) {
            return request(
                QuestionType.FUEL_GENERATION_TREND,
                DatasetSource.MBIE_GENERATION_ANNUAL,
                mapOf(
                    FilterKey.FUEL_TYPE, "hydro",
                    FilterKey.START_YEAR, 2018,
                    FilterKey.END_YEAR, 2023
                )
            );
        }
        if (q.contains("water quality state")) {
            // Exercise normalization: LAWA state question with trend dataset should be corrected.
            return request(
                QuestionType.WATER_QUALITY_OVERVIEW,
                DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR,
                mapOf(
                    FilterKey.REGION, "Canterbury"
                )
            );
        }
        if (q.contains("water quality trends across regions")) {
            return request(
                QuestionType.REGIONAL_TREND_COMPARISON,
                DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR,
                null
            );
        }
        if (q.contains("water quality improving")) {
            return request(
                QuestionType.WATER_QUALITY_TRENDS,
                DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR,
                mapOf(
                    FilterKey.REGION, "Auckland"
                )
            );
        }
        if (q.contains("trend direction for e. coli")) {
            return request(
                QuestionType.WATER_QUALITY_TRENDS,
                DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR,
                mapOf(
                    FilterKey.INDICATOR, "E. coli",
                    FilterKey.REGION, "Auckland"
                )
            );
        }

        // Unknown/unsupported prompts rely on service refusal handling.
        return null;
    }

    private ExplanationRequest request(QuestionType questionType, DatasetSource datasetSource, Map<String, Object> filters) {
        return new ExplanationRequest(
            questionType.wireValue(),
            datasetSource.wireValue(),
            filters
        );
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> out = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            FilterKey key = (FilterKey) entries[i];
            Object value = entries[i + 1];
            out.put(key.wireValue(), value);
        }
        return out;
    }

    private String signatureFor(IntentParseResponse parsed, RequestValidationService validationService) {
        if (!parsed.isOk()) {
            String category = parsed.getRefusal() != null ? parsed.getRefusal().getCategory() : null;
            return "PARSE_REFUSAL|category=" + category + "|parser=" + parsed.getParserUsed();
        }

        ExplanationRequest req = parsed.getRequest();
        RequestValidationService.ValidationResult validation = validationService.validateRequest(req);
        Map<String, Object> normalizedFilters = req.getFilters() == null ? Map.of() : new TreeMap<>(req.getFilters());

        return "PARSE_OK"
            + "|questionType=" + req.getQuestionType()
            + "|datasetSource=" + req.getDatasetSource()
            + "|filters=" + normalizedFilters
            + "|validationValid=" + validation.isValid()
            + "|validationCategory=" + validation.getRefusalCategory();
    }
}
