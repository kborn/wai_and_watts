package nz.waiwatts.api.lawa.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LawaTrendMultiYearRecordDto(
    String lawaSiteId,
    String siteName,
    String region,
    String catchment,
    BigDecimal latitude,
    BigDecimal longitude,
    String indicatorRaw,
    String indicatorNorm,
    String trendRaw,
    String trendNorm,
    Integer trendScore,
    Integer trendPeriodYears,
    String trendDataFrequency,
    String periodType,
    int periodStartYear,
    int periodEndYear,
    UUID releaseId
) {}
