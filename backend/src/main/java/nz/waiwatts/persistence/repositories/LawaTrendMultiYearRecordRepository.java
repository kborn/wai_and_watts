package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface LawaTrendMultiYearRecordRepository extends JpaRepository<LawaTrendMultiYearRecord, Long> {
    List<LawaTrendMultiYearRecord> findByDatasetReleaseId(UUID datasetReleaseId);
    
    @Query("SELECT DISTINCT m.region FROM LawaTrendMultiYearRecord m ORDER BY m.region")
    List<String> findDistinctRegionOrderByRegion();
    
    @Query("SELECT DISTINCT m.indicatorNorm FROM LawaTrendMultiYearRecord m ORDER BY m.indicatorNorm")
    List<String> findDistinctIndicatorNormOrderByIndicatorNorm();
}
