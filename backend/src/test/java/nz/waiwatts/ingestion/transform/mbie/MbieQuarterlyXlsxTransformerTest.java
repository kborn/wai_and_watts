package nz.waiwatts.ingestion.transform.mbie;

import nz.waiwatts.ingestion.mbie.MbieGenerationQuarterlyCsvParser;
import nz.waiwatts.ingestion.mbie.MbieGenerationQuarterlyParsedRecord;
import nz.waiwatts.ingestion.mbie.MbieGenerationQuarterlyParser;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MbieQuarterlyXlsxTransformerTest {

    @Test
    void transform_xlsx_matches_expected_contract_csv() throws IOException {
        byte[] csvBytes = transformer().transform(snapshotStream());
        String actual = new String(csvBytes, StandardCharsets.UTF_8).trim();
        String expected = readExpected("real_snapshots/expected/mbie/generation/quarterly/mbie_generation_quarterly_expected.csv");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void transform_output_parses_with_contract_parser() throws IOException {
        byte[] csvBytes = transformer().transform(snapshotStream());
        MbieGenerationQuarterlyParser parser = new MbieGenerationQuarterlyCsvParser();
        List<MbieGenerationQuarterlyParsedRecord> records = parser.parse(new ByteArrayInputStream(csvBytes));
        assertThat(records).isNotEmpty();
    }

    private MbieQuarterlyXlsxTransformer transformer() {
        return new MbieQuarterlyXlsxTransformer();
    }

    private InputStream snapshotStream() throws IOException {
        return new ClassPathResource("real_snapshots/mbie/electricity-sept-2025-q3.xlsx").getInputStream();
    }

    private String readExpected(String classpath) throws IOException {
        return new String(new ClassPathResource(classpath).getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    }
}
