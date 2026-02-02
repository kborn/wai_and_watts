package nz.waiwatts.ingestion.lawa;

import org.springframework.stereotype.Component;

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

@Component
public class LawaStateMultiYearCsvParser implements LawaStateMultiYearParser {

    @Override
    public List<LawaStateMultiYearParsedRecord> parse(InputStream input) throws IOException {
        List<LawaStateMultiYearParsedRecord> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            // FIXME - is there a better way to parse csv in java?
            String line = reader.readLine(); // header
            if (line == null) return result;
            // strict header validation per design/007
            String header = stripBom(line);
            if (!EXPECTED_HEADER.equals(header)) {
                throw new IOException("Invalid CSV header. Expected: '" + EXPECTED_HEADER + "' but was: '" + header + "'");
            }
            // expect header with 17 columns as per design/007
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                String[] parts = splitCsv(line);
                if (parts.length < 17) {
                    throw new IOException("Invalid CSV at line " + lineNo + ": expected 17 columns, got " + parts.length);
                }
                String lawaSiteId = parts[0].trim();
                String siteName = parts[1].trim();
                String region = parts[2].trim();
                BigDecimal latitude = parseBigDecimal(parts[3]);
                BigDecimal longitude = parseBigDecimal(parts[4]);
                String indicatorRaw = parts[5].trim();
                String indicatorNorm = normalizeIndicator(indicatorRaw, parts[6]);
                String units = parts[7].trim();
                String attributeBand = parts[8].trim();
                String stateNorm = normalizeState(attributeBand, parts[9]);
                BigDecimal median = parseBigDecimal(parts[10]);
                BigDecimal p95 = parseBigDecimal(parts[11]);
                BigDecimal recHealth260 = parseBigDecimal(parts[12]);
                BigDecimal recHealth540 = parseBigDecimal(parts[13]);
                String periodType = parts[14].trim();
                int periodStartYear = Integer.parseInt(parts[15].trim());
                int periodEndYear = Integer.parseInt(parts[16].trim());

                result.add(new LawaStateMultiYearParsedRecord(lawaSiteId, siteName, region, latitude, longitude, indicatorRaw, indicatorNorm, units, attributeBand, stateNorm, median, p95, recHealth260, recHealth540, periodType, periodStartYear, periodEndYear));
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

    private static String normalizeIndicator(String indicatorRaw, String indicatorNormFromFile) {
        // If the CSV already provides a normalized value, trust it (trim only)
        if (isNotBlank(indicatorNormFromFile)) {
            return indicatorNormFromFile.trim();
        }

        String raw = indicatorRaw == null ? "" : collapseWhitespace(indicatorRaw.trim());

        // Mapping from design/007-lawa-state-multi-year-schema.md
        String mapped = INDICATOR_MAP.get(raw);
        return mapped != null ? mapped : "OTHER";
    }

    private static String normalizeState(String attributeBand, String stateNormFromFile) {
        // If the CSV already provides normalized state, trust it (trim only)
        if (isNotBlank(stateNormFromFile)) {
            return stateNormFromFile.trim();
        }

        String band = attributeBand == null ? "" : attributeBand.trim().toUpperCase();
        return switch (band) {
            case "A" -> "EXCELLENT";
            case "B" -> "GOOD";
            case "C" -> "FAIR";
            case "D" -> "POOR";
            case "E" -> "VERY_POOR";
            default -> "UNKNOWN"; // fail-safe default to satisfy NOT NULL contract; unknown bands should be surfaced upstream
        };
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String collapseWhitespace(String s) {
        return s.replaceAll("\\s+", " ");
    }

    private static BigDecimal parseBigDecimal(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return new BigDecimal(t);
    }

    private static final Map<String, String> INDICATOR_MAP = createIndicatorMap();

    private static final String EXPECTED_HEADER =
            "lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year,period_end_year";

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static Map<String, String> createIndicatorMap() {
        Map<String, String> m = new HashMap<>();
        // Recommended initial mappings per design/007-lawa-state-multi-year-schema.md
        m.put("E.coli", "ECOLI");
        m.put("Clarity / Suspended fine sediment", "CLARITY");
        m.put("Dissolved reactive phosphorus", "DRP");
        m.put("NO3N", "NO3N");
        m.put("TON", "TON");
        m.put("Ammonical nitrogen / Ammonia (toxicity)", "AMMONIA_TOXICITY");
        m.put("Nitrate nitrogen / Nitrate (toxicity)", "NITRATE_TOXICITY");
        return m;
    }

}