package nz.waiwatts.ingestion.lawa;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LawaStateMultiYearCsvParserTest {

    @Test
    void parse_fixture_success_and_basic_assertions() throws Exception {
        InputStream is = this.getClass().getClassLoader()
                .getResourceAsStream("fixtures/lawa/water_quality/state/multi_year/lawa_state_multi_year_fixture.csv");
        assertThat(is).as("fixture should be on classpath").isNotNull();

        LawaStateMultiYearCsvParser parser = new LawaStateMultiYearCsvParser();
        List<LawaStateMultiYearParsedRecord> records = parser.parse(is);

        // Expect 108 data rows (9 fuels * 12 quarters covered)
        assertThat(records).hasSize(37);

        Set<String> allowedIndicators = Set.of("ECOLI", "CLARITY", "DRP", "NO3N", "TON", "AMMONIA_TOXICITY", "NITRATE_TOXICITY");
        Set<String> allowedStates = Set.of("EXCELLENT", "GOOD", "FAIR", "POOR", "VERY_POOR");

        for (LawaStateMultiYearParsedRecord r : records) {
            assertThat(r.getLawaSiteId()).isNotNull();
            assertThat(r.getSiteName()).isNotNull();
            assertThat(r.getRegion()).isNotNull();
            assertThat(r.getIndicatorRaw()).isNotNull();
            assertThat(r.getIndicatorNorm()).isNotNull();
            assertThat(allowedIndicators).contains(r.getIndicatorNorm());
            assertThat(r.getAttributeBand()).isNotNull();
            assertThat(r.getStateNorm()).isNotNull();
            assertThat(allowedStates).contains(r.getStateNorm());
            assertThat(r.getPeriodType()).isEqualTo("HYDRO_5YR_ROLLING");
            assertThat(r.getPeriodStartYear()).isBetween(1950,2050);
            assertThat(r.getPeriodEndYear()).isBetween(1950,2050);
        }

        // Spot check: verify all fields for first data row in fixture (line 2)
        LawaStateMultiYearParsedRecord first = records.stream()
                .filter(r -> r.getLawaSiteId().equals("arc-00001") && r.getIndicatorNorm().equals("AMMONIA_TOXICITY"))
                .findFirst()
                .orElse(null);
        assertThat(first).as("first fixture row should be present").isNotNull();
        assertThat(first.getLawaSiteId()).isEqualTo("arc-00001");
        assertThat(first.getSiteName()).isEqualTo("Cascades LTB");
        assertThat(first.getRegion()).isEqualTo("auckland");
        assertThat(first.getLatitude()).isEqualByComparingTo(new BigDecimal("-36.88888973"));
        assertThat(first.getLongitude()).isEqualByComparingTo(new BigDecimal("174.52211474"));
        assertThat(first.getIndicatorRaw()).isEqualTo("Ammonical nitrogen / Ammonia (toxicity)");
        assertThat(first.getIndicatorNorm()).isEqualTo("AMMONIA_TOXICITY");
        assertThat(first.getUnits()).isEqualTo("mg/L");
        assertThat(first.getAttributeBand()).isEqualTo("A");
        assertThat(first.getStateNorm()).isEqualTo("EXCELLENT");
        assertThat(first.getMedian()).isEqualByComparingTo(new BigDecimal("0.0015"));
        assertThat(first.getP95()).isEqualByComparingTo(new BigDecimal("0.005"));
        assertThat(first.getRecHealthExceed260Pct()).isNull();
        assertThat(first.getRecHealthExceed540Pct()).isNull();
        assertThat(first.getPeriodType()).isEqualTo("HYDRO_5YR_ROLLING");
        assertThat(first.getPeriodStartYear()).isEqualTo(2019);
        assertThat(first.getPeriodEndYear()).isEqualTo(2024);
    }

    @Test
    void parse_missingRequiredColumn_fails() {
        String csv = """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year
            arc-1,Site,region,-36.1,174.1,E.coli,ECOLI,mg/L,A,EXCELLENT,1.1,2.2,,,HYDRO_5YR_ROLLING,2019
            """;
        LawaStateMultiYearCsvParser parser = new LawaStateMultiYearCsvParser();
        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Missing required columns")
                .hasMessageContaining("period_end_year");
    }

    @Test
    void parse_extraColumn_passes() throws Exception {
        String csv = """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year,period_end_year,extra_col
            arc-1,Site,region,-36.1,174.1,E.coli,ECOLI,mg/L,A,EXCELLENT,1.1,2.2,,,HYDRO_5YR_ROLLING,2019,2024,ignored
            """;
        LawaStateMultiYearCsvParser parser = new LawaStateMultiYearCsvParser();
        List<LawaStateMultiYearParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getLawaSiteId()).isEqualTo("arc-1");
    }

    @Test
    void parse_reorderedColumns_passes() throws Exception {
        String csv = """
            region,lawa_site_id,site_name,longitude,latitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year,period_end_year
            region,arc-1,Site,174.1,-36.1,E.coli,ECOLI,mg/L,A,EXCELLENT,1.1,2.2,,,HYDRO_5YR_ROLLING,2019,2024
            """;
        LawaStateMultiYearCsvParser parser = new LawaStateMultiYearCsvParser();
        List<LawaStateMultiYearParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getRegion()).isEqualTo("region");
    }

    @Test
    void parse_bomHeader_passes() throws Exception {
        String csv = """
            \uFEFFlawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year,period_end_year
            arc-1,Site,region,-36.1,174.1,E.coli,ECOLI,mg/L,A,EXCELLENT,1.1,2.2,,,HYDRO_5YR_ROLLING,2019,2024
            """;
        LawaStateMultiYearCsvParser parser = new LawaStateMultiYearCsvParser();
        List<LawaStateMultiYearParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
    }

    @Test
    void parse_blankRows_skipped() throws Exception {
        String csv = """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year,period_end_year

               \s
            arc-1,Site,region,-36.1,174.1,E.coli,ECOLI,mg/L,A,EXCELLENT,1.1,2.2,,,HYDRO_5YR_ROLLING,2019,2024
            """;
        LawaStateMultiYearCsvParser parser = new LawaStateMultiYearCsvParser();
        List<LawaStateMultiYearParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
    }

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
