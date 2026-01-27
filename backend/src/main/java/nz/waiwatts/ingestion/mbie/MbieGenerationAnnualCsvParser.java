package nz.waiwatts.ingestion.mbie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
            if (line == null) return result;
            // expect: period_year,fuel_type_raw,fuel_type_norm,generation_gwh
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                String[] parts = splitCsv(line);
                if (parts.length < 4) {
                    throw new IOException("Invalid CSV at line " + lineNo + ": expected 4 columns, got " + parts.length);
                }
                int year = Integer.parseInt(parts[0].trim());
                String raw = parts[1];
                String norm = parts[2] != null ? parts[2].trim() : "";
                if (norm.isEmpty()) {
                    norm = normalizeFuel(raw);
                }
                norm = mapToKnown(norm);
                BigDecimal gwh = new BigDecimal(parts[3].trim());
                result.add(new MbieGenerationAnnualParsedRecord(year, raw, norm, gwh));
            }
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
}
