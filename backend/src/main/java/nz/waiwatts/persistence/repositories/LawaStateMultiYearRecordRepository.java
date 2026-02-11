package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface LawaStateMultiYearRecordRepository extends JpaRepository<LawaStateMultiYearRecord, Long> {
    List<LawaStateMultiYearRecord> findByDatasetReleaseId(UUID datasetReleaseId);
    
    @Query("SELECT DISTINCT m.region FROM LawaStateMultiYearRecord m ORDER BY m.region")
    List<String> findDistinctRegionOrderByRegion();
    
    @Query("SELECT DISTINCT m.indicatorNorm FROM LawaStateMultiYearRecord m ORDER BY m.indicatorNorm")
    List<String> findDistinctIndicatorNormOrderByIndicatorNorm();
}
