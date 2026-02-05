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

                String lawaSiteId = getRequired(parts, headerIndex, "lawa_site_id", lineNo);
                String siteName = getRequired(parts, headerIndex, "site_name", lineNo);
                String region = getRequired(parts, headerIndex, "region", lineNo);
                BigDecimal latitude = parseBigDecimal(getOptional(parts, headerIndex, "latitude"));
                BigDecimal longitude = parseBigDecimal(getOptional(parts, headerIndex, "longitude"));
                String indicatorRaw = getRequired(parts, headerIndex, "indicator_raw", lineNo);
                String indicatorNorm = normalizeIndicator(indicatorRaw, getOptional(parts, headerIndex, "indicator_norm"));
                String units = getRequired(parts, headerIndex, "units", lineNo);
                String trendRaw = getRequired(parts, headerIndex, "trend_raw", lineNo);
                String trendNorm = normalizeTrend(trendRaw, getOptional(parts, headerIndex, "trend_norm"));
                Integer trendScore = parseInteger(getOptional(parts, headerIndex, "trend_score"));
                Integer trendPeriodYears = parseInteger(getOptional(parts, headerIndex, "trend_period_years"));
                String trendDataFrequency = getRequired(parts, headerIndex, "trend_data_frequency", lineNo);
                String periodType = getRequired(parts, headerIndex, "period_type", lineNo);
                int periodStartYear = Integer.parseInt(getRequired(parts, headerIndex, "period_start_year", lineNo));
                int periodEndYear = Integer.parseInt(getRequired(parts, headerIndex, "period_end_year", lineNo));

                result.add(new LawaTrendMultiYearParsedRecord(
                        lawaSiteId, siteName, region, latitude, longitude,
                        indicatorRaw, indicatorNorm, units,
                        trendRaw, trendNorm, trendScore, trendPeriodYears,
                        trendDataFrequency, periodType, periodStartYear, periodEndYear
                ));
            }
        }
        if (result.isEmpty()) {
            throw new IOException("CSV contains header but no data rows");
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

    private static final List<String> REQUIRED_COLUMNS = List.of(
            "lawa_site_id",
            "site_name",
            "region",
            "latitude",
            "longitude",
            "indicator_raw",
            "indicator_norm",
            "units",
            "trend_raw",
            "trend_norm",
            "trend_score",
            "trend_period_years",
            "trend_data_frequency",
            "period_type",
            "period_start_year",
            "period_end_year"
    );
}
