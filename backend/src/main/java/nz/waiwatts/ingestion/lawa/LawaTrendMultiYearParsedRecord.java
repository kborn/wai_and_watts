package nz.waiwatts.ingestion.lawa;

import java.math.BigDecimal;

public class LawaTrendMultiYearParsedRecord {
    private final String lawaSiteId;
    private final String siteName;
    private final String region;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String indicatorRaw;
    private final String indicatorNorm;
    private final String trendRaw;
    private final String trendNorm;
    private final Integer trendScore;
    private final Integer trendPeriodYears;
    private final String trendDataFrequency;
    private final String periodType;
    private final int periodStartYear;
    private final int periodEndYear;

    public LawaTrendMultiYearParsedRecord(String lawaSiteId, String siteName, String region, BigDecimal latitude, BigDecimal longitude,
                                          String indicatorRaw, String indicatorNorm,
                                          String trendRaw, String trendNorm, Integer trendScore, Integer trendPeriodYears,
                                          String trendDataFrequency, String periodType, int periodStartYear, int periodEndYear) {
        this.lawaSiteId = lawaSiteId;
        this.siteName = siteName;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.indicatorRaw = indicatorRaw;
        this.indicatorNorm = indicatorNorm;
        this.trendRaw = trendRaw;
        this.trendNorm = trendNorm;
        this.trendScore = trendScore;
        this.trendPeriodYears = trendPeriodYears;
        this.trendDataFrequency = trendDataFrequency;
        this.periodType = periodType;
        this.periodStartYear = periodStartYear;
        this.periodEndYear = periodEndYear;
    }

    public String getLawaSiteId() { return lawaSiteId; }
    public String getSiteName() { return siteName; }
    public String getRegion() { return region; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getIndicatorRaw() { return indicatorRaw; }
    public String getIndicatorNorm() { return indicatorNorm; }
    public String getTrendRaw() { return trendRaw; }
    public String getTrendNorm() { return trendNorm; }
    public Integer getTrendScore() { return trendScore; }
    public Integer getTrendPeriodYears() { return trendPeriodYears; }
    public String getTrendDataFrequency() { return trendDataFrequency; }
    public String getPeriodType() { return periodType; }
    public int getPeriodStartYear() { return periodStartYear; }
    public int getPeriodEndYear() { return periodEndYear; }
}
