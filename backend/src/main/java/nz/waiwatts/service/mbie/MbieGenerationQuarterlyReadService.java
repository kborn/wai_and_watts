package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationQuarterlyRecordDto;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface MbieGenerationQuarterlyReadService {
    /**
     * Returns persisted MBIE quarterly rows (release-transparent); each DTO carries its source releaseId.
     * Result sets may include rows from multiple dataset releases.
     */
    List<MbieGenerationQuarterlyRecordDto> find(@Nullable Integer fromYear,
                                                @Nullable Integer toYear,
                                                @Nullable Integer quarter,
                                                @Nullable String fuelType);

    /**
     * Returns all distinct fuel types available in MBIE quarterly generation data.
     * Results are sorted alphabetically for consistent UI ordering.
     *
     * @return List of unique, sorted fuel type codes
     */
    List<String> getFuelTypes();
}
