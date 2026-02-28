package nz.waiwatts.api.context.dto;

import java.time.Instant;

public record RegionContextFactPackDto(
    String regionId,
    Instant generatedAt,
    WaterContextDto water,
    EnergySummaryDto energy
) {}
