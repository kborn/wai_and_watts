package nz.waiwatts.ingestion.mbie;

import java.math.BigDecimal;

public class MbieGenerationParsedRecord {
    private final int periodYear;
    private final String fuelTypeRaw;
    private final String fuelTypeNorm;
    private final BigDecimal generationGwh;

    public MbieGenerationParsedRecord(int periodYear, String fuelTypeRaw, String fuelTypeNorm, BigDecimal generationGwh) {
        this.periodYear = periodYear;
        this.fuelTypeRaw = fuelTypeRaw;
        this.fuelTypeNorm = fuelTypeNorm;
        this.generationGwh = generationGwh;
    }

    public int getPeriodYear() {
        return periodYear;
    }

    public String getFuelTypeRaw() {
        return fuelTypeRaw;
    }

    public String getFuelTypeNorm() {
        return fuelTypeNorm;
    }

    public BigDecimal getGenerationGwh() {
        return generationGwh;
    }
}
