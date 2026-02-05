package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LawaTrendMultiYearRecordRepository extends JpaRepository<LawaTrendMultiYearRecord, Long> {
    List<LawaTrendMultiYearRecord> findByDatasetReleaseId(UUID datasetReleaseId);
}
