package nz.waiwatts.domain.lawa;

import jakarta.persistence.*;
import nz.waiwatts.domain.datasets.DatasetRelease;

import java.math.BigDecimal;

@Entity
@Table(name = "lawa_state_multi_year_record")
public class LawaStateMultiYearRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_release_id", nullable = false)
    private DatasetRelease datasetRelease;

    @Column(name = "lawa_site_id", nullable = false)
    private String lawaSiteId;

    @Column(name = "site_name", nullable = false)
    private String siteName;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "indicator_raw", nullable = false)
    private String indicatorRaw;

    @Column(name = "indicator_norm", nullable = false)
    private String indicatorNorm;

    @Column(name = "units")
    private String units;

    @Column(name = "attribute_band", nullable = false)
    private String attributeBand;

    @Column(name = "state_norm", nullable = false)
    private String stateNorm;

    @Column(name = "median", precision = 14, scale = 8)
    private BigDecimal median;

    @Column(name = "p95", precision = 16, scale = 8)
    private BigDecimal p95;

    @Column(name = "rec_health_exceed_260_pct", precision = 7, scale = 4)
    private BigDecimal recHealthExceed260Pct;

    @Column(name = "rec_health_exceed_540_pct", precision = 7, scale = 4)
    private BigDecimal recHealthExceed540Pct;

    @Column(name = "period_type", nullable = false)
    private String periodType;

    @Column(name = "period_start_year", nullable = false)
    private int periodStartYear;

    @Column(name = "period_end_year", nullable = false)
    private int periodEndYear;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DatasetRelease getDatasetRelease() {
        return datasetRelease;
    }

    public void setDatasetRelease(DatasetRelease datasetRelease) {
        this.datasetRelease = datasetRelease;
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
}
