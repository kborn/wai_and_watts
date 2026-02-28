package nz.waiwatts.ingestion.lawa;

import java.math.BigDecimal;

public record LawaTrendMultiYearParsedRecord(String lawaSiteId, String siteName,
                                             String region, String catchment,
                                             BigDecimal latitude,
                                             BigDecimal longitude,
                                             String indicatorRaw,
                                             String indicatorNorm,
                                             String trendRaw, String trendNorm,
                                             Integer trendScore,
                                             Integer trendPeriodYears,
                                             String trendDataFrequency,
                                             String periodType,
                                             int periodStartYear,
                                             int periodEndYear) {
}
