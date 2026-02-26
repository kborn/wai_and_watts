package nz.waiwatts.explanations.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Tag("contract")
class AskLatencyTrendContractTest {

    private static final int TOTAL_RUNS = 25;
    private static final int WARMUP_RUNS = 5;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void reportAskEndpointP95LatencyForSeededTestProfile_nonBlockingBudgetTrend() throws Exception {
        String requestBody = """
            {
                "question": "Explain renewable generation trends between 2020 and 2023"
            }
            """;

        List<Double> samplesMs = new ArrayList<>();
        for (int i = 0; i < TOTAL_RUNS; i++) {
            long start = System.nanoTime();
            mockMvc.perform(post("/api/v1/explanations/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk());
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            if (i >= WARMUP_RUNS) {
                samplesMs.add(elapsedMs);
            }
        }

        Collections.sort(samplesMs);
        int p95Index = (int) Math.ceil(0.95 * samplesMs.size()) - 1;
        p95Index = Math.max(0, Math.min(p95Index, samplesMs.size() - 1));
        double p95Ms = samplesMs.get(p95Index);

        System.out.printf("ASK_LATENCY_P95_MS=%.2f samples=%d warmup=%d%n", p95Ms, samplesMs.size(), WARMUP_RUNS);
        assertTrue(p95Ms >= 0.0, "P95 latency must be non-negative");
    }
}
