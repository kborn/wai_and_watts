package nz.waiwatts.api.context.dto;

public class EnergySummaryDto {
    private int latestYear;
    private double latestRenewablePct;
    private double renewable5YrDeltaPct;
    private double fossilLatestPct;

    public EnergySummaryDto() {}

    public EnergySummaryDto(int latestYear, double latestRenewablePct, 
                            double renewable5YrDeltaPct, double fossilLatestPct) {
        this.latestYear = latestYear;
        this.latestRenewablePct = latestRenewablePct;
        this.renewable5YrDeltaPct = renewable5YrDeltaPct;
        this.fossilLatestPct = fossilLatestPct;
    }

    public int getLatestYear() { return latestYear; }
    public void setLatestYear(int latestYear) { this.latestYear = latestYear; }
    public double getLatestRenewablePct() { return latestRenewablePct; }
    public void setLatestRenewablePct(double latestRenewablePct) { this.latestRenewablePct = latestRenewablePct; }
    public double getRenewable5YrDeltaPct() { return renewable5YrDeltaPct; }
    public void setRenewable5YrDeltaPct(double renewable5YrDeltaPct) { this.renewable5YrDeltaPct = renewable5YrDeltaPct; }
    public double getFossilLatestPct() { return fossilLatestPct; }
    public void setFossilLatestPct(double fossilLatestPct) { this.fossilLatestPct = fossilLatestPct; }
}
