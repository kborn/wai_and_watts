package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MbieGenerationAnnualRecordRepository extends JpaRepository<MbieGenerationAnnualRecord, Long> {
    List<MbieGenerationAnnualRecord> findByDatasetReleaseId(UUID datasetReleaseId);
}
