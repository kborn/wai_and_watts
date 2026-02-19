package nz.waiwatts.explanations.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.provider.OpenAiResponseClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

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

        OpenAiIntentParser parser = new OpenAiIntentParser(client, objectMapper, "gpt-test");
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
}
