package nz.waiwatts.ingestion.mbie;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.ingestion.core.DatasetIngestionRequest;
import nz.waiwatts.ingestion.core.DatasetIngestionService;
import nz.waiwatts.ingestion.core.FileIngestionUtil;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MbieQuarterlyIngestion {

    private final DatasetSourceRepository datasetSourceRepository;
    private final DatasetReleaseRepository datasetReleaseRepository;
    private final DatasetIngestionService datasetIngestionService;
    private final MbieGenerationQuarterlyParser parser;
    private final MbieGenerationQuarterlyRecordRepository recordRepository;

    public MbieQuarterlyIngestion(DatasetSourceRepository datasetSourceRepository,
                                  DatasetReleaseRepository datasetReleaseRepository,
                                  DatasetIngestionService datasetIngestionService,
                                  MbieGenerationQuarterlyParser parser,
                                  MbieGenerationQuarterlyRecordRepository recordRepository) {
        this.datasetSourceRepository = datasetSourceRepository;
        this.datasetReleaseRepository = datasetReleaseRepository;
        this.datasetIngestionService = datasetIngestionService;
        this.parser = parser;
        this.recordRepository = recordRepository;
    }

    @Transactional
    public UUID ingestFixture(String datasetSourceCode,
                              String classpathFixture,
                              LocalDate publishedDate,
                              String releaseLabel) {
        DatasetSource source = datasetSourceRepository.findByCode(datasetSourceCode)
                .orElseThrow(() -> new IllegalArgumentException("DatasetSource not found for code: " + datasetSourceCode));

        byte[] bytes = readAllBytes(classpathFixture);
        String sha256 = sha256Hex(bytes);

        Optional<DatasetRelease> existing = datasetReleaseRepository
                .findFirstByDatasetSourceIdAndContentHash(source.getId(), sha256);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        DatasetIngestionRequest req = new DatasetIngestionRequest();
        req.setDatasetSourceCode(datasetSourceCode);
        req.setReleaseLabel(releaseLabel);
        req.setPublishedDate(publishedDate);
        req.setSourceUri(null);
        req.setContentHash(sha256);
        UUID releaseId = datasetIngestionService.ingest(req);

        List<MbieGenerationQuarterlyParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(releaseId)
                .orElseThrow();
        for (MbieGenerationQuarterlyParsedRecord r : rows) {
            MbieGenerationQuarterlyRecord e = new MbieGenerationQuarterlyRecord();
            e.setDatasetRelease(release);
            e.setPeriodYear(r.periodYear());
            e.setPeriodQuarter(r.periodQuarter());
            e.setFuelTypeRaw(r.fuelTypeRaw());
            e.setFuelTypeNorm(r.fuelTypeNorm());
            BigDecimal gwh = r.generationGwh();
            e.setGenerationGwh(gwh);
            recordRepository.save(e);
        }
        return releaseId;
    }

    /**
     * Ingests MBIE quarterly generation data from a local file path. Implements idempotency via (source, content_hash).
     * Reuses the same pipeline and lifecycle as fixture ingestion.
     *
     * @param datasetSourceCode stable code of the DatasetSource (e.g., "mbie.generation.quarterly")
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
        
        DatasetSource source = datasetSourceRepository.findByCode(datasetSourceCode)
                .orElseThrow(() -> new IllegalArgumentException("DatasetSource not found for code: " + datasetSourceCode));

        byte[] bytes = FileIngestionUtil.readFileBytes(resolvedFilePath);
        String sha256 = FileIngestionUtil.sha256Hex(bytes);

        Optional<DatasetRelease> existing = datasetReleaseRepository
                .findFirstByDatasetSourceIdAndContentHash(source.getId(), sha256);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        DatasetIngestionRequest req = new DatasetIngestionRequest();
        req.setDatasetSourceCode(datasetSourceCode);
        req.setReleaseLabel(releaseLabel);
        req.setPublishedDate(publishedDate);
        req.setSourceUri(FileIngestionUtil.fileUri(resolvedFilePath));
        req.setContentHash(sha256);
        UUID releaseId = datasetIngestionService.ingest(req);

        List<MbieGenerationQuarterlyParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(releaseId)
                .orElseThrow();
        for (MbieGenerationQuarterlyParsedRecord r : rows) {
            MbieGenerationQuarterlyRecord e = new MbieGenerationQuarterlyRecord();
            e.setDatasetRelease(release);
            e.setPeriodYear(r.periodYear());
            e.setPeriodQuarter(r.periodQuarter());
            e.setFuelTypeRaw(r.fuelTypeRaw());
            e.setFuelTypeNorm(r.fuelTypeNorm());
            BigDecimal gwh = r.generationGwh();
            e.setGenerationGwh(gwh);
            recordRepository.save(e);
        }
        return releaseId;
    }

    private List<MbieGenerationQuarterlyParsedRecord> parse(byte[] bytes) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse MBIE quarterly CSV", e);
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
