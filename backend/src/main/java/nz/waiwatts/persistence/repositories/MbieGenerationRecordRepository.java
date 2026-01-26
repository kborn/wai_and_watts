package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.mbie.MbieGenerationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MbieGenerationRecordRepository extends JpaRepository<MbieGenerationRecord, Long> {
}
