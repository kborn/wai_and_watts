package nz.waiwatts.ingestion.transform.lawa;

import nz.waiwatts.ingestion.lawa.LawaStateMultiYearCsvParser;
import nz.waiwatts.ingestion.lawa.LawaStateMultiYearParsedRecord;
import nz.waiwatts.ingestion.lawa.LawaStateMultiYearParser;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LawaStateMultiYearXlsxTransformerTest {

    @Test
    void transform_xlsx_matches_expected_contract_csv() throws IOException {
        byte[] csvBytes = transformer().transform(snapshotStream());
        String actual = new String(csvBytes, StandardCharsets.UTF_8).trim();
        String expected = readExpected("real_snapshots/expected/lawa/water_quality/state/multi_year/lawa_state_multi_year_expected.csv");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void transform_output_parses_with_contract_parser() throws IOException {
        byte[] csvBytes = transformer().transform(snapshotStream());
        LawaStateMultiYearParser parser = new LawaStateMultiYearCsvParser();
        List<LawaStateMultiYearParsedRecord> records = parser.parse(new ByteArrayInputStream(csvBytes));
        assertThat(records).isNotEmpty();
    }

    private LawaStateMultiYearXlsxTransformer transformer() {
        return new LawaStateMultiYearXlsxTransformer();
    }

    private InputStream snapshotStream() throws IOException {
        return new ClassPathResource("real_snapshots/lawa/lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx").getInputStream();
    }

    private String readExpected(String classpath) throws IOException {
        return new String(new ClassPathResource(classpath).getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    }
}
