package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LawaTrendMultiYearRecordRepository extends JpaRepository<LawaTrendMultiYearRecord, Long> {
}
