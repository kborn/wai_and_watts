package nz.waiwatts.ingestion.mbie;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MbieGenerationAnnualCsvParserTest {

    @Test
    void parse_fixture_success_and_basic_assertions() throws Exception {
        InputStream is = this.getClass().getClassLoader()
                .getResourceAsStream("fixtures/mbie/generation/annual/mbie_generation_annual_fixture_phase6.csv");
        assertThat(is).as("fixture should be on classpath").isNotNull();

        MbieGenerationAnnualParser parser = new MbieGenerationAnnualCsvParser();
        List<MbieGenerationAnnualParsedRecord> records = parser.parse(is);

        // exact count: years 2003-2024 inclusive (22) * 8 fuel types = 176
        assertThat(records).hasSize(176);

        // basic field checks
        Set<String> allowed = Set.of("HYDRO", "GEOTHERMAL", "WIND", "SOLAR", "GAS", "COAL", "OTHER");
        for (MbieGenerationAnnualParsedRecord r : records) {
            assertThat(r.getFuelTypeRaw()).isNotNull();
            assertThat(r.getFuelTypeNorm()).isNotNull();
            assertThat(allowed).contains(r.getFuelTypeNorm());
            assertThat(r.getPeriodYear()).isBetween(2003, 2024);
            assertThat(r.getGenerationGwh()).isNotNull();
        }

        // spot checks: a couple of known lines
        assertThat(records).anySatisfy(r -> {
            if (r.getPeriodYear() == 2003 && r.getFuelTypeNorm().equals("HYDRO")) {
                assertThat(r.getGenerationGwh()).isEqualByComparingTo(new BigDecimal("23388.8"));
            }
        });

        assertThat(records).anySatisfy(r -> {
            if (r.getPeriodYear() == 2024 && r.getFuelTypeNorm().equals("WIND")) {
                assertThat(r.getGenerationGwh()).isEqualByComparingTo(new BigDecimal("3918.6"));
            }
        });
    }
}
