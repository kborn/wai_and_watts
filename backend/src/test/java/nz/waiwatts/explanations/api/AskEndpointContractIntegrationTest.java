package nz.waiwatts.explanations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.service.DatasetSelectionService;
import nz.waiwatts.explanations.service.RequestValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Tag("contract")
class AskEndpointContractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private nz.waiwatts.explanations.service.IntentParserService intentParserService;

    @MockitoBean
    private nz.waiwatts.explanations.service.DatasetSelectionService datasetSelectionService;

    @MockitoBean
    private nz.waiwatts.explanations.service.RequestValidationService validationService;

    @MockitoBean
    private nz.waiwatts.explanations.service.ExplanationService explanationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void parseRefusalUnsupportedIntentMapsToUnsupportedCapabilityCode() throws Exception {
        IntentParseResponse parseRefusal = IntentParseResponse.refusal(
            "UNSUPPORTED_INTENT",
            "I can't confidently map this question."
        );
        parseRefusal.setParserUsed("OPENAI");
        when(intentParserService.parseQuestion(any())).thenReturn(parseRefusal);

        mockMvc.perform(post("/api/v1/explanations/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"What should I invest in?\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRefusal").value(true))
            .andExpect(jsonPath("$.refusal.code").value("UNSUPPORTED_CAPABILITY"))
            .andExpect(jsonPath("$.refusal.message").value("I can't confidently map this question."))
            .andExpect(jsonPath("$.debug.refusalTrigger").value("PARSE"))
            .andExpect(jsonPath("$.datasetSelection.strategy").value("NONE"));
    }

    @Test
    void explanationNoDataRefusalUsesCanonicalCodeAndMessage() throws Exception {
        ExplanationRequest parsedRequest = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("startYear", 2020, "endYear", 2023)
        );

        IntentParseResponse parseSuccess = IntentParseResponse.success(parsedRequest);
        parseSuccess.setParserUsed("OPENAI");
        when(intentParserService.parseQuestion(any())).thenReturn(parseSuccess);

        when(datasetSelectionService.selectDataset(any(), any())).thenReturn(
            DatasetSelectionService.DatasetSelectionResult.selected(
                "mbie.generation.annual",
                "Parsed dataset retained.",
                DatasetSelectionService.DatasetSelectionStrategy.EXPLICIT
            )
        );

        when(validationService.validateRequest(any()))
            .thenReturn(RequestValidationService.ValidationResult.success());

        when(explanationService.generateExplanation(any()))
            .thenReturn(Explanation.refusal("No facts available to answer the question"));

        mockMvc.perform(post("/api/v1/explanations/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Explain renewable generation trends between 2020 and 2023\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRefusal").value(true))
            .andExpect(jsonPath("$.refusal.code").value("NO_DATA"))
            .andExpect(jsonPath("$.refusal.message").value("Valid request, but no rows matched."))
            .andExpect(jsonPath("$.debug.refusalTrigger").value("EXPLANATION"));
    }

    @Test
    void successfulAskResponseHasStableEnvelopeShape() throws Exception {
        ExplanationRequest parsedRequest = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("startYear", 2020, "endYear", 2023)
        );

        IntentParseResponse parseSuccess = IntentParseResponse.success(parsedRequest);
        parseSuccess.setParserUsed("OPENAI");
        when(intentParserService.parseQuestion(any())).thenReturn(parseSuccess);

        when(datasetSelectionService.selectDataset(any(), any())).thenReturn(
            DatasetSelectionService.DatasetSelectionResult.selected(
                "mbie.generation.annual",
                "Parsed dataset retained.",
                DatasetSelectionService.DatasetSelectionStrategy.EXPLICIT
            )
        );

        when(validationService.validateRequest(any()))
            .thenReturn(RequestValidationService.ValidationResult.success());

        when(explanationService.generateExplanation(any())).thenReturn(
            new Explanation(
                "Renewable generation increased over the selected period.",
                List.of("ts:mbie:renewable_generation_gwh:2020_2023")
            )
        );

        MvcResult result = mockMvc.perform(post("/api/v1/explanations/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Explain renewable generation trends between 2020 and 2023\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRefusal").value(false))
            .andExpect(jsonPath("$.refusal.code").doesNotExist())
            .andExpect(jsonPath("$.selectedDatasetSource").value("mbie.generation.annual"))
            .andExpect(jsonPath("$.citations").isArray())
            .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("EXPLICIT", body.path("datasetSelection").path("strategy").asText());
    }
}
