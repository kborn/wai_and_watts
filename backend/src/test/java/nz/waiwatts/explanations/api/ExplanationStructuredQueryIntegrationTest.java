package nz.waiwatts.explanations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.domain.datasets.ExpectedFormat;
import nz.waiwatts.domain.datasets.Publisher;
import nz.waiwatts.domain.datasets.ReleaseStatus;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class ExplanationStructuredQueryIntegrationTest {

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

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        ensureBaselineDatasetSources();
        annualRepository.deleteAll();
        quarterlyRepository.deleteAll();
        lawaStateRepository.deleteAll();
        lawaTrendRepository.deleteAll();
        datasetReleaseRepository.deleteAll();
    }

    @Test
    void mbieGenerationMixOverview_nullOptionalFilters_executesQueryPath() throws Exception {
        DatasetSource source = datasetSource("mbie.generation.annual");
        DatasetRelease release = release(source, "sha256:mbie-mix", LocalDateTime.of(2026, 1, 10, 10, 0));

        annualRecord(release, 2023, "Hydro", "HYDRO", "1000");
        annualRecord(release, 2023, "Geothermal", "GEOTHERMAL", "600");

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "questionType", "generation_mix_overview",
            "datasetSource", "mbie.generation.annual",
            "filters", Map.of()
        ));

        mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRefusal").value(false))
            .andExpect(jsonPath("$.citations").isArray());
    }

    @Test
    void mbieHydroTrend_mixedCaseFuelFilter_executesCaseInsensitiveQueryPath_withoutSqlError() throws Exception {
        DatasetSource source = datasetSource("mbie.generation.annual");
        DatasetRelease release = release(source, "sha256:mbie-hydro", LocalDateTime.of(2026, 1, 11, 10, 0));

        annualRecord(release, 2020, "Hydro", "HYDRO", "120");
        annualRecord(release, 2023, "Hydro", "HYDRO", "180");

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "questionType", "fuel_generation_trend",
            "datasetSource", "mbie.generation.annual",
            "filters", Map.of("fuelType", "HyDrO", "startYear", 2020, "endYear", 2023)
        ));

        mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRefusal").value(true))
            .andExpect(jsonPath("$.refusalReason").exists());
    }

    @Test
    void lawaStateOverview_nullOptionalFilters_executesQueryPath() throws Exception {
        DatasetSource source = datasetSource("lawa.water_quality.state.multi_year");
        DatasetRelease release = release(source, "sha256:lawa-state", LocalDateTime.of(2026, 1, 12, 10, 0));

        LawaStateMultiYearRecord rec = getLawaStateMultiYearRecord(release);
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
            .andExpect(jsonPath("$.isRefusal").value(false))
            .andExpect(jsonPath("$.citations").isArray());
    }

    private static @NotNull LawaStateMultiYearRecord getLawaStateMultiYearRecord(DatasetRelease release) {
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
        return rec;
    }

    @Test
    void lawaTrendQuestion_mixedCaseOptionalFilters_executesFindForAskPath() throws Exception {
        DatasetSource source = datasetSource("lawa.water_quality.trend.multi_year");
        DatasetRelease release = release(source, "sha256:lawa-trend", LocalDateTime.of(2026, 1, 13, 10, 0));

        LawaTrendMultiYearRecord rec = getLawaTrendMultiYearRecord(release);
        lawaTrendRepository.saveAndFlush(rec);

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "questionType", "water_quality_trends",
            "datasetSource", "lawa.water_quality.trend.multi_year",
            "filters", Map.of(
                "indicator", "eCoLi",
                "region", "auCKLand",
                "trend", "imProVing",
                "startYear", 2019,
                "endYear", 2024
            )
        ));

        mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRefusal").value(false))
            .andExpect(jsonPath("$.citations").isArray());
    }

    private static @NotNull LawaTrendMultiYearRecord getLawaTrendMultiYearRecord(DatasetRelease release) {
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
        return rec;
    }

    @Test
    void mbieDeterminism_multipleReleases_pinsToCanonicalReleaseStably() throws Exception {
        DatasetSource source = datasetSource("mbie.generation.annual");

        DatasetRelease older = release(source, "sha256:mbie-old", LocalDateTime.of(2026, 1, 1, 10, 0));
        annualRecord(older, 2020, "Hydro", "HYDRO", "100");
        annualRecord(older, 2023, "Hydro", "HYDRO", "150");

        DatasetRelease newer = release(source, "sha256:mbie-new", LocalDateTime.of(2026, 2, 1, 10, 0));
        annualRecord(newer, 2020, "Hydro", "HYDRO", "200");
        annualRecord(newer, 2023, "Hydro", "HYDRO", "300");

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "questionType", "renewable_generation_trend",
            "datasetSource", "mbie.generation.annual",
            "filters", Map.of("startYear", 2020, "endYear", 2023)
        ));

        MvcResult firstResult = mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRefusal").value(false))
            .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/v1/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRefusal").value(false))
            .andReturn();

        JsonNode first = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        JsonNode second = objectMapper.readTree(secondResult.getResponse().getContentAsString());

        assertThat(first.path("citations")).isEqualTo(second.path("citations"));
        assertThat(first.path("explanationText").asText()).isEqualTo(second.path("explanationText").asText());
        assertThat(first.path("explanationText").asText()).contains("200");
        assertThat(first.path("explanationText").asText()).contains("300");
        assertThat(first.path("explanationText").asText()).doesNotContain("100");
        assertThat(first.path("explanationText").asText()).doesNotContain("150");
    }

    @Test
    void factPackProvenance_multipleReleases_pinsToCanonicalReleaseIdStably() throws Exception {
        DatasetSource source = datasetSource("mbie.generation.annual");

        DatasetRelease older = release(source, "sha256:mbie-old-prov", LocalDateTime.of(2026, 1, 1, 10, 0));
        annualRecord(older, 2020, "Hydro", "HYDRO", "100");
        annualRecord(older, 2023, "Hydro", "HYDRO", "150");

        DatasetRelease newer = release(source, "sha256:mbie-new-prov", LocalDateTime.of(2026, 2, 1, 10, 0));
        annualRecord(newer, 2020, "Hydro", "HYDRO", "200");
        annualRecord(newer, 2023, "Hydro", "HYDRO", "300");

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "questionType", "renewable_generation_trend",
            "datasetSource", "mbie.generation.annual",
            "filters", Map.of("startYear", 2020, "endYear", 2023)
        ));

        MvcResult firstResult = mockMvc.perform(post("/api/v1/explanations/fact-pack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/v1/explanations/fact-pack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode first = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        JsonNode second = objectMapper.readTree(secondResult.getResponse().getContentAsString());

        String firstReleaseId = first.at("/provenance/datasetSources/0/datasetReleaseId").asText();
        String secondReleaseId = second.at("/provenance/datasetSources/0/datasetReleaseId").asText();

        assertThat(firstReleaseId).isEqualTo(newer.getId().toString());
        assertThat(secondReleaseId).isEqualTo(newer.getId().toString());
        assertThat(firstReleaseId).isEqualTo(secondReleaseId);
    }

    private DatasetSource datasetSource(String code) {
        return datasetSourceRepository.findByCode(code).orElseThrow(() ->
            new IllegalStateException("Missing dataset source code in baseline data: " + code)
        );
    }

    private void ensureBaselineDatasetSources() {
        ensureDatasetSource(
            "mbie.generation.annual",
            "MBIE Electricity Generation (Fuel Type, Annual)",
            Publisher.MBIE,
            "https://example.test/mbie/annual.xlsx",
            ExpectedFormat.XLSX,
            "quarterly"
        );
        ensureDatasetSource(
            "mbie.generation.quarterly",
            "MBIE Electricity Generation (Fuel Type, Quarterly)",
            Publisher.MBIE,
            "https://example.test/mbie/quarterly.xlsx",
            ExpectedFormat.XLSX,
            "quarterly"
        );
        ensureDatasetSource(
            "lawa.water_quality.state.multi_year",
            "LAWA River Water Quality State (Multi-Year)",
            Publisher.LAWA,
            "https://example.test/lawa/state.xlsx",
            ExpectedFormat.XLSX,
            "annual"
        );
        ensureDatasetSource(
            "lawa.water_quality.trend.multi_year",
            "LAWA River Water Quality Trend (Multi-Year)",
            Publisher.LAWA,
            "https://example.test/lawa/trend.xlsx",
            ExpectedFormat.XLSX,
            "annual"
        );
    }

    private void ensureDatasetSource(
        String code,
        String name,
        Publisher publisher,
        String sourceUrl,
        ExpectedFormat expectedFormat,
        String updateCadence
    ) {
        if (datasetSourceRepository.findByCode(code).isPresent()) {
            return;
        }
        DatasetSource source = new DatasetSource();
        source.setCode(code);
        source.setName(name);
        source.setPublisher(publisher);
        source.setSourceUrl(sourceUrl);
        source.setExpectedFormat(expectedFormat);
        source.setUpdateCadence(updateCadence);
        datasetSourceRepository.saveAndFlush(source);
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
