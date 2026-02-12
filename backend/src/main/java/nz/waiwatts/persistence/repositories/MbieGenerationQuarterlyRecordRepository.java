package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MbieGenerationQuarterlyRecordRepository extends JpaRepository<MbieGenerationQuarterlyRecord, Long> {
    List<MbieGenerationQuarterlyRecord> findByDatasetReleaseId(UUID datasetReleaseId);

    @Query("SELECT DISTINCT m.fuelTypeNorm FROM MbieGenerationQuarterlyRecord m ORDER BY m.fuelTypeNorm")
    List<String> findDistinctFuelTypeNormOrderByFuelTypeNorm();
}
