package nz.waiwatts.explanations.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OpenAiExplanationProviderTest {

    private OpenAiExplanationProvider provider() {
        return new OpenAiExplanationProvider(mock(OpenAiResponseClient.class), new ObjectMapper(), "gpt-test");
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
}
