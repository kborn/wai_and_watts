package nz.waiwatts.api.lawa.dto;

import java.util.List;

/**
 * DTO for LAWA regions response
 */
public record LawaRegionsResponseDto(
    List<String> regions
) {
    public List<String> getRegions() { return regions; }
}
