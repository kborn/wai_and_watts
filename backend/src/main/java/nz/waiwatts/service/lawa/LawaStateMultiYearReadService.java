package nz.waiwatts.service.lawa;

import nz.waiwatts.api.lawa.dto.LawaStateMultiYearRecordDto;

import java.util.List;

import org.springframework.lang.Nullable;

public interface LawaStateMultiYearReadService {
    /**
     * Returns persisted LAWA state rows (release-transparent); each DTO carries its source releaseId.
     */
    List<LawaStateMultiYearRecordDto> find(@Nullable Integer fromYear,
                                           @Nullable Integer toYear,
                                           @Nullable String indicator,
                                           @Nullable String region);
    
    /**
     * Returns all distinct regions available in LAWA state data.
     * Results are sorted alphabetically for consistent UI ordering.
     *
     * @return List of unique, sorted region names
     */
    List<String> getRegions();
    
    /**
     * Returns all distinct indicators available in LAWA state data.
     * Results are sorted alphabetically for consistent UI ordering.
     *
     * @return List of unique, sorted indicator codes
     */
    List<String> getIndicators();
}
