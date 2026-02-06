package nz.waiwatts.ingestion.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CsvParserTest {

    @Test
    void parseLine_simpleFields() {
        String[] result = CsvParser.parseLine("field1,field2,field3");
        assertThat(result).containsExactly("field1", "field2", "field3");
    }

    @Test
    void parseLine_quotedFields() {
        String[] result = CsvParser.parseLine("field1,\"field2,with,commas\",field3");
        assertThat(result).containsExactly("field1", "field2,with,commas", "field3");
    }

    @Test
    void parseLine_quotedFieldsWithEscapedQuotes() {
        String[] result = CsvParser.parseLine("field1,\"field2 with \"\"quotes\"\"\",field3");
        assertThat(result).containsExactly("field1", "field2 with \"quotes\"", "field3");
    }

    @Test
    void parseLine_emptyFields() {
        String[] result = CsvParser.parseLine("field1,,field3");
        assertThat(result).containsExactly("field1", "", "field3");
    }

    @Test
    void parseLine_allEmptyFields() {
        String[] result = CsvParser.parseLine(",,");
        assertThat(result).containsExactly("", "", "");
    }

    @Test
    void parseLine_trimmed() {
        String[] result = CsvParser.parseLineTrimmed(" field1 , \"field2,with,commas\" , field3 ");
        assertThat(result).containsExactly("field1", "field2,with,commas", "field3");
    }

    @Test
    void parseLine_realWorldExample() {
        String line = "R001,\"Bush Stream Rangitata Gorge Road, at bridge\",Canterbury,-43.123,171.456,E.coli,ECOLI,MPN/100ml,C,FAIR,2.5,5.0,10.2,3.1,HYDRO_NYR_WINDOW,2018,2023";
        String[] result = CsvParser.parseLineTrimmed(line);
        assertThat(result[1]).isEqualTo("Bush Stream Rangitata Gorge Road, at bridge");
        assertThat(result).hasSize(17);
    }

    @Test
    void parseLine_malformedQuotes() {
        assertThatThrownBy(() -> CsvParser.parseLine("field1,\"unclosed field,field2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Malformed CSV line: unclosed quotes");
    }

    @Test
    void parseLine_nullInput() {
        String[] result = CsvParser.parseLine(null);
        assertThat(result).isEmpty();
    }

    @Test
    void parseLine_emptyInput() {
        String[] result = CsvParser.parseLine("");
        assertThat(result).containsExactly("");
    }
}