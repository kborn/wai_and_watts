package nz.waiwatts.ingestion.lawa;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LawaTrendMultiYearCsvParserTest {

    @Test
    void parse_fixture_success_and_basic_assertions() throws Exception {
        InputStream is = this.getClass().getClassLoader()
                .getResourceAsStream("fixtures/lawa/water_quality/trend/multi_year/lawa_trend_multi_year_fixture.csv");
        assertThat(is).as("fixture should be on classpath").isNotNull();

        LawaTrendMultiYearCsvParser parser = new LawaTrendMultiYearCsvParser();
        List<LawaTrendMultiYearParsedRecord> records = parser.parse(is);

        assertThat(records).as("fixture should yield some records").isNotEmpty();

        Set<String> allowedIndicators = Set.of("ECOLI", "CLARITY", "DRP", "NITRATE_N", "TOTAL_N", "AMMONIACAL_N", "OTHER");
        Set<String> allowedTrends = Set.of("IMPROVING", "DEGRADING", "NO_CHANGE", "INSUFFICIENT_DATA");

        for (LawaTrendMultiYearParsedRecord r : records) {
            assertThat(r.lawaSiteId()).isNotNull();
            assertThat(r.siteName()).isNotNull();
            assertThat(r.region()).isNotNull();
            assertThat(r.indicatorRaw()).isNotNull();
            assertThat(r.indicatorNorm()).isNotNull();
            assertThat(allowedIndicators).contains(r.indicatorNorm());
            assertThat(r.trendRaw()).isNotNull();
            assertThat(r.trendNorm()).isNotNull();
            assertThat(allowedTrends).contains(r.trendNorm());
            assertThat(r.trendScore()).isNotNull();
            assertThat(r.trendPeriodYears()).isNotNull();
            assertThat(r.periodType()).isEqualTo("HYDRO_NYR_WINDOW");
            assertThat(r.periodEndYear()).isEqualTo(2024);
            assertThat(r.periodStartYear()).isLessThanOrEqualTo(r.periodEndYear());
        }
    }

    @Test
    void parse_missingRequiredColumn_fails() {
        String csv = """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,trend_raw,trend_norm,trend_score,trend_period_years,trend_data_frequency,period_type,period_start_year
            arc-1,Site,region,-36.1,174.1,E.coli,ECOLI,Improving,IMPROVING,10,10,Annual,HYDRO_NYR_WINDOW,2014
            """;
        LawaTrendMultiYearCsvParser parser = new LawaTrendMultiYearCsvParser();
        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Missing required columns")
                .hasMessageContaining("period_end_year");
    }

    @Test
    void parse_extraColumn_passes() throws Exception {
        String csv = """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,trend_raw,trend_norm,trend_score,trend_period_years,trend_data_frequency,period_type,period_start_year,period_end_year,extra_col
            arc-1,Site,region,-36.1,174.1,E.coli,ECOLI,Improving,IMPROVING,10,10,Annual,HYDRO_NYR_WINDOW,2014,2024,ignored
            """;
        LawaTrendMultiYearCsvParser parser = new LawaTrendMultiYearCsvParser();
        List<LawaTrendMultiYearParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().trendNorm()).isEqualTo("IMPROVING");
    }

    @Test
    void parse_reorderedColumns_passes() throws Exception {
        String csv = """
            region,lawa_site_id,site_name,longitude,latitude,indicator_raw,indicator_norm,trend_raw,trend_norm,trend_score,trend_period_years,trend_data_frequency,period_type,period_start_year,period_end_year
            region,arc-1,Site,174.1,-36.1,E.coli,ECOLI,Improving,IMPROVING,10,10,Annual,HYDRO_NYR_WINDOW,2014,2024
            """;
        LawaTrendMultiYearCsvParser parser = new LawaTrendMultiYearCsvParser();
        List<LawaTrendMultiYearParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().region()).isEqualTo("region");
    }

    @Test
    void parse_bomHeader_passes() throws Exception {
        String csv = """
            \uFEFFlawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,trend_raw,trend_norm,trend_score,trend_period_years,trend_data_frequency,period_type,period_start_year,period_end_year
            arc-1,Site,region,-36.1,174.1,E.coli,ECOLI,Improving,IMPROVING,10,10,Annual,HYDRO_NYR_WINDOW,2014,2024
            """;
        LawaTrendMultiYearCsvParser parser = new LawaTrendMultiYearCsvParser();
        List<LawaTrendMultiYearParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
    }

    @Test
    void parse_blankRows_skipped() throws Exception {
        String csv = """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,trend_raw,trend_norm,trend_score,trend_period_years,trend_data_frequency,period_type,period_start_year,period_end_year

               \s
            arc-1,Site,region,-36.1,174.1,E.coli,ECOLI,Improving,IMPROVING,10,10,Annual,HYDRO_NYR_WINDOW,2014,2024
            """;
        LawaTrendMultiYearCsvParser parser = new LawaTrendMultiYearCsvParser();
        List<LawaTrendMultiYearParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
    }

    @Test
    void parse_contractWithoutUnitsColumn_passes() throws Exception {
        String csv = """
            lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,trend_raw,trend_norm,trend_score,trend_period_years,trend_data_frequency,period_type,period_start_year,period_end_year
            arc-1,Site,region,-36.1,174.1,E.coli,ECOLI,Improving,IMPROVING,10,10,Annual,HYDRO_NYR_WINDOW,2014,2024
            """;
        LawaTrendMultiYearCsvParser parser = new LawaTrendMultiYearCsvParser();
        List<LawaTrendMultiYearParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().indicatorNorm()).isEqualTo("ECOLI");
    }

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
