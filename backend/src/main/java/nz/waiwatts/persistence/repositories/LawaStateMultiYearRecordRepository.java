package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LawaStateMultiYearRecordRepository extends JpaRepository<LawaStateMultiYearRecord, Long> {
    List<LawaStateMultiYearRecord> findByDatasetReleaseId(UUID datasetReleaseId);
}
