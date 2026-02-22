package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.datasets.*;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class LawaTrendMultiYearRecordRepositoryTest {

    @Autowired
    private DatasetSourceRepository sourceRepo;

    @Autowired
    private DatasetReleaseRepository releaseRepo;

    @Autowired
    private LawaTrendMultiYearRecordRepository lawaTrendMultiYearRepo;

    @Test
    void saveAndRead() {

        DatasetSource src = new DatasetSource();
        src.setName("LAWA Water Quality Trend (Multi-Year) Test");
        src.setPublisher(Publisher.LAWA);
        src.setCode("test.lawa.water_quality.trend.multi_year");
        src.setSourceUrl("https://example.com/lawa-trend-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV); // or XLSX per fixture
        src.setUpdateCadence("annual");
        src = sourceRepo.saveAndFlush(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 1, 1));
        rel.setReleaseLabel("LAWA Trend 2025");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy-lawa-trend");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.saveAndFlush(rel);

        LawaTrendMultiYearRecord rec = new LawaTrendMultiYearRecord();
        rec.setDatasetRelease(rel);
        rec.setLawaSiteId("arc-00036");
        rec.setSiteName("Avondale @ Shadbolt");
        rec.setRegion("auckland");
        rec.setIndicatorRaw("E. coli");
        rec.setIndicatorNorm("E_COLI");
        rec.setTrendRaw("Improving");
        rec.setTrendNorm("IMPROVING");
        rec.setTrendScore(3);
        rec.setTrendPeriodYears(5);
        rec.setTrendDataFrequency("Monthly");
        rec.setPeriodType("HYDRO_5YR_ROLLING");
        rec.setPeriodEndYear(2024);
        rec.setPeriodStartYear(2019);
        lawaTrendMultiYearRepo.save(rec);

        assertThat(lawaTrendMultiYearRepo.count()).isEqualTo(1);
        LawaTrendMultiYearRecord saved = lawaTrendMultiYearRepo.findAll().getFirst();
        assertThat(saved.getDatasetRelease().getId()).isEqualTo(rel.getId());
        assertThat(saved.getPeriodEndYear()).isEqualTo(2024);
        assertThat(saved.getIndicatorNorm()).isEqualTo("E_COLI");
        assertThat(saved.getTrendNorm()).isEqualTo("IMPROVING");
        assertThat(saved.getTrendScore()).isEqualTo(3);

    }
}