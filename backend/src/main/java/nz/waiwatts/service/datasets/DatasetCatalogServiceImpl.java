package nz.waiwatts.service.datasets;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DatasetCatalogServiceImpl implements DatasetCatalogService {

    private final DatasetSourceRepository sourceRepository;
    private final DatasetReleaseRepository releaseRepository;

    public DatasetCatalogServiceImpl(DatasetSourceRepository sourceRepository,
                                     DatasetReleaseRepository releaseRepository) {
        this.sourceRepository = sourceRepository;
        this.releaseRepository = releaseRepository;
    }

    @Override
    public List<DatasetSource> findAllSources() {
        return sourceRepository.findAll();
    }

    @Override
    public Optional<DatasetSource> findSourceById(UUID id) {
        return sourceRepository.findById(id);
    }

    @Override
    public List<DatasetRelease> findReleasesBySourceId(UUID sourceId) {
        return releaseRepository.findByDatasetSourceId(sourceId);
    }
}
