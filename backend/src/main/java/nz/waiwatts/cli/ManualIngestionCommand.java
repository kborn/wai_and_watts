package nz.waiwatts.cli;

import nz.waiwatts.WaiWattsApplication;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.ingestion.lawa.LawaStateMultiYearIngestion;
import nz.waiwatts.ingestion.lawa.LawaTrendMultiYearIngestion;
import nz.waiwatts.ingestion.mbie.MbieAnnualIngestion;
import nz.waiwatts.ingestion.mbie.MbieQuarterlyIngestion;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;

public class ManualIngestionCommand {

    private static final int EXIT_USAGE = 1;
    private static final int EXIT_VALIDATION = 2;
    private static final int EXIT_INGESTION = 3;
    private static final Set<String> ALLOWED_DATASET_CODES = Set.of(
            DatasetSource.MBIE_GENERATION_ANNUAL.wireValue(),
            DatasetSource.MBIE_GENERATION_QUARTERLY.wireValue(),
            DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue(),
            DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue()
    );

    public static void main(String[] args) {
        int exitCode = new ManualIngestionCommand().run(args);
        System.exit(exitCode);
    }

    int run(String[] args) {
        if (args.length < 2 || args.length > 4) {
            printUsage();
            return EXIT_USAGE;
        }

        String datasetSourceCode = args[0];
        String filePath = args[1];
        String publishedDateArg = args.length >= 3 ? args[2] : null;
        String releaseLabel = args.length == 4 ? args[3] : null;

        LocalDate publishedDate;
        try {
            publishedDate = parsePublishedDate(publishedDateArg);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            return EXIT_VALIDATION;
        }

        try {
            validateFile(filePath);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            return EXIT_VALIDATION;
        }

        if (!ALLOWED_DATASET_CODES.contains(datasetSourceCode)) {
            System.err.println("ERROR: Unsupported dataset source code: " + datasetSourceCode);
            return EXIT_VALIDATION;
        }

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(WaiWattsApplication.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run()) {

            DatasetSourceRepository sourceRepository = context.getBean(DatasetSourceRepository.class);
            if (sourceRepository.findByCode(datasetSourceCode).isEmpty()) {
                System.err.println("ERROR: Unknown dataset source code: " + datasetSourceCode);
                return EXIT_VALIDATION;
            }

            printStartMessage(datasetSourceCode, filePath, publishedDate, releaseLabel);

            IngestionResult result = ingest(context, datasetSourceCode, filePath, publishedDate, releaseLabel);
            System.out.println("SUCCESS");
            System.out.println("Release ID: " + result.releaseId());
            System.out.println("Rows persisted: " + result.rowsPersisted());
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            return EXIT_VALIDATION;
        } catch (Exception e) {
            System.err.println("ERROR: Ingestion failed: " + e.getMessage());
            return EXIT_INGESTION;
        }
    }

    private static void printUsage() {
        System.out.println("Usage: ManualIngestionCommand <dataset_source_code> <file_path> [published_date] [release_label]");
        System.out.println("Example: ManualIngestionCommand mbie.generation.annual data.csv 2025-01-01 \"MBIE Workbook\"");
    }

    private static LocalDate parsePublishedDate(String publishedDateArg) {
        if (publishedDateArg == null || publishedDateArg.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(publishedDateArg);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Published date must be ISO-8601 (YYYY-MM-DD): " + publishedDateArg);
        }
    }

    private static void validateFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (filePath.contains("..") || filePath.contains("~")) {
            throw new IllegalArgumentException("File path contains potentially unsafe characters: " + filePath);
        }
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("File is not readable: " + filePath);
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path is not a regular file: " + filePath);
        }
        try {
            if (Files.size(path) == 0) {
                throw new IllegalArgumentException("File is empty: " + filePath);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read file size: " + filePath);
        }
    }

    private static void printStartMessage(String datasetSourceCode,
                                          String filePath,
                                          LocalDate publishedDate,
                                          String releaseLabel) {
        System.out.println("Starting ingestion...");
        System.out.println("Dataset: " + datasetSourceCode);
        System.out.println("File: " + filePath);
        System.out.println("Published Date: " + (publishedDate == null ? "(none)" : publishedDate));
        System.out.println("Release Label: " + (releaseLabel == null ? "(none)" : releaseLabel));
    }

    private static IngestionResult ingest(ConfigurableApplicationContext context,
                                          String datasetSourceCode,
                                          String filePath,
                                          LocalDate publishedDate,
                                          String releaseLabel) {
        DatasetSource datasetSource = DatasetSource.fromWireValue(datasetSourceCode)
            .orElseThrow(() -> new IllegalArgumentException("No ingestion handler for dataset source code: " + datasetSourceCode));
        return switch (datasetSource) {
            case MBIE_GENERATION_ANNUAL -> {
                MbieAnnualIngestion ingestion = context.getBean(MbieAnnualIngestion.class);
                UUID releaseId = ingestion.ingestFile(datasetSourceCode, filePath, publishedDate, releaseLabel);
                MbieGenerationAnnualRecordRepository repo = context.getBean(MbieGenerationAnnualRecordRepository.class);
                yield new IngestionResult(releaseId, repo.findByDatasetReleaseId(releaseId).size());
            }
            case MBIE_GENERATION_QUARTERLY -> {
                MbieQuarterlyIngestion ingestion = context.getBean(MbieQuarterlyIngestion.class);
                UUID releaseId = ingestion.ingestFile(datasetSourceCode, filePath, publishedDate, releaseLabel);
                MbieGenerationQuarterlyRecordRepository repo = context.getBean(MbieGenerationQuarterlyRecordRepository.class);
                yield new IngestionResult(releaseId, repo.findByDatasetReleaseId(releaseId).size());
            }
            case LAWA_WATER_QUALITY_STATE_MULTI_YEAR -> {
                LawaStateMultiYearIngestion ingestion = context.getBean(LawaStateMultiYearIngestion.class);
                UUID releaseId = ingestion.ingestFile(datasetSourceCode, filePath, publishedDate, releaseLabel);
                LawaStateMultiYearRecordRepository repo = context.getBean(LawaStateMultiYearRecordRepository.class);
                yield new IngestionResult(releaseId, repo.findByDatasetReleaseId(releaseId).size());
            }
            case LAWA_WATER_QUALITY_TREND_MULTI_YEAR -> {
                LawaTrendMultiYearIngestion ingestion = context.getBean(LawaTrendMultiYearIngestion.class);
                UUID releaseId = ingestion.ingestFile(datasetSourceCode, filePath, publishedDate, releaseLabel);
                LawaTrendMultiYearRecordRepository repo = context.getBean(LawaTrendMultiYearRecordRepository.class);
                yield new IngestionResult(releaseId, repo.findByDatasetReleaseId(releaseId).size());
            }
        };
    }

    private record IngestionResult(UUID releaseId, int rowsPersisted) {
    }
}
