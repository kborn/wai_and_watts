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
public class LawaTrendMultiYearCsvParser implements LawaTrendMultiYearParser {

    @Override
    public List<LawaTrendMultiYearParsedRecord> parse(InputStream input) throws IOException {
        List<LawaTrendMultiYearParsedRecord> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            if (line == null) return result;
            String header = stripBom(line);
            if (!EXPECTED_HEADER.equals(header)) {
                throw new IOException("Invalid CSV header. Expected: '" + EXPECTED_HEADER + "' but was: '" + header + "'");
            }

            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                String[] parts = splitCsv(line);
                if (parts.length < 16) {
                    throw new IOException("Invalid CSV at line " + lineNo + ": expected 16 columns, got " + parts.length);
                }

                String lawaSiteId = parts[0].trim();
                String siteName = parts[1].trim();
                String region = parts[2].trim();
                BigDecimal latitude = parseBigDecimal(parts[3]);
                BigDecimal longitude = parseBigDecimal(parts[4]);
                String indicatorRaw = parts[5].trim();
                String indicatorNorm = normalizeIndicator(indicatorRaw, parts[6]);
                String units = parts[7].trim();
                String trendRaw = parts[8].trim();
                String trendNorm = normalizeTrend(trendRaw, parts[9]);
                Integer trendScore = parseInteger(parts[10]);
                Integer trendPeriodYears = parseInteger(parts[11]);
                String trendDataFrequency = parts[12].trim();
                String periodType = parts[13].trim();
                int periodStartYear = Integer.parseInt(parts[14].trim());
                int periodEndYear = Integer.parseInt(parts[15].trim());

                result.add(new LawaTrendMultiYearParsedRecord(
                        lawaSiteId, siteName, region, latitude, longitude,
                        indicatorRaw, indicatorNorm, units,
                        trendRaw, trendNorm, trendScore, trendPeriodYears,
                        trendDataFrequency, periodType, periodStartYear, periodEndYear
                ));
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
        if (isNotBlank(indicatorNormFromFile)) {
            return indicatorNormFromFile.trim();
        }
        String raw = indicatorRaw == null ? "" : collapseWhitespace(indicatorRaw.trim());
        String mapped = INDICATOR_MAP.get(raw);
        return mapped != null ? mapped : "OTHER";
    }

    private static String normalizeTrend(String trendRaw, String trendNormFromFile) {
        if (isNotBlank(trendNormFromFile)) {
            return trendNormFromFile.trim();
        }
        String raw = trendRaw == null ? "" : trendRaw.trim().toLowerCase();
        if (raw.contains("improv")) return "IMPROVING";
        if (raw.contains("degrad")) return "DEGRADING";
        if (raw.contains("no change")) return "NO_CHANGE";
        return "INSUFFICIENT_DATA";
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

    private static Integer parseInteger(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return Integer.parseInt(t);
    }

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static final Map<String, String> INDICATOR_MAP = createIndicatorMap();

    private static Map<String, String> createIndicatorMap() {
        Map<String, String> m = new HashMap<>();
        // Recommended initial mappings per design/008-lawa-trend-multi-year-schema.md
        m.put("E.coli", "ECOLI");
        m.put("Clarity", "CLARITY");
        m.put("Dissolved reactive phosphorus", "DRP");
        m.put("Nitrate nitrogen", "NITRATE_N");
        m.put("Total nitrogen", "TOTAL_N");
        m.put("Ammoniacal nitrogen", "AMMONIACAL_N");
        return m;
    }

    private static final String EXPECTED_HEADER =
            "lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,trend_raw,trend_norm,trend_score,trend_period_years,trend_data_frequency,period_type,period_start_year,period_end_year";
}
