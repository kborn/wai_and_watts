package nz.waiwatts.ingestion.lawa;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.ingestion.core.DatasetIngestionRequest;
import nz.waiwatts.ingestion.core.DatasetIngestionService;
import nz.waiwatts.ingestion.core.FileIngestionUtil;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
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
public class LawaStateMultiYearIngestion {

    private final DatasetSourceRepository datasetSourceRepository;
    private final DatasetReleaseRepository datasetReleaseRepository;
    private final DatasetIngestionService datasetIngestionService;
    private final LawaStateMultiYearParser parser;
    private final LawaStateMultiYearRecordRepository recordRepository;

    public LawaStateMultiYearIngestion(DatasetSourceRepository datasetSourceRepository,
                                       DatasetReleaseRepository datasetReleaseRepository,
                                       DatasetIngestionService datasetIngestionService,
                                       LawaStateMultiYearParser parser,
                                       LawaStateMultiYearRecordRepository recordRepository) {
        this.datasetSourceRepository = datasetSourceRepository;
        this.datasetReleaseRepository = datasetReleaseRepository;
        this.datasetIngestionService = datasetIngestionService;
        this.parser = parser;
        this.recordRepository = recordRepository;
    }

    /**
     * Ingests the LAWA state multi year fixture from classpath. Implements idempotency via (source, content_hash).
     * Only persists domain rows when a new release is created.
     *
     * @param datasetSourceCode stable code of the DatasetSource (e.g., "lawa.water_quality.state.multi_year")
     * @param classpathFixture  e.g., "fixtures/lawa/water_quality/state/multi_year/lawa_state_multi_year_fixture.csv"
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
            // Duplicate content hash detected for this source; short-circuit without persisting rows
            // (idempotent no-op)
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
        List<LawaStateMultiYearParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(releaseId)
                .orElseThrow();
        java.util.ArrayList<LawaStateMultiYearRecord> batch = new java.util.ArrayList<>(rows.size());
        for (LawaStateMultiYearParsedRecord r : rows) {
            LawaStateMultiYearRecord e = new LawaStateMultiYearRecord();
            e.setDatasetRelease(release);
            e.setLawaSiteId(r.lawaSiteId());
            e.setSiteName(r.siteName());
            e.setRegion(normalizeRegion(r.region()));
            e.setCatchment(normalizeCatchment(r.catchment()));
            e.setLatitude(r.latitude());
            e.setLongitude(r.longitude());
            e.setIndicatorRaw(r.indicatorRaw());
            e.setIndicatorNorm(r.indicatorNorm());
            e.setUnits(r.units());
            e.setAttributeBand(r.attributeBand());
            e.setStateNorm(r.stateNorm());
            e.setMedian(r.median());
            e.setP95(r.p95());
            e.setRecHealthExceed260Pct(r.recHealthExceed260Pct());
            e.setRecHealthExceed540Pct(r.recHealthExceed540Pct());
            e.setPeriodType(r.periodType());
            e.setPeriodStartYear(r.periodStartYear());
            e.setPeriodEndYear(r.periodEndYear());
            batch.add(e);
        }
        if (!batch.isEmpty()) {
            recordRepository.saveAll(batch);
        }
        return releaseId;
    }

    /**
     * Ingests LAWA state multi-year data from a local file path. Implements idempotency via (source, content_hash).
     * Reuses the same pipeline and lifecycle as fixture ingestion.
     *
     * @param datasetSourceCode stable code of the DatasetSource (e.g., "lawa.water_quality.state.multi_year")
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

        List<LawaStateMultiYearParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(releaseId)
                .orElseThrow();
        java.util.ArrayList<LawaStateMultiYearRecord> batch = new java.util.ArrayList<>(rows.size());
        for (LawaStateMultiYearParsedRecord r : rows) {
            LawaStateMultiYearRecord e = new LawaStateMultiYearRecord();
            e.setDatasetRelease(release);
            e.setLawaSiteId(r.lawaSiteId());
            e.setSiteName(r.siteName());
            e.setRegion(normalizeRegion(r.region()));
            e.setCatchment(normalizeCatchment(r.catchment()));
            e.setLatitude(r.latitude());
            e.setLongitude(r.longitude());
            e.setIndicatorRaw(r.indicatorRaw());
            e.setIndicatorNorm(r.indicatorNorm());
            e.setUnits(r.units());
            e.setAttributeBand(r.attributeBand());
            e.setStateNorm(r.stateNorm());
            e.setMedian(r.median());
            e.setP95(r.p95());
            e.setRecHealthExceed260Pct(r.recHealthExceed260Pct());
            e.setRecHealthExceed540Pct(r.recHealthExceed540Pct());
            e.setPeriodType(r.periodType());
            e.setPeriodStartYear(r.periodStartYear());
            e.setPeriodEndYear(r.periodEndYear());
            batch.add(e);
        }
        if (!batch.isEmpty()) {
            recordRepository.saveAll(batch);
        }
        return releaseId;
    }

    private List<LawaStateMultiYearParsedRecord> parse(byte[] bytes) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse LAWA state multi-year CSV", e);
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
