package nz.waiwatts.cli;

import nz.waiwatts.ingestion.transform.lawa.LawaStateMultiYearXlsxTransformer;
import nz.waiwatts.ingestion.transform.lawa.LawaTrendMultiYearXlsxTransformer;
import nz.waiwatts.ingestion.transform.mbie.MbieAnnualXlsxTransformer;
import nz.waiwatts.ingestion.transform.mbie.MbieQuarterlyXlsxTransformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ManualTransformCommand {

    private static final int EXIT_USAGE = 1;
    private static final int EXIT_VALIDATION = 2;
    private static final int EXIT_FAILURE = 3;
    private static final String OUTPUT_EXTENSION = ".csv";
    private static final java.util.Set<String> SUPPORTED_DATASETS = java.util.Set.of(
            "mbie.generation.annual",
            "mbie.generation.quarterly",
            "lawa.water_quality.state.multi_year",
            "lawa.water_quality.trend.multi_year"
    );

    public static void main(String[] args) {
        int exitCode = new ManualTransformCommand().run(args);
        System.exit(exitCode);
    }

    int run(String[] args) {
        if (args.length == 1 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            printUsage();
            return 0;
        }
        if (args.length != 3) {
            printUsage();
            return EXIT_USAGE;
        }

        String datasetSourceCode = args[0];
        String inputPath = args[1];
        String outputPath = args[2];

        try {
            validateInputFile(inputPath);
            validateOutputPath(outputPath);
            validateDatasetSource(datasetSourceCode);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            return EXIT_VALIDATION;
        }

        try {
            byte[] csvBytes;
            try (var in = Files.newInputStream(Paths.get(inputPath))) {
                csvBytes = switch (datasetSourceCode) {
                    case "mbie.generation.annual" -> new MbieAnnualXlsxTransformer().transform(in);
                    case "mbie.generation.quarterly" -> new MbieQuarterlyXlsxTransformer().transform(in);
                    case "lawa.water_quality.state.multi_year" -> new LawaStateMultiYearXlsxTransformer().transform(in);
                    case "lawa.water_quality.trend.multi_year" -> new LawaTrendMultiYearXlsxTransformer().transform(in);
                    default -> throw new IllegalArgumentException(buildUnsupportedDatasetMessage(datasetSourceCode));
                };
            }

            Path out = Paths.get(outputPath);
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
            Files.write(out, csvBytes);
            System.out.println("SUCCESS");
            System.out.println("Output: " + out.toAbsolutePath());
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            return EXIT_VALIDATION;
        } catch (IOException e) {
            System.err.println("ERROR: Transform failed: " + e.getMessage());
            return EXIT_FAILURE;
        }
    }

    private static void printUsage() {
        System.out.println("Usage: ManualTransformCommand <dataset_source_code> <input_xlsx_path> <output_csv_path>");
        System.out.println("Example: ManualTransformCommand mbie.generation.annual workbook.xlsx /tmp/mbie_annual.csv");
        System.out.println("Supported datasets: " + String.join(", ", SUPPORTED_DATASETS));
    }

    private static void validateInputFile(String inputPath) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Input path cannot be null or empty");
        }
        Path path = Paths.get(inputPath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Input file does not exist: " + inputPath);
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("Input file is not readable: " + inputPath);
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Input path is not a regular file: " + inputPath);
        }
        try {
            if (Files.size(path) == 0) {
                throw new IllegalArgumentException("Input file is empty: " + inputPath);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read input file size: " + inputPath);
        }
    }

    private static void validateOutputPath(String outputPath) {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be null or empty");
        }
        if (!outputPath.endsWith(OUTPUT_EXTENSION)) {
            throw new IllegalArgumentException("Output path must end with " + OUTPUT_EXTENSION + ": " + outputPath);
        }
        Path path = Paths.get(outputPath);
        if (Files.exists(path) && !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Output path must be a file: " + outputPath);
        }
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to create output directory: " + parent);
            }
        } else if (parent != null && !Files.isWritable(parent)) {
            throw new IllegalArgumentException("Output directory is not writable: " + parent);
        }
    }

    private static void validateDatasetSource(String datasetSourceCode) {
        if (!SUPPORTED_DATASETS.contains(datasetSourceCode)) {
            throw new IllegalArgumentException(buildUnsupportedDatasetMessage(datasetSourceCode));
        }
    }

    private static String buildUnsupportedDatasetMessage(String datasetSourceCode) {
        return "Unsupported dataset source code: " + datasetSourceCode
                + ". Supported: " + String.join(", ", SUPPORTED_DATASETS);
    }
}
