package nz.waiwatts.explanations.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.llm.OpenAiApiClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiExplanationGeneratorTest {

    private OpenAiExplanationGenerator generator() {
        return new OpenAiExplanationGenerator(mock(OpenAiApiClient.class), new ObjectMapper(), "gpt-test");
    }

    private OpenAiExplanationGenerator generator(OpenAiApiClient client) {
        return new OpenAiExplanationGenerator(client, new ObjectMapper(), "gpt-test");
    }

    @Test
    void validateCitations_supportsAnyWildcardFamily() {
        OpenAiExplanationGenerator generator = generator();
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of("metric:lawa:improving_sites_pct:__any__"));

        Explanation explanation = new Explanation("ok", List.of("metric:lawa:improving_sites_pct:waikato"));

        assertTrue(generator.validateCitations(explanation, factPack));
    }

    @Test
    void validateCitations_supportsLawaFamilyMatch() {
        OpenAiExplanationGenerator generator = generator();
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of("class:lawa:water_quality_state:EXCELLENT"));

        Explanation explanation = new Explanation("ok", List.of("class:lawa:water_quality_state:GOOD"));

        assertTrue(generator.validateCitations(explanation, factPack));
    }

    @Test
    void validateCitations_failsWhenRequiredCitationMissing() {
        OpenAiExplanationGenerator generator = generator();
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of("metric:mbie:generation_gwh:2024:HYDRO"));

        Explanation explanation = new Explanation("ok", List.of("metric:mbie:generation_gwh:2024:WIND"));

        assertFalse(generator.validateCitations(explanation, factPack));
    }

    @Test
    void generateExplanation_refusesWhenNonRefusalPayloadHasBlankExplanationText() {
        OpenAiApiClient client = mock(OpenAiApiClient.class);
        OpenAiExplanationGenerator generator = generator(client);
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of());

        when(client.createResponse(
            anyString(),
            anyString(),
            anyString()
        )).thenReturn("{\"isRefusal\":false,\"explanationText\":\"   \",\"citations\":[\"metric:mbie:generation_gwh:2024:HYDRO\"]}");

        Explanation result = generator.generateExplanation("fuel_generation_trend", factPack);

        assertTrue(result.isRefusal());
        assertEquals("LLM response parse failure", result.getRefusalReason());
    }

    @Test
    void generateExplanation_acceptsRefusalPayloadWithBlankExplanationText() {
        OpenAiApiClient client = mock(OpenAiApiClient.class);
        OpenAiExplanationGenerator generator = generator(client);
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of());

        when(client.createResponse(
            anyString(),
            anyString(),
            anyString()
        )).thenReturn("{\"isRefusal\":true,\"refusalReason\":\"Insufficient data\",\"explanationText\":\"\"}");

        Explanation result = generator.generateExplanation("fuel_generation_trend", factPack);

        assertTrue(result.isRefusal());
        assertEquals("Insufficient data", result.getRefusalReason());
    }
}
