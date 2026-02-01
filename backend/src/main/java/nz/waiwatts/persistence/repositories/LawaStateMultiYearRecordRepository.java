package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LawaStateMultiYearRecordRepository extends JpaRepository<LawaStateMultiYearRecord, Long> {
}
