package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
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

/**
 * Additional comprehensive tests for MbieGenerationQuarterlyFactPackBuilder
 */
@ExtendWith(MockitoExtension.class)
class MbieGenerationQuarterlyFactPackBuilderComprehensiveTest {

    @Mock
    private MbieGenerationQuarterlyRecordRepository repository;

    private MbieGenerationQuarterlyFactPackBuilder builder;

    @BeforeEach
    void setUp() {
        repository = mock(MbieGenerationQuarterlyRecordRepository.class);
        builder = new MbieGenerationQuarterlyFactPackBuilder(repository);
    }

    @Test
    void testCanHandle_WithQuarterlyDatasetSource_ReturnsTrue() {
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("renewable_generation_trend");
        request.setFilters(Map.of("datasetSource", "mbie.generation.quarterly"));

        assertTrue(builder.canHandle(request));
    }

    @Test
    void testCanHandle_WithAnnualDatasetSource_ReturnsFalse() {
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("renewable_generation_trend");
        request.setFilters(Map.of("datasetSource", "mbie.generation.annual"));

        assertFalse(builder.canHandle(request));
    }

    @Test
    void testGetSupportedDatasetSourceCode_ReturnsCorrectCode() {
        assertEquals("mbie.generation.quarterly", builder.getSupportedDatasetSourceCode());
    }

    @Test
    void testBuildFactPack_RenewableGenerationTrend_CreatesCorrectTimeSeries() {
        // Setup test data
        DatasetRelease release = createDatasetRelease();

        MbieGenerationQuarterlyRecord record1 = new MbieGenerationQuarterlyRecord();
        record1.setDatasetRelease(release);
        record1.setPeriodYear(2023);
        record1.setPeriodQuarter(1);
        record1.setFuelTypeNorm("HYDRO");
        record1.setGenerationGwh(new BigDecimal("6500"));

        MbieGenerationQuarterlyRecord record2 = new MbieGenerationQuarterlyRecord();
        record2.setDatasetRelease(release);
        record2.setPeriodYear(2023);
        record2.setPeriodQuarter(2);
        record2.setFuelTypeNorm("WIND");
        record2.setGenerationGwh(new BigDecimal("2800"));

        MbieGenerationQuarterlyRecord record3 = new MbieGenerationQuarterlyRecord();
        record3.setDatasetRelease(release);
        record3.setPeriodYear(2023);
        record3.setPeriodQuarter(3);
        record3.setFuelTypeNorm("HYDRO");
        record3.setGenerationGwh(new BigDecimal("7000"));

        MbieGenerationQuarterlyRecord record4 = new MbieGenerationQuarterlyRecord();
        record4.setDatasetRelease(release);
        record4.setPeriodYear(2023);
        record4.setPeriodQuarter(4);
        record4.setFuelTypeNorm("SOLAR");
        record4.setGenerationGwh(new BigDecimal("400"));

        when(repository.findForReadApi(any(), any(), any(), any())).thenReturn(List.of(record1, record2, record3, record4));

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("renewable_generation_trend");
        request.setFilters(Map.of("datasetSource", "mbie.generation.quarterly"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify structure
        assertNotNull(factPack);
        assertEquals("renewable_generation_trend", factPack.getRequestContext().getQuestionType());
        assertEquals(List.of("mbie.generation.quarterly"), factPack.getRequestContext().getDatasetScope());
        
        // Verify time series
        assertEquals(1, factPack.getFacts().getTimeSeries().size());
        
        var timeSeries = factPack.getFacts().getTimeSeries().getFirst();
        assertNotNull(timeSeries);
        assertEquals("renewable_generation_gwh_quarterly", timeSeries.getMetricName());
        assertEquals("GWh", timeSeries.getUnit());
        
        // Verify quarterly aggregation (HYDRO: 6500 + 7000 = 13500, WIND: 2800, SOLAR: 400)
        var points = timeSeries.getPoints();
        assertEquals(4, points.size());
        
        assertTrue(points.stream().anyMatch(p -> p.getPeriod().equals("2023-Q1") && p.getValue().equals(new BigDecimal("6500"))));
        assertTrue(points.stream().anyMatch(p -> p.getPeriod().equals("2023-Q2") && p.getValue().equals(new BigDecimal("2800"))));
        assertTrue(points.stream().anyMatch(p -> p.getPeriod().equals("2023-Q3") && p.getValue().equals(new BigDecimal("7000"))));
        assertTrue(points.stream().anyMatch(p -> p.getPeriod().equals("2023-Q4") && p.getValue().equals(new BigDecimal("400")))); // SOLAR only
    }

    @Test
    void testBuildFactPack_RenewableGenerationTrend_WithShareMetricType_ComputesPercentages() {
        DatasetRelease release = createDatasetRelease();

        MbieGenerationQuarterlyRecord hydro = new MbieGenerationQuarterlyRecord();
        hydro.setDatasetRelease(release);
        hydro.setPeriodYear(2023);
        hydro.setPeriodQuarter(1);
        hydro.setFuelTypeNorm("HYDRO");
        hydro.setGenerationGwh(new BigDecimal("60"));

        MbieGenerationQuarterlyRecord wind = new MbieGenerationQuarterlyRecord();
        wind.setDatasetRelease(release);
        wind.setPeriodYear(2023);
        wind.setPeriodQuarter(1);
        wind.setFuelTypeNorm("WIND");
        wind.setGenerationGwh(new BigDecimal("30"));

        MbieGenerationQuarterlyRecord gas = new MbieGenerationQuarterlyRecord();
        gas.setDatasetRelease(release);
        gas.setPeriodYear(2023);
        gas.setPeriodQuarter(1);
        gas.setFuelTypeNorm("GAS");
        gas.setGenerationGwh(new BigDecimal("10"));

        when(repository.findForReadApi(any(), any(), any(), any())).thenReturn(List.of(hydro, wind, gas));

        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("renewable_generation_trend");
        request.setDatasetSource("mbie.generation.quarterly");
        request.setFilters(Map.of("metricType", "renewable_share_pct"));

        FactPack factPack = builder.buildFactPack(request);

        assertEquals(1, factPack.getFacts().getTimeSeries().size());
        var series = factPack.getFacts().getTimeSeries().getFirst();
        assertEquals("%", series.getUnit());
        assertEquals("renewable_share_pct_quarterly", series.getMetricName());
        assertEquals(new BigDecimal("90.00"), series.getPoints().getFirst().getValue());
    }

    @Test
    void testBuildFactPack_HydroGenerationTrend_CreatesTimeSeriesAndComparison() {
        // Setup test data with quarterly hydro data
        DatasetRelease release = createDatasetRelease();

        MbieGenerationQuarterlyRecord record1 = new MbieGenerationQuarterlyRecord();
        record1.setDatasetRelease(release);
        record1.setPeriodYear(2023);
        record1.setPeriodQuarter(1);
        record1.setFuelTypeNorm("HYDRO");
        record1.setGenerationGwh(new BigDecimal("6500"));

        MbieGenerationQuarterlyRecord record2 = new MbieGenerationQuarterlyRecord();
        record2.setDatasetRelease(release);
        record2.setPeriodYear(2023);
        record2.setPeriodQuarter(2);
        record2.setFuelTypeNorm("HYDRO");
        record2.setGenerationGwh(new BigDecimal("7000"));

        MbieGenerationQuarterlyRecord record3 = new MbieGenerationQuarterlyRecord();
        record3.setDatasetRelease(release);
        record3.setPeriodYear(2023);
        record3.setPeriodQuarter(3);
        record3.setFuelTypeNorm("HYDRO");
        record3.setGenerationGwh(new BigDecimal("6800"));

        MbieGenerationQuarterlyRecord record4 = new MbieGenerationQuarterlyRecord();
        record4.setDatasetRelease(release);
        record4.setPeriodYear(2023);
        record4.setPeriodQuarter(4);
        record4.setFuelTypeNorm("HYDRO");
        record4.setGenerationGwh(new BigDecimal("7200"));

        when(repository.findForReadApi(any(), any(), any(), any())).thenReturn(List.of(record1, record2, record3, record4));

        // Create request with fuel type filter
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("fuel_generation_trend");
        request.setFilters(Map.of(
            "datasetSource", "mbie.generation.quarterly",
            "fuelType", "HYDRO"
        ));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify time series
        assertEquals(1, factPack.getFacts().getTimeSeries().size());
        
        var timeSeries = factPack.getFacts().getTimeSeries().getFirst();
        assertNotNull(timeSeries);
        assertEquals("fuel_generation_gwh_quarterly", timeSeries.getMetricName());
        assertEquals("GWh", timeSeries.getUnit());
        
        // Verify quarterly hydro data
        var points = timeSeries.getPoints();
        assertEquals(4, points.size());
        
        assertTrue(points.stream().anyMatch(p -> p.getPeriod().equals("2023-Q1") && p.getValue().equals(new BigDecimal("6500"))));
        assertTrue(points.stream().anyMatch(p -> p.getPeriod().equals("2023-Q2") && p.getValue().equals(new BigDecimal("7000"))));
        assertTrue(points.stream().anyMatch(p -> p.getPeriod().equals("2023-Q3") && p.getValue().equals(new BigDecimal("6800"))));
        assertTrue(points.stream().anyMatch(p -> p.getPeriod().equals("2023-Q4") && p.getValue().equals(new BigDecimal("7200"))));
        
        // Verify comparison (Q4 vs Q3)
        var comparisons = factPack.getFacts().getComparisons();
        assertEquals(1, comparisons.size());
        
        var comparison = comparisons.getFirst();
        assertNotNull(comparison);
        assertEquals("2023-Q3", comparison.getBaselinePeriod());
        assertEquals("2023-Q4", comparison.getComparisonPeriod());
        assertEquals(new BigDecimal("400"), comparison.getDeltaAbsolute()); // 7200 - 6800 = 400
    }

    @Test
    void testBuildFactPack_FuelTypeComparison_CreatesMetrics() {
        // Setup test data with mixed fuel types for a specific quarter
        DatasetRelease release = createDatasetRelease();

        MbieGenerationQuarterlyRecord record1 = new MbieGenerationQuarterlyRecord();
        record1.setDatasetRelease(release);
        record1.setPeriodYear(2023);
        record1.setPeriodQuarter(4);
        record1.setFuelTypeNorm("HYDRO");
        record1.setGenerationGwh(new BigDecimal("7200"));

        MbieGenerationQuarterlyRecord record2 = new MbieGenerationQuarterlyRecord();
        record2.setDatasetRelease(release);
        record2.setPeriodYear(2023);
        record2.setPeriodQuarter(4);
        record2.setFuelTypeNorm("WIND");
        record2.setGenerationGwh(new BigDecimal("3200"));

        MbieGenerationQuarterlyRecord record3 = new MbieGenerationQuarterlyRecord();
        record3.setDatasetRelease(release);
        record3.setPeriodYear(2023);
        record3.setPeriodQuarter(4);
        record3.setFuelTypeNorm("GEOTHERMAL");
        record3.setGenerationGwh(new BigDecimal("1500"));

        MbieGenerationQuarterlyRecord record4 = new MbieGenerationQuarterlyRecord();
        record4.setDatasetRelease(release);
        record4.setPeriodYear(2023);
        record4.setPeriodQuarter(4);
        record4.setFuelTypeNorm("GAS");
        record4.setGenerationGwh(new BigDecimal("800"));

        when(repository.findForReadApi(any(), any(), any(), any())).thenReturn(List.of(record1, record2, record3, record4));

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("fuel_type_comparison");
        request.setFilters(Map.of("datasetSource", "mbie.generation.quarterly"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify metrics for each fuel type
        var metrics = factPack.getFacts().getMetrics();
        assertEquals(4, metrics.size());
        
        var hydroMetric = metrics.stream()
            .filter(m -> m.getId().contains("HYDRO"))
            .findFirst()
            .orElse(null);
        var windMetric = metrics.stream()
            .filter(m -> m.getId().contains("WIND"))
            .findFirst()
            .orElse(null);
        var geothermalMetric = metrics.stream()
            .filter(m -> m.getId().contains("GEOTHERMAL"))
            .findFirst()
            .orElse(null);
        var gasMetric = metrics.stream()
            .filter(m -> m.getId().contains("GAS"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(hydroMetric);
        assertNotNull(windMetric);
        assertNotNull(geothermalMetric);
        assertNotNull(gasMetric);
        
        assertEquals(new BigDecimal("7200"), hydroMetric.getValue());
        assertEquals(new BigDecimal("3200"), windMetric.getValue());
        assertEquals(new BigDecimal("1500"), geothermalMetric.getValue());
        assertEquals(new BigDecimal("800"), gasMetric.getValue());
        
        // Verify all metrics are for the same period
        assertEquals("2023-Q4", hydroMetric.getPeriod());
        assertEquals("2023-Q4", windMetric.getPeriod());
        assertEquals("2023-Q4", geothermalMetric.getPeriod());
        assertEquals("2023-Q4", gasMetric.getPeriod());
    }

    @Test
    void testBuildFactPack_WithTimeRangeFilters_AppliesCorrectly() {
        // Setup test data spanning multiple years
        DatasetRelease release = createDatasetRelease();

        MbieGenerationQuarterlyRecord record_included1 = new MbieGenerationQuarterlyRecord();
        record_included1.setDatasetRelease(release);
        record_included1.setPeriodYear(2023);
        record_included1.setPeriodQuarter(1);
        record_included1.setFuelTypeNorm("HYDRO");
        record_included1.setGenerationGwh(new BigDecimal("6500"));

        MbieGenerationQuarterlyRecord record_included2 = new MbieGenerationQuarterlyRecord();
        record_included2.setDatasetRelease(release);
        record_included2.setPeriodYear(2023);
        record_included2.setPeriodQuarter(2);
        record_included2.setFuelTypeNorm("HYDRO");
        record_included2.setGenerationGwh(new BigDecimal("7000"));

        MbieGenerationQuarterlyRecord record_included3 = new MbieGenerationQuarterlyRecord();
        record_included3.setDatasetRelease(release);
        record_included3.setPeriodYear(2023);
        record_included3.setPeriodQuarter(3);
        record_included3.setFuelTypeNorm("HYDRO");
        record_included3.setGenerationGwh(new BigDecimal("6800"));

        when(repository.findForReadApi(2023, 2023, null, "hydro"))
            .thenReturn(List.of(record_included1, record_included2, record_included3));

        // Create request with time range filter
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("fuel_generation_trend");
        request.setFilters(Map.of(
            "datasetSource", "mbie.generation.quarterly",
            "startYear", 2023,
            "endYear", 2023,
            "fuelType", "HYDRO"
        ));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify only included records are processed
        var timeSeries = factPack.getFacts().getTimeSeries().getFirst();
        var points = timeSeries.getPoints();
        
        assertEquals(3, points.size()); // Only 3 included records (Q1, Q2, Q3)
        assertTrue(points.stream().allMatch(p -> Integer.parseInt(p.getPeriod().split("-Q")[1]) >= 1 && 
                                             Integer.parseInt(p.getPeriod().split("-Q")[1]) <= 3 &&
                                             p.getPeriod().startsWith("2023-")));
        
        // Verify period coverage in provenance
        var provenance = factPack.getProvenance().getDatasetSources().getFirst();
        assertTrue(provenance.getPeriodCoverage().contains("2023"));
    }

    @Test
    void testBuildFactPack_EmptyRecords_ReturnsBasicFactsWithEmptyGuardrails() {
        // Setup empty repository
        when(repository.findForReadApi(any(), any(), any(), any())).thenReturn(List.of());

        // Create request
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("renewable_generation_trend");
        request.setFilters(Map.of("datasetSource", "mbie.generation.quarterly"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify empty facts but valid structure
        assertTrue(factPack.getFacts().getTimeSeries().isEmpty());
        assertTrue(factPack.getFacts().getMetrics().isEmpty());
        assertTrue(factPack.getFacts().getComparisons().isEmpty());
        
        // Verify empty guardrails for unsupported question
        assertTrue(factPack.getGuardrails().getAllowedClaims().isEmpty());
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("forecast"));
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("causation"));
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("policy_recommendation"));
    }

    @Test
    void testBuildFactPack_UnsupportedQuestionType_ReturnsBasicFacts() {
        // Setup test data
        DatasetRelease release = createDatasetRelease();

        MbieGenerationQuarterlyRecord record = new MbieGenerationQuarterlyRecord();
        record.setDatasetRelease(release);
        record.setPeriodYear(2023);
        record.setPeriodQuarter(4);
        record.setFuelTypeNorm("HYDRO");
        record.setGenerationGwh(new BigDecimal("6500"));

        when(repository.findForReadApi(any(), any(), any(), any())).thenReturn(List.of(record));

        // Create request with unsupported question type
        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("unsupported_question");
        request.setFilters(Map.of("datasetSource", "mbie.generation.quarterly"));

        // Build fact pack
        FactPack factPack = builder.buildFactPack(request);

        // Verify basic facts are created
        var metrics = factPack.getFacts().getMetrics();
        assertEquals(1, metrics.size()); // Only total_generation_gwh_quarterly
        
        var totalMetric = metrics.getFirst();
        assertNotNull(totalMetric);
        assertTrue(totalMetric.getId().contains("total_generation_gwh_quarterly"));
        assertEquals(new BigDecimal("6500"), totalMetric.getValue());
        
        // Verify empty guardrails for unsupported question
        assertTrue(factPack.getGuardrails().getAllowedClaims().isEmpty());
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("forecast"));
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("causation"));
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("policy_recommendation"));
    }

    @Test
    void testDeterminism_SameInput_ProducesIdenticalOutput() {
        // Setup test data
        DatasetRelease release = createDatasetRelease();

        MbieGenerationQuarterlyRecord record = new MbieGenerationQuarterlyRecord();
        record.setDatasetRelease(release);
        record.setPeriodYear(2023);
        record.setPeriodQuarter(1);
        record.setFuelTypeNorm("HYDRO");
        record.setGenerationGwh(new BigDecimal("6500"));

        when(repository.findForReadApi(any(), any(), any(), any())).thenReturn(List.of(record));

        // Create identical requests
        ExplanationRequest request1 = new ExplanationRequest();
        request1.setQuestionType("renewable_generation_trend");
        request1.setFilters(Map.of("datasetSource", "mbie.generation.quarterly"));

        ExplanationRequest request2 = new ExplanationRequest();
        request2.setQuestionType("renewable_generation_trend");
        request2.setFilters(Map.of("datasetSource", "mbie.generation.quarterly"));

        // Build fact packs
        FactPack factPack1 = builder.buildFactPack(request1);
        FactPack factPack2 = builder.buildFactPack(request2);

        // Verify determinism - same structure and values
        assertEquals(factPack1.getRequestContext().getQuestionType(), factPack2.getRequestContext().getQuestionType());
        assertEquals(factPack1.getRequestContext().getDatasetScope(), factPack2.getRequestContext().getDatasetScope());
        
        assertEquals(factPack1.getFacts().getTimeSeries().size(), factPack2.getFacts().getTimeSeries().size());
        if (!factPack1.getFacts().getTimeSeries().isEmpty()) {
            var ts1 = factPack1.getFacts().getTimeSeries().getFirst();
            var ts2 = factPack2.getFacts().getTimeSeries().getFirst();
            assertEquals(ts1.getId(), ts2.getId());
            assertEquals(ts1.getMetricName(), ts2.getMetricName());
            assertEquals(ts1.getUnit(), ts2.getUnit());
            assertEquals(ts1.getPoints().size(), ts2.getPoints().size());
        }
    }

    @Test
    void testPinsToSingleCanonicalReleaseForAsk() {
        DatasetRelease olderRelease = createDatasetRelease("sha256:mbie_quarterly_old", LocalDateTime.of(2024, 1, 1, 0, 0));
        DatasetRelease newerRelease = createDatasetRelease("sha256:mbie_quarterly_new", LocalDateTime.of(2025, 1, 1, 0, 0));

        MbieGenerationQuarterlyRecord olderRecord = new MbieGenerationQuarterlyRecord();
        olderRecord.setDatasetRelease(olderRelease);
        olderRecord.setPeriodYear(2023);
        olderRecord.setPeriodQuarter(1);
        olderRecord.setFuelTypeNorm("HYDRO");
        olderRecord.setGenerationGwh(new BigDecimal("6500"));

        MbieGenerationQuarterlyRecord newerRecord = new MbieGenerationQuarterlyRecord();
        newerRecord.setDatasetRelease(newerRelease);
        newerRecord.setPeriodYear(2023);
        newerRecord.setPeriodQuarter(2);
        newerRecord.setFuelTypeNorm("HYDRO");
        newerRecord.setGenerationGwh(new BigDecimal("7000"));

        when(repository.findForReadApi(any(), any(), any(), any())).thenReturn(List.of(olderRecord, newerRecord));

        ExplanationRequest request = new ExplanationRequest();
        request.setQuestionType("fuel_generation_trend");
        request.setFilters(Map.of(
            "datasetSource", "mbie.generation.quarterly",
            "fuelType", "HYDRO"
        ));

        FactPack factPack = builder.buildFactPack(request);

        assertEquals(1, factPack.getProvenance().getDatasetSources().size());
        assertEquals("sha256:mbie_quarterly_new", factPack.getProvenance().getDatasetSources().getFirst().getContentHash());
    }

    private DatasetRelease createDatasetRelease() {
        return createDatasetRelease("sha256:mbie_quarterly123def456", LocalDateTime.of(2025, 1, 1, 0, 0));
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
