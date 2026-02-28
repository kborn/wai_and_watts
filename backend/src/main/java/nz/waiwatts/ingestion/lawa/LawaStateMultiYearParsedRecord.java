package nz.waiwatts.ingestion.lawa;

import java.math.BigDecimal;

public record LawaStateMultiYearParsedRecord(String lawaSiteId, String siteName,
                                             String region, String catchment,
                                             BigDecimal latitude,
                                             BigDecimal longitude,
                                             String indicatorRaw,
                                             String indicatorNorm, String units,
                                             String attributeBand,
                                             String stateNorm,
                                             BigDecimal median, BigDecimal p95,
                                             BigDecimal recHealthExceed260Pct,
                                             BigDecimal recHealthExceed540Pct,
                                             String periodType,
                                             int periodStartYear,
                                             int periodEndYear) {
}
