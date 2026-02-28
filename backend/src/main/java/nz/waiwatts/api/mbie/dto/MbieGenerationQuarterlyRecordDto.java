package nz.waiwatts.api.mbie.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MbieGenerationQuarterlyRecordDto(
    int periodYear,
    int periodQuarter,
    String fuelType,
    String fuelTypeRaw,
    BigDecimal generationGwh,
    UUID releaseId
) {
    // Compatibility getters for existing call sites.
    public int getPeriodYear() { return periodYear; }
    public int getPeriodQuarter() { return periodQuarter; }
    public String getFuelType() { return fuelType; }
    public String getFuelTypeRaw() { return fuelTypeRaw; }
    public BigDecimal getGenerationGwh() { return generationGwh; }
    public UUID getReleaseId() { return releaseId; }
}
