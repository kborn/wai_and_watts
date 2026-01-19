package nz.waiwatts.service.datasets;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetCatalogService {
    List<DatasetSource> findAllSources();
    Optional<DatasetSource> findSourceById(UUID id);
    List<DatasetRelease> findReleasesBySourceId(UUID sourceId);
}
