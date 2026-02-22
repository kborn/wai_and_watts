package nz.waiwatts.explanations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import nz.waiwatts.explanations.service.IntentParserService;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers(disabledWithoutDocker = true)
class ExplanationPostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DatasetSourceRepository datasetSourceRepository;

    @Autowired
    private DatasetReleaseRepository datasetReleaseRepository;

    @Autowired
    private MbieGenerationAnnualRecordRepository annualRepository;

    @Autowired
    private MbieGenerationQuarterlyRecordRepository quarterlyRepository;

    @Autowired
    private LawaStateMultiYearRecordRepository lawaStateRepository;

    @Autowired
    private LawaTrendMultiYearRecordRepository lawaTrendRepository;

    @MockitoBean
    private IntentParserService intentParserService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        annualRepository.deleteAll();
        quarterlyRepository.deleteAll();
        lawaStateRepository.deleteAll();
        lawaTrendRepository.deleteAll();
        datasetReleaseRepository.deleteAll();
    }

    @Test
    void askEndpoint_mbieDemoRequest_executesAgainstPostgresWithoutSqlErrors() throws Exception {
        DatasetSource source = datasetSource("mbie.generation.annual");
        DatasetRelease release = release(source, "sha256:pg-mbie", LocalDateTime.of(2026, 1, 10, 10, 0));
        annualRecord(release, 2020, "Hydro", "HYDRO", "100");
        annualRecord(release, 2023, "Hydro", "HYDRO", "150");

        IntentParseResponse parse = IntentParseResponse.success(
            new ExplanationRequest(
                "renewable_generation_trend",
                "mbie.generation.annual",
                Map.of("startYear", 2020, "endYear", 2023)
            )
        );
        parse.setParserUsed("TEST");
        when(intentParserService.parseQuestion(anyString())).thenReturn(parse);

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "question", "Explain renewable generation trends between 2020 and 2023"
        ));

        mockMvc.perform(post("/api/v1/explanations/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parsedRequest.questionType").value("renewable_generation_trend"))
            .andExpect(jsonPath("$.selectedDatasetSource").value("mbie.generation.annual"));
    }

    @Test
    void askEndpoint_lawaTrendMixedCaseFilters_executesAgainstPostgresWithoutSqlErrors() throws Exception {
        DatasetSource source = datasetSource("lawa.water_quality.trend.multi_year");
        DatasetRelease release = release(source, "sha256:pg-lawa-trend", LocalDateTime.of(2026, 1, 11, 10, 0));

        LawaTrendMultiYearRecord rec = new LawaTrendMultiYearRecord();
        rec.setDatasetRelease(release);
        rec.setLawaSiteId("trend-site-1");
        rec.setSiteName("Trend Site");
        rec.setRegion("Auckland");
        rec.setIndicatorRaw("E.coli");
        rec.setIndicatorNorm("ECOLI");
        rec.setTrendRaw("Likely improving");
        rec.setTrendNorm("IMPROVING");
        rec.setTrendScore(1);
        rec.setTrendPeriodYears(5);
        rec.setTrendDataFrequency("Monthly");
        rec.setPeriodType("HYDRO_5YR_ROLLING");
        rec.setPeriodStartYear(2019);
        rec.setPeriodEndYear(2024);
        lawaTrendRepository.saveAndFlush(rec);

        IntentParseResponse parse = IntentParseResponse.success(
            new ExplanationRequest(
                "water_quality_trends",
                "lawa.water_quality.trend.multi_year",
                Map.of(
                    "indicator", "eCoLi",
                    "region", "auCKLand",
                    "trend", "imProVing",
                    "startYear", 2019,
                    "endYear", 2024
                )
            )
        );
        parse.setParserUsed("TEST");
        when(intentParserService.parseQuestion(anyString())).thenReturn(parse);

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "question", "Are rivers getting better or worse?"
        ));

        mockMvc.perform(post("/api/v1/explanations/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parsedRequest.questionType").value("water_quality_trends"))
            .andExpect(jsonPath("$.selectedDatasetSource").value("lawa.water_quality.trend.multi_year"));
    }

    @Test
    void structuredEndpoint_lawaStateNullOptionalFilters_executesAgainstPostgresWithoutSqlErrors() throws Exception {
        DatasetSource source = datasetSource("lawa.water_quality.state.multi_year");
        DatasetRelease release = release(source, "sha256:pg-lawa-state", LocalDateTime.of(2026, 1, 12, 10, 0));

        LawaStateMultiYearRecord rec = new LawaStateMultiYearRecord();
        rec.setDatasetRelease(release);
        rec.setLawaSiteId("state-site-1");
        rec.setSiteName("State Site");
        rec.setRegion("Auckland");
        rec.setIndicatorRaw("E.coli");
        rec.setIndicatorNorm("ECOLI");
        rec.setAttributeBand("A");
        rec.setStateNorm("EXCELLENT");
        rec.setUnits("#/100mL");
        rec.setMedian(new BigDecimal("42"));
        rec.setPeriodType("HYDRO_5YR_ROLLING");
        rec.setPeriodStartYear(2019);
        rec.setPeriodEndYear(2024);
        lawaStateRepository.saveAndFlush(rec);

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "questionType", "water_quality_overview",
            "datasetSource", "lawa.water_quality.state.multi_year",
            "filters", Map.of()
        ));

        mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRefusal").value(false));
    }

    private DatasetSource datasetSource(String code) {
        return datasetSourceRepository.findByCode(code).orElseThrow(() ->
            new IllegalStateException("Missing dataset source code in baseline data: " + code)
        );
    }

    private DatasetRelease release(DatasetSource source, String contentHash, LocalDateTime importedAt) {
        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(source);
        rel.setPublishedDate(LocalDate.of(2026, 1, 1));
        rel.setReleaseLabel(contentHash);
        rel.setRetrievedAt(importedAt);
        rel.setImportedAt(importedAt);
        rel.setContentHash(contentHash);
        rel.setStatus(ReleaseStatus.IMPORTED);
        return datasetReleaseRepository.saveAndFlush(rel);
    }

    private void annualRecord(
        DatasetRelease release,
        int year,
        String fuelTypeRaw,
        String fuelTypeNorm,
        String generationGwh
    ) {
        MbieGenerationAnnualRecord rec = new MbieGenerationAnnualRecord();
        rec.setDatasetRelease(release);
        rec.setPeriodYear(year);
        rec.setFuelTypeRaw(fuelTypeRaw);
        rec.setFuelTypeNorm(fuelTypeNorm);
        rec.setGenerationGwh(new BigDecimal(generationGwh));
        annualRepository.saveAndFlush(rec);
    }
}
