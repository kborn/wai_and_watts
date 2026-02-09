package nz.waiwatts.explanations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for refusal behavior through the API controller
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class ExplanationControllerRefusalIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private nz.waiwatts.explanations.service.ExplanationService explanationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testRefusalForUnsupportedQuestionType() throws Exception {
        // Mock service to return refusal for unsupported question type
        Explanation refusalExplanation = Explanation.refusal("Unsupported question type: forecasting");
        when(explanationService.generateExplanation(any(ExplanationRequest.class)))
            .thenReturn(refusalExplanation);

        ExplanationRequest request = new ExplanationRequest(
            "forecasting", 
            Map.of("datasetSource", "mbie.generation.annual")
        );

        mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRefusal").value(true))
                .andExpect(jsonPath("$.refusalReason").value("Unsupported question type: forecasting"))
                .andExpect(jsonPath("$.explanationText").value(
                    "I can't answer that using the available dataset facts. If you want, I can explain what facts would be needed."
                ));
    }

    @Test
    void testRefusalForMissingDataSource() throws Exception {
        // Mock service to return refusal for missing data source
        Explanation refusalExplanation = Explanation.refusal("No data source available for this request");
        when(explanationService.generateExplanation(any(ExplanationRequest.class)))
            .thenReturn(refusalExplanation);

        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend", 
            Map.of("datasetSource", "nonexistent.source")
        );

        mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRefusal").value(true))
                .andExpect(jsonPath("$.refusalReason").value("No data source available for this request"));
    }

    @Test
    void testRefusalForMissingCitations() throws Exception {
        // Mock service to return refusal for missing citations
        Explanation refusalExplanation = Explanation.refusal("Generated explanation missing required citations");
        when(explanationService.generateExplanation(any(ExplanationRequest.class)))
            .thenReturn(refusalExplanation);

        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend", 
            Map.of("datasetSource", "mbie.generation.annual")
        );

        mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRefusal").value(true))
                .andExpect(jsonPath("$.refusalReason").value("Generated explanation missing required citations"));
    }

    @Test
    void testSupportedQuestionTypesEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/explanations/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportedQuestionTypes").exists())
                .andExpect(jsonPath("$.supportedQuestionTypes.renewable_generation_trend").exists())
                .andExpect(jsonPath("$.supportedQuestionTypes.hydro_generation_trend").exists())
                .andExpect(jsonPath("$.supportedQuestionTypes.fuel_type_comparison").exists())
                .andExpect(jsonPath("$.unsupportedQuestionTypes").exists())
                .andExpect(jsonPath("$.unsupportedQuestionTypes.forecasting").exists())
                .andExpect(jsonPath("$.unsupportedQuestionTypes.causation").exists())
                .andExpect(jsonPath("$.requiredFilters.datasetSource").exists())
                .andExpect(jsonPath("$.filterStructure").exists());
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/explanations/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.service").value("explanation-api"))
                .andExpect(jsonPath("$.phase").value("11"));
    }

    @Test
    void testSuccessfulExplanation() throws Exception {
        // Mock service to return successful explanation
        Explanation successExplanation = new Explanation(
            "Based on the data, hydro generation increased by 1000 GWh.",
            List.of("cmp:mbie:generation_gwh:HYDRO:2023_vs_2022")
        );
        when(explanationService.generateExplanation(any(ExplanationRequest.class)))
            .thenReturn(successExplanation);

        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend", 
            Map.of("datasetSource", "mbie.generation.annual")
        );

        mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRefusal").value(false))
                .andExpect(jsonPath("$.explanationText").value("Based on the data, hydro generation increased by 1000 GWh."))
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.citations[0]").value("cmp:mbie:generation_gwh:HYDRO:2023_vs_2022"));
    }
}