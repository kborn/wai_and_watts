package nz.waiwatts.ingestion.lawa;

import nz.waiwatts.ingestion.util.AbstractCsvParser;
import nz.waiwatts.ingestion.util.CsvParser;

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
public class LawaTrendMultiYearCsvParser extends AbstractCsvParser implements LawaTrendMultiYearParser {

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
                String[] parts = CsvParser.parseLineTrimmed(line);
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
                        indicatorRaw, indicatorNorm,
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

    @Override
    protected String[] splitCsvLine(String line) {
        return CsvParser.parseLineTrimmed(line);
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
