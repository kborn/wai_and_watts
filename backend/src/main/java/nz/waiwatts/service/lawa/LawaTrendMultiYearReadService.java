package nz.waiwatts.service.lawa;

import nz.waiwatts.api.lawa.dto.LawaTrendMultiYearRecordDto;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface LawaTrendMultiYearReadService {
    /**
     * Returns persisted LAWA trend rows (release-transparent); each DTO carries its source releaseId.
     * Result sets may include rows from multiple dataset releases.
     */
    List<LawaTrendMultiYearRecordDto> find(@Nullable Integer fromYear,
                                           @Nullable Integer toYear,
                                           @Nullable String indicator,
                                           @Nullable String region);
    
    /**
     * Returns all distinct regions available in LAWA trend data.
     * Results are sorted alphabetically for consistent UI ordering.
     *
     * @return List of unique, sorted region names
     */
    List<String> getRegions();
    
    /**
     * Returns all distinct indicators available in LAWA trend data.
     * Results are sorted alphabetically for consistent UI ordering.
     *
     * @return List of unique, sorted indicator codes
     */
    List<String> getIndicators();
}
