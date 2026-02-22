package nz.waiwatts.ingestion.mbie;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ExpectedFormat;
import nz.waiwatts.domain.datasets.Publisher;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
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
class MbieQuarterlyIngestionIntegrationTest {

    @TempDir
    Path tempDir;

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

    @Test
    void ingestFile_whenNewFile_createsReleaseAndRecords() throws IOException {
        // Arrange
        Path tempFile = createTempMbieQuarterlyFile();
        String filePath = tempFile.toString();
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test Quarterly File Integration Release";

        long initialReleaseCount = releaseRepository.count();
        long initialRecordCount = recordRepository.count();

        // Act
        UUID releaseId = mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);

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

        // Verify domain records were created and linked correctly
        List<MbieGenerationQuarterlyRecord> quarterlyRecords = recordRepository.findByDatasetReleaseId(releaseId);
        assertThat(quarterlyRecords).hasSize(2); // 2 rows in test file

        // Verify specific domain record values
        MbieGenerationQuarterlyRecord record1 = quarterlyRecords.getFirst();
        assertThat(record1.getPeriodYear()).isEqualTo(2022);
        assertThat(record1.getPeriodQuarter()).isEqualTo(3);
        assertThat(record1.getFuelTypeRaw()).isEqualTo("Hydro");
        assertThat(record1.getFuelTypeNorm()).isEqualTo("HYDRO");
        assertThat(record1.getGenerationGwh().intValue()).isEqualTo(6500);

        MbieGenerationQuarterlyRecord record2 = quarterlyRecords.get(1);
        assertThat(record2.getPeriodYear()).isEqualTo(2022);
        assertThat(record2.getPeriodQuarter()).isEqualTo(3);
        assertThat(record2.getFuelTypeRaw()).isEqualTo("Geothermal");
        assertThat(record2.getFuelTypeNorm()).isEqualTo("GEOTHERMAL");
        assertThat(record2.getGenerationGwh().intValue()).isEqualTo(2100);

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void ingestFile_whenSameContentDifferentFilename_isIdempotent() throws IOException {
        String content = mbieQuarterlyContent();
        Path fileA = createTempMbieQuarterlyFileWithContent(content, "mbie-quarterly-a");
        Path fileB = createTempMbieQuarterlyFileWithContent(content, "mbie-quarterly-b");

        UUID releaseId1 = mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, fileA.toString(), LocalDate.of(2025, 1, 15), "Release A");
        UUID releaseId2 = mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, fileB.toString(), LocalDate.of(2025, 1, 15), "Release A");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_whenSameContentWithBom_isIdempotent() throws IOException {
        String content = "\uFEFF" + mbieQuarterlyContent();
        Path tempFile = createTempMbieQuarterlyFileWithContent(content, "mbie-quarterly-bom");

        UUID releaseId1 = mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "BOM Release");
        UUID releaseId2 = mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "BOM Release");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_whenTrailingBlankRows_isIdempotent() throws IOException {
        String content = mbieQuarterlyContent() + "\n\n   \n";
        Path tempFile = createTempMbieQuarterlyFileWithContent(content, "mbie-quarterly-blank");

        UUID releaseId1 = mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "Blank Rows");
        UUID releaseId2 = mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "Blank Rows");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_whenHeaderOnly_failsValidation() throws IOException {
        String content = "period_year,period_quarter,fuel_type_raw,fuel_type_norm,generation_gwh\n";
        Path tempFile = createTempMbieQuarterlyFileWithContent(content, "mbie-quarterly-header-only");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, null)
        );
        assertThat(exception.getMessage()).contains("Failed to parse MBIE quarterly CSV");
    }

    @Test
    void ingestFile_whenTruncatedRow_failsValidation() throws IOException {
        String content = """
            period_year,period_quarter,fuel_type_raw,fuel_type_norm,generation_gwh
            2022,3,Hydro
            """;
        Path tempFile = createTempMbieQuarterlyFileWithContent(content, "mbie-quarterly-truncated");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, null)
        );
        assertThat(exception.getMessage()).contains("Failed to parse MBIE quarterly CSV");
    }

    @Test
    void ingestFile_whenFileDoesNotExist_throwsException() {
        // Arrange
        String nonExistentPath = "/path/to/nonexistent/file.csv";
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test Non-existent Quarterly File";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> mbieQuarterlyIngestion.ingestFile(SOURCE_CODE, nonExistentPath, publishedDate, releaseLabel)
        );
        assertThat(exception.getMessage()).contains("File does not exist");
    }

    private Path createTempMbieQuarterlyFile() throws IOException {
        return createTempMbieQuarterlyFileWithContent(mbieQuarterlyContent(), "mbie-quarterly-test");
    }

    private Path createTempMbieQuarterlyFileWithContent(String content, String prefix) throws IOException {
        Path tempFile = tempDir.resolve(prefix + "-" + UUID.randomUUID() + ".csv");
        Files.writeString(tempFile, content);
        return tempFile;
    }

    private String mbieQuarterlyContent() {
        return """
            period_year,period_quarter,fuel_type_raw,fuel_type_norm,generation_gwh
            2022,3,Hydro,HYDRO,6500
            2022,3,Geothermal,GEOTHERMAL,2100
            """;
    }
}
