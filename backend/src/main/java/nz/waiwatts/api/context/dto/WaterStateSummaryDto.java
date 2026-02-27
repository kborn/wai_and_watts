package nz.waiwatts.api.context.dto;

import java.util.Map;

public record WaterStateSummaryDto(
    int unitCount,
    Map<String, Integer> bandDistribution
) {}
