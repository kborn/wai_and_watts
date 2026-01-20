package nz.waiwatts.ingestion.core;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class DatasetIngestionService {

    private final DatasetSourceRepository sourceRepository;
    private final DatasetReleaseRepository releaseRepository;

    public DatasetIngestionService(DatasetSourceRepository sourceRepository,
                                   DatasetReleaseRepository releaseRepository) {
        this.sourceRepository = sourceRepository;
        this.releaseRepository = releaseRepository;
    }

    @Transactional
    public UUID ingest(DatasetIngestionRequest req) {
        // Lookup source by stable code; URL is metadata
        DatasetSource source = sourceRepository.findByCode(req.getDatasetSourceCode())
                .orElseThrow(() -> new NoSuchElementException("DatasetSource not found for code: " + req.getDatasetSourceCode()));

        // Idempotency check: if release with same (source, content_hash) exists, return it
        return releaseRepository.findFirstByDatasetSourceIdAndContentHash(source.getId(), req.getContentHash())
                .map(DatasetRelease::getId)
                .orElseGet(() -> createImportedRelease(source, req));
    }

    private UUID createImportedRelease(DatasetSource source, DatasetIngestionRequest req) {
        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(source);
        rel.setPublishedDate(req.getPublishedDate());
        rel.setReleaseLabel(req.getReleaseLabel());
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash(req.getContentHash());
        rel.setStatus(ReleaseStatus.PENDING);

        // Persist PENDING first
        rel = releaseRepository.save(rel);

        // Immediately mark IMPORTED (stub success) and set imported_at
        rel.setImportedAt(LocalDateTime.now());
        rel.setStatus(ReleaseStatus.IMPORTED);
        rel = releaseRepository.save(rel);

        return rel.getId();
    }
}
