package nz.waiwatts.api.context.dto;

public record WaterContextDto(
    WaterTrendSummaryDto trend,
    WaterStateSummaryDto state
) {}
