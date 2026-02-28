package nz.waiwatts.ingestion.lawa;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.ingestion.core.DatasetReleaseRegistrationUtil;
import nz.waiwatts.ingestion.core.DatasetIngestionService;
import nz.waiwatts.ingestion.core.FileIngestionUtil;
import nz.waiwatts.ingestion.core.ReleaseRegistrationResult;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
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
        return ingestBytes(
            datasetSourceCode,
            FileIngestionUtil.readClasspathBytes(classpathFixture),
            publishedDate,
            releaseLabel,
            null
        );
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

        List<LawaTrendMultiYearParsedRecord> rows = parse(bytes);
        DatasetRelease release = datasetReleaseRepository.findById(registration.releaseId()).orElseThrow();
        java.util.ArrayList<LawaTrendMultiYearRecord> batch = new java.util.ArrayList<>(rows.size());
        for (LawaTrendMultiYearParsedRecord r : rows) {
            LawaTrendMultiYearRecord e = new LawaTrendMultiYearRecord();
            e.setDatasetRelease(release);
            e.setLawaSiteId(r.lawaSiteId());
            e.setSiteName(r.siteName());
            e.setRegion(normalizeRegion(r.region()));
            e.setCatchment(normalizeCatchment(r.catchment()));
            e.setLatitude(r.latitude());
            e.setLongitude(r.longitude());
            e.setIndicatorRaw(r.indicatorRaw());
            e.setIndicatorNorm(r.indicatorNorm());
            e.setTrendRaw(r.trendRaw());
            e.setTrendNorm(r.trendNorm());
            e.setTrendScore(r.trendScore());
            e.setTrendPeriodYears(r.trendPeriodYears());
            e.setTrendDataFrequency(r.trendDataFrequency());
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

    private List<LawaTrendMultiYearParsedRecord> parse(byte[] bytes) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse LAWA trend multi-year CSV", e);
        }
    }

    private static String normalizeRegion(String region) {
        return region == null ? null : region.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String normalizeCatchment(String catchment) {
        return catchment == null ? null : catchment.trim().replaceAll("\\s+", " ");
    }
}
