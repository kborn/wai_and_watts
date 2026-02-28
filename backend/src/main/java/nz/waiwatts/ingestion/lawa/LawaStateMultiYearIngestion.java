package nz.waiwatts.ingestion.lawa;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.ingestion.core.DatasetReleaseRegistrationUtil;
import nz.waiwatts.ingestion.core.DatasetIngestionService;
import nz.waiwatts.ingestion.core.FileIngestionUtil;
import nz.waiwatts.ingestion.core.ReleaseRegistrationResult;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
     * Ingests the LAWA state multi-year fixture from classpath. Implements idempotency via (source, content_hash).
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
        return ingestBytes(
            datasetSourceCode,
            FileIngestionUtil.readClasspathBytes(classpathFixture),
            publishedDate,
            releaseLabel,
            null
        );
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

        List<LawaStateMultiYearParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(registration.releaseId()).orElseThrow();
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
        return registration.releaseId();
    }

    private List<LawaStateMultiYearParsedRecord> parse(byte[] bytes) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse LAWA state multi-year CSV", e);
        }
    }

    private static String normalizeRegion(String region) {
        return region == null ? null : region.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String normalizeCatchment(String catchment) {
        return catchment == null ? null : catchment.trim().replaceAll("\\s+", " ");
    }
}
