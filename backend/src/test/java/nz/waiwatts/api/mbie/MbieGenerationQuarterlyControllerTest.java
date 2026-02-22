package nz.waiwatts.api.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationQuarterlyRecordDto;
import nz.waiwatts.service.mbie.MbieGenerationQuarterlyReadService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MbieGenerationQuarterlyController.class)
class MbieGenerationQuarterlyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MbieGenerationQuarterlyReadService readService;

    @Test
    void getQuarterly_returnsOk_withPayload() throws Exception {
        UUID relId = UUID.randomUUID();
        List<MbieGenerationQuarterlyRecordDto> payload = List.of(
                new MbieGenerationQuarterlyRecordDto(2024, 3, "WIND", "Wind", new BigDecimal("980.1"), relId)
        );
        when(readService.find(any(), any(), any(), any())).thenReturn(payload);

        mockMvc.perform(get("/api/v1/mbie/generation/quarterly")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].periodYear").value(2024))
                .andExpect(jsonPath("$[0].periodQuarter").value(3))
                .andExpect(jsonPath("$[0].fuelType").value("WIND"))
                .andExpect(jsonPath("$[0].fuelTypeRaw").value("Wind"))
                .andExpect(jsonPath("$[0].generationGwh").value(980.1))
                .andExpect(jsonPath("$[0].releaseId").value(relId.toString()));
    }

    @Test
    void getQuarterly_validatesParams() throws Exception {
        mockMvc.perform(get("/api/v1/mbie/generation/quarterly")
                        .param("fromYear", "2025")
                        .param("toYear", "2024")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("fromYear must be <= toYear"));

        mockMvc.perform(get("/api/v1/mbie/generation/quarterly")
                        .param("quarter", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("quarter must be between 1 and 4"));
    }
}
