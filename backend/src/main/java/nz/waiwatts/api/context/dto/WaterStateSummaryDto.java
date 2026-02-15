package nz.waiwatts.api.context.dto;

import java.util.Map;

public class WaterStateSummaryDto {
    private int unitCount;
    private Map<String, Integer> bandDistribution;

    public WaterStateSummaryDto() {}

    public WaterStateSummaryDto(int unitCount, Map<String, Integer> bandDistribution) {
        this.unitCount = unitCount;
        this.bandDistribution = bandDistribution;
    }

    public int getUnitCount() { return unitCount; }
    public void setUnitCount(int unitCount) { this.unitCount = unitCount; }
    public Map<String, Integer> getBandDistribution() { return bandDistribution; }
    public void setBandDistribution(Map<String, Integer> bandDistribution) { this.bandDistribution = bandDistribution; }
}
