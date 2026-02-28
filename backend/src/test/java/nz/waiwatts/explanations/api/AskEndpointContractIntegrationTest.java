package nz.waiwatts.explanations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nz.waiwatts.config.RequestCorrelationFilter;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.service.DatasetSelectionService;
import nz.waiwatts.explanations.service.RequestValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Autowired
    private RequestCorrelationFilter requestCorrelationFilter;

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
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(requestCorrelationFilter)
            .build();
        objectMapper = new ObjectMapper();
        meterRegistry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(meterRegistry);
    }

    @AfterEach
    void tearDown() {
        Metrics.globalRegistry.remove(meterRegistry);
        meterRegistry.close();
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

    @Test
    void askFlowCarriesRequestIdThroughHeadersAndServiceInvocation() throws Exception {
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

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        when(explanationService.generateExplanation(any())).thenAnswer(invocation -> {
            capturedRequestId.set(MDC.get("requestId"));
            return new Explanation(
                "Renewable generation increased over the selected period.",
                List.of("ts:mbie:renewable_generation_gwh:2020_2023")
            );
        });

        MvcResult result = mockMvc.perform(post("/api/v1/explanations/ask")
                .header("Request-Id", "obs-1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Explain renewable generation trends between 2020 and 2023\"}"))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("obs-1234", result.getResponse().getHeader("Request-Id"));
        assertEquals("obs-1234", result.getResponse().getHeader("X-Request-Id"));
        assertEquals("obs-1234", capturedRequestId.get());
    }

    @Test
    void askSuccessPathRecordsStageCountersAndTimers() throws Exception {
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

        mockMvc.perform(post("/api/v1/explanations/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Explain renewable generation trends between 2020 and 2023\"}"))
            .andExpect(status().isOk());

        assertEquals(1.0, meterRegistry.get("waiwatts.ask.stage.count").tag("stage", "parse").counter().count());
        assertEquals(1.0, meterRegistry.get("waiwatts.ask.stage.count").tag("stage", "selection").counter().count());
        assertEquals(1.0, meterRegistry.get("waiwatts.ask.stage.count").tag("stage", "validation").counter().count());
        assertEquals(1.0, meterRegistry.get("waiwatts.ask.stage.count").tag("stage", "explanation").counter().count());

        assertEquals(1L, meterRegistry.get("waiwatts.ask.stage.duration").tag("stage", "parse").timer().count());
        assertEquals(1L, meterRegistry.get("waiwatts.ask.stage.duration").tag("stage", "selection").timer().count());
        assertEquals(1L, meterRegistry.get("waiwatts.ask.stage.duration").tag("stage", "validation").timer().count());
        assertEquals(1L, meterRegistry.get("waiwatts.ask.stage.duration").tag("stage", "explanation").timer().count());
        assertEquals(1.0, meterRegistry.get("waiwatts.ask.success.count").counter().count());
    }

    @Test
    void parseRefusalRecordsRefusalCounterAndGeneratedRequestId() throws Exception {
        IntentParseResponse parseRefusal = IntentParseResponse.refusal(
            "UNSUPPORTED_INTENT",
            "I can't confidently map this question."
        );
        parseRefusal.setParserUsed("OPENAI");
        when(intentParserService.parseQuestion(any())).thenReturn(parseRefusal);

        MvcResult result = mockMvc.perform(post("/api/v1/explanations/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"What should I invest in?\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String requestId = result.getResponse().getHeader("Request-Id");
        assertNotNull(requestId);
        assertFalse(requestId.isBlank());
        assertEquals(
            1.0,
            meterRegistry.get("waiwatts.ask.refusal.count")
                .tags("stage", "parse", "code", "UNSUPPORTED_CAPABILITY")
                .counter()
                .count()
        );
    }
}
