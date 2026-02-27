package nz.waiwatts.api.lawa.dto;

import java.util.List;

/**
 * DTO for LAWA indicators response
 */
public record LawaIndicatorsResponseDto(
    List<String> indicators
) {}
