package nz.waiwatts.explanations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.service.DatasetSelectionService;
import nz.waiwatts.explanations.service.RequestValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Phase 12 Natural Language endpoint.
 *
 * Tests NL → intent parsing → validation → explanation pipeline.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class NaturalLanguageEndpointIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private nz.waiwatts.explanations.service.ExplanationService explanationService;

    @MockBean
    private nz.waiwatts.explanations.service.IntentParserService intentParserService;

    @MockBean
    private RequestValidationService validationService;

    @MockBean
    private DatasetSelectionService datasetSelectionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testSuccessfulQuestionProcessing() throws Exception {
        String requestBody = """
            {
                "question": "How did renewable generation change from 2018 to 2024?"
            }
            """;

        // Mock successful parsing and validation
        ExplanationRequest parsedRequest = new ExplanationRequest(
                "renewable_generation_trend",
                "mbie.generation.annual",
                Map.of("startYear", 2018, "endYear", 2024)
        );

        IntentParseResponse parseResponse = IntentParseResponse.success(parsedRequest);
        RequestValidationService.ValidationResult validationResult = RequestValidationService.ValidationResult.success();

        Explanation mockExplanation = new Explanation("Renewable generation increased from X to Y", List.of("fact1", "fact2"));

        when(intentParserService.parseQuestion(any())).thenReturn(parseResponse);
        when(datasetSelectionService.selectDataset(any(), any()))
            .thenReturn(DatasetSelectionService.DatasetSelectionResult.selected(
                "mbie.generation.annual",
                "Dataset source explicitly provided in parsed intent.",
                DatasetSelectionService.DatasetSelectionStrategy.EXPLICIT
            ));
        when(validationService.validateRequest(any())).thenReturn(validationResult);
        when(explanationService.generateExplanation(any())).thenReturn(mockExplanation);

        mockMvc.perform(post("/api/v1/explanations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value("Renewable generation increased from X to Y"))
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.isRefusal").value(false))
                .andExpect(jsonPath("$.refusal.code").value(nullValue()))
                .andExpect(jsonPath("$.refusal.message").value(nullValue()))
                .andExpect(jsonPath("$.selectedDatasetSource").value("mbie.generation.annual"))
                .andExpect(jsonPath("$.datasetSelection.strategy").value("EXPLICIT"));
    }

    @Test
    void testIntentParsingFailure() throws Exception {
        String requestBody = """
            {
                "question": "Tell me something random"
            }
            """;

        // Mock parsing failure
        IntentParseResponse parseResponse = IntentParseResponse.refusal("UNABLE_TO_PARSE",
                "I can't confidently map this question to a supported explanation type.");

        when(intentParserService.parseQuestion(any())).thenReturn(parseResponse);

        mockMvc.perform(post("/api/v1/explanations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRefusal").value(true))
                .andExpect(jsonPath("$.refusal.code").value("UNABLE_TO_PARSE"))
                .andExpect(jsonPath("$.refusal.message").exists())
                .andExpect(jsonPath("$.datasetSelection.strategy").value("NONE"));
    }

    @Test
    void testValidationFailure() throws Exception {
        String requestBody = """
            {
                "question": "Show generation from 2025 to 2020"
            }
            """;

        // Mock successful parsing but validation failure
        ExplanationRequest parsedRequest = new ExplanationRequest(
                "renewable_generation_trend",
                "mbie.generation.annual",
                Map.of("startYear", 2025, "endYear", 2020)
        );

        IntentParseResponse parseResponse = IntentParseResponse.success(parsedRequest);
        RequestValidationService.ValidationResult validationResult =
                RequestValidationService.ValidationResult.failure("INVALID_FILTERS", "startYear must be <= endYear");

        when(intentParserService.parseQuestion(any())).thenReturn(parseResponse);
        when(datasetSelectionService.selectDataset(any(), any()))
            .thenReturn(DatasetSelectionService.DatasetSelectionResult.selected(
                "mbie.generation.annual",
                "Dataset source explicitly provided in parsed intent.",
                DatasetSelectionService.DatasetSelectionStrategy.EXPLICIT
            ));
        when(validationService.validateRequest(any())).thenReturn(validationResult);

        mockMvc.perform(post("/api/v1/explanations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRefusal").value(true))
                .andExpect(jsonPath("$.refusal.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.refusal.message").exists())
                .andExpect(jsonPath("$.selectedDatasetSource").value("mbie.generation.annual"))
                .andExpect(jsonPath("$.datasetSelection.strategy").value("EXPLICIT"));
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

    @Test
    void testUnexpectedExceptionReturnsInternalErrorRefusalEnvelope() throws Exception {
        String requestBody = """
            {
                "question": "How did renewable generation change from 2018 to 2024?"
            }
            """;

        ExplanationRequest parsedRequest = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("startYear", 2018, "endYear", 2024)
        );

        IntentParseResponse parseResponse = IntentParseResponse.success(parsedRequest);
        RequestValidationService.ValidationResult validationResult = RequestValidationService.ValidationResult.success();

        when(intentParserService.parseQuestion(any())).thenReturn(parseResponse);
        when(datasetSelectionService.selectDataset(any(), any()))
            .thenReturn(DatasetSelectionService.DatasetSelectionResult.selected(
                "mbie.generation.annual",
                "Dataset source explicitly provided in parsed intent.",
                DatasetSelectionService.DatasetSelectionStrategy.EXPLICIT
            ));
        when(validationService.validateRequest(any())).thenReturn(validationResult);
        when(explanationService.generateExplanation(any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/v1/explanations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRefusal").value(true))
                .andExpect(jsonPath("$.refusal.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.refusal.message").value("An internal error occurred while processing your request. Please try again."))
                .andExpect(jsonPath("$.debug.refusalTrigger").value("EXCEPTION"));
    }
}
