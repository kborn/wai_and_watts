package nz.waiwatts.domain.lawa;

import jakarta.persistence.*;
import nz.waiwatts.domain.datasets.DatasetRelease;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lawa_water_quality_trend_multi_year_record",
       uniqueConstraints = @UniqueConstraint(name = "uk_lawa_trend_window",
               columnNames = {"dataset_release_id", "lawa_site_id", "indicator_norm", "period_type", "trend_period_years", "period_end_year"}))
public class LawaTrendMultiYearRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_release_id", nullable = false)
    private DatasetRelease datasetRelease;

    @Column(name = "lawa_site_id", nullable = false)
    private String lawaSiteId;

    @Column(name = "site_name", nullable = false)
    private String siteName;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "catchment")
    private String catchment;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "indicator_raw", nullable = false)
    private String indicatorRaw;

    @Column(name = "indicator_norm", nullable = false)
    private String indicatorNorm;

    @Column(name = "trend_raw", nullable = false)
    private String trendRaw;

    @Column(name = "trend_norm", nullable = false)
    private String trendNorm;

    @Column(name = "trend_score", nullable = false)
    private Integer trendScore;

    @Column(name = "trend_period_years", nullable = false)
    private Integer trendPeriodYears;

    @Column(name = "trend_data_frequency")
    private String trendDataFrequency;

    @Column(name = "period_type", nullable = false)
    private String periodType;

    @Column(name = "period_start_year", nullable = false)
    private Integer periodStartYear;

    @Column(name = "period_end_year", nullable = false)
    private Integer periodEndYear;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public DatasetRelease getDatasetRelease() { return datasetRelease; }
    public void setDatasetRelease(DatasetRelease datasetRelease) { this.datasetRelease = datasetRelease; }
    public String getLawaSiteId() { return lawaSiteId; }
    public void setLawaSiteId(String lawaSiteId) { this.lawaSiteId = lawaSiteId; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getCatchment() { return catchment; }
    public void setCatchment(String catchment) { this.catchment = catchment; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getIndicatorRaw() { return indicatorRaw; }
    public void setIndicatorRaw(String indicatorRaw) { this.indicatorRaw = indicatorRaw; }
    public String getIndicatorNorm() { return indicatorNorm; }
    public void setIndicatorNorm(String indicatorNorm) { this.indicatorNorm = indicatorNorm; }
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
    public Integer getPeriodStartYear() { return periodStartYear; }
    public void setPeriodStartYear(Integer periodStartYear) { this.periodStartYear = periodStartYear; }
    public Integer getPeriodEndYear() { return periodEndYear; }
    public void setPeriodEndYear(Integer periodEndYear) { this.periodEndYear = periodEndYear; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
