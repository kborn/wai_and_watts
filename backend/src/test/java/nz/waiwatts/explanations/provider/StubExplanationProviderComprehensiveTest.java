package nz.waiwatts.explanations.provider;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.dto.TimeSeriesFact;
import nz.waiwatts.explanations.dto.MetricFact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional comprehensive tests for StubExplanationProvider
 */
class StubExplanationProviderComprehensiveTest {

    private StubExplanationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new StubExplanationProvider();
    }

    @Test
    void testCitationValidationWithPartialCitationMatch() {
        FactPack factPack = createTimeSeriesFactPack();
        
        // Create explanation with partial citation (contains ID but not exact match)
        Explanation explanation = new Explanation(
            "Some explanation text",
            List.of("partial:ts:mbie:renewable_generation_gwh:2018_2024:extra")
        );
        
        // Should pass validation (contains the required ID)
        assertTrue(provider.validateCitations(explanation, factPack));
    }

    @Test
    void testCitationValidationWithMultipleRequiredCitations() {
        FactPack factPack = createMetricFactPack();
        
        // Create explanation with only one of three required citations
        Explanation partialExplanation = new Explanation(
            "Some explanation text",
            List.of("metric:mbie:generation_gwh:2023:HYDRO")
        );
        
        // Should fail validation
        assertFalse(provider.validateCitations(partialExplanation, factPack));
        
        // Create explanation with all required citations
        Explanation completeExplanation = new Explanation(
            "Some explanation text",
            List.of(
                "metric:mbie:generation_gwh:2023:HYDRO",
                "metric:mbie:generation_gwh:2023:WIND", 
                "metric:mbie:generation_gwh:2023:GEOTHERMAL"
            )
        );
        
        // Should pass validation
        assertTrue(provider.validateCitations(completeExplanation, factPack));
    }

    @Test
    void testRefusalForInsufficientData() {
        // Create Fact Pack with required citations but no facts
        FactPack emptyFactPack = new FactPack();
        var requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType("renewable_generation_trend");
        emptyFactPack.setRequestContext(requestContext);
        emptyFactPack.getGuardrails().setAllowedClaims(List.of("trend_increase"));
        emptyFactPack.getGuardrails().setRequiredCitations(List.of("ts:mbie:renewable_generation_gwh:2018_2024"));
        // No facts added
        
        Explanation explanation = provider.generateExplanation("renewable_generation_trend", emptyFactPack);
        
        // Should be a refusal due to insufficient data
        assertTrue(explanation.isRefusal());
        assertEquals("Insufficient data to answer the question", explanation.getRefusalReason());
    }

    @Test
    void testDeterministicExplanationForSameInput() {
        FactPack factPack = createTimeSeriesFactPack();
        
        // Generate explanation twice
        Explanation explanation1 = provider.generateExplanation("renewable_generation_trend", factPack);
        Explanation explanation2 = provider.generateExplanation("renewable_generation_trend", factPack);
        
        // Should be identical (deterministic)
        assertEquals(explanation1.getExplanationText(), explanation2.getExplanationText());
        assertEquals(explanation1.getCitations(), explanation2.getCitations());
        assertEquals(explanation1.isRefusal(), explanation2.isRefusal());
    }

    @Test
    void testExplanationForHydroGenerationTrend() {
        FactPack factPack = createHydroFactPack();
        
        Explanation explanation = provider.generateExplanation("hydro_generation_trend", factPack);
        
        // Should not be a refusal
        assertFalse(explanation.isRefusal());
        
        // Should contain comparison data
        assertTrue(explanation.getExplanationText().contains("change of"));
        assertTrue(explanation.getExplanationText().contains("GWh"));
        assertTrue(explanation.getExplanationText().contains("%"));
        
        // Should have citation
        assertNotNull(explanation.getCitations());
        assertFalse(explanation.getCitations().isEmpty());
        
        // Should validate citations
        assertTrue(provider.validateCitations(explanation, factPack));
    }

    @Test
    void testExplanationForFuelTypeComparison() {
        FactPack factPack = createMetricFactPack();
        
        Explanation explanation = provider.generateExplanation("fuel_type_comparison", factPack);
        
        // Should not be a refusal
        assertFalse(explanation.isRefusal());
        
        // Should identify the fuel type with highest generation
        assertTrue(explanation.getExplanationText().contains("highest electricity generation"));
        assertTrue(explanation.getExplanationText().contains("HYDRO")); // HYDRO has highest value
        
        // Should have citations for all fuel types
        assertEquals(3, explanation.getCitations().size());
        
        // Should validate citations
        assertTrue(provider.validateCitations(explanation, factPack));
    }

    @Test
    void testRefusalForUnknownQuestionType() {
        FactPack factPack = createTimeSeriesFactPack();
        
        Explanation explanation = provider.generateExplanation("unknown_question_type", factPack);
        
        // Should be a refusal
        assertTrue(explanation.isRefusal());
        assertEquals("Question type not supported in Phase 11: unknown_question_type", explanation.getRefusalReason());
    }

    private FactPack createHydroFactPack() {
        FactPack factPack = new FactPack();
        
        var requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType("hydro_generation_trend");
        factPack.setRequestContext(requestContext);
        
        // Create comparison fact
        var comparison = new nz.waiwatts.explanations.dto.ComparisonFact(
            "cmp:mbie:generation_gwh:HYDRO:2023_vs_2022",
            "generation_gwh",
            "2022",
            "2023",
            new BigDecimal("1000"), // +1000 GWh
            new BigDecimal("4.0"),  // +4%
            "GWh",
            Map.of("fuel_type", "HYDRO")
        );
        
        factPack.getFacts().getComparisons().add(comparison);
        
        // Set guardrails
        factPack.getGuardrails().setAllowedClaims(List.of("trend_increase", "trend_decrease"));
        factPack.getGuardrails().setRequiredCitations(List.of(comparison.getId()));
        
        return factPack;
    }

    private FactPack createTimeSeriesFactPack() {
        FactPack factPack = new FactPack();
        
        var requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType("renewable_generation_trend");
        factPack.setRequestContext(requestContext);
        
        TimeSeriesFact timeSeries = new TimeSeriesFact(
            "ts:mbie:renewable_generation_gwh:2018_2024",
            "renewable_generation_gwh",
            "GWh",
            Map.of("scope", "NZ")
        );
        
        timeSeries.setPoints(List.of(
            new TimeSeriesFact.DataPoint("2018", new BigDecimal("40000")),
            new TimeSeriesFact.DataPoint("2024", new BigDecimal("45000"))
        ));
        
        factPack.getFacts().getTimeSeries().add(timeSeries);
        
        factPack.getGuardrails().setAllowedClaims(List.of("trend_increase", "trend_decrease"));
        factPack.getGuardrails().setRequiredCitations(List.of(timeSeries.getId()));
        
        return factPack;
    }

    private FactPack createMetricFactPack() {
        FactPack factPack = new FactPack();
        
        var requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType("fuel_type_comparison");
        factPack.setRequestContext(requestContext);
        
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
        
        factPack.getGuardrails().setAllowedClaims(List.of("comparison", "largest_contributor"));
        factPack.getGuardrails().setRequiredCitations(List.of(
            "metric:mbie:generation_gwh:2023:HYDRO",
            "metric:mbie:generation_gwh:2023:WIND",
            "metric:mbie:generation_gwh:2023:GEOTHERMAL"
        ));
        
        return factPack;
    }
}