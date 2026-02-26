package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Additional comprehensive tests for LawaTrendMultiYearFactPackBuilder
 */
@ExtendWith(MockitoExtension.class)
class LawaTrendMultiYearFactPackBuilderComprehensiveTest {

    @Mock
    private LawaTrendMultiYearRecordRepository repository;

    private LawaTrendMultiYearFactPackBuilder builder;

    @BeforeEach
    void setUp() {
        repository = mock(LawaTrendMultiYearRecordRepository.class);
        builder = new LawaTrendMultiYearFactPackBuilder(repository);
    }

    @Test
    void testCanHandle_WithTrendDatasetSource_ReturnsTrue() {
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_trends");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        assertTrue(builder.canHandle(request));
    }

    @Test
    void testCanHandle_WithStateDatasetSource_ReturnsFalse() {
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_overview");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.state.multi_year"));

        assertFalse(builder.canHandle(request));
    }

    @Test
    void testGetSupportedDatasetSourceCode_ReturnsCorrectCode() {
        assertEquals("lawa.water_quality.trend.multi_year", builder.getSupportedDatasetSourceCode());
    }

    @Test
    void testBuildFactPack_EmptyRecords_ReturnsBasicFactsWithEmptyGuardrails() {
        // Setup empty repository
        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of());

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_trends");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify empty facts but valid structure
        assertTrue(factPack.getFacts().getClassifications().isEmpty());
        assertTrue(factPack.getFacts().getMetrics().isEmpty());
        assertTrue(factPack.getFacts().getTimeSeries().isEmpty());
        
        // Verify empty guardrails for unsupported question
        assertTrue(factPack.getGuardrails().getAllowedClaims().isEmpty());
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("forecast"));
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("site_specific_advice"));
    }

    @Test
    void testBuildFactPack_WithIndicatorOnlyFilter_PassesIndicatorToRepository() {
        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of());

        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_trends");
        request.setFilters(Map.of(
            "datasetSource", "lawa.water_quality.trend.multi_year",
            "indicator", "Nitrogen"
        ));

        builder.buildFactPack(request);

        verify(repository).findForAsk(null, null, "nitrogen", null, null);
    }

    @Test
    void testBuildFactPack_WaterQualityTrends_CreatesClassificationsAndMetrics() {
        // Setup test data with various trend classifications
        DatasetRelease release = createDatasetRelease();

        LawaTrendMultiYearRecord record1 = new LawaTrendMultiYearRecord();
        record1.setLawaSiteId("SITE001");
        record1.setSiteName("Site SITE001");
        record1.setRegion("Canterbury");
        record1.setTrendNorm("IMPROVING");
        record1.setTrendScore(3);
        record1.setTrendPeriodYears(10);
        record1.setPeriodType("HYDRO_NYR_WINDOW");
        record1.setPeriodStartYear(2019);
        record1.setPeriodEndYear(2023);
        record1.setDatasetRelease(release);

        LawaTrendMultiYearRecord record2 = new LawaTrendMultiYearRecord();
        record2.setLawaSiteId("SITE002");
        record2.setSiteName("Site SITE002");
        record2.setRegion("Waikato");
        record2.setTrendNorm("DEGRADING");
        record2.setTrendScore(-2);
        record2.setTrendPeriodYears(10);
        record2.setPeriodType("HYDRO_NYR_WINDOW");
        record2.setPeriodStartYear(2018);
        record2.setPeriodEndYear(2022);
        record2.setDatasetRelease(release);

        LawaTrendMultiYearRecord record3 = new LawaTrendMultiYearRecord();
        record3.setLawaSiteId("SITE003");
        record3.setSiteName("Site SITE003");
        record3.setRegion("Otago");
        record3.setTrendNorm("IMPROVING");
        record3.setTrendScore(1);
        record3.setTrendPeriodYears(10);
        record3.setPeriodType("HYDRO_NYR_WINDOW");
        record3.setPeriodStartYear(2017);
        record3.setPeriodEndYear(2021);
        record3.setDatasetRelease(release);

        LawaTrendMultiYearRecord record4 = new LawaTrendMultiYearRecord();
        record4.setLawaSiteId("SITE004");
        record4.setSiteName("Site SITE004");
        record4.setRegion("Canterbury");
        record4.setTrendNorm("INSUFFICIENT_DATA");
        record4.setTrendScore(0);
        record4.setTrendPeriodYears(10);
        record4.setPeriodType("HYDRO_NYR_WINDOW");
        record4.setPeriodStartYear(2020);
        record4.setPeriodEndYear(2024);
        record4.setDatasetRelease(release);

        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of(record1, record2, record3, record4));

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_trends");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify structure
        assertNotNull(factPack);
        assertEquals("water_quality_trends", factPack.getRequestContext().getQuestionType());
        assertEquals(List.of("lawa.water_quality.trend.multi_year"), factPack.getRequestContext().getDatasetScope());
        
        // Verify classifications - should have 3 unique states
        var classifications = factPack.getFacts().getClassifications();
        assertEquals(3, classifications.size()); // IMPROVING, DEGRADING, INSUFFICIENT_DATA
        
        // Verify percentage metrics
        var metrics = factPack.getFacts().getMetrics();
        var improvingMetric = metrics.stream()
            .filter(m -> m.getId().contains("improving"))
            .findFirst()
            .orElse(null);
        var degradingMetric = metrics.stream()
            .filter(m -> m.getId().contains("degrading"))
            .findFirst()
            .orElse(null);
        var avgScoreMetric = metrics.stream()
            .filter(m -> m.getId().contains("average_trend_score"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(improvingMetric);
        assertNotNull(degradingMetric);
        assertNotNull(avgScoreMetric);
        assertEquals(new BigDecimal("50.00"), improvingMetric.getValue()); // 2/4 = 50%
        assertEquals(new BigDecimal("25.00"), degradingMetric.getValue()); // 1/4 = 25%
        assertEquals(new BigDecimal("0.50"), avgScoreMetric.getValue()); // (3 + (-2) + 1 + 0) / 4 = 0.5
    }

    @Test
    void testBuildFactPack_ImprovingSitesTrend_CreatesTimeSeriesAndComparison() {
        // Setup test data across multiple years for improving sites
        DatasetRelease release = createDatasetRelease();

        LawaTrendMultiYearRecord record1 = new LawaTrendMultiYearRecord();
        record1.setLawaSiteId("SITE001");
        record1.setSiteName("Site SITE001");
        record1.setRegion("Canterbury");
        record1.setTrendNorm("IMPROVING");
        record1.setTrendScore(3);
        record1.setTrendPeriodYears(10);
        record1.setPeriodType("HYDRO_NYR_WINDOW");
        record1.setPeriodStartYear(2019);
        record1.setPeriodEndYear(2023);
        record1.setDatasetRelease(release);

        LawaTrendMultiYearRecord record2 = new LawaTrendMultiYearRecord();
        record2.setLawaSiteId("SITE002");
        record2.setSiteName("Site SITE002");
        record2.setRegion("Waikato");
        record2.setTrendNorm("IMPROVING");
        record2.setTrendScore(2);
        record2.setTrendPeriodYears(10);
        record2.setPeriodType("HYDRO_NYR_WINDOW");
        record2.setPeriodStartYear(2020);
        record2.setPeriodEndYear(2024);
        record2.setDatasetRelease(release);

        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of(record1, record2));

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("improving_sites_trend");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify time series
        var timeSeries = factPack.getFacts().getTimeSeries().getFirst();
        assertNotNull(timeSeries);
        assertEquals("improving_sites_count", timeSeries.getMetricName());
        assertEquals("sites", timeSeries.getUnit());
        assertEquals(2, timeSeries.getPoints().size()); // 2023, 2024 years
        
        // Verify comparison
        var comparisons = factPack.getFacts().getComparisons();
        assertEquals(1, comparisons.size());
        
        var comparison = comparisons.getFirst();
        assertNotNull(comparison);
        assertEquals("2023", comparison.getBaselinePeriod());
        assertEquals("2024", comparison.getComparisonPeriod());
        assertEquals(BigDecimal.ZERO, comparison.getDeltaAbsolute()); // 1 - 1 = 0
        assertEquals(new BigDecimal("0.00"), comparison.getDeltaPercent()); // 0%
    }

    @Test
    void testBuildFactPack_RegionalTrendComparison_CreatesRegionalClassificationsAndMetrics() {
        // Setup test data with regional distribution
        DatasetRelease release = createDatasetRelease();

        LawaTrendMultiYearRecord record1 = new LawaTrendMultiYearRecord();
        record1.setLawaSiteId("SITE001");
        record1.setSiteName("Site SITE001");
        record1.setRegion("Canterbury");
        record1.setTrendNorm("IMPROVING");
        record1.setTrendScore(3);
        record1.setTrendPeriodYears(10);
        record1.setPeriodType("HYDRO_NYR_WINDOW");
        record1.setPeriodStartYear(2019);
        record1.setPeriodEndYear(2023);
        record1.setDatasetRelease(release);

        LawaTrendMultiYearRecord record2 = new LawaTrendMultiYearRecord();
        record2.setLawaSiteId("SITE002");
        record2.setSiteName("Site SITE002");
        record2.setRegion("Waikato");
        record2.setTrendNorm("DEGRADING");
        record2.setTrendScore(-1);
        record2.setTrendPeriodYears(10);
        record2.setPeriodType("HYDRO_NYR_WINDOW");
        record2.setPeriodStartYear(2018);
        record2.setPeriodEndYear(2022);
        record2.setDatasetRelease(release);

        LawaTrendMultiYearRecord record3 = new LawaTrendMultiYearRecord();
        record3.setLawaSiteId("SITE003");
        record3.setSiteName("Site SITE003");
        record3.setRegion("Otago");
        record3.setTrendNorm("IMPROVING");
        record3.setTrendScore(2);
        record3.setTrendPeriodYears(10);
        record3.setPeriodType("HYDRO_NYR_WINDOW");
        record3.setPeriodStartYear(2020);
        record3.setPeriodEndYear(2024);
        record3.setDatasetRelease(release);

        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of(record1, record2, record3));

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("regional_trend_comparison");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

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
        var otagoClassifications = classifications.stream()
            .filter(c -> "Otago".equals(c.getDimensions().get("region")))
            .toList();
        
        assertEquals(3, classifications.size()); // One classification per region-trend present
        assertEquals(1, canterburyClassifications.size()); // Canterbury: IMPROVING
        assertEquals(1, waikatoClassifications.size()); // Waikato: DEGRADING
        assertEquals(1, otagoClassifications.size()); // Otago: IMPROVING
        
        // Verify regional percentage metrics
        var metrics = factPack.getFacts().getMetrics();
        var canterburyMetric = metrics.stream()
            .filter(m -> m.getId().contains("Canterbury") && m.getId().contains("improving"))
            .findFirst()
            .orElse(null);
        var waikatoMetric = metrics.stream()
            .filter(m -> m.getId().contains("Waikato") && m.getId().contains("improving"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(canterburyMetric);
        assertNotNull(waikatoMetric);
        assertEquals(new BigDecimal("100.00"), canterburyMetric.getValue()); // 1/1 = 100%
        assertEquals(new BigDecimal("0.00"), waikatoMetric.getValue()); // 0/1 = 0%
        assertEquals(
            List.of(
                "class:lawa:water_quality_trend:Canterbury:*",
                "class:lawa:water_quality_trend:Otago:*",
                "class:lawa:water_quality_trend:Waikato:*",
                "metric:lawa:improving_sites_percentage:*"
            ),
            factPack.getGuardrails().getRequiredCitations()
        );
    }

    @Test
    void testBuildFactPack_WithTrendFilter_AppliesCorrectly() {
        // Setup test data with mixed trend types
        DatasetRelease release = createDatasetRelease();

        LawaTrendMultiYearRecord record_improving = getLawaTrendMultiYearRecord(release);

        when(repository.findForAsk(null, null, null, null, "improving"))
            .thenReturn(List.of(record_improving));

        // Create request with trend filter
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("water_quality_trends");
        request.setFilters(Map.of(
            "datasetSource", "lawa.water_quality.trend.multi_year",
            "trend", "IMPROVING"
        ));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify only improving records are included
        var classifications = factPack.getFacts().getClassifications();
        var improvingClassifications = classifications.stream()
            .filter(c -> "IMPROVING".equals(c.getClassification()))
            .toList();
        var degradingClassifications = classifications.stream()
            .filter(c -> "DEGRADING".equals(c.getClassification()))
            .toList();
        
        assertEquals(1, classifications.size()); // Aggregated IMPROVING classification
        assertEquals(1, improvingClassifications.size()); // Only IMPROVING present after filter
        assertEquals(0, degradingClassifications.size()); // No DEGRADING classifications
        
        // Verify only improving sites are counted in metrics
        var metrics = factPack.getFacts().getMetrics();
        var improvingMetrics = metrics.stream()
            .filter(m -> m.getId().contains("improving"))
            .toList();
        
        assertEquals(1, improvingMetrics.size()); // Single improving percentage metric
    }

    private static @NotNull LawaTrendMultiYearRecord getLawaTrendMultiYearRecord(DatasetRelease release) {
        LawaTrendMultiYearRecord record_improving = new LawaTrendMultiYearRecord();
        record_improving.setLawaSiteId("SITE001");
        record_improving.setSiteName("Site SITE001");
        record_improving.setRegion("Canterbury");
        record_improving.setTrendNorm("IMPROVING");
        record_improving.setTrendScore(3);
        record_improving.setTrendPeriodYears(10);
        record_improving.setPeriodType("HYDRO_NYR_WINDOW");
        record_improving.setPeriodStartYear(2019);
        record_improving.setPeriodEndYear(2023);
        record_improving.setDatasetRelease(release);
        return record_improving;
    }

    @Test
    void testBuildFactPack_UnsupportedQuestionType_ReturnsBasicFacts() {
        // Setup test data
        DatasetRelease release = createDatasetRelease();

        LawaTrendMultiYearRecord record = new LawaTrendMultiYearRecord();
        record.setLawaSiteId("SITE001");
        record.setSiteName("Site SITE001");
        record.setRegion("Canterbury");
        record.setTrendNorm("IMPROVING");
        record.setTrendScore(3);
        record.setTrendPeriodYears(10);
        record.setPeriodType("HYDRO_NYR_WINDOW");
        record.setPeriodStartYear(2019);
        record.setPeriodEndYear(2023);
        record.setDatasetRelease(release);

        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of(record));

        // Create request with unsupported question type
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("unsupported_question");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify basic facts are created
        var metrics = factPack.getFacts().getMetrics();
        assertEquals(2, metrics.size()); // total_records + unique_sites
        
        var totalMetric = metrics.stream()
            .filter(m -> m.getId().contains("total_trend_records"))
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

        LawaTrendMultiYearRecord record = new LawaTrendMultiYearRecord();
        record.setLawaSiteId("SITE001");
        record.setSiteName("Site SITE001");
        record.setRegion("Canterbury");
        record.setTrendNorm("IMPROVING");
        record.setTrendScore(3);
        record.setTrendPeriodYears(10);
        record.setPeriodType("HYDRO_NYR_WINDOW");
        record.setPeriodStartYear(2019);
        record.setPeriodEndYear(2023);
        record.setDatasetRelease(release);

        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of(record));

        // Create identical requests
        ExplanationRequest request1 = new ExplanationRequest();
        request1.setQuestionType("water_quality_trends");
        request1.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        ExplanationRequest request2 = new ExplanationRequest();
        request2.setQuestionType("water_quality_trends");
        request2.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

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

    @Test
    void testDeterministicRequiredCitationsUnderInputShuffle() {
        DatasetRelease release = createDatasetRelease();

        LawaTrendMultiYearRecord canterburyImproving = getTrendMultiYearRecord(release);

        LawaTrendMultiYearRecord waikatoDegrading = new LawaTrendMultiYearRecord();
        waikatoDegrading.setLawaSiteId("SITE002");
        waikatoDegrading.setSiteName("Site SITE002");
        waikatoDegrading.setRegion("Waikato");
        waikatoDegrading.setTrendNorm("DEGRADING");
        waikatoDegrading.setTrendScore(-2);
        waikatoDegrading.setTrendPeriodYears(10);
        waikatoDegrading.setPeriodType("HYDRO_NYR_WINDOW");
        waikatoDegrading.setPeriodStartYear(2019);
        waikatoDegrading.setPeriodEndYear(2023);
        waikatoDegrading.setDatasetRelease(release);

        LawaTrendMultiYearRecord otagoInsufficient = new LawaTrendMultiYearRecord();
        otagoInsufficient.setLawaSiteId("SITE003");
        otagoInsufficient.setSiteName("Site SITE003");
        otagoInsufficient.setRegion("Otago");
        otagoInsufficient.setTrendNorm("INSUFFICIENT_DATA");
        otagoInsufficient.setTrendScore(0);
        otagoInsufficient.setTrendPeriodYears(10);
        otagoInsufficient.setPeriodType("HYDRO_NYR_WINDOW");
        otagoInsufficient.setPeriodStartYear(2019);
        otagoInsufficient.setPeriodEndYear(2023);
        otagoInsufficient.setDatasetRelease(release);

        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("regional_trend_comparison");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of(canterburyImproving, waikatoDegrading, otagoInsufficient));
        FactPack first = builder.buildFactPack(request);

        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of(otagoInsufficient, waikatoDegrading, canterburyImproving));
        FactPack second = builder.buildFactPack(request);

        assertEquals(
            first.getGuardrails().getRequiredCitations(),
            second.getGuardrails().getRequiredCitations()
        );
    }

    private static @NotNull LawaTrendMultiYearRecord getTrendMultiYearRecord(DatasetRelease release) {
        LawaTrendMultiYearRecord canterburyImproving = new LawaTrendMultiYearRecord();
        canterburyImproving.setLawaSiteId("SITE001");
        canterburyImproving.setSiteName("Site SITE001");
        canterburyImproving.setRegion("Canterbury");
        canterburyImproving.setTrendNorm("IMPROVING");
        canterburyImproving.setTrendScore(3);
        canterburyImproving.setTrendPeriodYears(10);
        canterburyImproving.setPeriodType("HYDRO_NYR_WINDOW");
        canterburyImproving.setPeriodStartYear(2019);
        canterburyImproving.setPeriodEndYear(2023);
        canterburyImproving.setDatasetRelease(release);
        return canterburyImproving;
    }

    @Test
    void testRegionalTrendComparisonUsesDeterministicRegionSubset() {
        DatasetRelease release = createDatasetRelease();
        List<String> regions = List.of("Auckland", "Waikato", "Canterbury", "Otago", "Taranaki");
        List<LawaTrendMultiYearRecord> records = regions.stream().map(region -> {
            LawaTrendMultiYearRecord r = new LawaTrendMultiYearRecord();
            r.setLawaSiteId("SITE-" + region);
            r.setSiteName("Site " + region);
            r.setRegion(region);
            r.setTrendNorm("IMPROVING");
            r.setTrendScore(2);
            r.setTrendPeriodYears(10);
            r.setPeriodType("HYDRO_NYR_WINDOW");
            r.setPeriodStartYear(2019);
            r.setPeriodEndYear(2023);
            r.setDatasetRelease(release);
            return r;
        }).toList();

        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("regional_trend_comparison");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(records);
        FactPack factPack = builder.buildFactPack(request);

        long metricRegionCount = factPack.getFacts().getMetrics().stream()
            .filter(m -> m.getId().startsWith("metric:lawa:improving_sites_percentage:"))
            .count();
        assertTrue(metricRegionCount <= 4);
    }

    @Test
    void testPinsToSingleCanonicalReleaseForAsk() {
        DatasetRelease olderRelease = createDatasetRelease("sha256:lawa_trend_old", LocalDateTime.of(2024, 1, 1, 0, 0));
        DatasetRelease newerRelease = createDatasetRelease("sha256:lawa_trend_new", LocalDateTime.of(2025, 1, 1, 0, 0));

        LawaTrendMultiYearRecord olderRecord = new LawaTrendMultiYearRecord();
        olderRecord.setLawaSiteId("SITE001");
        olderRecord.setRegion("Canterbury");
        olderRecord.setTrendNorm("IMPROVING");
        olderRecord.setTrendScore(2);
        olderRecord.setTrendPeriodYears(10);
        olderRecord.setPeriodType("HYDRO_NYR_WINDOW");
        olderRecord.setPeriodStartYear(2019);
        olderRecord.setPeriodEndYear(2023);
        olderRecord.setDatasetRelease(olderRelease);

        LawaTrendMultiYearRecord newerRecord = new LawaTrendMultiYearRecord();
        newerRecord.setLawaSiteId("SITE002");
        newerRecord.setRegion("Canterbury");
        newerRecord.setTrendNorm("IMPROVING");
        newerRecord.setTrendScore(2);
        newerRecord.setTrendPeriodYears(10);
        newerRecord.setPeriodType("HYDRO_NYR_WINDOW");
        newerRecord.setPeriodStartYear(2019);
        newerRecord.setPeriodEndYear(2024);
        newerRecord.setDatasetRelease(newerRelease);

        when(repository.findForAsk(any(), any(), any(), any(), any())).thenReturn(List.of(olderRecord, newerRecord));

        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("improving_sites_trend");
        request.setFilters(Map.of("datasetSource", "lawa.water_quality.trend.multi_year"));

        FactPack factPack = builder.buildFactPack(request);

        assertEquals(1, factPack.getProvenance().getDatasetSources().size());
        assertEquals("sha256:lawa_trend_new", factPack.getProvenance().getDatasetSources().getFirst().getContentHash());
    }

    private DatasetRelease createDatasetRelease() {
        return createDatasetRelease("sha256:lawa_trend123def456", LocalDateTime.of(2025, 1, 1, 0, 0));
    }

    private DatasetRelease createDatasetRelease(String contentHash, LocalDateTime importedAt) {
        DatasetRelease release = new DatasetRelease();
        release.setContentHash(contentHash);
        release.setPublishedDate(LocalDate.now());
        release.setRetrievedAt(importedAt);
        release.setImportedAt(importedAt);
        return release;
    }
}
