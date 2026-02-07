package nz.waiwatts.explanations.provider;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.dto.TimeSeriesFact;
import nz.waiwatts.explanations.dto.MetricFact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test citation validation for StubExplanationProvider
 */
class StubExplanationProviderTest {

    private StubExplanationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new StubExplanationProvider();
    }

    @Test
    void testCitationValidationForTimeSeriesExplanation() {
        // Create Fact Pack with time series
        FactPack factPack = createTimeSeriesFactPack();
        
        // Generate explanation
        Explanation explanation = provider.generateExplanation("renewable_generation_trend", factPack);
        
        // Should not be a refusal
        assertFalse(explanation.isRefusal());
        
        // Should have citations
        assertNotNull(explanation.getCitations());
        assertFalse(explanation.getCitations().isEmpty());
        
        // Citation should contain the time series ID
        assertTrue(explanation.getCitations().getFirst().contains("ts:mbie:renewable_generation_gwh:2018_2024"));

        // Validate citations passes
        assertTrue(provider.validateCitations(explanation, factPack));
    }

    @Test
    void testCitationValidationForMetricExplanation() {
        // Create Fact Pack with metrics
        FactPack factPack = createMetricFactPack();
        
        // Generate explanation
        Explanation explanation = provider.generateExplanation("fuel_type_comparison", factPack);
        
        // Should not be a refusal
        assertFalse(explanation.isRefusal());
        
        // Should have citations for all metrics
        assertNotNull(explanation.getCitations());
        assertEquals(3, explanation.getCitations().size()); // HYDRO, WIND, GEOTHERMAL
        
        // Validate citations passes
        assertTrue(provider.validateCitations(explanation, factPack));
    }

    @Test
    void testCitationValidationFailsForMissingCitations() {
        // Create Fact Pack with required citations
        FactPack factPack = createTimeSeriesFactPack();
        
        // Create explanation without citations
        Explanation explanation = new Explanation("Some explanation text", List.of());
        
        // Validate citations should fail
        assertFalse(provider.validateCitations(explanation, factPack));
    }

    @Test
    void testRefusalForUnsupportedQuestionType() {
        // Create Fact Pack with empty guardrails (unsupported)
        FactPack factPack = new FactPack();
        var requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType("unsupported_question");
        factPack.setRequestContext(requestContext);
        factPack.getGuardrails().setAllowedClaims(List.of()); // Empty = unsupported
        
        // Generate explanation
        Explanation explanation = provider.generateExplanation("unsupported_question", factPack);
        
        // Should be a refusal
        assertTrue(explanation.isRefusal());
        assertEquals("Unsupported question type: unsupported_question", explanation.getRefusalReason());
    }

    private FactPack createTimeSeriesFactPack() {
        FactPack factPack = new FactPack();
        
        // Set request context
        var requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType("renewable_generation_trend");
        factPack.setRequestContext(requestContext);
        
        // Create time series fact
        TimeSeriesFact timeSeries = new TimeSeriesFact(
            "ts:mbie:renewable_generation_gwh:2018_2024",
            "renewable_generation_gwh",
            "GWh",
            Map.of("scope", "NZ")
        );
        
        // Add data points
        timeSeries.setPoints(new ArrayList<>(List.of(
            new TimeSeriesFact.DataPoint("2018", new BigDecimal("40000")),
            new TimeSeriesFact.DataPoint("2024", new BigDecimal("45000"))
        )));

        factPack.getFacts().getTimeSeries().add(timeSeries);
        
        // Set guardrails
        factPack.getGuardrails().setAllowedClaims(List.of("trend_increase", "trend_decrease"));
        factPack.getGuardrails().setRequiredCitations(List.of(timeSeries.getId()));
        
        return factPack;
    }

    private FactPack createMetricFactPack() {
        FactPack factPack = new FactPack();
        
        // Set request context
        var requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType("fuel_type_comparison");
        factPack.setRequestContext(requestContext);
        
        // Create metric facts
        factPack.getFacts().getMetrics().add(new MetricFact(
            "metric:mbie:generation_gwh:2023:HYDRO",
            "generation_gwh",
            new BigDecimal("25000"),
            "GWh",
            "2023",
            Map.of("fuel_type", "HYDRO")
        ));
        
        factPack.getFacts().getMetrics().add(new MetricFact(
            "metric:mbie:generation_gwh:2023:WIND",
            "generation_gwh",
            new BigDecimal("6000"),
            "GWh",
            "2023",
            Map.of("fuel_type", "WIND")
        ));
        
        factPack.getFacts().getMetrics().add(new MetricFact(
            "metric:mbie:generation_gwh:2023:GEOTHERMAL",
            "generation_gwh",
            new BigDecimal("8000"),
            "GWh",
            "2023",
            Map.of("fuel_type", "GEOTHERMAL")
        ));
        
        // Set guardrails
        factPack.getGuardrails().setAllowedClaims(List.of("comparison", "largest_contributor"));
        factPack.getGuardrails().setRequiredCitations(List.of(
            "metric:mbie:generation_gwh:2023:HYDRO",
            "metric:mbie:generation_gwh:2023:WIND",
            "metric:mbie:generation_gwh:2023:GEOTHERMAL"
        ));
        
        return factPack;
    }
}