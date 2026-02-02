package nz.waiwatts.ingestion.lawa;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LawaStateMultiYearCsvParserTest {

    @Test
    void parse_fixture_success_and_basic_assertions() throws Exception {
        InputStream is = this.getClass().getClassLoader()
                .getResourceAsStream("fixtures/lawa/state/multi_year/lawa_state_multi_year_fixture.csv");
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
}
