package nz.waiwatts.explanations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for refusal behavior through the API controller
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Tag("contract")
class ExplanationControllerRefusalIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private nz.waiwatts.explanations.service.ExplanationService explanationService;

    @Autowired
    private DatasetCatalog datasetCatalog;

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
            "fuel_generation_trend", 
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
            "fuel_generation_trend", 
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
                .andExpect(jsonPath("$.supportedQuestionTypes.fuel_generation_trend").exists())
                .andExpect(jsonPath("$.supportedQuestionTypes.fuel_type_comparison").exists())
                .andExpect(jsonPath("$.supportedQuestionTypes.generation_mix_overview").exists())
                .andExpect(jsonPath("$.unsupportedQuestionTypes").exists())
                .andExpect(jsonPath("$.unsupportedQuestionTypes.forecasting").exists())
                .andExpect(jsonPath("$.unsupportedQuestionTypes.causation").exists())
                .andExpect(jsonPath("$.requiredFilters.datasetSource").exists())
                .andExpect(jsonPath("$.filterStructure").exists())
                .andExpect(jsonPath("$.suggestedValuesByToken.fuelType").isArray())
                .andExpect(jsonPath("$.metricTypes").exists())
                .andExpect(jsonPath("$.capabilities").isArray());
    }

    @Test
    void testCanonicalCapabilitiesEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/capabilities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supportedQuestionTypes").exists())
            .andExpect(jsonPath("$.metricTypes.renewable_generation_trend").isArray())
            .andExpect(jsonPath("$.examples.renewable_generation_trend").isArray())
            .andExpect(jsonPath("$.datasets").isArray())
            .andExpect(jsonPath("$.supportedDatasetSources").exists());
    }

    @Test
    void capabilitiesContractShapeRemainsStable() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/capabilities"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertObjectFieldSet(root, Set.of(
            "supportedQuestionTypes",
            "unsupportedQuestionTypes",
            "supportedDatasetSources",
            "requiredFilters",
            "filterStructure",
            "suggestedValuesByToken",
            "metricTypes",
            "examples",
            "capabilities",
            "datasets"
        ));

        JsonNode datasets = root.path("datasets");
        assertTrue(datasets.isArray(), "datasets must be an array");
        if (!datasets.isEmpty()) {
            JsonNode firstDataset = datasets.get(0);
            assertObjectFieldSet(firstDataset, Set.of(
                "datasetSource",
                "displayName",
                "description",
                "supportedQuestionTypes",
                "supportedFilters"
            ));
        }
    }

    @Test
    void capabilitiesEndpointsRemainEquivalent() throws Exception {
        MvcResult canonical = mockMvc.perform(get("/api/v1/capabilities"))
            .andExpect(status().isOk())
            .andReturn();
        MvcResult legacy = mockMvc.perform(get("/api/v1/explanations/capabilities"))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(
            objectMapper.readTree(canonical.getResponse().getContentAsString()),
            objectMapper.readTree(legacy.getResponse().getContentAsString())
        );
    }

    @Test
    void legacyCapabilitiesEndpointDeclaresDeprecationAndSunsetHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/explanations/capabilities"))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("true", result.getResponse().getHeader("Deprecation"));
        assertNotNull(result.getResponse().getHeader("Sunset"));
        String link = result.getResponse().getHeader("Link");
        assertNotNull(link);
        assertTrue(link.contains("/api/v1/capabilities"));
        assertNotNull(result.getResponse().getHeader("Warning"));
    }

    @Test
    void canonicalCapabilitiesEndpointHasNoDeprecationHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/capabilities"))
            .andExpect(status().isOk())
            .andReturn();

        assertNull(result.getResponse().getHeader("Deprecation"));
        assertNull(result.getResponse().getHeader("Sunset"));
    }

    @Test
    void capabilitiesSupportedQuestionTypesCoverAllCatalogQuestionTypes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/capabilities"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode supported = root.path("supportedQuestionTypes");

        for (var descriptor : datasetCatalog.getDatasets()) {
            for (String questionType : descriptor.supportedQuestionTypes()) {
                assertTrue(supported.has(questionType), "Missing supportedQuestionTypes entry for " + questionType);
            }
        }
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
    void legacyHealthEndpointDeclaresDeprecationAndSunsetHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/explanations/health"))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("true", result.getResponse().getHeader("Deprecation"));
        assertNotNull(result.getResponse().getHeader("Sunset"));
        String link = result.getResponse().getHeader("Link");
        assertNotNull(link);
        assertTrue(link.contains("/api/v1/health"));
        assertNotNull(result.getResponse().getHeader("Warning"));
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
            "fuel_generation_trend", 
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

    private static void assertObjectFieldSet(JsonNode node, Set<String> expectedFields) {
        Set<String> actualFields = new HashSet<>();
        node.fieldNames().forEachRemaining(actualFields::add);
        assertEquals(expectedFields, actualFields);
    }
}
