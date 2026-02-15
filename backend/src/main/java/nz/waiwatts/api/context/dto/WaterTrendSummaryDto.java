package nz.waiwatts.api.context.dto;

public class WaterTrendSummaryDto {
    private int unitCount;
    private double degradingPct;
    private double improvingPct;
    private double indeterminatePct;
    private double insufficientPct;

    public WaterTrendSummaryDto() {}

    public WaterTrendSummaryDto(int unitCount, double degradingPct, double improvingPct, 
                                double indeterminatePct, double insufficientPct) {
        this.unitCount = unitCount;
        this.degradingPct = degradingPct;
        this.improvingPct = improvingPct;
        this.indeterminatePct = indeterminatePct;
        this.insufficientPct = insufficientPct;
    }

    public int getUnitCount() { return unitCount; }
    public void setUnitCount(int unitCount) { this.unitCount = unitCount; }
    public double getDegradingPct() { return degradingPct; }
    public void setDegradingPct(double degradingPct) { this.degradingPct = degradingPct; }
    public double getImprovingPct() { return improvingPct; }
    public void setImprovingPct(double improvingPct) { this.improvingPct = improvingPct; }
    public double getIndeterminatePct() { return indeterminatePct; }
    public void setIndeterminatePct(double indeterminatePct) { this.indeterminatePct = indeterminatePct; }
    public double getInsufficientPct() { return insufficientPct; }
    public void setInsufficientPct(double insufficientPct) { this.insufficientPct = insufficientPct; }
}
