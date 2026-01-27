package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MbieGenerationQuarterlyRecordRepository extends JpaRepository<MbieGenerationQuarterlyRecord, Long> {
}
