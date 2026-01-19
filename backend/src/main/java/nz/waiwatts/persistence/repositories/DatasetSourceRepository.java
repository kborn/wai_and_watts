package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.datasets.DatasetSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DatasetSourceRepository extends JpaRepository<DatasetSource, UUID> {
    Optional<DatasetSource> findBySourceUrl(String sourceUrl);
}
