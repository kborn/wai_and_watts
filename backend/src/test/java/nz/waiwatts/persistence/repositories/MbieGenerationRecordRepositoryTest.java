package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.datasets.*;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MbieGenerationRecordRepositoryTest {

    @Autowired
    private DatasetSourceRepository sourceRepo;

    @Autowired
    private DatasetReleaseRepository releaseRepo;

    @Autowired
    private MbieGenerationAnnualRecordRepository genRepo;

    @Test
    void saveAndReadGenerationRecord() {
        // Arrange: create lineage
        DatasetSource src = new DatasetSource();
        src.setName("MBIE Electricity Generation Test");
        src.setPublisher(Publisher.MBIE);
        src.setCode("test.mbie.generation.annual");
        src.setSourceUrl("https://example.com/test-mbie-annual.xlsx");
        src.setExpectedFormat(ExpectedFormat.XLSX);
        src.setUpdateCadence("annual");
        src = sourceRepo.save(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 9, 1));
        rel.setReleaseLabel("Sept 2025 Q3 workbook");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.save(rel);

        // Act: persist a generation record
        MbieGenerationAnnualRecord rec = new MbieGenerationAnnualRecord();
        rec.setDatasetRelease(rel);
        rec.setPeriodYear(2022);
        rec.setFuelTypeRaw("Hydro");
        rec.setFuelTypeNorm("HYDRO");
        rec.setGenerationGwh(new BigDecimal("26071"));
        genRepo.save(rec);

        // Assert
        assertThat(genRepo.count()).isEqualTo(1);
        MbieGenerationAnnualRecord saved = genRepo.findAll().getFirst();
        assertThat(saved.getDatasetRelease().getId()).isEqualTo(rel.getId());
        assertThat(saved.getPeriodYear()).isEqualTo(2022);
        assertThat(saved.getFuelTypeNorm()).isEqualTo("HYDRO");
        assertThat(saved.getGenerationGwh()).isEqualTo(new BigDecimal("26071"));
    }

    @Test
    void findForReadApi_withNullAndCaseInsensitiveFilters_executesAndMatches() {
        DatasetSource src = new DatasetSource();
        src.setName("MBIE Electricity Generation Query Test");
        src.setPublisher(Publisher.MBIE);
        src.setCode("test.mbie.generation.annual.query");
        src.setSourceUrl("https://example.com/test-mbie-annual-query.xlsx");
        src.setExpectedFormat(ExpectedFormat.XLSX);
        src.setUpdateCadence("annual");
        src = sourceRepo.saveAndFlush(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2025, 9, 1));
        rel.setReleaseLabel("Sept 2025 Q3 workbook");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("sha256:dummy-query");
        rel.setStatus(ReleaseStatus.PENDING);
        rel = releaseRepo.saveAndFlush(rel);

        MbieGenerationAnnualRecord rec = new MbieGenerationAnnualRecord();
        rec.setDatasetRelease(rel);
        rec.setPeriodYear(2022);
        rec.setFuelTypeRaw("Hydro");
        rec.setFuelTypeNorm("HYDRO");
        rec.setGenerationGwh(new BigDecimal("26071"));
        genRepo.saveAndFlush(rec);

        List<MbieGenerationAnnualRecord> all = genRepo.findForReadApi(null, null, null);
        assertThat(all).isNotEmpty();

        List<MbieGenerationAnnualRecord> hydro = genRepo.findForReadApi(2020, 2024, "hydro");
        assertThat(hydro).hasSize(1);
        assertThat(hydro.getFirst().getFuelTypeNorm()).isEqualTo("HYDRO");
    }
}
