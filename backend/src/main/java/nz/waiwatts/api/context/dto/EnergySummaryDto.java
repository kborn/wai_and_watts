package nz.waiwatts.api.context.dto;

public record EnergySummaryDto(
    int latestYear,
    double latestRenewablePct,
    double renewable5YrDeltaPct,
    double fossilLatestPct
) {}
