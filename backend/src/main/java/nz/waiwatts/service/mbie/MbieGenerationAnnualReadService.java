package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationAnnualRecordDto;

import java.util.List;
import org.jspecify.annotations.Nullable;

public interface MbieGenerationAnnualReadService {
    /**
     * Returns persisted MBIE annual rows (release-transparent); each DTO carries its source releaseId.
     * Result sets may include rows from multiple dataset releases.
     */
    List<MbieGenerationAnnualRecordDto> find(@Nullable Integer fromYear,
                                             @Nullable Integer toYear,
                                             @Nullable String fuelType);

    /**
     * Returns all distinct fuel types available in MBIE annual generation data.
     * Results are sorted alphabetically for consistent UI ordering.
     * 
     * @return List of unique, sorted fuel type codes
     */
    List<String> getFuelTypes();
}
