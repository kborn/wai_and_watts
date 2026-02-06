package nz.waiwatts.ingestion.mbie;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ExpectedFormat;
import nz.waiwatts.domain.datasets.Publisher;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.ingestion.transform.mbie.MbieAnnualXlsxTransformer;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
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
class MbieAnnualIngestionIntegrationTest {

    @TempDir
    Path tempDir;

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

    @Test
    void ingestFile_whenNewFile_createsReleaseAndRecords() throws IOException {
        // Arrange
        Path tempFile = createTempMbieFile();
        String filePath = tempFile.toString();
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test File Integration Release";

        long initialReleaseCount = releaseRepository.count();
        long initialRecordCount = recordRepository.count();

        // Act
        UUID releaseId = mbieIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);

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
        List<MbieGenerationAnnualRecord> records = recordRepository.findByDatasetReleaseId(releaseId);
        assertThat(records).hasSize(2);
        
        MbieGenerationAnnualRecord record1 = records.getFirst();
        assertThat(record1.getDatasetRelease().getId()).isEqualTo(releaseId);
        assertThat(record1.getPeriodYear()).isEqualTo(2022);
        assertThat(record1.getFuelTypeRaw()).isEqualTo("Hydro");
        assertThat(record1.getFuelTypeNorm()).isEqualTo("HYDRO");
        assertThat(record1.getGenerationGwh().intValue()).isEqualTo(26071);

        MbieGenerationAnnualRecord record2 = records.get(1);
        assertThat(record2.getDatasetRelease().getId()).isEqualTo(releaseId);
        assertThat(record2.getPeriodYear()).isEqualTo(2022);
        assertThat(record2.getFuelTypeRaw()).isEqualTo("Geothermal");
        assertThat(record2.getFuelTypeNorm()).isEqualTo("GEOTHERMAL");
        assertThat(record2.getGenerationGwh().intValue()).isEqualTo(7984);

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void ingestFile_whenSameFileIngestedAgain_isIdempotent() throws IOException {
        // Arrange
        Path tempFile = createTempMbieFile();
        String filePath = tempFile.toString();
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test Idempotent File Release";

        long initialReleaseCount = releaseRepository.count();
        long initialRecordCount = recordRepository.count();

        // Act - ingest same file twice
        UUID releaseId1 = mbieIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);
        UUID releaseId2 = mbieIngestion.ingestFile(SOURCE_CODE, filePath, publishedDate, releaseLabel);

        // Assert
        assertThat(releaseId2).isEqualTo(releaseId1); // Same release ID returned
        assertThat(releaseRepository.count()).isEqualTo(initialReleaseCount + 1); // Only one new release
        assertThat(recordRepository.count()).isEqualTo(initialRecordCount + 2); // Only one set of records

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void ingestFile_whenSameContentDifferentFilename_isIdempotent() throws IOException {
        String content = mbieAnnualContent();
        Path fileA = createTempMbieFileWithContent(content, "mbie-annual-a");
        Path fileB = createTempMbieFileWithContent(content, "mbie-annual-b");

        UUID releaseId1 = mbieIngestion.ingestFile(SOURCE_CODE, fileA.toString(), LocalDate.of(2025, 1, 15), "Release A");
        UUID releaseId2 = mbieIngestion.ingestFile(SOURCE_CODE, fileB.toString(), LocalDate.of(2025, 1, 15), "Release A");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_whenSameContentWithBom_isIdempotent() throws IOException {
        String content = "\uFEFF" + mbieAnnualContent();
        Path tempFile = createTempMbieFileWithContent(content, "mbie-annual-bom");

        UUID releaseId1 = mbieIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "BOM Release");
        UUID releaseId2 = mbieIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "BOM Release");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_whenTrailingBlankRows_isIdempotent() throws IOException {
        String content = mbieAnnualContent() + "\n\n   \n";
        Path tempFile = createTempMbieFileWithContent(content, "mbie-annual-blank");

        UUID releaseId1 = mbieIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "Blank Rows");
        UUID releaseId2 = mbieIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, "Blank Rows");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_whenHeaderOnly_failsValidation() throws IOException {
        String content = "period_year,fuel_type_raw,fuel_type_norm,generation_gwh\n";
        Path tempFile = createTempMbieFileWithContent(content, "mbie-annual-header-only");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> mbieIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, null)
        );
        assertThat(exception.getMessage()).contains("Failed to parse MBIE CSV");
    }

    @Test
    void ingestFile_whenTruncatedRow_failsValidation() throws IOException {
        String content = """
            period_year,fuel_type_raw,fuel_type_norm,generation_gwh
            2022,Hydro
            """;
        Path tempFile = createTempMbieFileWithContent(content, "mbie-annual-truncated");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> mbieIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), null, null)
        );
        assertThat(exception.getMessage()).contains("Failed to parse MBIE CSV");
    }

    @Test
    void ingestFile_whenMetadataDiffersForSameContent_reusesRelease() throws IOException {
        Path tempFile = createTempMbieFile();

        UUID releaseId1 = mbieIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), LocalDate.of(2025, 1, 15), "Release A");
        UUID releaseId2 = mbieIngestion.ingestFile(SOURCE_CODE, tempFile.toString(), LocalDate.of(2026, 2, 1), "Release B");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
    }

    @Test
    void ingestFile_realSnapshot_isIdempotent() throws IOException {
        Path xlsxSnapshot = classpathToFile("real_snapshots/mbie/electricity-sept-2025-q3.xlsx");
        Path csv = transformToTempCsv(xlsxSnapshot, "mbie-annual-snapshot");

        UUID releaseId1 = mbieIngestion.ingestFile(SOURCE_CODE, csv.toString(), null, "MBIE snapshot");
        UUID releaseId2 = mbieIngestion.ingestFile(SOURCE_CODE, csv.toString(), null, "MBIE snapshot");

        assertThat(releaseId2).isEqualTo(releaseId1);
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isGreaterThan(0);
    }

    @Test
    void ingestFile_whenFileDoesNotExist_throwsException() {
        // Arrange
        String nonExistentPath = "/path/to/nonexistent/file.csv";
        LocalDate publishedDate = LocalDate.of(2025, 1, 15);
        String releaseLabel = "Test Non-existent File";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> mbieIngestion.ingestFile(SOURCE_CODE, nonExistentPath, publishedDate, releaseLabel)
        );
        assertThat(exception.getMessage()).contains("File does not exist");
    }

    @Test
    void ingestFile_whenDatasetSourceNotFound_throwsException() throws IOException {
        // Arrange
        String invalidDatasetSourceCode = "invalid.dataset.code";
        Path tempFile = createTempMbieFile();
        String filePath = tempFile.toString();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> mbieIngestion.ingestFile(invalidDatasetSourceCode, filePath, null, null)
        );
        assertThat(exception.getMessage()).contains("DatasetSource not found");

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    private Path createTempMbieFile() throws IOException {
        return createTempMbieFileWithContent(mbieAnnualContent(), "mbie-test");
    }

    private Path createTempMbieFileWithContent(String content, String prefix) throws IOException {
        Path tempFile = tempDir.resolve(prefix + "-" + UUID.randomUUID() + ".csv");
        Files.writeString(tempFile, content);
        return tempFile;
    }

    private String mbieAnnualContent() {
        return """
            period_year,fuel_type_raw,fuel_type_norm,generation_gwh
            2022,Hydro,HYDRO,26071
            2022,Geothermal,GEOTHERMAL,7984
            """;
    }

    private Path classpathToFile(String classpath) throws IOException {
        return new ClassPathResource(classpath).getFile().toPath();
    }

    private Path transformToTempCsv(Path xlsxSnapshot, String prefix) throws IOException {
        MbieAnnualXlsxTransformer transformer = new MbieAnnualXlsxTransformer();
        byte[] csvBytes = transformer.transform(Files.newInputStream(xlsxSnapshot));
        Path tempFile = tempDir.resolve(prefix + "-" + UUID.randomUUID() + ".csv");
        Files.write(tempFile, csvBytes);
        return tempFile;
    }
}
