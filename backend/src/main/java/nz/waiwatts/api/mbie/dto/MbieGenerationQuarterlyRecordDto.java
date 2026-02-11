package nz.waiwatts.api.mbie.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class MbieGenerationQuarterlyRecordDto {
    private int periodYear;
    private int periodQuarter;
    private String fuelType; // normalized fuel type (was source)
    private String fuelTypeRaw;
    private BigDecimal generationGwh;
    private UUID releaseId;

    public MbieGenerationQuarterlyRecordDto() {}

    public MbieGenerationQuarterlyRecordDto(int periodYear, int periodQuarter, String fuelType, String fuelTypeRaw, BigDecimal generationGwh, UUID releaseId) {
        this.periodYear = periodYear;
        this.periodQuarter = periodQuarter;
        this.fuelType = fuelType;
        this.fuelTypeRaw = fuelTypeRaw;
        this.generationGwh = generationGwh;
        this.releaseId = releaseId;
    }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public int getPeriodQuarter() { return periodQuarter; }
    public void setPeriodQuarter(int periodQuarter) { this.periodQuarter = periodQuarter; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getFuelTypeRaw() { return fuelTypeRaw; }
    public void setFuelTypeRaw(String fuelTypeRaw) { this.fuelTypeRaw = fuelTypeRaw; }

    public BigDecimal getGenerationGwh() { return generationGwh; }
    public void setGenerationGwh(BigDecimal generationGwh) { this.generationGwh = generationGwh; }

    public UUID getReleaseId() { return releaseId; }
    public void setReleaseId(UUID releaseId) { this.releaseId = releaseId; }
}
