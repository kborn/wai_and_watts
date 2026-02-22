package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MbieGenerationQuarterlyRecordRepository extends JpaRepository<MbieGenerationQuarterlyRecord, Long> {
    List<MbieGenerationQuarterlyRecord> findByDatasetReleaseId(UUID datasetReleaseId);

    @Query("""
            SELECT m
            FROM MbieGenerationQuarterlyRecord m
            WHERE (:fromYear IS NULL OR m.periodYear >= :fromYear)
              AND (:toYear IS NULL OR m.periodYear <= :toYear)
              AND (:quarter IS NULL OR m.periodQuarter = :quarter)
              AND (:fuelTypeNorm IS NULL OR LOWER(m.fuelTypeNorm) = :fuelTypeNorm)
            ORDER BY m.periodYear, m.periodQuarter, m.fuelTypeNorm, m.datasetRelease.id
            """)
    List<MbieGenerationQuarterlyRecord> findForReadApi(@Param("fromYear") Integer fromYear,
                                                       @Param("toYear") Integer toYear,
                                                       @Param("quarter") Integer quarter,
                                                       @Param("fuelTypeNorm") String fuelTypeNorm);

    @Query("SELECT DISTINCT m.fuelTypeNorm FROM MbieGenerationQuarterlyRecord m ORDER BY m.fuelTypeNorm")
    List<String> findDistinctFuelTypeNormOrderByFuelTypeNorm();
}
