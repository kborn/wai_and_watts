package nz.waiwatts.ingestion.mbie;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.ingestion.core.DatasetReleaseRegistrationUtil;
import nz.waiwatts.ingestion.core.DatasetIngestionService;
import nz.waiwatts.ingestion.core.FileIngestionUtil;
import nz.waiwatts.ingestion.core.ReleaseRegistrationResult;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MbieAnnualIngestion {

    private final DatasetSourceRepository datasetSourceRepository;
    private final DatasetReleaseRepository datasetReleaseRepository;
    private final DatasetIngestionService datasetIngestionService;
    private final MbieGenerationAnnualParser parser;
    private final MbieGenerationAnnualRecordRepository recordRepository;

    public MbieAnnualIngestion(DatasetSourceRepository datasetSourceRepository,
                               DatasetReleaseRepository datasetReleaseRepository,
                               DatasetIngestionService datasetIngestionService,
                               MbieGenerationAnnualParser parser,
                               MbieGenerationAnnualRecordRepository recordRepository) {
        this.datasetSourceRepository = datasetSourceRepository;
        this.datasetReleaseRepository = datasetReleaseRepository;
        this.datasetIngestionService = datasetIngestionService;
        this.parser = parser;
        this.recordRepository = recordRepository;
    }

    /**
     * Ingests the MBIE generation fixture from classpath. Implements idempotency via (source, content_hash).
     * Only persists domain rows when a new release is created.
     *
     * @param datasetSourceCode stable code of the DatasetSource (e.g., "mbie-generation")
     * @param classpathFixture  e.g., "fixtures/mbie/generation/mbie_generation_fixture_phase6.csv"
     * @param publishedDate     optional publication date
     * @param releaseLabel      optional label (e.g., "2025-Q3 workbook")
     * @return DatasetRelease id
     */
    @Transactional
    public UUID ingestFixture(String datasetSourceCode,
                              String classpathFixture,
                              LocalDate publishedDate,
                              String releaseLabel) {
        return ingestBytes(
            datasetSourceCode,
            FileIngestionUtil.readClasspathBytes(classpathFixture),
            publishedDate,
            releaseLabel,
            null
        );
    }

    /**
     * Ingests MBIE generation data from a local file path. Implements idempotency via (source, content_hash).
     * Reuses the same pipeline and lifecycle as fixture ingestion.
     *
     * @param datasetSourceCode stable code of the DatasetSource (e.g., "mbie.generation.annual")
     * @param filePath          absolute or relative path to the data file
     * @param publishedDate     optional publication date from the source
     * @param releaseLabel      optional label (e.g., "2025-Q3 workbook")
     * @return DatasetRelease id
     * @throws IllegalArgumentException if file does not exist or is not readable
     */
    @Transactional
    public UUID ingestFile(String datasetSourceCode,
                            String filePath,
                            LocalDate publishedDate,
                            String releaseLabel) {
        Path resolvedFilePath = FileIngestionUtil.resolveReadableRegularFile(filePath);
        return ingestBytes(
            datasetSourceCode,
            FileIngestionUtil.readFileBytes(resolvedFilePath),
            publishedDate,
            releaseLabel,
            FileIngestionUtil.fileUri(resolvedFilePath)
        );
    }

    private UUID ingestBytes(
        String datasetSourceCode,
        byte[] bytes,
        LocalDate publishedDate,
        String releaseLabel,
        String sourceUri
    ) {
        String sha256 = FileIngestionUtil.sha256Hex(bytes);
        ReleaseRegistrationResult registration = DatasetReleaseRegistrationUtil.registerRelease(
            datasetSourceRepository,
            datasetReleaseRepository,
            datasetIngestionService,
            datasetSourceCode,
            sha256,
            publishedDate,
            releaseLabel,
            sourceUri
        );
        if (!registration.created()) {
            return registration.releaseId();
        }

        List<MbieGenerationAnnualParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(registration.releaseId()).orElseThrow();
        for (MbieGenerationAnnualParsedRecord r : rows) {
            MbieGenerationAnnualRecord e = new MbieGenerationAnnualRecord();
            e.setDatasetRelease(release);
            e.setPeriodYear(r.periodYear());
            e.setFuelTypeRaw(r.fuelTypeRaw());
            e.setFuelTypeNorm(r.fuelTypeNorm());
            BigDecimal gwh = r.generationGwh();
            e.setGenerationGwh(gwh);
            recordRepository.save(e);
        }
        return registration.releaseId();
    }

    private List<MbieGenerationAnnualParsedRecord> parse(byte[] bytes) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse MBIE CSV", e);
        }
    }

}
