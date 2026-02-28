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
            assertThat(r.fuelTypeRaw()).isNotNull();
            assertThat(r.fuelTypeNorm()).isNotNull();
            assertThat(allowed).contains(r.fuelTypeNorm());
            assertThat(r.periodYear()).isBetween(2022, 2025);
            assertThat(r.periodQuarter()).isBetween(1, 4);
            assertThat(r.generationGwh()).isNotNull();
        }

        // Spot check: first Hydro row 2022-Q4 should be present (7176.9)
        boolean found = records.stream().anyMatch(r ->
                r.periodYear() == 2022 && r.periodQuarter() == 4 &&
                "HYDRO".equals(r.fuelTypeNorm()) &&
                "Hydro".equals(r.fuelTypeRaw()) &&
                r.generationGwh().compareTo(new BigDecimal("7176.9")) == 0
        );
        assertThat(found).isTrue();
    }

    @Test
    void parse_missingRequiredColumn_fails() {
        String csv = """
            period_year,fuel_type_raw,fuel_type_norm,generation_gwh
            2022,Hydro,HYDRO,26071
            """;
        MbieGenerationQuarterlyParser parser = new MbieGenerationQuarterlyCsvParser();
        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Missing required columns")
                .hasMessageContaining("period_quarter");
    }

    @Test
    void parse_extraColumn_passes() throws Exception {
        String csv = """
            period_year,period_quarter,fuel_type_raw,fuel_type_norm,generation_gwh,extra_col
            2022,4,Hydro,HYDRO,7176.9,ignored
            """;
        MbieGenerationQuarterlyParser parser = new MbieGenerationQuarterlyCsvParser();
        List<MbieGenerationQuarterlyParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().periodQuarter()).isEqualTo(4);
    }

    @Test
    void parse_reorderedColumns_passes() throws Exception {
        String csv = """
            fuel_type_raw,period_quarter,generation_gwh,period_year,fuel_type_norm
            Hydro,4,7176.9,2022,HYDRO
            """;
        MbieGenerationQuarterlyParser parser = new MbieGenerationQuarterlyCsvParser();
        List<MbieGenerationQuarterlyParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().periodYear()).isEqualTo(2022);
    }

    @Test
    void parse_bomHeader_passes() throws Exception {
        String csv = """
            \uFEFFperiod_year,period_quarter,fuel_type_raw,fuel_type_norm,generation_gwh
            2022,4,Hydro,HYDRO,7176.9
            """;
        MbieGenerationQuarterlyParser parser = new MbieGenerationQuarterlyCsvParser();
        List<MbieGenerationQuarterlyParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
    }

    @Test
    void parse_blankRows_skipped() throws Exception {
        String csv = """
            period_year,period_quarter,fuel_type_raw,fuel_type_norm,generation_gwh

               \s
            2022,4,Hydro,HYDRO,7176.9
            """;
        MbieGenerationQuarterlyParser parser = new MbieGenerationQuarterlyCsvParser();
        List<MbieGenerationQuarterlyParsedRecord> records = parser.parse(toStream(csv));
        assertThat(records).hasSize(1);
    }

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
