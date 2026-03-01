package nz.waiwatts.explanations.generator;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.dto.ClassificationFact;
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
 * Test citation validation for DemoExplanationGenerator
 */
class DemoExplanationGeneratorTest {

    private DemoExplanationGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DemoExplanationGenerator();
    }

    @Test
    void testCitationValidationForTimeSeriesExplanation() {
        // Create Fact Pack with time series
        FactPack factPack = createTimeSeriesFactPack();
        
        // Generate explanation
        Explanation explanation = generator.generateExplanation("renewable_generation_trend", factPack);
        
        // Should not be a refusal
        assertFalse(explanation.isRefusal());
        
        // Should have citations
        assertNotNull(explanation.getCitations());
        assertFalse(explanation.getCitations().isEmpty());
        
        // Citation should contain the time series ID
        assertTrue(explanation.getCitations().getFirst().contains("ts:mbie:renewable_generation_gwh:2018_2024"));

        // Validate citations passes
        assertTrue(generator.validateCitations(explanation, factPack));
    }

    @Test
    void testCitationValidationForMetricExplanation() {
        // Create Fact Pack with metrics
        FactPack factPack = createMetricFactPack();
        
        // Generate explanation
        Explanation explanation = generator.generateExplanation("fuel_type_comparison", factPack);
        
        // Should not be a refusal
        assertFalse(explanation.isRefusal());
        
        // Should have citations for all metrics
        assertNotNull(explanation.getCitations());
        assertEquals(3, explanation.getCitations().size()); // HYDRO, WIND, GEOTHERMAL
        
        // Validate citations passes
        assertTrue(generator.validateCitations(explanation, factPack));
    }

    @Test
    void testCitationValidationForGenerationMixOverview() {
        FactPack factPack = createMetricFactPack();

        Explanation explanation = generator.generateExplanation("generation_mix_overview", factPack);

        assertFalse(explanation.isRefusal());
        assertNotNull(explanation.getCitations());
        assertEquals(3, explanation.getCitations().size());
        assertTrue(generator.validateCitations(explanation, factPack));
    }

    @Test
    void testCitationValidationFailsForMissingCitations() {
        // Create Fact Pack with required citations
        FactPack factPack = createTimeSeriesFactPack();
        
        // Create explanation without citations
        Explanation explanation = new Explanation("Some explanation text", List.of());
        
        // Validate citations should fail
        assertFalse(generator.validateCitations(explanation, factPack));
    }

    @Test
    void testGuidelineExceedanceSitesExplanation() {
        FactPack factPack = new FactPack();
        var requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType("guideline_exceedance_sites");
        factPack.setRequestContext(requestContext);
        factPack.getGuardrails().setAllowedClaims(List.of("site_list", "count"));
        factPack.getGuardrails().setRequiredCitations(List.of(
            "class:lawa:guideline_exceedance_site:2024:SITE001",
            "class:lawa:guideline_exceedance_site:2024:SITE002"
        ));
        factPack.getFacts().getMetrics().add(new MetricFact(
            "metric:lawa:guideline_exceedance_sites_count:2024",
            "guideline_exceedance_sites_count",
            new BigDecimal("2"),
            "sites",
            "2024",
            Map.of("scope", "latest_year")
        ));
        factPack.getFacts().getClassifications().add(new ClassificationFact(
            "class:lawa:guideline_exceedance_site:2024:SITE001",
            "site",
            "guideline_exceedance_site",
            "Site One",
            2024,
            2024,
            Map.of("region", "Auckland")
        ));
        factPack.getFacts().getClassifications().add(new ClassificationFact(
            "class:lawa:guideline_exceedance_site:2024:SITE002",
            "site",
            "guideline_exceedance_site",
            "Site Two",
            2024,
            2024,
            Map.of("region", "Auckland")
        ));

        Explanation explanation = generator.generateExplanation("guideline_exceedance_sites", factPack);

        assertFalse(explanation.isRefusal());
        assertTrue(explanation.getExplanationText().contains("2 sites exceeded the guideline in 2024"));
        assertEquals(2, explanation.getCitations().size());
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
        Explanation explanation = generator.generateExplanation("unsupported_question", factPack);
        
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
