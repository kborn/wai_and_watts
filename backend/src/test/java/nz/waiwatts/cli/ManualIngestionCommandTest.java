package nz.waiwatts.cli;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ExpectedFormat;
import nz.waiwatts.domain.datasets.Publisher;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ManualIngestionCommandTest {

    private static final String SOURCE_CODE = "mbie.generation.annual";

    @TempDir
    Path tempDir;

    @Autowired
    private DatasetSourceRepository sourceRepository;

    @Autowired
    private DatasetReleaseRepository releaseRepository;

    @Autowired
    private MbieGenerationAnnualRecordRepository recordRepository;

    @Autowired
    private DataSource dataSource;

    private static String previousProfiles;
    private static String previousDatasourceUrl;
    private static String previousDatasourceUsername;
    private static String previousDatasourcePassword;
    private static String previousDatasourceDriver;

    @BeforeAll
    static void setProfiles() {
        previousProfiles = System.getProperty("spring.profiles.active");
        previousDatasourceUrl = System.getProperty("spring.datasource.url");
        previousDatasourceUsername = System.getProperty("spring.datasource.username");
        previousDatasourcePassword = System.getProperty("spring.datasource.password");
        previousDatasourceDriver = System.getProperty("spring.datasource.driver-class-name");
        System.setProperty("spring.profiles.active", "test");
    }

    @AfterAll
    static void restoreProfiles() {
        if (previousProfiles == null) {
            System.clearProperty("spring.profiles.active");
        } else {
            System.setProperty("spring.profiles.active", previousProfiles);
        }
        restoreSystemProperty("spring.datasource.url", previousDatasourceUrl);
        restoreSystemProperty("spring.datasource.username", previousDatasourceUsername);
        restoreSystemProperty("spring.datasource.password", previousDatasourcePassword);
        restoreSystemProperty("spring.datasource.driver-class-name", previousDatasourceDriver);
    }

    @BeforeEach
    void setup() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            System.setProperty("spring.datasource.url", connection.getMetaData().getURL());
        }
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");

        recordRepository.deleteAll();
        releaseRepository.deleteAll();
        sourceRepository.deleteAll();

        DatasetSource src = new DatasetSource();
        src.setName("MBIE Electricity Generation (Fuel Type, Annual)");
        src.setPublisher(Publisher.MBIE);
        src.setCode(SOURCE_CODE);
        src.setSourceUrl("https://example.com/mbie-" + UUID.randomUUID());
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("annual");
        sourceRepository.save(src);
    }

    private static void restoreSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    void run_whenValidArguments_ingestsFileAndReturnsSuccess() throws IOException {
        Path tempFile = createTempMbieFile();
        String[] args = {
                SOURCE_CODE,
                tempFile.toString(),
                "2025-01-01",
                "MBIE Test Workbook"
        };

        ManualIngestionCommand command = new ManualIngestionCommand();
        int exitCode = command.run(args);

        assertThat(exitCode).isZero();
        assertThat(releaseRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);

        DatasetRelease release = releaseRepository.findAll().getFirst();
        assertThat(release.getStatus()).isEqualTo(ReleaseStatus.IMPORTED);
        assertThat(release.getPublishedDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(release.getReleaseLabel()).isEqualTo("MBIE Test Workbook");

        List<MbieGenerationAnnualRecord> records = recordRepository.findByDatasetReleaseId(release.getId());
        assertThat(records).hasSize(2);
        assertThat(records.getFirst().getDatasetRelease().getId()).isEqualTo(release.getId());
    }

    @Test
    void run_whenUnknownDataset_returnsValidationExitCode() throws IOException {
        Path tempFile = createTempMbieFile();
        String[] args = {
                "unknown.dataset.code",
                tempFile.toString()
        };

        ManualIngestionCommand command = new ManualIngestionCommand();
        int exitCode = command.run(args);

        assertThat(exitCode).isEqualTo(2);
    }

    private Path createTempMbieFile() throws IOException {
        String content = """
            period_year,fuel_type_raw,fuel_type_norm,generation_gwh
            2022,Hydro,HYDRO,26071
            2022,Geothermal,GEOTHERMAL,7984
            """;
        Path tempFile = tempDir.resolve("mbie-cli-" + UUID.randomUUID() + ".csv");
        Files.writeString(tempFile, content);
        return tempFile;
    }
}
