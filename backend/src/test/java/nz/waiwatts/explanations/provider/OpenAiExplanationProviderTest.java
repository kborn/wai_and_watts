package nz.waiwatts.explanations.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiExplanationProviderTest {

    private OpenAiExplanationProvider provider() {
        return new OpenAiExplanationProvider(mock(OpenAiResponseClient.class), new ObjectMapper(), "gpt-test");
    }

    private OpenAiExplanationProvider provider(OpenAiResponseClient client) {
        return new OpenAiExplanationProvider(client, new ObjectMapper(), "gpt-test");
    }

    @Test
    void validateCitations_supportsAnyWildcardFamily() {
        OpenAiExplanationProvider provider = provider();
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of("metric:lawa:improving_sites_pct:__any__"));

        Explanation explanation = new Explanation("ok", List.of("metric:lawa:improving_sites_pct:waikato"));

        assertTrue(provider.validateCitations(explanation, factPack));
    }

    @Test
    void validateCitations_supportsLawaFamilyMatch() {
        OpenAiExplanationProvider provider = provider();
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of("class:lawa:water_quality_state:EXCELLENT"));

        Explanation explanation = new Explanation("ok", List.of("class:lawa:water_quality_state:GOOD"));

        assertTrue(provider.validateCitations(explanation, factPack));
    }

    @Test
    void validateCitations_failsWhenRequiredCitationMissing() {
        OpenAiExplanationProvider provider = provider();
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of("metric:mbie:generation_gwh:2024:HYDRO"));

        Explanation explanation = new Explanation("ok", List.of("metric:mbie:generation_gwh:2024:WIND"));

        assertFalse(provider.validateCitations(explanation, factPack));
    }

    @Test
    void generateExplanation_refusesWhenNonRefusalPayloadHasBlankExplanationText() {
        OpenAiResponseClient client = mock(OpenAiResponseClient.class);
        OpenAiExplanationProvider provider = provider(client);
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of());

        when(client.createResponse(
            anyString(),
            anyString(),
            anyString()
        )).thenReturn("{\"isRefusal\":false,\"explanationText\":\"   \",\"citations\":[\"metric:mbie:generation_gwh:2024:HYDRO\"]}");

        Explanation result = provider.generateExplanation("fuel_generation_trend", factPack);

        assertTrue(result.isRefusal());
        assertEquals("LLM response parse failure", result.getRefusalReason());
    }

    @Test
    void generateExplanation_acceptsRefusalPayloadWithBlankExplanationText() {
        OpenAiResponseClient client = mock(OpenAiResponseClient.class);
        OpenAiExplanationProvider provider = provider(client);
        FactPack factPack = new FactPack();
        factPack.getGuardrails().setRequiredCitations(List.of());

        when(client.createResponse(
            anyString(),
            anyString(),
            anyString()
        )).thenReturn("{\"isRefusal\":true,\"refusalReason\":\"Insufficient data\",\"explanationText\":\"\"}");

        Explanation result = provider.generateExplanation("fuel_generation_trend", factPack);

        assertTrue(result.isRefusal());
        assertEquals("Insufficient data", result.getRefusalReason());
    }
}
