package nz.waiwatts.ingestion.lawa;

import java.math.BigDecimal;

public class LawaStateMultiYearParsedRecord {
    private final String lawaSiteId;
    private final String siteName;
    private final String region;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String indicatorRaw;
    private final String indicatorNorm;
    private final String units;
    private final String attributeBand;
    private final String stateNorm;
    private final BigDecimal median;
    private final BigDecimal p95;
    private final BigDecimal recHealthExceed260Pct;
    private final BigDecimal recHealthExceed540Pct;
    private final String periodType;
    private final int periodStartYear;
    private final int periodEndYear;

    public LawaStateMultiYearParsedRecord(String lawaSiteId, String siteName, String region, BigDecimal latitude, BigDecimal longitude, String indicatorRaw, String indicatorNorm, String units, String attributeBand, String stateNorm, BigDecimal median, BigDecimal p95, BigDecimal recHealthExceed260Pct, BigDecimal recHealthExceed540Pct, String periodType, int periodStartYear, int periodEndYear) {
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
    }

    public String getLawaSiteId() {
        return lawaSiteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getRegion() {
        return region;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getIndicatorRaw() {
        return indicatorRaw;
    }

    public String getIndicatorNorm() {
        return indicatorNorm;
    }

    public String getUnits() {
        return units;
    }

    public String getAttributeBand() {
        return attributeBand;
    }

    public String getStateNorm() {
        return stateNorm;
    }

    public BigDecimal getMedian() {
        return median;
    }

    public BigDecimal getP95() {
        return p95;
    }

    public BigDecimal getRecHealthExceed260Pct() {
        return recHealthExceed260Pct;
    }

    public BigDecimal getRecHealthExceed540Pct() {
        return recHealthExceed540Pct;
    }

    public String getPeriodType() {
        return periodType;
    }

    public int getPeriodStartYear() {
        return periodStartYear;
    }

    public int getPeriodEndYear() {
        return periodEndYear;
    }
}
