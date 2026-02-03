package nz.waiwatts.api.lawa.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class LawaStateMultiYearRecordDto {

    private String lawaSiteId;
    private String siteName;
    private String region;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String indicatorRaw;
    private String indicatorNorm;
    private String units;
    private String attributeBand;
    private String stateNorm;
    private BigDecimal median;
    private BigDecimal p95;
    private BigDecimal recHealthExceed260Pct;
    private BigDecimal recHealthExceed540Pct;
    private String periodType;
    private int periodStartYear;
    private int periodEndYear;
    private UUID releaseId;

    public LawaStateMultiYearRecordDto() {
    }

    public LawaStateMultiYearRecordDto(String lawaSiteId, String siteName, String region, BigDecimal latitude, BigDecimal longitude, String indicatorRaw, String indicatorNorm, String units, String attributeBand, String stateNorm, BigDecimal median, BigDecimal p95, BigDecimal recHealthExceed260Pct, BigDecimal recHealthExceed540Pct, String periodType, int periodStartYear, int periodEndYear, UUID releaseId) {
        this.lawaSiteId = lawaSiteId;
        this.siteName = siteName;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.indicatorRaw = indicatorRaw;
        this.indicatorNorm = indicatorNorm;
        this.units = units;
        this.attributeBand = attributeBand;
        this.stateNorm = stateNorm;
        this.median = median;
        this.p95 = p95;
        this.recHealthExceed260Pct = recHealthExceed260Pct;
        this.recHealthExceed540Pct = recHealthExceed540Pct;
        this.periodType = periodType;
        this.periodStartYear = periodStartYear;
        this.periodEndYear = periodEndYear;
        this.releaseId = releaseId;
    }

    public String getLawaSiteId() {
        return lawaSiteId;
    }

    public void setLawaSiteId(String lawaSiteId) {
        this.lawaSiteId = lawaSiteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getIndicatorRaw() {
        return indicatorRaw;
    }

    public void setIndicatorRaw(String indicatorRaw) {
        this.indicatorRaw = indicatorRaw;
    }

    public String getIndicatorNorm() {
        return indicatorNorm;
    }

    public void setIndicatorNorm(String indicatorNorm) {
        this.indicatorNorm = indicatorNorm;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getAttributeBand() {
        return attributeBand;
    }

    public void setAttributeBand(String attributeBand) {
        this.attributeBand = attributeBand;
    }

    public String getStateNorm() {
        return stateNorm;
    }

    public void setStateNorm(String stateNorm) {
        this.stateNorm = stateNorm;
    }

    public BigDecimal getMedian() {
        return median;
    }

    public void setMedian(BigDecimal median) {
        this.median = median;
    }

    public BigDecimal getP95() {
        return p95;
    }

    public void setP95(BigDecimal p95) {
        this.p95 = p95;
    }

    public BigDecimal getRecHealthExceed260Pct() {
        return recHealthExceed260Pct;
    }

    public void setRecHealthExceed260Pct(BigDecimal recHealthExceed260Pct) {
        this.recHealthExceed260Pct = recHealthExceed260Pct;
    }

    public BigDecimal getRecHealthExceed540Pct() {
        return recHealthExceed540Pct;
    }

    public void setRecHealthExceed540Pct(BigDecimal recHealthExceed540Pct) {
        this.recHealthExceed540Pct = recHealthExceed540Pct;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public int getPeriodStartYear() {
        return periodStartYear;
    }

    public void setPeriodStartYear(int periodStartYear) {
        this.periodStartYear = periodStartYear;
    }

    public int getPeriodEndYear() {
        return periodEndYear;
    }

    public void setPeriodEndYear(int periodEndYear) {
        this.periodEndYear = periodEndYear;
    }

    public UUID getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(UUID releaseId) {
        this.releaseId = releaseId;
    }
}
