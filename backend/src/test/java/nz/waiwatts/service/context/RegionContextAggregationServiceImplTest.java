package nz.waiwatts.service.context;

import nz.waiwatts.api.context.dto.*;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class RegionContextAggregationServiceImplTest {

    private LawaTrendMultiYearRecordRepository trendRepository;
    private LawaStateMultiYearRecordRepository stateRepository;
    private MbieGenerationAnnualRecordRepository mbieRepository;
    private RegionContextAggregationServiceImpl service;

    @BeforeEach
    void setup() {
        trendRepository = Mockito.mock(LawaTrendMultiYearRecordRepository.class);
        stateRepository = Mockito.mock(LawaStateMultiYearRecordRepository.class);
        mbieRepository = Mockito.mock(MbieGenerationAnnualRecordRepository.class);
        service = new RegionContextAggregationServiceImpl(trendRepository, stateRepository, mbieRepository);
    }

    @Test
    void getRegionContext_noData_returnsZeros() {
        when(trendRepository.findAll()).thenReturn(List.of());
        when(stateRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Northland", null, null);

        assertThat(result.getRegionId()).isEqualTo("Northland");
        assertThat(result.getWater().getTrend().getTotalSites()).isEqualTo(0);
        assertThat(result.getWater().getState().getTotalSites()).isEqualTo(0);
        assertThat(result.getEnergy().getLatestYear()).isEqualTo(0);
    }

    @Test
    void getRegionContext_trendData_computesPercentages() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord improving = createTrendRecord(release, "Auckland", "E. coli", 2);
        LawaTrendMultiYearRecord degrading = createTrendRecord(release, "Auckland", "E. coli", -1);
        LawaTrendMultiYearRecord indeterminate = createTrendRecord(release, "Auckland", "E. coli", 0);
        LawaTrendMultiYearRecord insufficient = createTrendRecord(release, "Auckland", "E. coli", -99);

        when(trendRepository.findAll()).thenReturn(List.of(improving, degrading, indeterminate, insufficient));

        RegionContextFactPackDto result = service.getRegionContext("Auckland", null, null);

        assertThat(result.getWater().getTrend().getTotalSites()).isEqualTo(4);
        assertThat(result.getWater().getTrend().getImprovingPct()).isEqualTo(25.0);
        assertThat(result.getWater().getTrend().getDegradingPct()).isEqualTo(25.0);
        assertThat(result.getWater().getTrend().getIndeterminatePct()).isEqualTo(25.0);
        assertThat(result.getWater().getTrend().getInsufficientPct()).isEqualTo(25.0);
    }

    @Test
    void getRegionContext_stateData_computesBandDistribution() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaStateMultiYearRecord bandA = createStateRecord(release, "Auckland", "E. coli", "A");
        LawaStateMultiYearRecord bandB1 = createStateRecord(release, "Auckland", "E. coli", "B");
        LawaStateMultiYearRecord bandB2 = createStateRecord(release, "Auckland", "E. coli", "B");
        LawaStateMultiYearRecord bandC = createStateRecord(release, "Auckland", "E. coli", "C");
        LawaStateMultiYearRecord insufficient = createStateRecord(release, "Auckland", "E. coli", null);

        when(stateRepository.findAll()).thenReturn(List.of(bandA, bandB1, bandB2, bandC, insufficient));

        RegionContextFactPackDto result = service.getRegionContext("Auckland", null, null);

        assertThat(result.getWater().getState().getTotalSites()).isEqualTo(5);
        assertThat(result.getWater().getState().getBandDistribution().get("A")).isEqualTo(1);
        assertThat(result.getWater().getState().getBandDistribution().get("B")).isEqualTo(2);
        assertThat(result.getWater().getState().getBandDistribution().get("C")).isEqualTo(1);
        assertThat(result.getWater().getState().getBandDistribution().get("INSUFFICIENT")).isEqualTo(1);
    }

    @Test
    void getRegionContext_mbieData_computesRenewableShare() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        MbieGenerationAnnualRecord hydro2022 = createMbieRecord(release, 2022, "HYDRO", "6000");
        MbieGenerationAnnualRecord wind2022 = createMbieRecord(release, 2022, "WIND", "2000");
        MbieGenerationAnnualRecord coal2022 = createMbieRecord(release, 2022, "COAL", "2000");

        MbieGenerationAnnualRecord hydro2017 = createMbieRecord(release, 2017, "HYDRO", "5000");
        MbieGenerationAnnualRecord wind2017 = createMbieRecord(release, 2017, "WIND", "1000");
        MbieGenerationAnnualRecord coal2017 = createMbieRecord(release, 2017, "COAL", "4000");

        when(mbieRepository.findAll()).thenReturn(List.of(hydro2022, wind2022, coal2022, hydro2017, wind2017, coal2017));

        RegionContextFactPackDto result = service.getRegionContext("Auckland", null, null);

        assertThat(result.getEnergy().getLatestYear()).isEqualTo(2022);
        assertThat(result.getEnergy().getLatestRenewablePct()).isEqualTo(80.0);
        assertThat(result.getEnergy().getFossilLatestPct()).isEqualTo(20.0);
        assertThat(result.getEnergy().getRenewable5YrDeltaPct()).isEqualTo(20.0);
    }

    @Test
    void getRegionContext_withFilters_filtersCorrectly() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord regional = createTrendRecord(release, "Auckland", "E. coli", 2);
        LawaTrendMultiYearRecord otherRegion = createTrendRecord(release, "Waikato", "E. coli", -1);
        LawaTrendMultiYearRecord otherIndicator = createTrendRecord(release, "Auckland", "TN", 1);

        when(trendRepository.findAll()).thenReturn(List.of(regional, otherRegion, otherIndicator));

        RegionContextFactPackDto result = service.getRegionContext("Auckland", "E. coli", null);

        assertThat(result.getWater().getTrend().getTotalSites()).isEqualTo(1);
        assertThat(result.getWater().getTrend().getImprovingPct()).isEqualTo(100.0);
    }

    @Test
    void getRegionContext_noRegion_returnsAllData() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord auckland = createTrendRecord(release, "Auckland", "E. coli", 2);
        LawaTrendMultiYearRecord waikato = createTrendRecord(release, "Waikato", "E. coli", -1);

        when(trendRepository.findAll()).thenReturn(List.of(auckland, waikato));

        RegionContextFactPackDto result = service.getRegionContext(null, null, null);

        assertThat(result.getRegionId()).isEqualTo("ALL");
        assertThat(result.getWater().getTrend().getTotalSites()).isEqualTo(2);
    }

    private LawaTrendMultiYearRecord createTrendRecord(DatasetRelease release, String region, String indicator, Integer score) {
        LawaTrendMultiYearRecord r = new LawaTrendMultiYearRecord();
        r.setDatasetRelease(release);
        r.setRegion(region);
        r.setIndicatorNorm(indicator);
        r.setTrendScore(score);
        r.setTrendPeriodYears(5);
        return r;
    }

    private LawaStateMultiYearRecord createStateRecord(DatasetRelease release, String region, String indicator, String band) {
        LawaStateMultiYearRecord r = new LawaStateMultiYearRecord();
        r.setDatasetRelease(release);
        r.setRegion(region);
        r.setIndicatorNorm(indicator);
        r.setAttributeBand(band);
        r.setMedian(band == null ? new BigDecimal("-99") : new BigDecimal("50"));
        return r;
    }

    private MbieGenerationAnnualRecord createMbieRecord(DatasetRelease release, int year, String fuelType, String gwh) {
        MbieGenerationAnnualRecord r = new MbieGenerationAnnualRecord();
        r.setDatasetRelease(release);
        r.setPeriodYear(year);
        r.setFuelTypeNorm(fuelType);
        r.setGenerationGwh(new BigDecimal(gwh));
        return r;
    }
}
