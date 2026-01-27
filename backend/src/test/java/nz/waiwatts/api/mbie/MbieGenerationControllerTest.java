package nz.waiwatts.api.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationRecordDto;
import nz.waiwatts.service.mbie.MbieGenerationReadService;
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

@WebMvcTest(MbieGenerationController.class)
class MbieGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MbieGenerationReadService readService;

    @Test
    void getGeneration_returnsOk_withPayload() throws Exception {
        UUID relId = UUID.randomUUID();
        List<MbieGenerationRecordDto> payload = List.of(
                new MbieGenerationRecordDto(2024, "WIND", "Wind", new BigDecimal("3918.6"), relId)
        );
        when(readService.find(any(), any(), any())).thenReturn(payload);

        mockMvc.perform(get("/api/v1/mbie/generation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].periodYear").value(2024))
                .andExpect(jsonPath("$[0].source").value("WIND"))
                .andExpect(jsonPath("$[0].sourceRaw").value("Wind"))
                .andExpect(jsonPath("$[0].generationGwh").value(3918.6))
                .andExpect(jsonPath("$[0].releaseId").value(relId.toString()));
    }

    @Test
    void getGeneration_validatesFromTo() throws Exception {
        when(readService.find(anyInt(), anyInt(), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/mbie/generation")
                        .param("fromYear", "2025")
                        .param("toYear", "2024")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("fromYear must be <= toYear"));
    }
}
