package nz.waiwatts.cli;

import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.ingestion.core.FileIngestionUtil;
import nz.waiwatts.ingestion.transform.lawa.LawaStateMultiYearXlsxTransformer;
import nz.waiwatts.ingestion.transform.lawa.LawaTrendMultiYearXlsxTransformer;
import nz.waiwatts.ingestion.transform.mbie.MbieAnnualXlsxTransformer;
import nz.waiwatts.ingestion.transform.mbie.MbieQuarterlyXlsxTransformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ManualTransformCommand {

    private static final int EXIT_USAGE = 1;
    private static final int EXIT_VALIDATION = 2;
    private static final int EXIT_FAILURE = 3;
    private static final java.util.Set<String> ALLOWED_DATASETS = java.util.Set.of(
            DatasetSource.MBIE_GENERATION_ANNUAL.wireValue(),
            DatasetSource.MBIE_GENERATION_QUARTERLY.wireValue(),
            DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue(),
            DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue()
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
        Path inputPath;
        Path outputPath;

        try {
            validateDatasetSource(datasetSourceCode);
            inputPath = FileIngestionUtil.resolveReadableRegularFile(args[1]);
            outputPath = FileIngestionUtil.resolveWritableCsvOutputPath(args[2]);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            return EXIT_VALIDATION;
        }

        try {
            byte[] csvBytes;
            try (var in = Files.newInputStream(inputPath)) {
                DatasetSource datasetSource = DatasetSource.fromWireValue(datasetSourceCode)
                    .orElseThrow(() -> new IllegalArgumentException(buildUnsupportedDatasetMessage(datasetSourceCode)));
                csvBytes = switch (datasetSource) {
                    case MBIE_GENERATION_ANNUAL -> new MbieAnnualXlsxTransformer().transform(in);
                    case MBIE_GENERATION_QUARTERLY -> new MbieQuarterlyXlsxTransformer().transform(in);
                    case LAWA_WATER_QUALITY_STATE_MULTI_YEAR -> new LawaStateMultiYearXlsxTransformer().transform(in);
                    case LAWA_WATER_QUALITY_TREND_MULTI_YEAR -> new LawaTrendMultiYearXlsxTransformer().transform(in);
                };
            }

            Files.write(outputPath, csvBytes);
            System.out.println("SUCCESS");
            System.out.println("Output: " + outputPath);
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
        System.out.println("Supported datasets: " + String.join(", ", ALLOWED_DATASETS));
    }

    private static void validateDatasetSource(String datasetSourceCode) {
        if (!ALLOWED_DATASETS.contains(datasetSourceCode)) {
            throw new IllegalArgumentException(buildUnsupportedDatasetMessage(datasetSourceCode));
        }
    }

    private static String buildUnsupportedDatasetMessage(String datasetSourceCode) {
        return "Unsupported dataset source code: " + datasetSourceCode
                + ". Supported: " + String.join(", ", ALLOWED_DATASETS);
    }
}
