package nz.waiwatts.ingestion.mbie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class MbieGenerationAnnualCsvParser implements MbieGenerationAnnualParser {

    private static final Set<String> KNOWN = Set.of("HYDRO", "GEOTHERMAL", "WIND", "SOLAR", "GAS", "COAL", "OTHER");

    @Override
    public List<MbieGenerationAnnualParsedRecord> parse(InputStream input) throws IOException {
        List<MbieGenerationAnnualParsedRecord> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            if (line == null) {
                throw new IOException("CSV file is empty");
            }
            Map<String, Integer> headerIndex = parseHeader(line, REQUIRED_COLUMNS);
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                String[] parts = splitCsv(line);
                if (isRowBlank(parts)) {
                    continue;
                }
                int year = Integer.parseInt(getRequired(parts, headerIndex, "period_year", lineNo));
                String raw = getRequired(parts, headerIndex, "fuel_type_raw", lineNo);
                String norm = getOptional(parts, headerIndex, "fuel_type_norm");
                if (norm.isEmpty()) {
                    norm = normalizeFuel(raw);
                }
                norm = mapToKnown(norm);
                BigDecimal gwh = new BigDecimal(getRequired(parts, headerIndex, "generation_gwh", lineNo));
                result.add(new MbieGenerationAnnualParsedRecord(year, raw, norm, gwh));
            }
        }
        if (result.isEmpty()) {
            throw new IOException("CSV contains header but no data rows");
        }
        return result;
    }

    private static String[] splitCsv(String line) {
        // Simple split, fixture has no embedded commas/quotes
        String[] parts = line.split(",", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private static Map<String, Integer> parseHeader(String line, List<String> required) throws IOException {
        String[] headerParts = splitCsv(line);
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

    private static String getRequired(String[] parts, Map<String, Integer> index, String column, int lineNo) throws IOException {
        Integer idx = index.get(column);
        if (idx == null || idx >= parts.length) {
            throw new IOException("Invalid CSV at line " + lineNo + ": missing column '" + column + "'");
        }
        return parts[idx].trim();
    }

    private static String getOptional(String[] parts, Map<String, Integer> index, String column) {
        Integer idx = index.get(column);
        if (idx == null || idx >= parts.length) {
            return "";
        }
        return parts[idx] == null ? "" : parts[idx].trim();
    }

    private static boolean isRowBlank(String[] parts) {
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase();
    }

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    static String normalizeFuel(String raw) {
        if (raw == null) return "OTHER";
        String s = raw.trim();
        // Normalize hyphen variations: replace any non-alphanumeric with space, then collapse
        s = s.replace('\u2011', '-') // non-breaking hyphen
             .replace('\u2013', '-') // en dash
             .replace('\u2014', '-') // em dash
             .replace('-', ' ')
             .replace('/', ' ');
        // collapse whitespace
        s = s.replaceAll("\\s+", " ").trim().toUpperCase();
        // common synonyms
        if (s.contains("GEO")) return "GEOTHERMAL";
        if (s.contains("SOLAR")) return "SOLAR";
        if (s.contains("WIND")) return "WIND";
        if (s.contains("HYDRO")) return "HYDRO";
        if (s.contains("GAS")) return "GAS";
        if (s.contains("COAL")) return "COAL";
        return "OTHER";
    }

    static String mapToKnown(String token) {
        String t = token == null ? "" : token.trim().toUpperCase();
        if (KNOWN.contains(t)) return t;
        // try normalize
        return normalizeFuel(token);
    }

    private static final List<String> REQUIRED_COLUMNS = List.of(
            "period_year",
            "fuel_type_raw",
            "fuel_type_norm",
            "generation_gwh"
    );
}
