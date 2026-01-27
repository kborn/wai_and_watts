package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MbieGenerationAnnualRecordRepository extends JpaRepository<MbieGenerationAnnualRecord, Long> {
}
