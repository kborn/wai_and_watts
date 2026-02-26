package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.datasets.DatasetRelease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface DatasetReleaseRepository extends JpaRepository<DatasetRelease, UUID> {
    List<DatasetRelease> findByDatasetSourceId(UUID datasetSourceId);
    java.util.Optional<DatasetRelease> findFirstByDatasetSourceIdAndContentHash(UUID datasetSourceId, String contentHash);
}
