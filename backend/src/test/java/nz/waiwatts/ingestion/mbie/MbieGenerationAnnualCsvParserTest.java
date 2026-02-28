package nz.waiwatts.ingestion.mbie;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThat(r.fuelTypeRaw()).isNotNull();
            assertThat(r.fuelTypeNorm()).isNotNull();
            assertThat(allowed).contains(r.fuelTypeNorm());
            assertThat(r.periodYear()).isBetween(2003, 2024);
            assertThat(r.generationGwh()).isNotNull();
        }

        // spot checks: a couple of known lines
        assertThat(records).anySatisfy(r -> {
            if (r.periodYear() == 2003 && r.fuelTypeNorm().equals("HYDRO")) {
                assertThat(r.generationGwh()).isEqualByComparingTo(new BigDecimal("23388.8"));
            }
        });

        assertThat(records).anySatisfy(r -> {
            if (r.periodYear() == 2024 && r.fuelTypeNorm().equals("WIND")) {
                assertThat(r.generationGwh()).isEqualByComparingTo(new BigDecimal("3918.6"));
            }
        });
    }

    @Test
    void parse_missingRequiredColumn_fails() {
        String csv = """
            period_year,fuel_type_raw,generation_gwh
            2022,Hydro,26071
            """;
        MbieGenerationAnnualParser parser = new MbieGenerationAnnualCsvParser();
        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Missing required columns")
                .hasMessageContaining("fuel_type_norm");
    }

    @Test
    void parse_extraColumn_passes() throws Exception {
        String csv = """
            period_year,fuel_type_raw,fuel_type_norm,generation_gwh,extra_col
            2022,Hydro,HYDRO,26071,ignored
            """;
        MbieGenerationAnnualParser parser = new MbieGenerationAnnualCsvParser();
        List<MbieGenerationAnnualParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().fuelTypeNorm()).isEqualTo("HYDRO");
    }

    @Test
    void parse_reorderedColumns_passes() throws Exception {
        String csv = """
            fuel_type_raw,generation_gwh,period_year,fuel_type_norm
            Hydro,26071,2022,HYDRO
            """;
        MbieGenerationAnnualParser parser = new MbieGenerationAnnualCsvParser();
        List<MbieGenerationAnnualParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().periodYear()).isEqualTo(2022);
    }

    @Test
    void parse_bomHeader_passes() throws Exception {
        String csv = """
            \uFEFFperiod_year,fuel_type_raw,fuel_type_norm,generation_gwh
            2022,Hydro,HYDRO,26071
            """;
        MbieGenerationAnnualParser parser = new MbieGenerationAnnualCsvParser();
        List<MbieGenerationAnnualParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
    }

    @Test
    void parse_blankRows_skipped() throws Exception {
        String csv = """
            period_year,fuel_type_raw,fuel_type_norm,generation_gwh

               \s
            2022,Hydro,HYDRO,26071
            """;
        MbieGenerationAnnualParser parser = new MbieGenerationAnnualCsvParser();
        List<MbieGenerationAnnualParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
    }

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
