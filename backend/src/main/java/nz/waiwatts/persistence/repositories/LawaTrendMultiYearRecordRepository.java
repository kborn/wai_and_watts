package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LawaTrendMultiYearRecordRepository extends JpaRepository<LawaTrendMultiYearRecord, Long> {
    List<LawaTrendMultiYearRecord> findByDatasetReleaseId(UUID datasetReleaseId);

    @Query("""
            SELECT m
            FROM LawaTrendMultiYearRecord m
            WHERE (:fromYear IS NULL OR m.periodEndYear >= :fromYear)
              AND (:toYear IS NULL OR m.periodStartYear <= :toYear)
              AND (:indicatorNorm IS NULL OR LOWER(m.indicatorNorm) = :indicatorNorm)
              AND (:regionNorm IS NULL OR LOWER(m.region) = :regionNorm)
            ORDER BY m.periodEndYear, m.region, m.lawaSiteId, m.indicatorNorm, m.datasetRelease.id
            """)
    List<LawaTrendMultiYearRecord> findForReadApi(@Param("fromYear") Integer fromYear,
                                                  @Param("toYear") Integer toYear,
                                                  @Param("indicatorNorm") String indicatorNorm,
                                                  @Param("regionNorm") String regionNorm);

    @Query("""
            SELECT m
            FROM LawaTrendMultiYearRecord m
            WHERE (:fromYear IS NULL OR m.periodEndYear >= :fromYear)
              AND (:toYear IS NULL OR m.periodStartYear <= :toYear)
              AND (:indicatorNorm IS NULL OR LOWER(m.indicatorNorm) = :indicatorNorm)
              AND (:regionNorm IS NULL OR LOWER(m.region) = :regionNorm)
              AND (:trendNorm IS NULL OR LOWER(m.trendNorm) = :trendNorm)
            ORDER BY m.periodEndYear, m.region, m.lawaSiteId, m.indicatorNorm, m.datasetRelease.id
            """)
    List<LawaTrendMultiYearRecord> findForAsk(@Param("fromYear") Integer fromYear,
                                              @Param("toYear") Integer toYear,
                                              @Param("indicatorNorm") String indicatorNorm,
                                              @Param("regionNorm") String regionNorm,
                                              @Param("trendNorm") String trendNorm);
    
    @Query("SELECT DISTINCT LOWER(m.region) FROM LawaTrendMultiYearRecord m ORDER BY LOWER(m.region)")
    List<String> findDistinctRegionOrderByRegion();
    
    @Query("SELECT DISTINCT m.indicatorNorm FROM LawaTrendMultiYearRecord m ORDER BY m.indicatorNorm")
    List<String> findDistinctIndicatorNormOrderByIndicatorNorm();
}
