package nz.waiwatts.ingestion.core;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public final class DatasetReleaseRegistrationUtil {

    private DatasetReleaseRegistrationUtil() {
    }

    public static ReleaseRegistrationResult registerRelease(
        DatasetSourceRepository datasetSourceRepository,
        DatasetReleaseRepository datasetReleaseRepository,
        DatasetIngestionService datasetIngestionService,
        String datasetSourceCode,
        String contentHash,
        LocalDate publishedDate,
        String releaseLabel,
        String sourceUri
    ) {
        DatasetSource source = datasetSourceRepository.findByCode(datasetSourceCode)
            .orElseThrow(() -> new IllegalArgumentException("DatasetSource not found for code: " + datasetSourceCode));

        Optional<DatasetRelease> existing = datasetReleaseRepository
            .findFirstByDatasetSourceIdAndContentHash(source.getId(), contentHash);
        if (existing.isPresent()) {
            return new ReleaseRegistrationResult(existing.get().getId(), false);
        }

        DatasetIngestionRequest req = new DatasetIngestionRequest();
        req.setDatasetSourceCode(datasetSourceCode);
        req.setReleaseLabel(releaseLabel);
        req.setPublishedDate(publishedDate);
        req.setSourceUri(sourceUri);
        req.setContentHash(contentHash);
        UUID releaseId = datasetIngestionService.ingest(req);
        return new ReleaseRegistrationResult(releaseId, true);
    }
}
