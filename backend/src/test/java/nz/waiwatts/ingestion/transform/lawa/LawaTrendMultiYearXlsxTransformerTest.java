package nz.waiwatts.ingestion.transform.lawa;

import nz.waiwatts.ingestion.lawa.LawaTrendMultiYearCsvParser;
import nz.waiwatts.ingestion.lawa.LawaTrendMultiYearParsedRecord;
import nz.waiwatts.ingestion.lawa.LawaTrendMultiYearParser;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LawaTrendMultiYearXlsxTransformerTest {

    @Test
    void transform_xlsx_matches_expected_contract_csv() throws IOException {
        byte[] csvBytes = transformer().transform(snapshotStream());
        String actual = new String(csvBytes, StandardCharsets.UTF_8).trim();
        String expected = readExpected("real_snapshots/expected/lawa/water_quality/trend/multi_year/lawa_trend_multi_year_expected.csv");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void transform_output_parses_with_contract_parser() throws IOException {
        byte[] csvBytes = transformer().transform(snapshotStream());
        LawaTrendMultiYearParser parser = new LawaTrendMultiYearCsvParser();
        List<LawaTrendMultiYearParsedRecord> records = parser.parse(new ByteArrayInputStream(csvBytes));
        assertThat(records).isNotEmpty();
    }

    private LawaTrendMultiYearXlsxTransformer transformer() {
        return new LawaTrendMultiYearXlsxTransformer();
    }

    private InputStream snapshotStream() throws IOException {
        return new ClassPathResource("real_snapshots/lawa/lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx").getInputStream();
    }

    private String readExpected(String classpath) throws IOException {
        return new String(new ClassPathResource(classpath).getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    }
}
