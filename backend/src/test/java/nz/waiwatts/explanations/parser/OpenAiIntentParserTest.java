package nz.waiwatts.explanations.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.provider.OpenAiResponseClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiIntentParserTest {

    @Test
    void stripsNullStringFilters() {
        OpenAiResponseClient client = mock(OpenAiResponseClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(client.createResponseWithSchema(anyString(), anyString(), anyString(), any(), anyString()))
            .thenReturn("{" +
                "\"questionType\":\"renewable_generation_trend\"," +
                "\"datasetSource\":\"mbie.generation.annual\"," +
                "\"filters\":{" +
                    "\"fuelType\":\"null\"," +
                    "\"startYear\":\"2020\"," +
                    "\"endYear\":null," +
                    "\"region\":\" \"" +
                "}" +
            "}");

        OpenAiIntentParser parser = new OpenAiIntentParser(
            client,
            objectMapper,
            "gpt-test",
            new CapabilityRegistry(new DatasetCatalog())
        );
        ExplanationRequest request = parser.parseQuestion("Any question");

        assertNotNull(request);
        assertEquals("renewable_generation_trend", request.getQuestionType());
        assertEquals("mbie.generation.annual", request.getDatasetSource());

        Map<String, Object> filters = request.getFilters();
        assertNotNull(filters);
        assertEquals(1, filters.size());
        assertEquals(2020, filters.get("startYear"));
        assertFalse(filters.containsKey("fuelType"));
        assertFalse(filters.containsKey("endYear"));
    }

    @Test
    void parsesMetricTypeFilterWhenPresent() {
        OpenAiResponseClient client = mock(OpenAiResponseClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(client.createResponseWithSchema(anyString(), anyString(), anyString(), any(), anyString()))
            .thenReturn("{" +
                "\"questionType\":\"generation_mix_overview\"," +
                "\"datasetSource\":\"mbie.generation.annual\"," +
                "\"filters\":{" +
                    "\"metricType\":\"generation_share_pct\"" +
                "}" +
            "}");

        OpenAiIntentParser parser = new OpenAiIntentParser(
            client,
            objectMapper,
            "gpt-test",
            new CapabilityRegistry(new DatasetCatalog())
        );

        ExplanationRequest request = parser.parseQuestion("Show mix by share");
        assertNotNull(request);
        assertNotNull(request.getFilters());
        assertEquals("generation_share_pct", request.getFilters().get("metricType"));
    }
}
