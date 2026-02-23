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
public class LawaStateMultiYearCsvParser extends AbstractCsvParser implements LawaStateMultiYearParser {

    @Override
    public List<LawaStateMultiYearParsedRecord> parse(InputStream input) throws IOException {
        List<LawaStateMultiYearParsedRecord> result = new ArrayList<>();
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
                String catchment = getOptional(parts, headerIndex, "catchment");
                BigDecimal latitude = parseBigDecimal(getOptional(parts, headerIndex, "latitude"));
                BigDecimal longitude = parseBigDecimal(getOptional(parts, headerIndex, "longitude"));
                String indicatorRaw = getRequired(parts, headerIndex, "indicator_raw", lineNo);
                String indicatorNorm = normalizeIndicator(indicatorRaw, getOptional(parts, headerIndex, "indicator_norm"));
                String units = getRequired(parts, headerIndex, "units", lineNo);
                String attributeBand = getRequired(parts, headerIndex, "attribute_band", lineNo);
                String stateNorm = normalizeState(attributeBand, getOptional(parts, headerIndex, "state_norm"));
                BigDecimal median = parseBigDecimal(getOptional(parts, headerIndex, "median"));
                BigDecimal p95 = parseBigDecimal(getOptional(parts, headerIndex, "p95"));
                BigDecimal recHealth260 = parseBigDecimal(getOptional(parts, headerIndex, "rec_health_exceed_260_pct"));
                BigDecimal recHealth540 = parseBigDecimal(getOptional(parts, headerIndex, "rec_health_exceed_540_pct"));
                String periodType = getRequired(parts, headerIndex, "period_type", lineNo);
                int periodStartYear = Integer.parseInt(getRequired(parts, headerIndex, "period_start_year", lineNo));
                int periodEndYear = Integer.parseInt(getRequired(parts, headerIndex, "period_end_year", lineNo));

                result.add(new LawaStateMultiYearParsedRecord(lawaSiteId, siteName, region, catchment, latitude, longitude, indicatorRaw, indicatorNorm, units, attributeBand, stateNorm, median, p95, recHealth260, recHealth540, periodType, periodStartYear, periodEndYear));
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
        return LawaCsvNormalization.normalizeIndicator(indicatorRaw, indicatorNormFromFile, INDICATOR_MAP);
    }

    private static String normalizeState(String attributeBand, String stateNormFromFile) {
        // If the CSV already provides normalized state, trust it (trim only)
        if (LawaCsvNormalization.isNotBlank(stateNormFromFile)) {
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

    private static final Map<String, String> INDICATOR_MAP = createIndicatorMap();

    private static Map<String, String> createIndicatorMap() {
        Map<String, String> m = new HashMap<>();
        // Recommended initial mappings per engineering/design/007-lawa-state-multi-year-schema.md
        m.put("E.coli", "ECOLI");
        m.put("Clarity / Suspended fine sediment", "CLARITY");
        m.put("Dissolved reactive phosphorus", "DRP");
        m.put("NO3N", "NO3N");
        m.put("TON", "TON");
        m.put("Ammonical nitrogen / Ammonia (toxicity)", "AMMONIA_TOXICITY");
        m.put("Nitrate nitrogen / Nitrate (toxicity)", "NITRATE_TOXICITY");
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
            "attribute_band",
            "state_norm",
            "median",
            "p95",
            "rec_health_exceed_260_pct",
            "rec_health_exceed_540_pct",
            "period_type",
            "period_start_year",
            "period_end_year"
    );
}
