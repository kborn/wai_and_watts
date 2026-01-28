package nz.waiwatts.ingestion.mbie;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class MbieGenerationQuarterlyCsvParser implements MbieGenerationQuarterlyParser {

    private static final Set<String> KNOWN = Set.of("HYDRO", "GEOTHERMAL", "WIND", "SOLAR", "GAS", "COAL", "OTHER");

    @Override
    public List<MbieGenerationQuarterlyParsedRecord> parse(InputStream input) throws IOException {
        List<MbieGenerationQuarterlyParsedRecord> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            if (line == null) return result;
            // expect: period_year,period_quarter,fuel_type_raw,fuel_type_norm,generation_gwh
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                String[] parts = splitCsv(line);
                if (parts.length < 5) {
                    throw new IOException("Invalid CSV at line " + lineNo + ": expected 5 columns, got " + parts.length);
                }
                int year = Integer.parseInt(parts[0].trim());
                int quarter = Integer.parseInt(parts[1].trim());
                if (quarter < 1 || quarter > 4) {
                    throw new IOException("Invalid quarter at line " + lineNo + ": " + quarter);
                }
                String raw = parts[2];
                String norm = parts[3] != null ? parts[3].trim() : "";
                if (norm.isEmpty()) {
                    norm = normalizeFuel(raw);
                }
                norm = mapToKnown(norm);
                BigDecimal gwh = new BigDecimal(parts[4].trim());
                result.add(new MbieGenerationQuarterlyParsedRecord(year, quarter, raw, norm, gwh));
            }
        }
        return result;
    }

    private static String[] splitCsv(String line) {
        String[] parts = line.split(",", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    static String normalizeFuel(String raw) {
        if (raw == null) return "OTHER";
        String s = raw.trim();
        s = s.replace('\u2011', '-')
             .replace('\u2013', '-')
             .replace('\u2014', '-')
             .replace('-', ' ')
             .replace('/', ' ');
        s = s.replaceAll("\\s+", " ").trim().toUpperCase();
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
        return normalizeFuel(token);
    }
}
