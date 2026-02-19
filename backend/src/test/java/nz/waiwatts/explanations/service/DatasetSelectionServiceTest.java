package nz.waiwatts.explanations.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.config.LlmProperties;
import nz.waiwatts.explanations.config.LlmProvider;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.provider.OpenAiResponseClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatasetSelectionServiceTest {

    @Test
    void selectsCandidateWhenDatasetMissing() {
        DatasetCatalog catalog = new DatasetCatalog();
        OpenAiResponseClient client = mock(OpenAiResponseClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        LlmProperties properties = new LlmProperties();
        properties.setProvider(LlmProvider.OPENAI);
        properties.setModel("gpt-4.1");
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.openai.com");

        when(client.createResponseWithSchema(anyString(), anyString(), anyString(), any(), anyString()))
            .thenReturn("{\"candidates\": [\"mbie.generation.quarterly\"]}");

        DatasetSelectionService service = new DatasetSelectionService(
            catalog,
            client,
            objectMapper,
            properties
        );

        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            null,
            Map.of("startYear", 2020, "endYear", 2023)
        );

        DatasetSelectionService.DatasetSelectionResult result = service.selectDataset(
            "Explain renewable generation trends between 2020 and 2023",
            request
        );

        assertTrue(result.isSelected());
        assertEquals("mbie.generation.quarterly", result.getDatasetSource());
        assertEquals(DatasetSelectionService.DatasetSelectionStrategy.LLM_CANDIDATES, result.getStrategy());
    }

    @Test
    void refusesInvalidCandidate() {
        DatasetCatalog catalog = new DatasetCatalog();
        OpenAiResponseClient client = mock(OpenAiResponseClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        LlmProperties properties = new LlmProperties();
        properties.setProvider(LlmProvider.OPENAI);
        properties.setModel("gpt-4.1");
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.openai.com");

        when(client.createResponseWithSchema(anyString(), anyString(), anyString(), any(), anyString()))
            .thenReturn("{\"candidates\": [\"invalid.dataset\"]}");

        DatasetSelectionService service = new DatasetSelectionService(
            catalog,
            client,
            objectMapper,
            properties
        );

        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            null,
            null
        );

        DatasetSelectionService.DatasetSelectionResult result = service.selectDataset(
            "Explain renewable generation trends",
            request
        );

        assertFalse(result.isSelected());
        assertEquals("CAPABILITY_UNSUPPORTED", result.getRefusalCategory());
    }

    @Test
    void refusesEmptyCandidateList() {
        DatasetCatalog catalog = new DatasetCatalog();
        OpenAiResponseClient client = mock(OpenAiResponseClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        LlmProperties properties = new LlmProperties();
        properties.setProvider(LlmProvider.OPENAI);
        properties.setModel("gpt-4.1");
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.openai.com");

        when(client.createResponseWithSchema(anyString(), anyString(), anyString(), any(), anyString()))
            .thenReturn("{\"candidates\": []}");

        DatasetSelectionService service = new DatasetSelectionService(
            catalog,
            client,
            objectMapper,
            properties
        );

        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            null,
            null
        );

        DatasetSelectionService.DatasetSelectionResult result = service.selectDataset(
            "Explain renewable generation trends",
            request
        );

        assertFalse(result.isSelected());
        assertEquals("UNSUPPORTED_CAPABILITY", result.getRefusalCategory());
    }

    @Test
    void selectsLawaStateWithoutLlmWhenQuestionTypeFixed() {
        DatasetCatalog catalog = new DatasetCatalog();
        OpenAiResponseClient client = mock(OpenAiResponseClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        LlmProperties properties = new LlmProperties();
        properties.setProvider(LlmProvider.OPENAI);
        properties.setModel("gpt-4.1");
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.openai.com");

        DatasetSelectionService service = new DatasetSelectionService(
            catalog,
            client,
            objectMapper,
            properties
        );

        ExplanationRequest request = new ExplanationRequest(
            "water_quality_overview",
            null,
            null
        );

        DatasetSelectionService.DatasetSelectionResult result = service.selectDataset(
            "Summarize water quality state",
            request
        );

        assertTrue(result.isSelected());
        assertEquals("lawa.water_quality.state.multi_year", result.getDatasetSource());
        assertEquals(DatasetSelectionService.DatasetSelectionStrategy.HEURISTIC, result.getStrategy());
    }

    @Test
    void preservesExplicitRefusalMessageFromVerification() {
        DatasetCatalog catalog = new DatasetCatalog();
        OpenAiResponseClient client = mock(OpenAiResponseClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        LlmProperties properties = new LlmProperties();
        properties.setProvider(LlmProvider.OPENAI);
        properties.setModel("gpt-4.1");
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.openai.com");

        DatasetSelectionService service = new DatasetSelectionService(
            catalog,
            client,
            objectMapper,
            properties
        );

        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("indicator", "NITROGEN")
        );

        DatasetSelectionService.DatasetSelectionResult result = service.selectDataset(
            "Explain renewable generation trends",
            request
        );

        assertFalse(result.isSelected());
        assertEquals(DatasetSelectionService.DatasetSelectionStrategy.EXPLICIT, result.getStrategy());
        assertEquals("CAPABILITY_UNSUPPORTED", result.getRefusalCategory());
        assertEquals("Dataset mbie.generation.annual does not support filter: indicator", result.getRefusalMessage());
    }
}
