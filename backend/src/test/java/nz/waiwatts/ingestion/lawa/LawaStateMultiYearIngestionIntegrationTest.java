package nz.waiwatts.ingestion.lawa;

import nz.waiwatts.domain.datasets.*;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.ingestion.transform.lawa.LawaStateMultiYearXlsxTransformer;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
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
class LawaStateMultiYearIngestionIntegrationTest {

    @TempDir
    Path tempDir;

    private static final String SOURCE_CODE = "lawa.water_quality.state.multi_year";
    private static final String FIXTURE_PATH = "fixtures/lawa/water_quality/state/multi_year/lawa_state_multi_year_fixture.csv";

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

    @Test
    void ingestFile_whenNewFile_createsReleaseAndRecords() throws IOException {
        // Arrange
        Path tempFile = createTempLawaStateFile();
        String filePath = tempFile.toString();
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test LAWA State File Integration Release";

        long initialReleaseCount = releaseRepository.count();
        long initialRecordCount = recordRepository.count();

        // Act
        UUID releaseId = lawaIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);

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
        List<LawaStateMultiYearRecord> records = recordRepository.findByDatasetReleaseId(releaseId);
        assertThat(records).hasSize(2); // 2 rows in test file
        assertThat(records).extracting(LawaStateMultiYearRecord::getRegion)
                .containsExactlyInAnyOrder("hawke's bay", "waikato");

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void ingestFile_whenSameFileIngestedAgain_isIdempotent() throws IOException {
        // Arrange
        Path tempFile = createTempLawaStateFile();
        String filePath = tempFile.toString();
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test Idempotent LAWA State Release";

        long initialReleaseCount = releaseRepository.count();
        long initialRecordCount = recordRepository.count();

        // Act - ingest same file twice
        UUID releaseId1 = lawaIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);
        UUID releaseId2 = lawaIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);

        // Assert
        assertThat(releaseId2).isEqualTo(releaseId1); // Same release ID returned
        assertThat(releaseRepository.count()).isEqualTo(initialReleaseCount + 1); // Only one new release
        assertThat(recordRepository.count()).isEqualTo(initialRecordCount + 2); // Only one set of records

        // Verify domain records were linked correctly
        List<LawaStateMultiYearRecord> records = recordRepository.findByDatasetReleaseId(releaseId1);
        assertThat(records).hasSize(2); // 2 rows in test file

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void ingestFile_whenSameContentDifferentFilename_isIdempotent() throws IOException {
        String content = lawaStateContent();
        Path fileA = createTempLawaStateFileWithContent(content, "lawa-state-a");
        Path fileB = createTempLawaStateFileWithContent(content, "lawa-state-b");

        UUID releaseId1 = lawaIngestion.ingestFile(SOURCE_CODE, fileA.toString(), LocalDate.of(2025, 1, 15), "Release A");
        UUID releaseId2 = lawaIngestion.ingestFile(SOURCE_CODE, fileB.toString(), LocalDate.of(2025, 1, 15), "Release A");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_whenSameContentWithBom_isIdempotent() throws IOException {
        String content = "\uFEFF" + lawaStateContent();
        Path tempFile = createTempLawaStateFileWithContent(content, "lawa-state-bom");

        UUID releaseId1 = lawaIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "BOM Release");
        UUID releaseId2 = lawaIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "BOM Release");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_whenTrailingBlankRows_isIdempotent() throws IOException {
        String content = lawaStateContent() + "\n\n   \n";
        Path tempFile = createTempLawaStateFileWithContent(content, "lawa-state-blank");

        UUID releaseId1 = lawaIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "Blank Rows");
        UUID releaseId2 = lawaIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "Blank Rows");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_whenHeaderOnly_failsValidation() throws IOException {
        String content = "lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year,period_end_year\n";
        Path tempFile = createTempLawaStateFileWithContent(content, "lawa-state-header-only");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> lawaIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, null)
        );
        assertThat(exception.getMessage()).contains("Failed to parse LAWA state multi-year CSV");
    }

    @Test
    void ingestFile_whenTruncatedRow_failsValidation() throws IOException {
        String content = """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year,period_end_year
            R001,Kaituna River,Hawke's Bay,-40.95,175.55,E.coli
            """;
        Path tempFile = createTempLawaStateFileWithContent(content, "lawa-state-truncated");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> lawaIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, null)
        );
        assertThat(exception.getMessage()).contains("Failed to parse LAWA state multi-year CSV");
    }

    @Test
    void ingestFile_realSnapshot_isIdempotent() throws IOException {
        Path xlsxSnapshot = classpathToFile("real_snapshots/lawa/lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx");
        Path csv = transformToTempCsv(xlsxSnapshot, "lawa-state-snapshot");

        UUID releaseId1 = lawaIngestion.ingestFile(SOURCE_CODE, csv.toString(), null, "LAWA snapshot");
        UUID releaseId2 = lawaIngestion.ingestFile(SOURCE_CODE, csv.toString(), null, "LAWA snapshot");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isGreaterThan(0);
    }

    @Test
    void ingestFile_whenFileDoesNotExist_throwsException() {
        // Arrange
        String nonExistentPath = "/path/to/nonexistent/file.csv";
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test Non-existent LAWA State File";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> lawaIngestion.ingestFile(SOURCE_CODE, nonExistentPath, publishedDate, releaseLabel)
        );
        assertThat(exception.getMessage()).contains("File does not exist");
    }

    private Path createTempLawaStateFile() throws IOException {
        return createTempLawaStateFileWithContent(lawaStateContent(), "lawa-state-test");
    }

    private Path createTempLawaStateFileWithContent(String content, String prefix) throws IOException {
        Path tempFile = tempDir.resolve(prefix + "-" + UUID.randomUUID() + ".csv");
        Files.writeString(tempFile, content);
        return tempFile;
    }

    private String lawaStateContent() {
        return """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year,period_end_year
            R001,Kaituna River,Hawke's Bay,-40.95,175.55,E.coli,E_COLI,CFU/100mL,A,FAIR,45,85,5.2,0.8,HYDRO_NYR_WINDOW,2018,2022
            R002,Waikato River,Waikato,-37.85,175.35,E.coli,E_COLI,CFU/100mL,B,GOOD,20,50,1.1,0.3,HYDRO_NYR_WINDOW,2018,2022
            """;
    }

    private Path classpathToFile(String classpath) throws IOException {
        return new ClassPathResource(classpath).getFile().toPath();
    }

    private Path transformToTempCsv(Path xlsxSnapshot, String prefix) throws IOException {
        LawaStateMultiYearXlsxTransformer transformer = new LawaStateMultiYearXlsxTransformer();
        byte[] csvBytes = transformer.transform(Files.newInputStream(xlsxSnapshot));
        Path tempFile = tempDir.resolve(prefix + "-" + UUID.randomUUID() + ".csv");
        Files.write(tempFile, csvBytes);
        return tempFile;
    }
}
