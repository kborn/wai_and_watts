package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.dto.MetricFact;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Additional comprehensive tests for MbieGenerationAnnualFactPackBuilder
 */
@ExtendWith(MockitoExtension.class)
class MbieGenerationAnnualFactPackBuilderComprehensiveTest {

    @Mock
    private MbieGenerationAnnualRecordRepository repository;

    private MbieGenerationAnnualFactPackBuilder builder;
    private DatasetSource datasetSource;
    private DatasetRelease datasetRelease;

    @BeforeEach
    void setUp() {
        builder = new MbieGenerationAnnualFactPackBuilder(repository);
        
        datasetSource = new DatasetSource();
        datasetSource.setCode("mbie.generation.annual");
        
        datasetRelease = new DatasetRelease();
        datasetRelease.setId(UUID.randomUUID());
        datasetRelease.setDatasetSource(datasetSource);
        datasetRelease.setContentHash("sha256:test456");
        datasetRelease.setRetrievedAt(LocalDateTime.now());
        datasetRelease.setStatus(ReleaseStatus.IMPORTED);
    }

    @Test
    void testEmptyDatabaseProducesConsistentEmptyFactPack() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(repository.findForReadApi(any(), any(), any())).thenReturn(List.of());

        FactPack factPack1 = builder.buildFactPack(request);
        FactPack factPack2 = builder.buildFactPack(request);

        

        // Both should be empty but structured identically
        assertTrue(factPack1.getFacts().getTimeSeries().isEmpty());
        assertTrue(factPack2.getFacts().getTimeSeries().isEmpty());
        assertTrue(factPack1.getFacts().getMetrics().isEmpty());
        assertTrue(factPack2.getFacts().getMetrics().isEmpty());
        assertTrue(factPack1.getFacts().getComparisons().isEmpty());
        assertTrue(factPack2.getFacts().getComparisons().isEmpty());
        
        // Guardrails should be identical
        assertEquals(factPack1.getGuardrails().getAllowedClaims(), factPack2.getGuardrails().getAllowedClaims());
        assertEquals(factPack1.getGuardrails().getForbiddenClaims(), factPack2.getGuardrails().getForbiddenClaims());
        
        // Request context should be identical
        assertEquals(factPack1.getRequestContext().getQuestionType(), factPack2.getRequestContext().getQuestionType());
        assertEquals(factPack1.getRequestContext().getDatasetScope(), factPack2.getRequestContext().getDatasetScope());
    }

    @Test
    void testDeterminismWithUnorderedInputData() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_type_comparison",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        // Create unordered test data (random order)
        List<MbieGenerationAnnualRecord> unorderedRecords = List.of(
            createRecord(2023, "WIND", new BigDecimal("6000")),      // WIND before HYDRO
            createRecord(2023, "HYDRO", new BigDecimal("25000")),
            createRecord(2023, "GEOTHERMAL", new BigDecimal("8000")) // GEOTHERMAL last
        );

        List<MbieGenerationAnnualRecord> reversedRecords = List.of(
            createRecord(2023, "GEOTHERMAL", new BigDecimal("8000")), // Reverse order
            createRecord(2023, "HYDRO", new BigDecimal("25000")),
            createRecord(2023, "WIND", new BigDecimal("6000"))
        );

        when(repository.findForReadApi(any(), any(), any())).thenReturn(unorderedRecords);
        FactPack factPack1 = builder.buildFactPack(request);

        when(repository.findForReadApi(any(), any(), any())).thenReturn(reversedRecords);
        FactPack factPack2 = builder.buildFactPack(request);

        // Should produce identical results regardless of input ordering
        assertEquals(factPack1.getFacts().getMetrics().size(), factPack2.getFacts().getMetrics().size());
        
        // Sort both by ID for comparison (since builder should sort them deterministically)
        var sorted1 = factPack1.getFacts().getMetrics().stream()
            .sorted(Comparator.comparing(MetricFact::getId))
            .toList();
        var sorted2 = factPack2.getFacts().getMetrics().stream()
            .sorted(Comparator.comparing(MetricFact::getId))
            .toList();

        for (int i = 0; i < sorted1.size(); i++) {
            assertEquals(sorted1.get(i).getId(), sorted2.get(i).getId());
            assertEquals(sorted1.get(i).getValue(), sorted2.get(i).getValue());
            assertEquals(sorted1.get(i).getDimensions().get("fuel_type"), 
                        sorted2.get(i).getDimensions().get("fuel_type"));
        }
    }

    @Test
    void testDeterminismForHydroGenerationTrendWithMultipleYears() {
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        // Create multi-year data
        List<MbieGenerationAnnualRecord> records = List.of(
            createRecord(2020, "HYDRO", new BigDecimal("24000")),
            createRecord(2021, "HYDRO", new BigDecimal("24500")),
            createRecord(2022, "HYDRO", new BigDecimal("25000")),
            createRecord(2023, "HYDRO", new BigDecimal("26000"))
        );

        when(repository.findForReadApi(any(), any(), any())).thenReturn(records);

        FactPack factPack1 = builder.buildFactPack(request);
        FactPack factPack2 = builder.buildFactPack(request);

        // Should have time series and comparison facts
        assertEquals(1, factPack1.getFacts().getTimeSeries().size());
        assertEquals(1, factPack2.getFacts().getTimeSeries().size());
        assertEquals(1, factPack1.getFacts().getComparisons().size());
        assertEquals(1, factPack2.getFacts().getComparisons().size());

        // Time series should be identical
        var ts1 = factPack1.getFacts().getTimeSeries().getFirst();
        var ts2 = factPack2.getFacts().getTimeSeries().getFirst();
        assertEquals(ts1.getId(), ts2.getId());
        assertEquals(ts1.getPoints().size(), ts2.getPoints().size());

        // Check deterministic ordering of time series points
        for (int i = 0; i < ts1.getPoints().size(); i++) {
            assertEquals(ts1.getPoints().get(i).getPeriod(), ts2.getPoints().get(i).getPeriod());
            assertEquals(ts1.getPoints().get(i).getValue(), ts2.getPoints().get(i).getValue());
        }

        // Comparison facts should be identical (should compare 2023 vs 2022)
        var comp1 = factPack1.getFacts().getComparisons().getFirst();
        var comp2 = factPack2.getFacts().getComparisons().getFirst();
        assertEquals(comp1.getId(), comp2.getId());
        assertEquals(comp1.getDeltaAbsolute(), comp2.getDeltaAbsolute());
        assertEquals(comp1.getDeltaPercent(), comp2.getDeltaPercent());
        assertEquals("2022", comp1.getBaselinePeriod());
        assertEquals("2023", comp1.getComparisonPeriod());
    }

    @Test
    void testFuelTypeComparisonWithExplicitFuelsBuildsTimeSeries() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_type_comparison",
            Map.of(
                "datasetSource", "mbie.generation.annual",
                "fuelType", "HYDRO",
                "fuelTypeB", "GEOTHERMAL"
            )
        );

        List<MbieGenerationAnnualRecord> records = List.of(
            createRecord(2021, "HYDRO", new BigDecimal("24000")),
            createRecord(2022, "HYDRO", new BigDecimal("25000")),
            createRecord(2021, "GEOTHERMAL", new BigDecimal("7000")),
            createRecord(2022, "GEOTHERMAL", new BigDecimal("7200"))
        );

        when(repository.findForReadApi(any(), any(), any())).thenReturn(records);

        FactPack factPack = builder.buildFactPack(request);

        assertEquals(2, factPack.getFacts().getTimeSeries().size());
        assertTrue(factPack.getFacts().getTimeSeries().stream()
            .allMatch(ts -> ts.getDimensions().containsKey("fuel_type")));

        assertFalse(factPack.getGuardrails().getRequiredCitations().isEmpty());
    }

    @Test
    void testUnsupportedQuestionTypeProducesRefusalGuardrails() {
        ExplanationRequest request = new ExplanationRequest(
            "forecasting", // Unsupported type
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(repository.findForReadApi(any(), any(), any())).thenReturn(List.of(
            createRecord(2023, "HYDRO", new BigDecimal("25000"))
        ));

        FactPack factPack = builder.buildFactPack(request);

        // Should have empty allowed claims to trigger refusal
        assertTrue(factPack.getGuardrails().getAllowedClaims().isEmpty());
        
        // Should still have forbidden claims
        assertFalse(factPack.getGuardrails().getForbiddenClaims().isEmpty());
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("forecast"));
        assertTrue(factPack.getGuardrails().getForbiddenClaims().contains("causation"));
        
        // Should have empty required citations
        assertTrue(factPack.getGuardrails().getRequiredCitations().isEmpty());
    }

    @Test
    void testProvenanceBuiltCorrectly() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(repository.findForReadApi(any(), any(), any())).thenReturn(List.of(
            createRecord(2022, "HYDRO", new BigDecimal("25000")),
            createRecord(2023, "HYDRO", new BigDecimal("26000"))
        ));

        FactPack factPack = builder.buildFactPack(request);

        // Should have provenance
        assertNotNull(factPack.getProvenance());
        assertNotNull(factPack.getProvenance().getDatasetSources());
        assertEquals(1, factPack.getProvenance().getDatasetSources().size());

        var provenance = factPack.getProvenance().getDatasetSources().getFirst();
        assertEquals("mbie.generation.annual", provenance.getDatasetSourceCode());
        assertEquals(datasetRelease.getId().toString(), provenance.getDatasetReleaseId());
        assertEquals("sha256:test456", provenance.getContentHash());
        assertEquals("2022-2023", provenance.getPeriodCoverage());
    }

    private MbieGenerationAnnualRecord createRecord(int year, String fuelType, BigDecimal generation) {
        MbieGenerationAnnualRecord record = new MbieGenerationAnnualRecord();
        record.setDatasetRelease(datasetRelease);
        record.setPeriodYear(year);
        record.setFuelTypeRaw(fuelType);
        record.setFuelTypeNorm(fuelType);
        record.setGenerationGwh(generation);
        return record;
    }
}
