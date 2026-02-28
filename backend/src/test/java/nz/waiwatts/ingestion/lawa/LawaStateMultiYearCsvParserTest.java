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
            assertThat(r.lawaSiteId()).isNotNull();
            assertThat(r.siteName()).isNotNull();
            assertThat(r.region()).isNotNull();
            assertThat(r.indicatorRaw()).isNotNull();
            assertThat(r.indicatorNorm()).isNotNull();
            assertThat(allowedIndicators).contains(r.indicatorNorm());
            assertThat(r.attributeBand()).isNotNull();
            assertThat(r.stateNorm()).isNotNull();
            assertThat(allowedStates).contains(r.stateNorm());
            assertThat(r.periodType()).isEqualTo("HYDRO_5YR_ROLLING");
            assertThat(r.periodStartYear()).isBetween(1950,2050);
            assertThat(r.periodEndYear()).isBetween(1950,2050);
        }

        // Spot check: verify all fields for first data row in fixture (line 2)
        LawaStateMultiYearParsedRecord first = records.stream()
                .filter(r -> r.lawaSiteId().equals("arc-00001") && r.indicatorNorm().equals("AMMONIA_TOXICITY"))
                .findFirst()
                .orElse(null);
        assertThat(first).as("first fixture row should be present").isNotNull();
        assertThat(first.lawaSiteId()).isEqualTo("arc-00001");
        assertThat(first.siteName()).isEqualTo("Cascades LTB");
        assertThat(first.region()).isEqualTo("auckland");
        assertThat(first.latitude()).isEqualByComparingTo(new BigDecimal("-36.88888973"));
        assertThat(first.longitude()).isEqualByComparingTo(new BigDecimal("174.52211474"));
        assertThat(first.indicatorRaw()).isEqualTo("Ammonical nitrogen / Ammonia (toxicity)");
        assertThat(first.indicatorNorm()).isEqualTo("AMMONIA_TOXICITY");
        assertThat(first.units()).isEqualTo("mg/L");
        assertThat(first.attributeBand()).isEqualTo("A");
        assertThat(first.stateNorm()).isEqualTo("EXCELLENT");
        assertThat(first.median()).isEqualByComparingTo(new BigDecimal("0.0015"));
        assertThat(first.p95()).isEqualByComparingTo(new BigDecimal("0.005"));
        assertThat(first.recHealthExceed260Pct()).isNull();
        assertThat(first.recHealthExceed540Pct()).isNull();
        assertThat(first.periodType()).isEqualTo("HYDRO_5YR_ROLLING");
        assertThat(first.periodStartYear()).isEqualTo(2019);
        assertThat(first.periodEndYear()).isEqualTo(2024);
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
        assertThat(records.getFirst().lawaSiteId()).isEqualTo("arc-1");
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
        assertThat(records.getFirst().region()).isEqualTo("region");
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
