package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LawaStateMultiYearRecordRepository extends JpaRepository<LawaStateMultiYearRecord, Long> {
    List<LawaStateMultiYearRecord> findByDatasetReleaseId(UUID datasetReleaseId);

    @Query("""
            SELECT m
            FROM LawaStateMultiYearRecord m
            WHERE (:fromYear IS NULL OR m.periodEndYear >= :fromYear)
              AND (:toYear IS NULL OR m.periodStartYear <= :toYear)
              AND (:indicatorNorm IS NULL OR LOWER(m.indicatorNorm) = :indicatorNorm)
              AND (:regionNorm IS NULL OR LOWER(m.region) = :regionNorm)
            ORDER BY m.periodEndYear, m.region, m.lawaSiteId, m.indicatorNorm
            """)
    List<LawaStateMultiYearRecord> findForReadApi(@Param("fromYear") Integer fromYear,
                                                  @Param("toYear") Integer toYear,
                                                  @Param("indicatorNorm") String indicatorNorm,
                                                  @Param("regionNorm") String regionNorm);
    
    @Query("SELECT DISTINCT m.region FROM LawaStateMultiYearRecord m ORDER BY m.region")
    List<String> findDistinctRegionOrderByRegion();
    
    @Query("SELECT DISTINCT m.indicatorNorm FROM LawaStateMultiYearRecord m ORDER BY m.indicatorNorm")
    List<String> findDistinctIndicatorNormOrderByIndicatorNorm();
}
