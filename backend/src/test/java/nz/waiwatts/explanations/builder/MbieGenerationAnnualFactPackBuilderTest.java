package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test Fact Pack determinism - same DB state + same inputs should produce identical Fact Pack JSON
 */
@ExtendWith(MockitoExtension.class)
class MbieGenerationAnnualFactPackBuilderTest {

    @Mock
    private MbieGenerationAnnualRecordRepository repository;

    private MbieGenerationAnnualFactPackBuilder builder;
    private DatasetSource datasetSource;
    private DatasetRelease datasetRelease;

    @BeforeEach
    void setUp() {
        builder = new MbieGenerationAnnualFactPackBuilder(repository);
        
        // Setup test data
        datasetSource = new DatasetSource();
        datasetSource.setCode("mbie.generation.annual");
        
        datasetRelease = new DatasetRelease();
        datasetRelease.setId(UUID.randomUUID());
        datasetRelease.setDatasetSource(datasetSource);
        datasetRelease.setContentHash("sha256:test123");
        datasetRelease.setRetrievedAt(LocalDateTime.now());
        datasetRelease.setStatus(ReleaseStatus.IMPORTED);
    }

    @Test
    void testDeterminismForRenewableGenerationTrend() {
        // Setup request
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        // Setup mock data
        List<MbieGenerationAnnualRecord> records = createTestRecords();
        when(repository.findAll()).thenReturn(records);

        // Build Fact Pack twice
        FactPack factPack1 = builder.buildFactPack(request);
        FactPack factPack2 = builder.buildFactPack(request);

        // Assert determinism - identical structure and content
        assertEquals(factPack1.getFactPackVersion(), factPack2.getFactPackVersion());
        assertEquals(factPack1.getRequestContext().getQuestionType(), factPack2.getRequestContext().getQuestionType());
        assertEquals(factPack1.getRequestContext().getDatasetScope(), factPack2.getRequestContext().getDatasetScope());
        
        // Check that time series facts are identical
        assertEquals(factPack1.getFacts().getTimeSeries().size(), factPack2.getFacts().getTimeSeries().size());
        if (!factPack1.getFacts().getTimeSeries().isEmpty()) {
            var ts1 = factPack1.getFacts().getTimeSeries().get(0);
            var ts2 = factPack2.getFacts().getTimeSeries().get(0);
            assertEquals(ts1.getId(), ts2.getId());
            assertEquals(ts1.getMetricName(), ts2.getMetricName());
            assertEquals(ts1.getUnit(), ts2.getUnit());
            assertEquals(ts1.getPoints().size(), ts2.getPoints().size());
            
            // Check stable ordering of data points
            for (int i = 0; i < ts1.getPoints().size(); i++) {
                assertEquals(ts1.getPoints().get(i).getPeriod(), ts2.getPoints().get(i).getPeriod());
                assertEquals(ts1.getPoints().get(i).getValue(), ts2.getPoints().get(i).getValue());
            }
        }
    }

    @Test
    void testCanHandle() {
        ExplanationRequest supportedRequest = new ExplanationRequest(
            "any",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        ExplanationRequest unsupportedRequest = new ExplanationRequest(
            "any",
            Map.of("datasetSource", "other.source")
        );

        assertTrue(builder.canHandle(supportedRequest));
        assertFalse(builder.canHandle(unsupportedRequest));
    }

    @Test
    void testGetSupportedDatasetSourceCode() {
        assertEquals("mbie.generation.annual", builder.getSupportedDatasetSourceCode());
    }

    private List<MbieGenerationAnnualRecord> createTestRecords() {
        // Create deterministic test data with stable ordering
        return List.of(
            createRecord(2022, "HYDRO", new BigDecimal("25000")),
            createRecord(2022, "WIND", new BigDecimal("5000")),
            createRecord(2022, "GEOTHERMAL", new BigDecimal("8000")),
            createRecord(2023, "HYDRO", new BigDecimal("26000")),
            createRecord(2023, "WIND", new BigDecimal("6000")),
            createRecord(2023, "GEOTHERMAL", new BigDecimal("8200"))
        );
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