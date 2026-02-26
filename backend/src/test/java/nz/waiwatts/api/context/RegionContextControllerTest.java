package nz.waiwatts.api.context;

import nz.waiwatts.api.context.dto.*;
import nz.waiwatts.service.context.RegionContextAggregationService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegionContextController.class)
class RegionContextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegionContextAggregationService aggregationService;

    @Test
    void getRegionContext_returnsOk_withFactPack() throws Exception {
        RegionContextFactPackDto factPack = getRegionContextFactPackDto();

        when(aggregationService.getRegionContext(any(), any(), any())).thenReturn(factPack);

        mockMvc.perform(get("/api/v1/region-context")
                        .param("region", "Auckland")
                        .param("indicator", "E. coli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionId").value("Auckland"))
                .andExpect(jsonPath("$.water.trend.unitCount").value(10))
                .andExpect(jsonPath("$.water.trend.degradingPct").value(20.0))
                .andExpect(jsonPath("$.water.trend.improvingPct").value(30.0))
                .andExpect(jsonPath("$.water.trend.indeterminatePct").value(40.0))
                .andExpect(jsonPath("$.water.trend.insufficientPct").value(10.0))
                .andExpect(jsonPath("$.water.state.unitCount").value(10))
                .andExpect(jsonPath("$.water.state.bandDistribution.A").value(2))
                .andExpect(jsonPath("$.water.state.bandDistribution.B").value(3))
                .andExpect(jsonPath("$.energy.latestYear").value(2024))
                .andExpect(jsonPath("$.energy.latestRenewablePct").value(82.5))
                .andExpect(jsonPath("$.energy.renewable5YrDeltaPct").value(5.2))
                .andExpect(jsonPath("$.energy.fossilLatestPct").value(17.5));
    }

    private static @NotNull RegionContextFactPackDto getRegionContextFactPackDto() {
        WaterTrendSummaryDto trend = new WaterTrendSummaryDto(10, 20.0, 30.0, 40.0, 10.0);
        WaterStateSummaryDto state = new WaterStateSummaryDto(10, Map.of("A", 2, "B", 3, "C", 3, "D", 1, "E", 1, "INSUFFICIENT", 0));
        EnergySummaryDto energy = new EnergySummaryDto(2024, 82.5, 5.2, 17.5);
        WaterContextDto water = new WaterContextDto(trend, state);

        return new RegionContextFactPackDto(
                "Auckland", Instant.parse("2026-02-14T10:00:00Z"), water, energy);
    }

    @Test
    void getRegionContext_noRegion_returnsAllData() throws Exception {
        WaterTrendSummaryDto trend = new WaterTrendSummaryDto(0, 0.0, 0.0, 0.0, 0.0);
        WaterStateSummaryDto state = new WaterStateSummaryDto(0, Map.of("A", 0, "B", 0, "C", 0, "D", 0, "E", 0, "INSUFFICIENT", 0));
        EnergySummaryDto energy = new EnergySummaryDto(0, 0.0, 0.0, 0.0);
        WaterContextDto water = new WaterContextDto(trend, state);

        RegionContextFactPackDto factPack = new RegionContextFactPackDto(
                "ALL", Instant.now(), water, energy);

        when(aggregationService.getRegionContext(isNull(), isNull(), isNull())).thenReturn(factPack);

        mockMvc.perform(get("/api/v1/region-context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionId").value("ALL"));
    }
}
