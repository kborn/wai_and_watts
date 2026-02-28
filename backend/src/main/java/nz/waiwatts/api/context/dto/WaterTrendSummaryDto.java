package nz.waiwatts.api.context.dto;

public record WaterTrendSummaryDto(
    int unitCount,
    double degradingPct,
    double improvingPct,
    double indeterminatePct,
    double insufficientPct
) {}
