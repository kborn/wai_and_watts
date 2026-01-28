package nz.waiwatts.ingestion.mbie;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ExpectedFormat;
import nz.waiwatts.domain.datasets.Publisher;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
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
class MbieQuarterlyIngestionIntegrationTest {

    private static final String SOURCE_CODE = "mbie.generation.quarterly";
    private static final String FIXTURE_PATH = "fixtures/mbie/generation/quarterly/mbie_generation_quarterly_fixture_phase7.csv";

    @Autowired
    private DatasetSourceRepository sourceRepository;

    @Autowired
    private DatasetReleaseRepository releaseRepository;

    @Autowired
    private MbieQuarterlyIngestion mbieQuarterlyIngestion;

    @Autowired
    private MbieGenerationQuarterlyRecordRepository recordRepository;

    @BeforeEach
    void setup() {
        recordRepository.deleteAll();
        releaseRepository.deleteAll();
        sourceRepository.deleteAll();

        DatasetSource src = new DatasetSource();
        src.setId(UUID.randomUUID());
        src.setName("MBIE Electricity Generation (Fuel Type, Quarterly)");
        src.setPublisher(Publisher.MBIE);
        src.setCode(SOURCE_CODE);
        src.setSourceUrl("https://example.com/mbie-quarterly-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("quarterly");
        sourceRepository.save(src);
    }

    @Test
    void ingest_fixture_persists_rows_and_is_idempotent() {
        UUID rel1 = mbieQuarterlyIngestion.ingestFixture(SOURCE_CODE, FIXTURE_PATH, LocalDate.of(2025, 9, 1), "MBIE Q3 2025 workbook");

        List<DatasetRelease> releasesAfterFirst = releaseRepository.findAll();
        assertThat(releasesAfterFirst).hasSize(1);
        assertThat(releasesAfterFirst.getFirst().getStatus()).isEqualTo(ReleaseStatus.IMPORTED);
        assertThat(recordRepository.count()).isEqualTo(108);

        UUID rel2 = mbieQuarterlyIngestion.ingestFixture(SOURCE_CODE, FIXTURE_PATH, LocalDate.of(2025, 9, 1), "MBIE Q3 2025 workbook");

        assertThat(rel2).isEqualTo(rel1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(108);
    }
}
