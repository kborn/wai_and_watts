package nz.waiwatts.api.context.dto;

import java.time.Instant;

public class RegionContextFactPackDto {
    private String regionId;
    private Instant generatedAt;
    private WaterContextDto water;
    private EnergySummaryDto energy;

    public RegionContextFactPackDto() {}

    public RegionContextFactPackDto(String regionId, Instant generatedAt, 
                                    WaterContextDto water, EnergySummaryDto energy) {
        this.regionId = regionId;
        this.generatedAt = generatedAt;
        this.water = water;
        this.energy = energy;
    }

    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public WaterContextDto getWater() { return water; }
    public void setWater(WaterContextDto water) { this.water = water; }
    public EnergySummaryDto getEnergy() { return energy; }
    public void setEnergy(EnergySummaryDto energy) { this.energy = energy; }
}
