package nz.waiwatts.ingestion.lawa;

import nz.waiwatts.domain.datasets.*;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
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
class LawaStateMultiYearIngestionIntegrationTest {

    private static final String SOURCE_CODE = "lawa.water_quality.state.multi_year";
    private static final String FIXTURE_PATH = "fixtures/lawa/state/multi_year/lawa_state_multi_year_fixture.csv";

    @Autowired
    private DatasetSourceRepository sourceRepository;

    @Autowired
    private DatasetReleaseRepository releaseRepository;

    @Autowired
    private LawaStateMultiYearIngestion lawaIngestion;

    @Autowired
    private LawaStateMultiYearRecordRepository recordRepository;

    @BeforeEach
    void setup() {
        recordRepository.deleteAll();
        releaseRepository.deleteAll();
        sourceRepository.deleteAll();

        DatasetSource src = new DatasetSource();
        src.setId(UUID.randomUUID());
        src.setName("Lawa State Multi Year");
        src.setPublisher(Publisher.LAWA);
        src.setCode(SOURCE_CODE);
        src.setSourceUrl("https://example.com/lawa-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("multi-year");
        sourceRepository.save(src);
    }

    @Test
    void ingest_fixture_persists_rows_and_is_idempotent() {
        // First ingest
        UUID rel1 = lawaIngestion.ingestFixture(SOURCE_CODE, FIXTURE_PATH, LocalDate.of(2025, 9, 1), "LAWA Q3 2025 workbook");

        List<DatasetRelease> releasesAfterFirst = releaseRepository.findAll();
        assertThat(releasesAfterFirst).hasSize(1);
        assertThat(releasesAfterFirst.getFirst().getStatus()).isEqualTo(ReleaseStatus.IMPORTED);
        assertThat(recordRepository.count()).isEqualTo(37);

        // Second ingest (same content) should be a no-op (no new release, no new rows)
        UUID rel2 = lawaIngestion.ingestFixture(SOURCE_CODE, FIXTURE_PATH, LocalDate.of(2025, 9, 1), "LAWA Q3 2025 workbook");

        assertThat(rel2).isEqualTo(rel1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(37);
    }
}
