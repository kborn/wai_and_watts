package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.datasets.DatasetRelease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DatasetReleaseRepository extends JpaRepository<DatasetRelease, UUID> {
}
