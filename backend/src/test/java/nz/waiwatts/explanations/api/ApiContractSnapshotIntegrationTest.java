package nz.waiwatts.explanations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.service.DatasetSelectionService;
import nz.waiwatts.explanations.service.RequestValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Tag("contract")
class ApiContractSnapshotIntegrationTest {

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
    void capabilitiesContractProjectionMatchesSnapshot() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/capabilities"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode actual = objectMapper.readTree(result.getResponse().getContentAsString());
        assertSnapshotEquals(
            "contracts/api/capabilities-contract-projection.json",
            projectCapabilitiesContract(actual)
        );
    }

    @Test
    void askSuccessEnvelopeProjectionMatchesSnapshot() throws Exception {
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
            .andReturn();

        JsonNode actual = objectMapper.readTree(result.getResponse().getContentAsString());
        assertSnapshotEquals(
            "contracts/api/ask-success-envelope-projection.json",
            projectAskEnvelope(actual)
        );
    }

    @Test
    void askRefusalEnvelopeProjectionMatchesSnapshot() throws Exception {
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

        MvcResult result = mockMvc.perform(post("/api/v1/explanations/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Explain renewable generation trends between 2020 and 2023\"}"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode actual = objectMapper.readTree(result.getResponse().getContentAsString());
        assertSnapshotEquals(
            "contracts/api/ask-refusal-envelope-projection.json",
            projectAskEnvelope(actual)
        );
    }

    private void assertSnapshotEquals(String resourcePath, JsonNode actual) throws IOException {
        JsonNode expected;
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            expected = objectMapper.readTree(inputStream);
        }
        assertEquals(expected, actual, "Contract snapshot mismatch for " + resourcePath);
    }

    private ObjectNode projectCapabilitiesContract(JsonNode root) {
        ObjectNode projection = objectMapper.createObjectNode();
        projection.set("rootFields", sortedFieldNames(root));
        projection.set("supportedQuestionTypesKeys", sortedFieldNames(root.path("supportedQuestionTypes")));
        projection.set("unsupportedQuestionTypesKeys", sortedFieldNames(root.path("unsupportedQuestionTypes")));
        projection.set("supportedDatasetSourcesKeys", sortedFieldNames(root.path("supportedDatasetSources")));
        projection.set("requiredFiltersKeys", sortedFieldNames(root.path("requiredFilters")));
        projection.set("filterStructureKeys", sortedFieldNames(root.path("filterStructure")));
        projection.set("suggestedValuesByTokenKeys", sortedFieldNames(root.path("suggestedValuesByToken")));
        projection.set("metricTypesKeys", sortedFieldNames(root.path("metricTypes")));
        projection.set("examplesKeys", sortedFieldNames(root.path("examples")));
        projection.set("capabilityRowFields", firstArrayObjectFieldNames(root.path("capabilities")));
        projection.set("datasetRowFields", firstArrayObjectFieldNames(root.path("datasets")));
        return projection;
    }

    private ObjectNode projectAskEnvelope(JsonNode root) {
        ObjectNode projection = objectMapper.createObjectNode();
        projection.set("rootFields", sortedFieldNames(root));
        projection.set("refusalFields", sortedFieldNames(root.path("refusal")));
        projection.set("refusalDetailKeys", sortedFieldNames(root.path("refusal").path("details")));
        projection.set("parsedRequestFields", sortedFieldNames(root.path("parsedRequest")));
        projection.set("parsedRequestFilterKeys", sortedFieldNames(root.path("parsedRequest").path("filters")));
        projection.set("datasetSelectionFields", sortedFieldNames(root.path("datasetSelection")));
        projection.set("debugFields", sortedFieldNames(root.path("debug")));
        projection.set("citationFields", firstArrayObjectFieldNames(root.path("citations")));
        JsonNode firstCitation = root.path("citations").isArray() && !root.path("citations").isEmpty()
            ? root.path("citations").get(0)
            : objectMapper.nullNode();
        projection.set("citationPeriodFields", sortedFieldNames(firstCitation.path("period")));
        return projection;
    }

    private ArrayNode sortedFieldNames(JsonNode node) {
        ArrayNode array = objectMapper.createArrayNode();
        if (node == null || !node.isObject()) {
            return array;
        }
        new TreeSet<>(toFieldNameList(node)).forEach(array::add);
        return array;
    }

    private ArrayNode firstArrayObjectFieldNames(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty() || !node.get(0).isObject()) {
            return objectMapper.createArrayNode();
        }
        return sortedFieldNames(node.get(0));
    }

    private List<String> toFieldNameList(JsonNode node) {
        return node == null || !node.isObject()
            ? List.of()
            : node.properties().stream()
                .map(Map.Entry::getKey)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
