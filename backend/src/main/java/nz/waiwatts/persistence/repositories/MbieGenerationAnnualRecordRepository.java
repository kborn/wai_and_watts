package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MbieGenerationAnnualRecordRepository extends JpaRepository<MbieGenerationAnnualRecord, Long> {
    List<MbieGenerationAnnualRecord> findByDatasetReleaseId(UUID datasetReleaseId);

    @Query("""
            SELECT m
            FROM MbieGenerationAnnualRecord m
            WHERE (:fromYear IS NULL OR m.periodYear >= :fromYear)
              AND (:toYear IS NULL OR m.periodYear <= :toYear)
              AND (:fuelTypeNorm IS NULL OR LOWER(m.fuelTypeNorm) = :fuelTypeNorm)
            ORDER BY m.periodYear, m.fuelTypeNorm, m.datasetRelease.id
            """)
    List<MbieGenerationAnnualRecord> findForReadApi(@Param("fromYear") Integer fromYear,
                                                    @Param("toYear") Integer toYear,
                                                    @Param("fuelTypeNorm") String fuelTypeNorm);
    
    @Query("SELECT DISTINCT m.fuelTypeNorm FROM MbieGenerationAnnualRecord m ORDER BY m.fuelTypeNorm")
    List<String> findDistinctFuelTypeNormOrderByFuelTypeNorm();
}
