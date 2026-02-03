package nz.waiwatts.api.lawa;

import nz.waiwatts.api.lawa.dto.LawaStateMultiYearRecordDto;
import nz.waiwatts.service.lawa.LawaStateMultiYearReadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(LawaStateMultiYearController.class)
public class LawaStateMultiYearControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LawaStateMultiYearReadService readService;

    @Test
    void getGeneration_returnsOk_withPayload() throws Exception {
        UUID relId = UUID.randomUUID();
        List<LawaStateMultiYearRecordDto> payload = List.of(
                new LawaStateMultiYearRecordDto("arc-00001","Cascades LTB","auckland", new BigDecimal("-36.88888973"), new BigDecimal("174.52211474"),"Ammonical nitrogen / Ammonia (toxicity)","AMMONIA_TOXICITY","mg/L","A","EXCELLENT",new BigDecimal("0.0015"), new BigDecimal("0.005"), new BigDecimal("88.135593220339"), new BigDecimal("61.4035087719298"),"HYDRO_5YR_ROLLING",2019,2024, relId)
        );
        when(readService.find(any(), any(), any(), any())).thenReturn(payload);

        mockMvc.perform(get("/api/v1/lawa/water-quality/state/multiyear")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].lawaSiteId").value("arc-00001"))
                .andExpect(jsonPath("$[0].siteName").value("Cascades LTB"))
                .andExpect(jsonPath("$[0].region").value("auckland"))
                .andExpect(jsonPath("$[0].latitude").value(-36.88888973))
                .andExpect(jsonPath("$[0].longitude").value(174.52211474))
                .andExpect(jsonPath("$[0].indicatorRaw").value("Ammonical nitrogen / Ammonia (toxicity)"))
                .andExpect(jsonPath("$[0].indicatorNorm").value("AMMONIA_TOXICITY"))
                .andExpect(jsonPath("$[0].units").value("mg/L"))
                .andExpect(jsonPath("$[0].attributeBand").value("A"))
                .andExpect(jsonPath("$[0].stateNorm").value("EXCELLENT"))
                .andExpect(jsonPath("$[0].median").value(0.0015))
                .andExpect(jsonPath("$[0].p95").value(0.005))
                .andExpect(jsonPath("$[0].recHealthExceed260Pct").value(88.135593220339))
                .andExpect(jsonPath("$[0].recHealthExceed540Pct").value(61.4035087719298))
                .andExpect(jsonPath("$[0].periodType").value("HYDRO_5YR_ROLLING"))
                .andExpect(jsonPath("$[0].periodStartYear").value(2019))
                .andExpect(jsonPath("$[0].periodEndYear").value(2024))
                .andExpect(jsonPath("$[0].releaseId").value(relId.toString()));
    }

    @Test
    void getGeneration_validatesFromTo() throws Exception {
        when(readService.find(anyInt(), anyInt(), anyString(), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/lawa/water-quality/state/multiyear")
                        .param("fromYear", "2025")
                        .param("toYear", "2024")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("fromYear must be <= toYear"));
    }
}
