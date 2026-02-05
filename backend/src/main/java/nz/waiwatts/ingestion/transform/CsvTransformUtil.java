package nz.waiwatts.ingestion.transform;

import java.util.ArrayList;
import java.util.List;

public final class CsvTransformUtil {

    private CsvTransformUtil() {
    }

    public static String toCsvLine(List<String> values) {
        List<String> escaped = new ArrayList<>(values.size());
        for (String value : values) {
            escaped.add(escape(value));
        }
        return String.join(",", escaped);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String cleaned = value.replace("\"", "\"\"");
        if (needsQuotes) {
            return "\"" + cleaned + "\"";
        }
        return cleaned;
    }
}
