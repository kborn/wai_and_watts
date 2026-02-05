package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MbieGenerationQuarterlyRecordRepository extends JpaRepository<MbieGenerationQuarterlyRecord, Long> {
    List<MbieGenerationQuarterlyRecord> findByDatasetReleaseId(UUID datasetReleaseId);
}
