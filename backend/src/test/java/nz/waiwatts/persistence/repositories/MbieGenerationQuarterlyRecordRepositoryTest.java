package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.datasets.*;
import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MbieGenerationQuarterlyRecordRepositoryTest {

    @Autowired
    private DatasetSourceRepository sourceRepo;

    @Autowired
    private DatasetReleaseRepository releaseRepo;

    @Autowired
    private MbieGenerationQuarterlyRecordRepository quarterlyRepo;

    @Test
    void saveAndReadQuarterlyRecord() {
        // Arrange: create lineage
        DatasetSource src = new DatasetSource();
        src.setName("MBIE Electricity Generation (Fuel Type, Quarterly) Test");
        src.setPublisher(Publisher.MBIE);
        src.setCode("test.mbie.generation.quarterly");
        src.setSourceUrl("https://example.com/mbie-q-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("quarterly");
        src = sourceRepo.saveAndFlush(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 9, 1));
        rel.setReleaseLabel("Sept 2025 Q3 workbook");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy-q");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.saveAndFlush(rel);

        // Act
        MbieGenerationQuarterlyRecord rec = new MbieGenerationQuarterlyRecord();
        rec.setDatasetRelease(rel);
        rec.setPeriodYear(2024);
        rec.setPeriodQuarter(3);
        rec.setFuelTypeRaw("Hydro");
        rec.setFuelTypeNorm("HYDRO");
        rec.setGenerationGwh(new BigDecimal("5566.9"));
        quarterlyRepo.save(rec);

        // Assert
        assertThat(quarterlyRepo.count()).isEqualTo(1);
        MbieGenerationQuarterlyRecord saved = quarterlyRepo.findAll().getFirst();
        assertThat(saved.getDatasetRelease().getId()).isEqualTo(rel.getId());
        assertThat(saved.getPeriodYear()).isEqualTo(2024);
        assertThat(saved.getPeriodQuarter()).isEqualTo(3);
        assertThat(saved.getFuelTypeNorm()).isEqualTo("HYDRO");
        assertThat(saved.getGenerationGwh()).isEqualByComparingTo("5566.9");
    }

    @Test
    void findForReadApi_withNullAndCaseInsensitiveFilters_executesAndMatches() {
        DatasetSource src = new DatasetSource();
        src.setName("MBIE Electricity Generation Quarterly Query Test");
        src.setPublisher(Publisher.MBIE);
        src.setCode("test.mbie.generation.quarterly.query");
        src.setSourceUrl("https://example.com/mbie-q-query-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("quarterly");
        src = sourceRepo.saveAndFlush(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 9, 1));
        rel.setReleaseLabel("Sept 2025 Q3 workbook");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy-q-query");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.saveAndFlush(rel);

        MbieGenerationQuarterlyRecord rec = new MbieGenerationQuarterlyRecord();
        rec.setDatasetRelease(rel);
        rec.setPeriodYear(2024);
        rec.setPeriodQuarter(3);
        rec.setFuelTypeRaw("Hydro");
        rec.setFuelTypeNorm("HYDRO");
        rec.setGenerationGwh(new BigDecimal("5566.9"));
        quarterlyRepo.saveAndFlush(rec);

        List<MbieGenerationQuarterlyRecord> all = quarterlyRepo.findForReadApi(null, null, null, null);
        assertThat(all).isNotEmpty();

        List<MbieGenerationQuarterlyRecord> hydroQ3 = quarterlyRepo.findForReadApi(2024, 2024, 3, "hydro");
        assertThat(hydroQ3).hasSize(1);
        assertThat(hydroQ3.getFirst().getFuelTypeNorm()).isEqualTo("HYDRO");
    }
}
