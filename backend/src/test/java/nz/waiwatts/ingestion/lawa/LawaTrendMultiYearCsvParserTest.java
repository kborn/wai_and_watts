package nz.waiwatts.ingestion.lawa;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
            assertThat(r.getLawaSiteId()).isNotNull();
            assertThat(r.getSiteName()).isNotNull();
            assertThat(r.getRegion()).isNotNull();
            assertThat(r.getIndicatorRaw()).isNotNull();
            assertThat(r.getIndicatorNorm()).isNotNull();
            assertThat(allowedIndicators).contains(r.getIndicatorNorm());
            assertThat(r.getTrendRaw()).isNotNull();
            assertThat(r.getTrendNorm()).isNotNull();
            assertThat(allowedTrends).contains(r.getTrendNorm());
            assertThat(r.getTrendScore()).isNotNull();
            assertThat(r.getTrendPeriodYears()).isNotNull();
            assertThat(r.getPeriodType()).isEqualTo("HYDRO_NYR_WINDOW");
            assertThat(r.getPeriodEndYear()).isEqualTo(2024);
            assertThat(r.getPeriodStartYear()).isLessThanOrEqualTo(r.getPeriodEndYear());
        }
    }
}
