package nz.waiwatts.api.mbie.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public class MbieGenerationQuarterlyRecordDto {
    private int periodYear;
    private int periodQuarter;
    @JsonProperty("fuelTypeNorm")
    private String fuelTypeNorm; // normalized fuel type
    @JsonProperty("fuelTypeRaw")
    private String fuelTypeRaw;
    private BigDecimal generationGwh;
    private UUID releaseId;

    public MbieGenerationQuarterlyRecordDto() {}

    public MbieGenerationQuarterlyRecordDto(int periodYear, int periodQuarter, String fuelTypeNorm, String fuelTypeRaw, BigDecimal generationGwh, UUID releaseId) {
        this.periodYear = periodYear;
        this.periodQuarter = periodQuarter;
        this.fuelTypeNorm = fuelTypeNorm;
        this.fuelTypeRaw = fuelTypeRaw;
        this.generationGwh = generationGwh;
        this.releaseId = releaseId;
    }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public int getPeriodQuarter() { return periodQuarter; }
    public void setPeriodQuarter(int periodQuarter) { this.periodQuarter = periodQuarter; }

    @JsonProperty("fuelTypeNorm")
    public String getFuelTypeNorm() { return fuelTypeNorm; }
    public void setFuelTypeNorm(String fuelTypeNorm) { this.fuelTypeNorm = fuelTypeNorm; }

    @JsonProperty("fuelTypeRaw")
    public String getFuelTypeRaw() { return fuelTypeRaw; }
    public void setFuelTypeRaw(String fuelTypeRaw) { this.fuelTypeRaw = fuelTypeRaw; }

    public BigDecimal getGenerationGwh() { return generationGwh; }
    public void setGenerationGwh(BigDecimal generationGwh) { this.generationGwh = generationGwh; }

    public UUID getReleaseId() { return releaseId; }
    public void setReleaseId(UUID releaseId) { this.releaseId = releaseId; }
}
