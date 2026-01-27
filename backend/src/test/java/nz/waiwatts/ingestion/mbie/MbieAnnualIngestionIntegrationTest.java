package nz.waiwatts.ingestion.mbie;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ExpectedFormat;
import nz.waiwatts.domain.datasets.Publisher;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MbieAnnualIngestionIntegrationTest {

    private static final String SOURCE_CODE = "mbie.generation.annual";
    private static final String FIXTURE_PATH = "fixtures/mbie/generation/annual/mbie_generation_annual_fixture_phase6.csv";

    @Autowired
    private DatasetSourceRepository sourceRepository;

    @Autowired
    private DatasetReleaseRepository releaseRepository;

    @Autowired
    private MbieAnnualIngestion mbieIngestion;

    @Autowired
    private MbieGenerationAnnualRecordRepository recordRepository;

    @BeforeEach
    void setup() {
        recordRepository.deleteAll();
        releaseRepository.deleteAll();
        sourceRepository.deleteAll();

        DatasetSource src = new DatasetSource();
        src.setId(UUID.randomUUID());
        src.setName("MBIE Electricity Generation (Fuel Type, Annual)");
        src.setPublisher(Publisher.MBIE);
        src.setCode(SOURCE_CODE);
        src.setSourceUrl("https://example.com/mbie-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("annual");
        sourceRepository.save(src);
    }

    @Test
    void ingest_fixture_persists_rows_and_is_idempotent() {
        // First ingest
        UUID rel1 = mbieIngestion.ingestFixture(SOURCE_CODE, FIXTURE_PATH, LocalDate.of(2025, 9, 1), "MBIE Q3 2025 workbook");

        List<DatasetRelease> releasesAfterFirst = releaseRepository.findAll();
        assertThat(releasesAfterFirst).hasSize(1);
        assertThat(releasesAfterFirst.getFirst().getStatus()).isEqualTo(ReleaseStatus.IMPORTED);
        assertThat(recordRepository.count()).isEqualTo(176); // 2003-2024 inclusive * 8 fuels

        // Second ingest (same content) should be a no-op (no new release, no new rows)
        UUID rel2 = mbieIngestion.ingestFixture(SOURCE_CODE, FIXTURE_PATH, LocalDate.of(2025, 9, 1), "MBIE Q3 2025 workbook");

        assertThat(rel2).isEqualTo(rel1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(176);
    }
}
