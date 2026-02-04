package nz.waiwatts.api.lawa.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class LawaTrendMultiYearRecordDto {
    private String lawaSiteId;
    private String siteName;
    private String region;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String indicatorRaw;
    private String indicatorNorm;
    private String units;
    private String trendRaw;
    private String trendNorm;
    private Integer trendScore;
    private Integer trendPeriodYears;
    private String trendDataFrequency;
    private String periodType;
    private int periodStartYear;
    private int periodEndYear;
    private UUID releaseId;

    public LawaTrendMultiYearRecordDto() {}

    public LawaTrendMultiYearRecordDto(String lawaSiteId, String siteName, String region, BigDecimal latitude, BigDecimal longitude,
                                       String indicatorRaw, String indicatorNorm, String units,
                                       String trendRaw, String trendNorm, Integer trendScore, Integer trendPeriodYears,
                                       String trendDataFrequency, String periodType, int periodStartYear, int periodEndYear,
                                       UUID releaseId) {
        this.lawaSiteId = lawaSiteId;
        this.siteName = siteName;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.indicatorRaw = indicatorRaw;
        this.indicatorNorm = indicatorNorm;
        this.units = units;
        this.trendRaw = trendRaw;
        this.trendNorm = trendNorm;
        this.trendScore = trendScore;
        this.trendPeriodYears = trendPeriodYears;
        this.trendDataFrequency = trendDataFrequency;
        this.periodType = periodType;
        this.periodStartYear = periodStartYear;
        this.periodEndYear = periodEndYear;
        this.releaseId = releaseId;
    }

    public String getLawaSiteId() { return lawaSiteId; }
    public void setLawaSiteId(String lawaSiteId) { this.lawaSiteId = lawaSiteId; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getIndicatorRaw() { return indicatorRaw; }
    public void setIndicatorRaw(String indicatorRaw) { this.indicatorRaw = indicatorRaw; }
    public String getIndicatorNorm() { return indicatorNorm; }
    public void setIndicatorNorm(String indicatorNorm) { this.indicatorNorm = indicatorNorm; }
    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = units; }
    public String getTrendRaw() { return trendRaw; }
    public void setTrendRaw(String trendRaw) { this.trendRaw = trendRaw; }
    public String getTrendNorm() { return trendNorm; }
    public void setTrendNorm(String trendNorm) { this.trendNorm = trendNorm; }
    public Integer getTrendScore() { return trendScore; }
    public void setTrendScore(Integer trendScore) { this.trendScore = trendScore; }
    public Integer getTrendPeriodYears() { return trendPeriodYears; }
    public void setTrendPeriodYears(Integer trendPeriodYears) { this.trendPeriodYears = trendPeriodYears; }
    public String getTrendDataFrequency() { return trendDataFrequency; }
    public void setTrendDataFrequency(String trendDataFrequency) { this.trendDataFrequency = trendDataFrequency; }
    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public int getPeriodStartYear() { return periodStartYear; }
    public void setPeriodStartYear(int periodStartYear) { this.periodStartYear = periodStartYear; }
    public int getPeriodEndYear() { return periodEndYear; }
    public void setPeriodEndYear(int periodEndYear) { this.periodEndYear = periodEndYear; }
    public UUID getReleaseId() { return releaseId; }
    public void setReleaseId(UUID releaseId) { this.releaseId = releaseId; }
}
