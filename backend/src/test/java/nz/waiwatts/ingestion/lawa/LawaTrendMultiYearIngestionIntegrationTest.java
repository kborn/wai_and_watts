package nz.waiwatts.ingestion.lawa;

import nz.waiwatts.domain.datasets.*;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LawaTrendMultiYearIngestionIntegrationTest {

    @TempDir
    Path tempDir;

    private static final String SOURCE_CODE = "lawa.water_quality.trend.multi_year";
    private static final String FIXTURE_PATH = "fixtures/lawa/water_quality/trend/multi_year/lawa_trend_multi_year_fixture.csv";

    @Autowired
    private DatasetSourceRepository sourceRepository;

    @Autowired
    private DatasetReleaseRepository releaseRepository;

    @Autowired
    private LawaTrendMultiYearIngestion trendIngestion;

    @Autowired
    private LawaTrendMultiYearRecordRepository recordRepository;

    @BeforeEach
    void setup() {
        recordRepository.deleteAll();
        releaseRepository.deleteAll();
        sourceRepository.deleteAll();

        DatasetSource src = new DatasetSource();
        src.setId(UUID.randomUUID());
        src.setName("LAWA Trend Multi Year");
        src.setPublisher(Publisher.LAWA);
        src.setCode(SOURCE_CODE);
        src.setSourceUrl("https://example.com/lawa-trend-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("multi-year");
        sourceRepository.save(src);
    }

    @Test
    void ingest_fixture_persists_rows_and_is_idempotent() {
        // First ingest
        UUID rel1 = trendIngestion.ingestFixture(SOURCE_CODE, FIXTURE_PATH, LocalDate.of(2025, 10, 30), "LAWA Trend 2025 workbook");

        List<DatasetRelease> releasesAfterFirst = releaseRepository.findAll();
        assertThat(releasesAfterFirst).hasSize(1);
        assertThat(releasesAfterFirst.getFirst().getStatus()).isEqualTo(ReleaseStatus.IMPORTED);
        assertThat(recordRepository.count()).isGreaterThan(0);

        // Second ingest (same content) should be a no-op
        UUID rel2 = trendIngestion.ingestFixture(SOURCE_CODE, FIXTURE_PATH, LocalDate.of(2025, 10, 30), "LAWA Trend 2025 workbook");

        assertThat(rel2).isEqualTo(rel1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(recordRepository.count());
    }

    @Test
    void ingestFile_whenNewFile_createsReleaseAndRecords() throws IOException {
        // Arrange
        Path tempFile = createTempLawaTrendFile();
        String filePath = tempFile.toString();
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test LAWA Trend File Integration Release";

        long initialReleaseCount = releaseRepository.count();
        long initialRecordCount = recordRepository.count();

        // Act
        UUID releaseId = trendIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);

        // Assert
        assertThat(releaseId).isNotNull();
        assertThat(releaseRepository.count()).isEqualTo(initialReleaseCount + 1);
        assertThat(recordRepository.count()).isEqualTo(initialRecordCount + 2); // 2 rows in test file

        // Verify release was created correctly
        DatasetRelease release = releaseRepository.findById(releaseId).orElseThrow();
        assertThat(release.getPublishedDate()).isEqualTo(publishedDate);
        assertThat(release.getReleaseLabel()).isEqualTo(releaseLabel);
        assertThat(release.getStatus()).isEqualTo(ReleaseStatus.IMPORTED);
        assertThat(release.getContentHash()).isNotEmpty();
        assertThat(release.getRetrievedAt()).isNotNull();
        assertThat(release.getImportedAt()).isNotNull();

        // Verify domain records were created and linked
        List<LawaTrendMultiYearRecord> records = recordRepository.findByDatasetReleaseId(releaseId);
        assertThat(records).hasSize(2); // 2 rows in test file

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void ingestFile_whenSameFileIngestedAgain_isIdempotent() throws IOException {
        // Arrange
        Path tempFile = createTempLawaTrendFile();
        String filePath = tempFile.toString();
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test Idempotent LAWA Trend Release";

        long initialReleaseCount = releaseRepository.count();
        long initialRecordCount = recordRepository.count();

        // Act - ingest same file twice
        UUID releaseId1 = trendIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);
        UUID releaseId2 = trendIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);

        // Assert
        assertThat(releaseId2).isEqualTo(releaseId1); // Same release ID returned
        assertThat(releaseRepository.count()).isEqualTo(initialReleaseCount + 1); // Only one new release
        assertThat(recordRepository.count()).isEqualTo(initialRecordCount + 2); // Only one set of records

        // Verify domain records were linked correctly
        List<LawaTrendMultiYearRecord> records = recordRepository.findByDatasetReleaseId(releaseId1);
        assertThat(records).hasSize(2); // 2 rows in test file

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void ingestFile_whenFileDoesNotExist_throwsException() {
        // Arrange
        String nonExistentPath = "/path/to/nonexistent/file.csv";
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test Non-existent LAWA Trend File";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> trendIngestion.ingestFile(SOURCE_CODE, nonExistentPath, publishedDate, releaseLabel)
        );
        assertThat(exception.getMessage()).contains("File does not exist");
    }

    private Path createTempLawaTrendFile() throws IOException {
        String content = """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,trend_raw,trend_norm,trend_score,trend_period_years,trend_data_frequency,period_type,period_start_year,period_end_year
            R001,Kaituna River,Hawke's Bay,-40.95,175.55,E.coli,E_COLI,CFU/100mL,Degrading,DEGRADING,2,10,Annual,HYDRO_NYR_WINDOW,2013,2022
            R002,Waikato River,Waikato,-37.85,175.35,E.coli,E_COLI,CFU/100mL,Improving,IMPROVING,1,9,Annual,HYDRO_NYR_WINDOW,2014,2022
            """;
        
        Path tempFile = tempDir.resolve("lawa-trend-test-" + UUID.randomUUID() + ".csv");
        Files.writeString(tempFile, content);
        return tempFile;
    }
}
