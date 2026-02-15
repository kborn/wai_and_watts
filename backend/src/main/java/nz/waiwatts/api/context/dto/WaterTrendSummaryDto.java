package nz.waiwatts.api.context.dto;

import java.util.Map;

public class WaterTrendSummaryDto {
    private int totalSites;
    private double degradingPct;
    private double improvingPct;
    private double indeterminatePct;
    private double insufficientPct;

    public WaterTrendSummaryDto() {}

    public WaterTrendSummaryDto(int totalSites, double degradingPct, double improvingPct, 
                                double indeterminatePct, double insufficientPct) {
        this.totalSites = totalSites;
        this.degradingPct = degradingPct;
        this.improvingPct = improvingPct;
        this.indeterminatePct = indeterminatePct;
        this.insufficientPct = insufficientPct;
    }

    public int getTotalSites() { return totalSites; }
    public void setTotalSites(int totalSites) { this.totalSites = totalSites; }
    public double getDegradingPct() { return degradingPct; }
    public void setDegradingPct(double degradingPct) { this.degradingPct = degradingPct; }
    public double getImprovingPct() { return improvingPct; }
    public void setImprovingPct(double improvingPct) { this.improvingPct = improvingPct; }
    public double getIndeterminatePct() { return indeterminatePct; }
    public void setIndeterminatePct(double indeterminatePct) { this.indeterminatePct = indeterminatePct; }
    public double getInsufficientPct() { return insufficientPct; }
    public void setInsufficientPct(double insufficientPct) { this.insufficientPct = insufficientPct; }
}
