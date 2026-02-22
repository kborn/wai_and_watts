package nz.waiwatts.api.lawa;

import nz.waiwatts.api.lawa.dto.LawaTrendMultiYearRecordDto;
import nz.waiwatts.service.lawa.LawaTrendMultiYearReadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LawaTrendMultiYearController.class)
public class LawaTrendMultiYearControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LawaTrendMultiYearReadService readService;

    @Test
    void getTrend_returnsOk_withPayload() throws Exception {
        UUID relId = UUID.randomUUID();
        List<LawaTrendMultiYearRecordDto> payload = List.of(
                new LawaTrendMultiYearRecordDto("arc-00036","Avondale @ Shadbolt","auckland","avondale",
                        new BigDecimal("-36.9232796"), new BigDecimal("174.69177898"),
                        "E.coli","ECOLI",
                        "Improving","IMPROVING", 3, 5, "monthly",
                        "HYDRO_NYR_WINDOW", 2020, 2024, relId)
        );
        when(readService.find(any(), any(), any(), any())).thenReturn(payload);

        mockMvc.perform(get("/api/v1/lawa/water-quality/trend/multiyear").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].lawaSiteId").value("arc-00036"))
                .andExpect(jsonPath("$[0].siteName").value("Avondale @ Shadbolt"))
                .andExpect(jsonPath("$[0].region").value("auckland"))
                .andExpect(jsonPath("$[0].catchment").value("avondale"))
                .andExpect(jsonPath("$[0].latitude").value(-36.9232796))
                .andExpect(jsonPath("$[0].longitude").value(174.69177898))
                .andExpect(jsonPath("$[0].indicatorRaw").value("E.coli"))
                .andExpect(jsonPath("$[0].indicatorNorm").value("ECOLI"))
                .andExpect(jsonPath("$[0].trendRaw").value("Improving"))
                .andExpect(jsonPath("$[0].trendNorm").value("IMPROVING"))
                .andExpect(jsonPath("$[0].trendScore").value(3))
                .andExpect(jsonPath("$[0].trendPeriodYears").value(5))
                .andExpect(jsonPath("$[0].trendDataFrequency").value("monthly"))
                .andExpect(jsonPath("$[0].periodType").value("HYDRO_NYR_WINDOW"))
                .andExpect(jsonPath("$[0].periodStartYear").value(2020))
                .andExpect(jsonPath("$[0].periodEndYear").value(2024))
                .andExpect(jsonPath("$[0].releaseId").value(relId.toString()));
    }

    @Test
    void getTrend_validatesFromTo() throws Exception {
        when(readService.find(anyInt(), anyInt(), anyString(), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/lawa/water-quality/trend/multiyear")
                        .param("fromYear", "2025")
                        .param("toYear", "2024")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("fromYear must be <= toYear"));
    }
}
