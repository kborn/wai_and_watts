package nz.waiwatts.ingestion.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractCsvParser {

    protected abstract String[] splitCsvLine(String line);

    protected Map<String, Integer> parseHeader(String line, List<String> required) throws IOException {
        String[] headerParts = splitCsvLine(line);
        if (headerParts.length == 0) {
            throw new IOException("Missing CSV header");
        }
        headerParts[0] = stripBom(headerParts[0]);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headerParts.length; i++) {
            String key = normalizeHeader(headerParts[i]);
            if (!key.isEmpty() && !index.containsKey(key)) {
                index.put(key, i);
            }
        }
        List<String> missing = new ArrayList<>();
        for (String col : required) {
            if (!index.containsKey(col)) {
                missing.add(col);
            }
        }
        if (!missing.isEmpty()) {
            throw new IOException("Missing required columns: " + String.join(", ", missing));
        }
        return index;
    }

    protected String getRequired(String[] parts, Map<String, Integer> index, String column, int lineNo) throws IOException {
        Integer idx = index.get(column);
        if (idx == null || idx >= parts.length) {
            throw new IOException("Invalid CSV at line " + lineNo + ": missing column '" + column + "'");
        }
        return parts[idx].trim();
    }

    protected String getOptional(String[] parts, Map<String, Integer> index, String column) {
        Integer idx = index.get(column);
        if (idx == null || idx >= parts.length) {
            return "";
        }
        return parts[idx] == null ? "" : parts[idx].trim();
    }

    protected boolean isRowBlank(String[] parts) {
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    protected BigDecimal parseBigDecimal(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return new BigDecimal(t);
    }

    protected Integer parseInteger(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return Integer.parseInt(t);
    }

    protected String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase();
    }

    protected String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }
}
