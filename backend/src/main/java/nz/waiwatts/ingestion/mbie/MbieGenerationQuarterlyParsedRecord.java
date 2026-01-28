package nz.waiwatts.ingestion.mbie;

import java.math.BigDecimal;

public class MbieGenerationQuarterlyParsedRecord {
    private final int periodYear;
    private final int periodQuarter;
    private final String fuelTypeRaw;
    private final String fuelTypeNorm;
    private final BigDecimal generationGwh;

    public MbieGenerationQuarterlyParsedRecord(int periodYear, int periodQuarter, String fuelTypeRaw, String fuelTypeNorm, BigDecimal generationGwh) {
        this.periodYear = periodYear;
        this.periodQuarter = periodQuarter;
        this.fuelTypeRaw = fuelTypeRaw;
        this.fuelTypeNorm = fuelTypeNorm;
        this.generationGwh = generationGwh;
    }

    public int getPeriodYear() { return periodYear; }
    public int getPeriodQuarter() { return periodQuarter; }
    public String getFuelTypeRaw() { return fuelTypeRaw; }
    public String getFuelTypeNorm() { return fuelTypeNorm; }
    public BigDecimal getGenerationGwh() { return generationGwh; }
}
