package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.config.LlmProperties;
import nz.waiwatts.explanations.config.LlmProvider;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.parser.IntentParser;
import nz.waiwatts.explanations.parser.UnsupportedIntentDetector;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntentParserServiceImplTest {

    @Test
    void noKeyModeUsesDemoParserForSampleQuestion() {
        LlmProperties props = new LlmProperties();
        props.setProvider(LlmProvider.OPENAI);
        props.setModel("gpt-4.1");
        props.setBaseUrl("https://api.openai.com");
        // apiKey omitted -> not configured

        IntentParser llmParser = mock(IntentParser.class);
        IntentParserServiceImpl service = new IntentParserServiceImpl(
            llmParser,
            props,
            new UnsupportedIntentDetector()
        );

        IntentParseResponse response = service.parseQuestion(
            "Explain renewable generation trends between 2020 and 2023"
        );

        assertTrue(response.isOk());
        assertEquals("DEMO", response.getParserUsed());
        verifyNoInteractions(llmParser);
    }

    @Test
    void noKeyModeRefusesNonSampleQuestion() {
        LlmProperties props = new LlmProperties();
        props.setProvider(LlmProvider.OPENAI);
        props.setModel("gpt-4.1");
        props.setBaseUrl("https://api.openai.com");

        IntentParser llmParser = mock(IntentParser.class);
        IntentParserServiceImpl service = new IntentParserServiceImpl(
            llmParser,
            props,
            new UnsupportedIntentDetector()
        );

        IntentParseResponse response = service.parseQuestion("Tell me something random");

        assertFalse(response.isOk());
        assertEquals("LLM_REQUIRED", response.getRefusal().getCategory());
        assertEquals("DEMO", response.getParserUsed());
        verifyNoInteractions(llmParser);
    }

    @Test
    void llmModeRefusesUnsupportedIntentBeforeParsing() {
        LlmProperties props = new LlmProperties();
        props.setProvider(LlmProvider.OPENAI);
        props.setModel("gpt-4.1");
        props.setApiKey("test-key");
        props.setBaseUrl("https://api.openai.com");

        IntentParser llmParser = mock(IntentParser.class);
        IntentParserServiceImpl service = new IntentParserServiceImpl(
            llmParser,
            props,
            new UnsupportedIntentDetector()
        );

        IntentParseResponse response = service.parseQuestion("Why did hydro generation fall?");

        assertFalse(response.isOk());
        assertEquals("UNSUPPORTED_INTENT", response.getRefusal().getCategory());
        assertEquals("LLM", response.getParserUsed());
        verifyNoInteractions(llmParser);
    }

    @Test
    void llmModeRefusesDerivedAnalyticsBeforeParsing() {
        LlmProperties props = new LlmProperties();
        props.setProvider(LlmProvider.OPENAI);
        props.setModel("gpt-4.1");
        props.setApiKey("test-key");
        props.setBaseUrl("https://api.openai.com");

        IntentParser llmParser = mock(IntentParser.class);
        IntentParserServiceImpl service = new IntentParserServiceImpl(
            llmParser,
            props,
            new UnsupportedIntentDetector()
        );

        IntentParseResponse response = service.parseQuestion("Which fuel has grown the most since 2005?");

        assertFalse(response.isOk());
        assertEquals("UNSUPPORTED_CAPABILITY", response.getRefusal().getCategory());
        assertEquals("LLM", response.getParserUsed());
        verifyNoInteractions(llmParser);
    }

    @Test
    void llmModeRefusesWhenParserReturnsNull() {
        LlmProperties props = new LlmProperties();
        props.setProvider(LlmProvider.OPENAI);
        props.setModel("gpt-4.1");
        props.setApiKey("test-key");
        props.setBaseUrl("https://api.openai.com");

        IntentParser llmParser = mock(IntentParser.class);
        when(llmParser.parseQuestion(any())).thenReturn(null);

        IntentParserServiceImpl service = new IntentParserServiceImpl(
            llmParser,
            props,
            new UnsupportedIntentDetector()
        );

        IntentParseResponse response = service.parseQuestion("Explain renewable generation trends");

        assertFalse(response.isOk());
        assertEquals("UNABLE_TO_PARSE", response.getRefusal().getCategory());
        assertEquals("LLM", response.getParserUsed());
        verify(llmParser, times(1)).parseQuestion(any());
    }

    @Test
    void llmModeNormalizesUnknownMetricTypeAsAbsent() {
        LlmProperties props = new LlmProperties();
        props.setProvider(LlmProvider.OPENAI);
        props.setModel("gpt-4.1");
        props.setApiKey("test-key");
        props.setBaseUrl("https://api.openai.com");

        IntentParser llmParser = mock(IntentParser.class);
        Map<String, Object> filters = new HashMap<>();
        filters.put("fuelType", "geothermal");
        filters.put("startYear", 2005);
        filters.put("metricType", "unknown");

        when(llmParser.parseQuestion(any())).thenReturn(
            new ExplanationRequest(
                "fuel_generation_trend",
                "mbie.generation.annual",
                filters
            )
        );

        IntentParserServiceImpl service = new IntentParserServiceImpl(
            llmParser,
            props,
            new UnsupportedIntentDetector()
        );

        IntentParseResponse response = service.parseQuestion("How has geothermal generation changed since 2005?");

        assertTrue(response.isOk());
        assertEquals("LLM", response.getParserUsed());
        assertNotNull(response.getRequest());
        assertNotNull(response.getRequest().getFilters());
        assertFalse(response.getRequest().getFilters().containsKey("metricType"));
    }
}
