package nz.waiwatts.ingestion.lawa;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.ingestion.core.DatasetIngestionRequest;
import nz.waiwatts.ingestion.core.DatasetIngestionService;
import nz.waiwatts.ingestion.core.FileIngestionUtil;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class LawaTrendMultiYearIngestion {

    private final DatasetSourceRepository datasetSourceRepository;
    private final DatasetReleaseRepository datasetReleaseRepository;
    private final DatasetIngestionService datasetIngestionService;
    private final LawaTrendMultiYearParser parser;
    private final LawaTrendMultiYearRecordRepository recordRepository;

    public LawaTrendMultiYearIngestion(DatasetSourceRepository datasetSourceRepository,
                                       DatasetReleaseRepository datasetReleaseRepository,
                                       DatasetIngestionService datasetIngestionService,
                                       LawaTrendMultiYearParser parser,
                                       LawaTrendMultiYearRecordRepository recordRepository) {
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

        // If release already exists for (source, hash), return it without duplicating rows
        Optional<DatasetRelease> existing = datasetReleaseRepository
                .findFirstByDatasetSourceIdAndContentHash(source.getId(), sha256);
        if (existing.isPresent()) {
            // Duplicate content hash detected for this source; short-circuit without persisting rows
            // (idempotent no-op)
            return existing.get().getId();
        }

        // Create/import release via lifecycle service
        DatasetIngestionRequest req = new DatasetIngestionRequest();
        req.setDatasetSourceCode(datasetSourceCode);
        req.setReleaseLabel(releaseLabel);
        req.setPublishedDate(publishedDate);
        req.setSourceUri(null);
        req.setContentHash(sha256);
        UUID releaseId = datasetIngestionService.ingest(req);

        // Parse and persist rows linked to the new release
        List<LawaTrendMultiYearParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(releaseId)
                .orElseThrow();
        java.util.ArrayList<LawaTrendMultiYearRecord> batch = new java.util.ArrayList<>(rows.size());
        for (LawaTrendMultiYearParsedRecord r : rows) {
            LawaTrendMultiYearRecord e = new LawaTrendMultiYearRecord();
            e.setDatasetRelease(release);
            e.setLawaSiteId(r.getLawaSiteId());
            e.setSiteName(r.getSiteName());
            e.setRegion(normalizeRegion(r.getRegion()));
            e.setCatchment(normalizeCatchment(r.getCatchment()));
            e.setLatitude(r.getLatitude());
            e.setLongitude(r.getLongitude());
            e.setIndicatorRaw(r.getIndicatorRaw());
            e.setIndicatorNorm(r.getIndicatorNorm());
            e.setTrendRaw(r.getTrendRaw());
            e.setTrendNorm(r.getTrendNorm());
            e.setTrendScore(r.getTrendScore());
            e.setTrendPeriodYears(r.getTrendPeriodYears());
            e.setTrendDataFrequency(r.getTrendDataFrequency());
            e.setPeriodType(r.getPeriodType());
            e.setPeriodStartYear(r.getPeriodStartYear());
            e.setPeriodEndYear(r.getPeriodEndYear());
            batch.add(e);
        }
        if (!batch.isEmpty()) {
            recordRepository.saveAll(batch);
        }
        return releaseId;
    }

    /**
     * Ingests LAWA trend multi-year data from a local file path. Implements idempotency via (source, content_hash).
     * Reuses the same pipeline and lifecycle as fixture ingestion.
     *
     * @param datasetSourceCode stable code of the DatasetSource (e.g., "lawa.water_quality.trend.multi_year")
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

        Optional<DatasetRelease> existing = datasetReleaseRepository
                .findFirstByDatasetSourceIdAndContentHash(source.getId(), sha256);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        DatasetIngestionRequest req = new DatasetIngestionRequest();
        req.setDatasetSourceCode(datasetSourceCode);
        req.setReleaseLabel(releaseLabel);
        req.setPublishedDate(publishedDate);
        req.setSourceUri(Paths.get(filePath).toUri().toString());
        req.setContentHash(sha256);
        UUID releaseId = datasetIngestionService.ingest(req);

        List<LawaTrendMultiYearParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(releaseId)
                .orElseThrow();
        java.util.ArrayList<LawaTrendMultiYearRecord> batch = new java.util.ArrayList<>(rows.size());
        for (LawaTrendMultiYearParsedRecord r : rows) {
            LawaTrendMultiYearRecord e = new LawaTrendMultiYearRecord();
            e.setDatasetRelease(release);
            e.setLawaSiteId(r.getLawaSiteId());
            e.setSiteName(r.getSiteName());
            e.setRegion(normalizeRegion(r.getRegion()));
            e.setCatchment(normalizeCatchment(r.getCatchment()));
            e.setLatitude(r.getLatitude());
            e.setLongitude(r.getLongitude());
            e.setIndicatorRaw(r.getIndicatorRaw());
            e.setIndicatorNorm(r.getIndicatorNorm());
            e.setTrendRaw(r.getTrendRaw());
            e.setTrendNorm(r.getTrendNorm());
            e.setTrendScore(r.getTrendScore());
            e.setTrendPeriodYears(r.getTrendPeriodYears());
            e.setTrendDataFrequency(r.getTrendDataFrequency());
            e.setPeriodType(r.getPeriodType());
            e.setPeriodStartYear(r.getPeriodStartYear());
            e.setPeriodEndYear(r.getPeriodEndYear());
            batch.add(e);
        }
        if (!batch.isEmpty()) {
            recordRepository.saveAll(batch);
        }
        return releaseId;
    }

    private List<LawaTrendMultiYearParsedRecord> parse(byte[] bytes) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse LAWA trend multi-year CSV", e);
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

    private static String normalizeRegion(String region) {
        return region == null ? null : region.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String normalizeCatchment(String catchment) {
        return catchment == null ? null : catchment.trim().replaceAll("\\s+", " ");
    }
}
