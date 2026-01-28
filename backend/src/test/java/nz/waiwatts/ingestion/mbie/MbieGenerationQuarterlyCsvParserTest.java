package nz.waiwatts.ingestion.mbie;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MbieGenerationQuarterlyCsvParserTest {

    @Test
    void parse_fixture_success_and_basic_assertions() throws Exception {
        InputStream is = this.getClass().getClassLoader()
                .getResourceAsStream("fixtures/mbie/generation/quarterly/mbie_generation_quarterly_fixture_phase7.csv");
        assertThat(is).as("fixture should be on classpath").isNotNull();

        MbieGenerationQuarterlyParser parser = new MbieGenerationQuarterlyCsvParser();
        List<MbieGenerationQuarterlyParsedRecord> records = parser.parse(is);

        // Expect 108 data rows (9 fuels * 12 quarters covered)
        assertThat(records).hasSize(108);

        Set<String> allowed = Set.of("HYDRO", "GEOTHERMAL", "WIND", "SOLAR", "GAS", "COAL", "OTHER");
        for (MbieGenerationQuarterlyParsedRecord r : records) {
            assertThat(r.getFuelTypeRaw()).isNotNull();
            assertThat(r.getFuelTypeNorm()).isNotNull();
            assertThat(allowed).contains(r.getFuelTypeNorm());
            assertThat(r.getPeriodYear()).isBetween(2022, 2025);
            assertThat(r.getPeriodQuarter()).isBetween(1, 4);
            assertThat(r.getGenerationGwh()).isNotNull();
        }

        // Spot check: first Hydro row 2022-Q4 should be present (7176.9)
        boolean found = records.stream().anyMatch(r ->
                r.getPeriodYear() == 2022 && r.getPeriodQuarter() == 4 &&
                "HYDRO".equals(r.getFuelTypeNorm()) &&
                "Hydro".equals(r.getFuelTypeRaw()) &&
                r.getGenerationGwh().compareTo(new BigDecimal("7176.9")) == 0
        );
        assertThat(found).isTrue();
    }
}
