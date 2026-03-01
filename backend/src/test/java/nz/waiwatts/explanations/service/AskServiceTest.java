package nz.waiwatts.explanations.service;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("contract")
class AskServiceTest {

    private IntentParserService intentParserService;
    private RequestValidationService validationService;
    private DatasetSelectionService datasetSelectionService;
    private ExplanationService explanationService;
    private AskService askService;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        intentParserService = mock(IntentParserService.class);
        validationService = mock(RequestValidationService.class);
        datasetSelectionService = mock(DatasetSelectionService.class);
        explanationService = mock(ExplanationService.class);
        askService = new AskService(
            intentParserService,
            validationService,
            datasetSelectionService,
            explanationService,
            new CapabilityRegistry(new DatasetCatalog()),
            new CitationMapper(),
            new AskRefusalMapper()
        );
        meterRegistry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(meterRegistry);
    }

    @AfterEach
    void tearDown() {
        Metrics.globalRegistry.remove(meterRegistry);
        meterRegistry.close();
    }

    @Test
    void blankQuestionReturnsBadRequestValidationRefusal() {
        AskService.AskResponse response = askService.ask(" ");

        assertEquals(HttpStatus.BAD_REQUEST, response.httpStatus());
        assertTrue(response.result().isRefusal());
        assertEquals("VALIDATION_FAILED", response.result().getRefusal().getCode());
        assertEquals("Question is required", response.result().getRefusal().getMessage());
        assertEquals("REQUEST", response.result().getDebug().getRefusalTrigger());
    }

    @Test
    void parseRefusalUsesCentralCapabilityMapping() {
        IntentParseResponse parseRefusal = IntentParseResponse.refusal(
            "UNSUPPORTED_INTENT",
            "I can't confidently map this question."
        );
        parseRefusal.setParserUsed("OPENAI");
        when(intentParserService.parseQuestion(anyString())).thenReturn(parseRefusal);

        AskService.AskResponse response = askService.ask("What should I invest in?");

        assertEquals(HttpStatus.OK, response.httpStatus());
        assertTrue(response.result().isRefusal());
        assertEquals("UNSUPPORTED_CAPABILITY", response.result().getRefusal().getCode());
        assertEquals("PARSE", response.result().getDebug().getRefusalTrigger());
        assertEquals(
            1.0,
            meterRegistry.get("waiwatts.ask.refusal.count")
                .tag("stage", "parse")
                .tag("code", "UNSUPPORTED_CAPABILITY")
                .counter()
                .count()
        );
    }

    @Test
    void successPathReturnsMappedCitationsAndStageMetrics() {
        ExplanationRequest parsedRequest = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("startYear", 2020, "endYear", 2023)
        );
        IntentParseResponse parseSuccess = IntentParseResponse.success(parsedRequest);
        parseSuccess.setParserUsed("OPENAI");
        when(intentParserService.parseQuestion(anyString())).thenReturn(parseSuccess);
        when(datasetSelectionService.selectDataset(anyString(), any())).thenReturn(
            DatasetSelectionService.DatasetSelectionResult.selected(
                "mbie.generation.annual",
                "Parsed dataset retained.",
                DatasetSelectionService.DatasetSelectionStrategy.EXPLICIT
            )
        );
        when(validationService.validateRequest(any())).thenReturn(RequestValidationService.ValidationResult.success());
        when(explanationService.generateExplanation(any())).thenReturn(
            new Explanation(
                "Renewable generation increased over the selected period.",
                List.of("ts:mbie:renewable_generation_gwh:2020_2023")
            )
        );

        AskService.AskResponse response = askService.ask("Explain renewable generation trends between 2020 and 2023");

        assertEquals(HttpStatus.OK, response.httpStatus());
        assertFalse(response.result().isRefusal());
        assertEquals("mbie.generation.annual", response.result().getSelectedDatasetSource());
        assertEquals(1, response.result().getCitations().size());
        assertEquals("TIME_SERIES", response.result().getCitations().getFirst().getType());
        assertEquals(
            1.0,
            meterRegistry.get("waiwatts.ask.success.count").counter().count()
        );
        verify(explanationService).generateExplanation(any());
    }
}
