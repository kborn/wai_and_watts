package nz.waiwatts.api.context.dto;

import java.util.Map;

public class WaterStateSummaryDto {
    private int totalSites;
    private Map<String, Integer> bandDistribution;

    public WaterStateSummaryDto() {}

    public WaterStateSummaryDto(int totalSites, Map<String, Integer> bandDistribution) {
        this.totalSites = totalSites;
        this.bandDistribution = bandDistribution;
    }

    public int getTotalSites() { return totalSites; }
    public void setTotalSites(int totalSites) { this.totalSites = totalSites; }
    public Map<String, Integer> getBandDistribution() { return bandDistribution; }
    public void setBandDistribution(Map<String, Integer> bandDistribution) { this.bandDistribution = bandDistribution; }
}
