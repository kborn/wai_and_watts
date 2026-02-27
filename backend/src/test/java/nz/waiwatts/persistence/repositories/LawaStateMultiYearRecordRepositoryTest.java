package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.datasets.*;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class LawaStateMultiYearRecordRepositoryTest {

    @Autowired
    private DatasetSourceRepository sourceRepo;

    @Autowired
    private DatasetReleaseRepository releaseRepo;

    @Autowired
    private LawaStateMultiYearRecordRepository lawaStateMultiYearRepo;

    @Test
    void saveAndRead() {

        DatasetSource src = new DatasetSource();
        src.setName("LAWA Water Quality State (Multi-Year) Test");
        src.setPublisher(Publisher.LAWA);
        src.setCode("test.lawa.water_quality.state.multi_year");
        src.setSourceUrl("https://example.com/lawa-state-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV); // or XLSX per fixture
        src.setUpdateCadence("annual");
        src = sourceRepo.saveAndFlush(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 1, 1));
        rel.setReleaseLabel("LAWA State 2025");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy-lawa");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.saveAndFlush(rel);

        LawaStateMultiYearRecord rec = new LawaStateMultiYearRecord();
        rec.setDatasetRelease(rel);
        rec.setLawaSiteId("arc-00036");
        rec.setSiteName("Avondale @ Shadbolt");
        rec.setRegion("auckland");
        rec.setIndicatorRaw("E. coli");
        rec.setIndicatorNorm("E_COLI");
        rec.setAttributeBand("B");
        rec.setStateNorm("GOOD");
        rec.setPeriodType("HYDRO_5YR_ROLLING");
        rec.setPeriodEndYear(2024);
        rec.setPeriodStartYear(2019);
        lawaStateMultiYearRepo.save(rec);

        assertThat(lawaStateMultiYearRepo.count()).isEqualTo(1);
        LawaStateMultiYearRecord saved = lawaStateMultiYearRepo.findAll().getFirst();
        assertThat(saved.getDatasetRelease().getId()).isEqualTo(rel.getId());
        assertThat(saved.getPeriodEndYear()).isEqualTo(2024);
        assertThat(saved.getIndicatorNorm()).isEqualTo("E_COLI");

    }

    @Test
    void findForReadApi_regionFilter_isCaseInsensitive() {
        DatasetSource src = new DatasetSource();
        src.setName("LAWA Water Quality State (Multi-Year) Test");
        src.setPublisher(Publisher.LAWA);
        src.setCode("test.lawa.water_quality.state.multi_year.case");
        src.setSourceUrl("https://example.com/lawa-state-case-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("annual");
        src = sourceRepo.saveAndFlush(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 1, 1));
        rel.setReleaseLabel("LAWA State 2025");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy-lawa-case");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.saveAndFlush(rel);

        LawaStateMultiYearRecord rec = new LawaStateMultiYearRecord();
        rec.setDatasetRelease(rel);
        rec.setLawaSiteId("arc-00037");
        rec.setSiteName("Case Test Site");
        rec.setRegion("Auckland");
        rec.setIndicatorRaw("E. coli");
        rec.setIndicatorNorm("e_coli");
        rec.setAttributeBand("B");
        rec.setStateNorm("GOOD");
        rec.setPeriodType("HYDRO_5YR_ROLLING");
        rec.setPeriodEndYear(2024);
        rec.setPeriodStartYear(2019);
        lawaStateMultiYearRepo.saveAndFlush(rec);

        List<LawaStateMultiYearRecord> out = lawaStateMultiYearRepo.findForReadApi(
                null, null, null, "auckland");

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getRegion()).isEqualTo("Auckland");
    }

    @Test
    void findDistinctRegionOrderByRegion_returnsNormalizedLowercaseValues() {
        DatasetSource src = new DatasetSource();
        src.setName("LAWA Water Quality State (Multi-Year) Test");
        src.setPublisher(Publisher.LAWA);
        src.setCode("test.lawa.water_quality.state.multi_year.regions");
        src.setSourceUrl("https://example.com/lawa-state-regions-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("annual");
        src = sourceRepo.saveAndFlush(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 1, 1));
        rel.setReleaseLabel("LAWA State 2025");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy-lawa-regions");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.saveAndFlush(rel);

        LawaStateMultiYearRecord rec1 = new LawaStateMultiYearRecord();
        rec1.setDatasetRelease(rel);
        rec1.setLawaSiteId("arc-00038");
        rec1.setSiteName("Region A");
        rec1.setRegion("Auckland");
        rec1.setIndicatorRaw("E. coli");
        rec1.setIndicatorNorm("e_coli");
        rec1.setAttributeBand("B");
        rec1.setStateNorm("GOOD");
        rec1.setPeriodType("HYDRO_5YR_ROLLING");
        rec1.setPeriodEndYear(2024);
        rec1.setPeriodStartYear(2019);

        LawaStateMultiYearRecord rec2 = new LawaStateMultiYearRecord();
        rec2.setDatasetRelease(rel);
        rec2.setLawaSiteId("arc-00039");
        rec2.setSiteName("Region B");
        rec2.setRegion("auckland");
        rec2.setIndicatorRaw("E. coli");
        rec2.setIndicatorNorm("e_coli");
        rec2.setAttributeBand("B");
        rec2.setStateNorm("GOOD");
        rec2.setPeriodType("HYDRO_5YR_ROLLING");
        rec2.setPeriodEndYear(2024);
        rec2.setPeriodStartYear(2019);

        lawaStateMultiYearRepo.saveAndFlush(rec1);
        lawaStateMultiYearRepo.saveAndFlush(rec2);

        List<String> regions = lawaStateMultiYearRepo.findDistinctRegionOrderByRegion();
        assertThat(regions).contains("auckland");
        assertThat(regions).doesNotContain("Auckland");
    }

    @Test
    void findForReadApi_withAllOptionalFiltersNull_returnsRecords() {
        DatasetSource src = new DatasetSource();
        src.setName("LAWA Water Quality State (Multi-Year) Null Filters");
        src.setPublisher(Publisher.LAWA);
        src.setCode("test.lawa.water_quality.state.multi_year.null");
        src.setSourceUrl("https://example.com/lawa-state-null-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("annual");
        src = sourceRepo.saveAndFlush(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 1, 1));
        rel.setReleaseLabel("LAWA State 2025");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy-lawa-state-null");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.saveAndFlush(rel);

        LawaStateMultiYearRecord rec = new LawaStateMultiYearRecord();
        rec.setDatasetRelease(rel);
        rec.setLawaSiteId("arc-state-null");
        rec.setSiteName("State Null Site");
        rec.setRegion("Auckland");
        rec.setIndicatorRaw("E. coli");
        rec.setIndicatorNorm("ECOLI");
        rec.setAttributeBand("B");
        rec.setStateNorm("GOOD");
        rec.setPeriodType("HYDRO_5YR_ROLLING");
        rec.setPeriodStartYear(2019);
        rec.setPeriodEndYear(2024);
        lawaStateMultiYearRepo.saveAndFlush(rec);

        List<LawaStateMultiYearRecord> out = lawaStateMultiYearRepo.findForReadApi(
                null, null, null, null
        );

        assertThat(out).isNotEmpty();
        assertThat(out).anyMatch(r -> "arc-state-null".equals(r.getLawaSiteId()));
    }

    @Test
    void findForReadApi_indicatorFilter_isCaseInsensitive() {
        DatasetSource src = new DatasetSource();
        src.setName("LAWA Water Quality State (Multi-Year) Indicator Case");
        src.setPublisher(Publisher.LAWA);
        src.setCode("test.lawa.water_quality.state.multi_year.indicator.case");
        src.setSourceUrl("https://example.com/lawa-state-indicator-case-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("annual");
        src = sourceRepo.saveAndFlush(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 1, 1));
        rel.setReleaseLabel("LAWA State 2025");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy-lawa-state-indicator-case");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.saveAndFlush(rel);

        LawaStateMultiYearRecord rec = new LawaStateMultiYearRecord();
        rec.setDatasetRelease(rel);
        rec.setLawaSiteId("arc-state-ind");
        rec.setSiteName("Indicator Case Site");
        rec.setRegion("Auckland");
        rec.setIndicatorRaw("E. coli");
        rec.setIndicatorNorm("ECOLI");
        rec.setAttributeBand("B");
        rec.setStateNorm("GOOD");
        rec.setPeriodType("HYDRO_5YR_ROLLING");
        rec.setPeriodStartYear(2019);
        rec.setPeriodEndYear(2024);
        lawaStateMultiYearRepo.saveAndFlush(rec);

        List<LawaStateMultiYearRecord> out = lawaStateMultiYearRepo.findForReadApi(
                2019, 2024, "ecoli", "auckland"
        );

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getLawaSiteId()).isEqualTo("arc-state-ind");
    }
}
