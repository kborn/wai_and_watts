package nz.waiwatts.api.datasets;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ExpectedFormat;
import nz.waiwatts.domain.datasets.Publisher;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DatasetCatalogController.class)
class DatasetCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private nz.waiwatts.service.datasets.DatasetCatalogService datasetSourceService;

    @Test
    void listSources_returnsOk() throws Exception {
        DatasetSource src = new DatasetSource();
        UUID srcId = UUID.randomUUID();
        src.setId(srcId);
        src.setName("LAWA water quality");
        src.setPublisher(Publisher.LAWA);
        src.setSourceUrl("https://example.com/lawa.csv");
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("monthly");

        when(datasetSourceService.findAllSources()).thenReturn(List.of(src));

        mockMvc.perform(get("/api/v1/datasets/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(srcId.toString()))
                .andExpect(jsonPath("$[0].name").value("LAWA water quality"))
                .andExpect(jsonPath("$[0].publisher").value("LAWA"))
                .andExpect(jsonPath("$[0].expectedFormat").value("CSV"));
    }

    @Test
    void listReleasesBySource_returnsOk() throws Exception {
        UUID srcId = UUID.randomUUID();
        DatasetSource src = new DatasetSource();
        src.setId(srcId);
        src.setName("LAWA water quality");
        src.setPublisher(Publisher.LAWA);
        src.setSourceUrl("https://example.com/lawa.csv");
        src.setExpectedFormat(ExpectedFormat.CSV);

        DatasetRelease rel = new DatasetRelease();
        UUID relId = UUID.randomUUID();
        rel.setId(relId);
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2024, 1, 1));
        rel.setReleaseLabel("2024-01");
        rel.setRetrievedAt(LocalDateTime.of(2024, 1, 2, 10, 0));
        rel.setContentHash("abc123");
        rel.setStatus(ReleaseStatus.PENDING);

        when(datasetSourceService.findSourceById(srcId)).thenReturn(Optional.of(src));
        when(datasetSourceService.findReleasesBySourceId(srcId)).thenReturn(List.of(rel));

        mockMvc.perform(get("/api/v1/datasets/sources/" + srcId + "/releases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(relId.toString()))
                .andExpect(jsonPath("$[0].datasetSourceId").value(srcId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
}
