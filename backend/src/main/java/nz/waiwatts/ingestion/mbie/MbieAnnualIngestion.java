package nz.waiwatts.ingestion.mbie;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.ingestion.core.DatasetIngestionRequest;
import nz.waiwatts.ingestion.core.DatasetIngestionService;
import nz.waiwatts.ingestion.core.FileIngestionUtil;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
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
        DatasetSource source = datasetSourceRepository.findByCode(datasetSourceCode)
                .orElseThrow(() -> new IllegalArgumentException("DatasetSource not found for code: " + datasetSourceCode));

        byte[] bytes = readAllBytes(classpathFixture);
        String sha256 = sha256Hex(bytes);

        // If release already exists for (source, hash), return it without duplicating rows
        Optional<DatasetRelease> existing = datasetReleaseRepository
                .findFirstByDatasetSourceIdAndContentHash(source.getId(), sha256);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        // Create/import the release via lifecycle service
        DatasetIngestionRequest req = new DatasetIngestionRequest();
        req.setDatasetSourceCode(datasetSourceCode);
        req.setReleaseLabel(releaseLabel);
        req.setPublishedDate(publishedDate);
        req.setSourceUri(null);
        req.setContentHash(sha256);
        UUID releaseId = datasetIngestionService.ingest(req);

        // Parse and persist rows linked to the new release
        List<MbieGenerationAnnualParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(releaseId)
                .orElseThrow();
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
        return releaseId;
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
        // Validate file path first
        FileIngestionUtil.validateFilePath(filePath);
        
        DatasetSource source = datasetSourceRepository.findByCode(datasetSourceCode)
                .orElseThrow(() -> new IllegalArgumentException("DatasetSource not found for code: " + datasetSourceCode));

        byte[] bytes = FileIngestionUtil.readFileBytes(filePath);
        String sha256 = FileIngestionUtil.sha256Hex(bytes);

        // If release already exists for (source, hash), return it without duplicating rows
        Optional<DatasetRelease> existing = datasetReleaseRepository
                .findFirstByDatasetSourceIdAndContentHash(source.getId(), sha256);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        // Create/import the release via lifecycle service
        DatasetIngestionRequest req = new DatasetIngestionRequest();
        req.setDatasetSourceCode(datasetSourceCode);
        req.setReleaseLabel(releaseLabel);
        req.setPublishedDate(publishedDate);
        req.setSourceUri(Paths.get(filePath).toUri().toString());
        req.setContentHash(sha256);
        UUID releaseId = datasetIngestionService.ingest(req);

        // Parse and persist rows linked to the new release
        List<MbieGenerationAnnualParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(releaseId)
                .orElseThrow();
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
        return releaseId;
    }

    private List<MbieGenerationAnnualParsedRecord> parse(byte[] bytes) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse MBIE CSV", e);
        }
    }

    private byte[] readAllBytes(String classpath) {
        try {
            ClassPathResource res = new ClassPathResource(classpath);
            try (InputStream is = res.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load fixture from classpath: " + classpath, e);
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
