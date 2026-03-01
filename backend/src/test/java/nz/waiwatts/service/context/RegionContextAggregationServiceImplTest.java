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

        assertThat(result.regionId()).isEqualTo("Northland");
        assertThat(result.water().trend().unitCount()).isEqualTo(0);
        assertThat(result.water().state().unitCount()).isEqualTo(0);
        assertThat(result.energy().latestYear()).isEqualTo(0);
    }

    @Test
    void getRegionContext_trendData_computesPercentages() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord improving = createTrendRecord(release, "Auckland", "ECOLI", 2);
        LawaTrendMultiYearRecord degrading = createTrendRecord(release, "Auckland", "ECOLI", -1);
        LawaTrendMultiYearRecord indeterminate = createTrendRecord(release, "Auckland", "ECOLI", 0);
        LawaTrendMultiYearRecord insufficient = createTrendRecord(release, "Auckland", "ECOLI", -99);

        when(trendRepository.findAll()).thenReturn(List.of(improving, degrading, indeterminate, insufficient));
        when(stateRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", null, null);

        assertThat(result.water().trend().unitCount()).isEqualTo(4);
        assertThat(result.water().trend().improvingPct()).isEqualTo(33.3);
        assertThat(result.water().trend().degradingPct()).isEqualTo(33.3);
        assertThat(result.water().trend().indeterminatePct()).isEqualTo(33.3);
        assertThat(result.water().trend().insufficientPct()).isEqualTo(25.0);
    }

    @Test
    void getRegionContext_stateData_computesBandDistribution() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaStateMultiYearRecord bandA = createStateRecord(release, "Auckland", "ECOLI", "A", "site1");
        LawaStateMultiYearRecord bandB1 = createStateRecord(release, "Auckland", "ECOLI", "B", "site2");
        LawaStateMultiYearRecord bandB2 = createStateRecord(release, "Auckland", "ECOLI", "B", "site3");
        LawaStateMultiYearRecord bandC = createStateRecord(release, "Auckland", "ECOLI", "C", "site4");
        LawaStateMultiYearRecord insufficient = createStateRecord(release, "Auckland", "ECOLI", null, "site5");

        when(stateRepository.findAll()).thenReturn(List.of(bandA, bandB1, bandB2, bandC, insufficient));
        when(trendRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", null, null);

        assertThat(result.water().state().unitCount()).isEqualTo(5);
        assertThat(result.water().state().bandDistribution().get("A")).isEqualTo(1);
        assertThat(result.water().state().bandDistribution().get("B")).isEqualTo(2);
        assertThat(result.water().state().bandDistribution().get("C")).isEqualTo(1);
        assertThat(result.water().state().bandDistribution().get("INSUFFICIENT")).isEqualTo(1);
    }

    @Test
    void getRegionContext_indicatorFilter_acceptsDisplayLabelForNormalizedIndicators() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord trend = createTrendRecord(release, "Auckland", "ECOLI", 2);
        LawaStateMultiYearRecord state = createStateRecord(release, "Auckland", "ECOLI", "A", "site1");

        when(trendRepository.findAll()).thenReturn(List.of(trend));
        when(stateRepository.findAll()).thenReturn(List.of(state));
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", "E. coli", null);

        assertThat(result.water().trend().unitCount()).isEqualTo(1);
        assertThat(result.water().state().unitCount()).isEqualTo(1);
    }

    @Test
    void getRegionContext_stateData_withoutIndicatorFilter_excludesNullUnitKeysFromUnitCount() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaStateMultiYearRecord valid = createStateRecord(release, "Auckland", "ECOLI", "A", "site1");
        LawaStateMultiYearRecord nullSite = createStateRecord(release, "Auckland", "ECOLI", "B", null);
        LawaStateMultiYearRecord nullIndicator = createStateRecord(release, "Auckland", null, "C", "site2");

        when(stateRepository.findAll()).thenReturn(List.of(valid, nullSite, nullIndicator));
        when(trendRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", null, null);

        assertThat(result.water().state().unitCount()).isEqualTo(1);
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
        when(trendRepository.findAll()).thenReturn(List.of());
        when(stateRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", null, null);

        assertThat(result.energy().latestYear()).isEqualTo(2022);
        assertThat(result.energy().latestRenewablePct()).isEqualTo(80.0);
        assertThat(result.energy().fossilLatestPct()).isEqualTo(20.0);
        assertThat(result.energy().renewable5YrDeltaPct()).isEqualTo(20.0);
    }

    @Test
    void getRegionContext_withIndicatorFilter_usesSiteUnit() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord site1 = createTrendRecord(release, "Auckland", "ECOLI", 2);
        LawaTrendMultiYearRecord site2 = createTrendRecord(release, "Auckland", "ECOLI", -1);

        when(trendRepository.findAll()).thenReturn(List.of(site1, site2));
        when(stateRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", "ECOLI", null);

        assertThat(result.water().trend().unitCount()).isEqualTo(2);
        assertThat(result.water().trend().improvingPct()).isEqualTo(50.0);
    }

    @Test
    void getRegionContext_trendDeduplicatesByCanonicalPeriod() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord period5 = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", 2, 5);
        LawaTrendMultiYearRecord period10 = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", 1, 10);

        when(trendRepository.findAll()).thenReturn(List.of(period5, period10));
        when(stateRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", "ECOLI", null);

        assertThat(result.water().trend().unitCount()).isEqualTo(1);
        assertThat(result.water().trend().improvingPct()).isEqualTo(100.0);
    }

    @Test
    void getRegionContext_trendCanonicalFallback_prefersCanonicalOverHigherNonCanonicalPeriod() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord period25 = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", -1, 25);
        LawaTrendMultiYearRecord period20 = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", 2, 20);

        when(trendRepository.findAll()).thenReturn(List.of(period25, period20));
        when(stateRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", "ECOLI", null);

        assertThat(result.water().trend().unitCount()).isEqualTo(1);
        assertThat(result.water().trend().improvingPct()).isEqualTo(100.0);
        assertThat(result.water().trend().degradingPct()).isEqualTo(0.0);
    }

    @Test
    void getRegionContext_trendCanonicalFallback_prefersHigherCanonicalPriorityOrder() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord period15 = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", -1, 15);
        LawaTrendMultiYearRecord period20 = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", 2, 20);

        when(trendRepository.findAll()).thenReturn(List.of(period15, period20));
        when(stateRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", "ECOLI", null);

        assertThat(result.water().trend().unitCount()).isEqualTo(1);
        assertThat(result.water().trend().improvingPct()).isEqualTo(100.0);
        assertThat(result.water().trend().degradingPct()).isEqualTo(0.0);
    }

    @Test
    void getRegionContext_trendWithPreferredPeriod_filtersCorrectly() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord period5 = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", 2, 5);
        LawaTrendMultiYearRecord period10 = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", -1, 10);

        when(trendRepository.findAll()).thenReturn(List.of(period5, period10));
        when(stateRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", "ECOLI", 10);

        assertThat(result.water().trend().unitCount()).isEqualTo(1);
        assertThat(result.water().trend().degradingPct()).isEqualTo(100.0);
    }

    @Test
    void getRegionContext_trendWithPreferredPeriod_doesNotReplaceExistingPreferredRecord() {
        DatasetRelease release = new DatasetRelease();
        release.setId(UUID.randomUUID());

        LawaTrendMultiYearRecord preferredExisting = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", -1, 10);
        LawaTrendMultiYearRecord preferredLater = createTrendRecordWithPeriod(release, "Auckland", "ECOLI", 2, 10);

        when(trendRepository.findAll()).thenReturn(List.of(preferredExisting, preferredLater));
        when(stateRepository.findAll()).thenReturn(List.of());
        when(mbieRepository.findAll()).thenReturn(List.of());

        RegionContextFactPackDto result = service.getRegionContext("Auckland", "ECOLI", 10);

        assertThat(result.water().trend().unitCount()).isEqualTo(1);
        assertThat(result.water().trend().degradingPct()).isEqualTo(100.0);
        assertThat(result.water().trend().improvingPct()).isEqualTo(0.0);
    }

    private LawaTrendMultiYearRecord createTrendRecord(DatasetRelease release, String region, String indicator, Integer score) {
        LawaTrendMultiYearRecord r = new LawaTrendMultiYearRecord();
        r.setDatasetRelease(release);
        r.setRegion(region);
        r.setLawaSiteId("site-" + UUID.randomUUID().toString().substring(0, 8));
        r.setIndicatorNorm(indicator);
        r.setTrendScore(score);
        r.setTrendPeriodYears(5);
        return r;
    }

    private LawaTrendMultiYearRecord createTrendRecordWithPeriod(DatasetRelease release, String region, String indicator, Integer score, Integer period) {
        LawaTrendMultiYearRecord r = new LawaTrendMultiYearRecord();
        r.setDatasetRelease(release);
        r.setRegion(region);
        r.setLawaSiteId("site-1");
        r.setIndicatorNorm(indicator);
        r.setTrendScore(score);
        r.setTrendPeriodYears(period);
        return r;
    }

    private LawaStateMultiYearRecord createStateRecord(DatasetRelease release, String region, String indicator, String band, String siteId) {
        LawaStateMultiYearRecord r = new LawaStateMultiYearRecord();
        r.setDatasetRelease(release);
        r.setRegion(region);
        r.setLawaSiteId(siteId);
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
