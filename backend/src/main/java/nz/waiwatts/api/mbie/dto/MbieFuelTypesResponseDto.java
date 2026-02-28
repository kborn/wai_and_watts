package nz.waiwatts.api.mbie.dto;

import java.util.List;

/**
 * DTO for MBIE fuel types response
 */
public record MbieFuelTypesResponseDto(
    List<String> fuelTypes
) {}
