package nz.waiwatts.explanations.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Phase 12 Natural Language endpoint.
 *
 * Uses real parser/selection/validation/explanation services under the test profile.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Tag("contract")
class NaturalLanguageEndpointIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testRealPipeline_DemoQuestion_usesRealParseSelectionValidationAndExplanationPath() throws Exception {
        String requestBody = """
            {
                "question": "Explain renewable generation trends between 2020 and 2023"
            }
            """;

        mockMvc.perform(post("/api/v1/explanations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsedRequest.questionType").value("renewable_generation_trend"))
                .andExpect(jsonPath("$.parsedRequest.datasetSource").value("mbie.generation.annual"))
                .andExpect(jsonPath("$.selectedDatasetSource").value("mbie.generation.annual"))
                .andExpect(jsonPath("$.datasetSelection.strategy").value("EXPLICIT"))
                .andExpect(jsonPath("$.debug.parserUsed").value("DEMO"));
    }

    @Test
    void testRealPipeline_UnsupportedQuestion_RefusesAtParseStage() throws Exception {
        String requestBody = """
            {
                "question": "Tell me something random"
            }
            """;

        mockMvc.perform(post("/api/v1/explanations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRefusal").value(true))
                .andExpect(jsonPath("$.refusal.code").value("LLM_REQUIRED"))
                .andExpect(jsonPath("$.refusal.message").exists())
                .andExpect(jsonPath("$.refusal.details.category").value("LLM_REQUIRED"))
                .andExpect(jsonPath("$.refusal.details.examples").isArray())
                .andExpect(jsonPath("$.datasetSelection.strategy").value("NONE"))
                .andExpect(jsonPath("$.debug.parserUsed").value("DEMO"));
    }

    @Test
    void testMissingQuestionField() throws Exception {
        String requestBody = """
            {
                "notQuestion": "This should fail"
            }
            """;

        mockMvc.perform(post("/api/v1/explanations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEmptyQuestionField() throws Exception {
        String requestBody = """
            {
                "question": ""
            }
            """;

        mockMvc.perform(post("/api/v1/explanations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMalformedJson() throws Exception {
        String requestBody = """
            {
                "question": "This is malformed json
            }
            """;

        mockMvc.perform(post("/api/v1/explanations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
