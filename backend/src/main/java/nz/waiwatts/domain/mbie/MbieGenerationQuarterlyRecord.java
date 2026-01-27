package nz.waiwatts.domain.mbie;

import jakarta.persistence.*;
import nz.waiwatts.domain.datasets.DatasetRelease;

import java.math.BigDecimal;

@Entity
@Table(name = "mbie_generation_quarterly_record")
public class MbieGenerationQuarterlyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_release_id", nullable = false)
    private DatasetRelease datasetRelease;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_quarter", nullable = false)
    private int periodQuarter; // 1..4

    @Column(name = "fuel_type_raw", nullable = false)
    private String fuelTypeRaw;

    @Column(name = "fuel_type_norm", nullable = false)
    private String fuelTypeNorm;

    @Column(name = "generation_gwh", nullable = false, precision = 18, scale = 3)
    private BigDecimal generationGwh;

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

    public int getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(int periodYear) {
        this.periodYear = periodYear;
    }

    public int getPeriodQuarter() {
        return periodQuarter;
    }

    public void setPeriodQuarter(int periodQuarter) {
        this.periodQuarter = periodQuarter;
    }

    public String getFuelTypeRaw() {
        return fuelTypeRaw;
    }

    public void setFuelTypeRaw(String fuelTypeRaw) {
        this.fuelTypeRaw = fuelTypeRaw;
    }

    public String getFuelTypeNorm() {
        return fuelTypeNorm;
    }

    public void setFuelTypeNorm(String fuelTypeNorm) {
        this.fuelTypeNorm = fuelTypeNorm;
    }

    public BigDecimal getGenerationGwh() {
        return generationGwh;
    }

    public void setGenerationGwh(BigDecimal generationGwh) {
        this.generationGwh = generationGwh;
    }
}
