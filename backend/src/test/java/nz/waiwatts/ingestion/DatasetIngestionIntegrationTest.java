package nz.waiwatts.ingestion;

import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ExpectedFormat;
import nz.waiwatts.domain.datasets.Publisher;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatasetIngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatasetSourceRepository sourceRepository;

    @Autowired
    private DatasetReleaseRepository releaseRepository;

    @Test
    void ingest_createsRelease_and_isIdempotent() throws Exception {
        // Ensure clean state in the in-memory DB for this test
        releaseRepository.deleteAll();
        sourceRepository.deleteAll();

        // Seed a dataset source
        DatasetSource src = new DatasetSource();
        src.setId(UUID.randomUUID());
        src.setName("LAWA water quality");
        src.setPublisher(Publisher.LAWA);
        src.setCode("lawa");
        // Use a unique URL per test run to avoid clashes with any existing data
        String uniqueUrl = "https://example.com/lawa-" + UUID.randomUUID() + ".csv";
        src.setSourceUrl(uniqueUrl);
        src.setExpectedFormat(ExpectedFormat.CSV);
        sourceRepository.save(src);

        String body = String.format("{\n" +
                "  \"datasetSourceCode\": \"%s\",\n" +
                "  \"releaseLabel\": \"2024-01\",\n" +
                "  \"publishedDate\": \"2024-01-01\",\n" +
                "  \"sourceUri\": \"%s\",\n" +
                "  \"contentHash\": \"abc123\"\n" +
                "}", "lawa", uniqueUrl);

        // First call creates the release
        mockMvc.perform(post("/api/v1/internal/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Token", "dev-token")
                        .content(body))
                .andExpect(status().isOk());

        long afterFirst = releaseRepository.count();
        assertThat(afterFirst).isEqualTo(1);

        // Second call with same contentHash is a no-op (idempotent)
        mockMvc.perform(post("/api/v1/internal/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Token", "dev-token")
                        .content(body))
                .andExpect(status().isOk());

        long afterSecond = releaseRepository.count();
        assertThat(afterSecond).isEqualTo(1);
    }
}
