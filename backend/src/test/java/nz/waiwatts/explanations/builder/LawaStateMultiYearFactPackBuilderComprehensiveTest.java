package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Additional comprehensive tests for LawaStateMultiYearFactPackBuilder
 */
@ExtendWith(MockitoExtension.class)
class LawaStateMultiYearFactPackBuilderComprehensiveTest {

    @Mock
    private LawaStateMultiYearRecordRepository repository;

    private LawaStateMultiYearFactPackBuilder builder;

    @BeforeEach
    void setUp() {
        repository = mock(LawaStateMultiYearRecordRepository.class);
        builder = new LawaStateMultiYearFactPackBuilder(repository);
    }

    @Test
    void testCanHandle_WithStateDatasetSource_ReturnsTrue() {
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_overview");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.state.multi_year"));

        assertTrue(builder.canHandle(request));
    }

    @Test
    void testCanHandle_WithTrendDatasetSource_ReturnsFalse() {
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_trends");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        assertFalse(builder.canHandle(request));
    }

    @Test
    void testGetSupportedDatasetSourceCode_ReturnsCorrectCode() {
        assertEquals("lawa.water_quality.state.multi_year", builder.getSupportedDatasetSourceCode());
    }

    @Test
    void testBuildFactPack_EmptyRecords_ReturnsBasicFactsWithEmptyGuardrails() {
        // Setup empty repository
        when(repository.findAll()).thenReturn(List.of());

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_overview");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.state.multi_year"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify empty facts but valid structure
        assertTrue(factPack.getFacts().getClassifications().isEmpty());
        assertTrue(factPack.getFacts().getMetrics().isEmpty());
        assertTrue(factPack.getFacts().getTimeSeries().isEmpty());
        
        // Verify guardrails for empty case - should have empty allowed claims
        assertTrue(factPack.getGuardrails().getAllowedClaims().isEmpty());
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("forecast"));
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("site_specific_advice"));
    }

    @Test
    void testBuildFactPack_WaterQualityOverview_CreatesClassificationsAndMetrics() {
        // Setup test data with various state classifications
        DatasetRelease release = createDatasetRelease();

        LawaStateMultiYearRecord record1 = new LawaStateMultiYearRecord();
        record1.setLawaSiteId("SITE001");
        record1.setSiteName("Site SITE001");
        record1.setRegion("Canterbury");
        record1.setAttributeBand("A");
        record1.setStateNorm("EXCELLENT");
        record1.setPeriodType("HYDRO_NYR_WINDOW");
        record1.setPeriodStartYear(2019);
        record1.setPeriodEndYear(2023);
        record1.setDatasetRelease(release);

        LawaStateMultiYearRecord record2 = new LawaStateMultiYearRecord();
        record2.setLawaSiteId("SITE002");
        record2.setSiteName("Site SITE002");
        record2.setRegion("Waikato");
        record2.setAttributeBand("B");
        record2.setStateNorm("GOOD");
        record2.setPeriodType("HYDRO_NYR_WINDOW");
        record2.setPeriodStartYear(2018);
        record2.setPeriodEndYear(2022);
        record2.setDatasetRelease(release);

        LawaStateMultiYearRecord record3 = new LawaStateMultiYearRecord();
        record3.setLawaSiteId("SITE003");
        record3.setSiteName("Site SITE003");
        record3.setRegion("Otago");
        record3.setAttributeBand("D");
        record3.setStateNorm("POOR");
        record3.setPeriodType("HYDRO_NYR_WINDOW");
        record3.setPeriodStartYear(2017);
        record3.setPeriodEndYear(2021);
        record3.setDatasetRelease(release);

        LawaStateMultiYearRecord record4 = new LawaStateMultiYearRecord();
        record4.setLawaSiteId("SITE004");
        record4.setSiteName("Site SITE004");
        record4.setRegion("Canterbury");
        record4.setAttributeBand("A");
        record4.setStateNorm("EXCELLENT");
        record4.setPeriodType("HYDRO_NYR_WINDOW");
        record4.setPeriodStartYear(2020);
        record4.setPeriodEndYear(2024);
        record4.setDatasetRelease(release);

        // Use four records (two Excellent/A, one Good/B, one Poor/D)
        when(repository.findAll()).thenReturn(List.of(record1, record2, record3, record4));

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_overview");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.state.multi_year"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify structure
        assertNotNull(factPack);
        assertEquals("water_quality_overview", factPack.getRequestContext().getQuestionType());
        assertEquals(List.of("lawa.water_quality.state.multi_year"), factPack.getRequestContext().getDatasetScope());
        
        // Verify classifications - should have 3 unique states (A, B, D)
        var classifications = factPack.getFacts().getClassifications();
        assertEquals(3, classifications.size()); // 3 unique states (A, B, D)
        
        // Verify percentage metrics
        var metrics = factPack.getFacts().getMetrics();
        var excellentMetric = metrics.stream()
            .filter(m -> m.getId().contains("excellent"))
            .findFirst()
            .orElse(null);
        var poorMetric = metrics.stream()
            .filter(m -> m.getId().contains("poor"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(excellentMetric);
        assertNotNull(poorMetric);
        assertEquals(new BigDecimal("50.00"), excellentMetric.getValue()); // 2/4 = 50%
        assertEquals(new BigDecimal("25.00"), poorMetric.getValue()); // 1/4 = 25%
    }

    @Test
    void testBuildFactPack_ExcellentSitesTrend_CreatesTimeSeriesAndComparison() {
        // Setup test data across multiple years
        DatasetRelease release = createDatasetRelease();

        LawaStateMultiYearRecord record1 = new LawaStateMultiYearRecord();
        record1.setLawaSiteId("SITE001");
        record1.setSiteName("Site SITE001");
        record1.setRegion("Canterbury");
        record1.setAttributeBand("A");
        record1.setStateNorm("EXCELLENT");
        record1.setPeriodType("HYDRO_NYR_WINDOW");
        record1.setPeriodStartYear(2019);
        record1.setPeriodEndYear(2023);
        record1.setDatasetRelease(release);

        LawaStateMultiYearRecord record2 = new LawaStateMultiYearRecord();
        record2.setLawaSiteId("SITE002");
        record2.setSiteName("Site SITE002");
        record2.setRegion("Waikato");
        record2.setAttributeBand("A");
        record2.setStateNorm("EXCELLENT");
        record2.setPeriodType("HYDRO_NYR_WINDOW");
        record2.setPeriodStartYear(2020);
        record2.setPeriodEndYear(2024);
        record2.setDatasetRelease(release);

        LawaStateMultiYearRecord record3 = new LawaStateMultiYearRecord();
        record3.setLawaSiteId("SITE003");
        record3.setSiteName("Site SITE003");
        record3.setRegion("Otago");
        record3.setAttributeBand("A");
        record3.setStateNorm("EXCELLENT");
        record3.setPeriodType("HYDRO_NYR_WINDOW");
        record3.setPeriodStartYear(2017);
        record3.setPeriodEndYear(2021);
        record3.setDatasetRelease(release);

        when(repository.findAll()).thenReturn(List.of(record1, record2, record3));

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("excellent_sites_trend");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.state.multi_year"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify time series
        var timeSeriesList = factPack.getFacts().getTimeSeries();
        assertEquals(1, timeSeriesList.size());
        
        var timeSeries = timeSeriesList.getFirst();
        assertNotNull(timeSeries);
        assertEquals("excellent_sites_count", timeSeries.getMetricName());
        assertEquals("sites", timeSeries.getUnit());
        assertEquals(3, timeSeries.getPoints().size()); // 2021, 2023, 2024 years
        
        // Verify comparison
        var comparisons = factPack.getFacts().getComparisons();
        assertEquals(1, comparisons.size()); // Latest vs previous period
        
        var comparison = comparisons.getFirst();
        assertNotNull(comparison);
        assertEquals("2023", comparison.getBaselinePeriod());
        assertEquals("2024", comparison.getComparisonPeriod());
        assertEquals(BigDecimal.ZERO, comparison.getDeltaAbsolute()); // No change (1 vs 1)
        assertEquals(new BigDecimal("0.00"), comparison.getDeltaPercent()); // 0%
    }

    @Test
    void testBuildFactPack_RegionalWaterQuality_CreatesRegionalClassificationsAndMetrics() {
        // Setup test data with regional distribution
        DatasetRelease release = createDatasetRelease();

        LawaStateMultiYearRecord record1 = new LawaStateMultiYearRecord();
        record1.setLawaSiteId("SITE001");
        record1.setSiteName("Site SITE001");
        record1.setRegion("Canterbury");
        record1.setAttributeBand("A");
        record1.setStateNorm("EXCELLENT");
        record1.setPeriodType("HYDRO_NYR_WINDOW");
        record1.setPeriodStartYear(2019);
        record1.setPeriodEndYear(2023);
        record1.setDatasetRelease(release);

        LawaStateMultiYearRecord record2 = new LawaStateMultiYearRecord();
        record2.setLawaSiteId("SITE002");
        record2.setSiteName("Site SITE002");
        record2.setRegion("Canterbury");
        record2.setAttributeBand("C");
        record2.setStateNorm("FAIR");
        record2.setPeriodType("HYDRO_NYR_WINDOW");
        record2.setPeriodStartYear(2018);
        record2.setPeriodEndYear(2022);
        record2.setDatasetRelease(release);

        LawaStateMultiYearRecord record3 = new LawaStateMultiYearRecord();
        record3.setLawaSiteId("SITE003");
        record3.setSiteName("Site SITE003");
        record3.setRegion("Waikato");
        record3.setAttributeBand("A");
        record3.setStateNorm("EXCELLENT");
        record3.setPeriodType("HYDRO_NYR_WINDOW");
        record3.setPeriodStartYear(2020);
        record3.setPeriodEndYear(2024);
        record3.setDatasetRelease(release);

        when(repository.findAll()).thenReturn(List.of(record1, record2, record3));

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("regional_water_quality");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.state.multi_year"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify regional classifications
        var classifications = factPack.getFacts().getClassifications();
        var canterburyClassifications = classifications.stream()
            .filter(c -> "Canterbury".equals(c.getDimensions().get("region")))
            .toList();
        var waikatoClassifications = classifications.stream()
            .filter(c -> "Waikato".equals(c.getDimensions().get("region")))
            .toList();
        
        assertEquals(3, classifications.size()); // Aggregated by state per region: Canterbury(A,C), Waikato(A)
        assertEquals(2, canterburyClassifications.size()); // Canterbury: A, C
        assertEquals(1, waikatoClassifications.size()); // Waikato: A
        
        // Verify regional percentage metrics
        var metrics = factPack.getFacts().getMetrics();
        var canterburyMetric = metrics.stream()
            .filter(m -> m.getId().contains("Canterbury") && m.getId().contains("excellent"))
            .findFirst()
            .orElse(null);
        var waikatoMetric = metrics.stream()
            .filter(m -> m.getId().contains("Waikato") && m.getId().contains("excellent"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(canterburyMetric);
        assertNotNull(waikatoMetric);
        assertEquals(new BigDecimal("50.00"), canterburyMetric.getValue()); // 1/2 = 50%
        assertEquals(new BigDecimal("100.00"), waikatoMetric.getValue()); // 1/1 = 100%
    }

    @Test
    void testBuildFactPack_WithTimeRangeFilters_AppliesCorrectly() {
        // Setup test data spanning multiple years
        DatasetRelease release = createDatasetRelease();

        LawaStateMultiYearRecord record_included1 = new LawaStateMultiYearRecord();
        record_included1.setLawaSiteId("SITE001");
        record_included1.setSiteName("Site SITE001");
        record_included1.setRegion("Canterbury");
        record_included1.setAttributeBand("A");
        record_included1.setStateNorm("EXCELLENT");
        record_included1.setPeriodType("HYDRO_NYR_WINDOW");
        record_included1.setPeriodStartYear(2020);
        record_included1.setPeriodEndYear(2024);
        record_included1.setDatasetRelease(release);

        LawaStateMultiYearRecord record_included2 = new LawaStateMultiYearRecord();
        record_included2.setLawaSiteId("SITE002");
        record_included2.setSiteName("Site SITE002");
        record_included2.setRegion("Waikato");
        record_included2.setAttributeBand("A");
        record_included2.setStateNorm("EXCELLENT");
        record_included2.setPeriodType("HYDRO_NYR_WINDOW");
        record_included2.setPeriodStartYear(2021);
        record_included2.setPeriodEndYear(2023);
        record_included2.setDatasetRelease(release);

        LawaStateMultiYearRecord record_included3 = new LawaStateMultiYearRecord();
        record_included3.setLawaSiteId("SITE003");
        record_included3.setSiteName("Site SITE003");
        record_included3.setRegion("Otago");
        record_included3.setAttributeBand("A");
        record_included3.setStateNorm("EXCELLENT");
        record_included3.setPeriodType("HYDRO_NYR_WINDOW");
        record_included3.setPeriodStartYear(2022);
        record_included3.setPeriodEndYear(2024);
        record_included3.setDatasetRelease(release);

        // Record that should be filtered out (before 2020)
        LawaStateMultiYearRecord record_excluded = new LawaStateMultiYearRecord();
        record_excluded.setLawaSiteId("SITE004");
        record_excluded.setSiteName("Site SITE004");
        record_excluded.setRegion("Canterbury");
        record_excluded.setAttributeBand("A");
        record_excluded.setStateNorm("EXCELLENT");
        record_excluded.setPeriodType("HYDRO_NYR_WINDOW");
        record_excluded.setPeriodStartYear(2015);
        record_excluded.setPeriodEndYear(2019);
        record_excluded.setDatasetRelease(release);

        when(repository.findAll()).thenReturn(List.of(record_included1, record_included2, record_included3, record_excluded));

        // Create request with time range filter
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("excellent_sites_trend");
        request.setFilters(Map.of(
            "datasetSource", "lawa.water_quality.state.multi_year",
            "startYear", 2020,
            "endYear", 2024
        ));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify only included records are processed
        var timeSeries = factPack.getFacts().getTimeSeries().getFirst();
        var points = timeSeries.getPoints();
        
        assertEquals(2, points.size()); // Distinct end years within range: 2023, 2024
        assertTrue(points.stream().allMatch(p -> Integer.parseInt(p.getPeriod()) >= 2020 && Integer.parseInt(p.getPeriod()) <= 2024));
        
        // Verify period coverage in provenance
        var provenance = factPack.getProvenance().getDatasetSources().getFirst();
        assertTrue(provenance.getPeriodCoverage().contains("2020"));
        assertTrue(provenance.getPeriodCoverage().contains("2024"));
    }

    @Test
    void testBuildFactPack_UnsupportedQuestionType_ReturnsBasicFacts() {
        // Setup test data
        DatasetRelease release = createDatasetRelease();

        LawaStateMultiYearRecord record = new LawaStateMultiYearRecord();
        record.setLawaSiteId("SITE001");
        record.setSiteName("Site SITE001");
        record.setRegion("Canterbury");
        record.setAttributeBand("A");
        record.setStateNorm("EXCELLENT");
        record.setPeriodType("HYDRO_NYR_WINDOW");
        record.setPeriodStartYear(2019);
        record.setPeriodEndYear(2023);
        record.setDatasetRelease(release);

        when(repository.findAll()).thenReturn(List.of(record));

        // Create request with unsupported question type
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("unsupported_question");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.state.multi_year"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify basic facts are created
        var metrics = factPack.getFacts().getMetrics();
        assertEquals(2, metrics.size()); // total_records + unique_sites
        
        var totalMetric = metrics.stream()
            .filter(m -> m.getId().contains("total_state_records"))
            .findFirst()
            .orElse(null);
        var uniqueSitesMetric = metrics.stream()
            .filter(m -> m.getId().contains("unique_sites"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(totalMetric);
        assertNotNull(uniqueSitesMetric);
        assertEquals(BigDecimal.ONE, totalMetric.getValue());
        assertEquals(BigDecimal.ONE, uniqueSitesMetric.getValue());
        
        // Verify empty guardrails for unsupported question
        assertTrue(factPack.getGuardrails().getAllowedClaims().isEmpty());
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("forecast"));
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("site_specific_advice"));
    }

    @Test
    void testDeterminism_SameInput_ProducesIdenticalOutput() {
        // Setup test data
        DatasetRelease release = createDatasetRelease();

        LawaStateMultiYearRecord record = new LawaStateMultiYearRecord();
        record.setLawaSiteId("SITE001");
        record.setSiteName("Site SITE001");
        record.setRegion("Canterbury");
        record.setAttributeBand("A");
        record.setStateNorm("EXCELLENT");
        record.setPeriodType("HYDRO_NYR_WINDOW");
        record.setPeriodStartYear(2019);
        record.setPeriodEndYear(2023);
        record.setDatasetRelease(release);

        when(repository.findAll()).thenReturn(List.of(record));

        // Create identical requests
        ExplanationRequest request1 = new ExplanationRequest();
        request1.setQuestionType("water_quality_overview");
        request1.setFilters(Map.of("datasetSource", "lawa.water_quality.state.multi_year"));

        ExplanationRequest request2 = new ExplanationRequest();
        request2.setQuestionType("water_quality_overview");
        request2.setFilters(Map.of("datasetSource", "lawa.water_quality.state.multi_year"));

        // Build fact packs
        FactPack factPack1 = builder.buildFactPack(request1);
        FactPack factPack2 = builder.buildFactPack(request2);

        // Verify determinism - same structure and values
        assertEquals(factPack1.getRequestContext().getQuestionType(), factPack2.getRequestContext().getQuestionType());
        assertEquals(factPack1.getRequestContext().getDatasetScope(), factPack2.getRequestContext().getDatasetScope());
        
        assertEquals(factPack1.getFacts().getClassifications().size(), factPack2.getFacts().getClassifications().size());
        if (!factPack1.getFacts().getClassifications().isEmpty()) {
            var class1 = factPack1.getFacts().getClassifications().getFirst();
            var class2 = factPack2.getFacts().getClassifications().getFirst();
            assertEquals(class1.getId(), class2.getId());
            assertEquals(class1.getClassification(), class2.getClassification());
        }
    }

    private DatasetRelease createDatasetRelease() {
        DatasetRelease release = new DatasetRelease();
        release.setContentHash("sha256:lawa123def456");
        release.setPublishedDate(LocalDate.now());
        return release;
    }
}