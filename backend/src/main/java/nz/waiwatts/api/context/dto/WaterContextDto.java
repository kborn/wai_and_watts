package nz.waiwatts.api.context.dto;

public class WaterContextDto {
    private WaterTrendSummaryDto trend;
    private WaterStateSummaryDto state;

    public WaterContextDto() {}

    public WaterContextDto(WaterTrendSummaryDto trend, WaterStateSummaryDto state) {
        this.trend = trend;
        this.state = state;
    }

    public WaterTrendSummaryDto getTrend() { return trend; }
    public void setTrend(WaterTrendSummaryDto trend) { this.trend = trend; }
    public WaterStateSummaryDto getState() { return state; }
    public void setState(WaterStateSummaryDto state) { this.state = state; }
}
