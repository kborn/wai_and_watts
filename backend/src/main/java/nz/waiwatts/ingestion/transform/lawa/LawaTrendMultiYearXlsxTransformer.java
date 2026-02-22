package nz.waiwatts.ingestion.transform.lawa;

import nz.waiwatts.ingestion.transform.CsvTransformUtil;
import nz.waiwatts.ingestion.transform.XlsxTransformUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LawaTrendMultiYearXlsxTransformer {

    private static final String SHEET_TREND = "Trend";
    private static final String SHEET_STATE = "State Attribute Band";

    private static final String H_REGION = "region";
    private static final String H_CATCHMENT = "catchment";
    private static final String H_LAWA_SITE_ID = "lawasiteid";
    private static final String H_SITE_ID = "siteid";
    private static final String H_LATITUDE = "latitude";
    private static final String H_LONGITUDE = "longitude";
    private static final String H_INDICATOR = "indicator";
    private static final String H_INDICATOR_ALT = "indicatorattribute";
    private static final String H_TREND_RAW = "trenddescription";
    private static final String H_TREND_SCORE = "trendscore";
    private static final String H_TREND_PERIOD = "trendperiodyear";
    private static final String H_TREND_FREQUENCY = "trenddatafrequency";
    private static final String H_HYEAR = "hyear";

    public byte[] transform(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            int periodEnd = resolveAsOfYear(workbook);
            Sheet sheet = workbook.getSheet(SHEET_TREND);
            if (sheet == null) {
                throw new IOException("Sheet not found: " + SHEET_TREND);
            }
            DataFormatter formatter = new DataFormatter();
            HeaderMatch headerMatch = findHeaderRow(sheet, formatter);

            List<List<String>> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int r = headerMatch.rowIndex() + 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Integer indicatorIdx = resolveHeaderIndex(headerMatch.headerIndex(), List.of(H_INDICATOR, H_INDICATOR_ALT));
                String lawaSiteId = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_LAWA_SITE_ID), formatter);
                String indicatorRaw = XlsxTransformUtil.cellString(row, indicatorIdx, formatter);
                if (lawaSiteId.isEmpty() || indicatorRaw.isEmpty()) {
                    continue;
                }
                String siteName = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_SITE_ID), formatter);
                String region = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_REGION), formatter);
                String catchment = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_CATCHMENT), formatter);
                String latitude = toPlain(parseDecimalCell(row, headerMatch.headerIndex().get(H_LATITUDE), formatter));
                String longitude = toPlain(parseDecimalCell(row, headerMatch.headerIndex().get(H_LONGITUDE), formatter));
                String trendRaw = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_TREND_RAW), formatter);
                Integer trendScore = parseIntegerSafe(XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_TREND_SCORE), formatter));
                Integer trendPeriodYears = parseIntegerSafe(XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_TREND_PERIOD), formatter));
                String trendFrequency = normalizeTrendFrequency(XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_TREND_FREQUENCY), formatter));
                if (trendPeriodYears == null) {
                    continue;
                }

                String indicatorNorm = normalizeIndicator(indicatorRaw);
                String trendNorm = normalizeTrend(trendRaw);
                int periodStart = periodEnd - trendPeriodYears;

                rows.add(List.of(
                        lawaSiteId,
                        siteName,
                        region,
                        catchment,
                        latitude,
                        longitude,
                        indicatorRaw,
                        indicatorNorm,
                        trendRaw,
                        trendNorm,
                        trendScore == null ? "" : String.valueOf(trendScore),
                        String.valueOf(trendPeriodYears),
                        trendFrequency,
                        "HYDRO_NYR_WINDOW",
                        String.valueOf(periodStart),
                        String.valueOf(periodEnd)
                ));
            }
            rows.sort(Comparator
                    .comparing((List<String> r) -> r.get(2), Comparator.nullsLast(String::compareTo))
                    .thenComparing(List::getFirst)
                    .thenComparing(r -> r.get(7))
                    .thenComparing(r -> parseSortInt(r.get(11))));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String header = CsvTransformUtil.toCsvLine(List.of(
                    "lawa_site_id",
                    "site_name",
                    "region",
                    "catchment",
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
            ));
            out.write((header + "\n").getBytes(StandardCharsets.UTF_8));
            for (List<String> row : rows) {
                String line = CsvTransformUtil.toCsvLine(row);
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return out.toByteArray();
        }
    }

    private int resolveAsOfYear(Workbook workbook) throws IOException {
        Sheet sheet = workbook.getSheet(SHEET_STATE);
        if (sheet == null) {
            throw new IOException("Sheet not found: " + SHEET_STATE);
        }
        DataFormatter formatter = new DataFormatter();
        int maxScan = Math.min(30, sheet.getLastRowNum());
        int hYearCol = -1;
        int headerRowIndex = -1;
        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            Map<String, Integer> headerIndex = XlsxTransformUtil.readHeaderMap(row, formatter);
            if (headerIndex.containsKey(H_HYEAR)) {
                hYearCol = headerIndex.get(H_HYEAR);
                headerRowIndex = r;
                break;
            }
        }
        if (hYearCol < 0) {
            throw new IOException("Failed to locate hYear column in sheet: " + SHEET_STATE);
        }
        int maxYear = -1;
        for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Integer year = parseYearCell(row, hYearCol, formatter);
            if (year != null && year > maxYear) {
                maxYear = year;
            }
        }
        if (maxYear < 0) {
            throw new IOException("Failed to derive as_of_year from sheet: " + SHEET_STATE);
        }
        return maxYear;
    }

    private Integer parseYearCell(Row row, int idx, DataFormatter formatter) {
        String raw = XlsxTransformUtil.cellString(row, idx, formatter);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return XlsxTransformUtil.parseInteger(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private HeaderMatch findHeaderRow(Sheet sheet, DataFormatter formatter) throws IOException {
        int maxScan = Math.min(30, sheet.getLastRowNum());
        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            Map<String, Integer> headerIndex = XlsxTransformUtil.readHeaderMap(row, formatter);
            boolean hasIndicator = headerIndex.containsKey(H_INDICATOR) || headerIndex.containsKey(H_INDICATOR_ALT);
            if (headerIndex.containsKey(H_LAWA_SITE_ID) && hasIndicator && headerIndex.containsKey(H_TREND_RAW)) {
                return new HeaderMatch(r, headerIndex);
            }
        }
        throw new IOException("Failed to locate header row in sheet: " + SHEET_TREND);
    }

    private Integer resolveHeaderIndex(Map<String, Integer> headerIndex, List<String> keys) {
        for (String key : keys) {
            Integer idx = headerIndex.get(key);
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }

    private String toPlain(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private BigDecimal parseDecimalCell(Row row, Integer colIdx, DataFormatter formatter) {
        if (row == null || colIdx == null) {
            return null;
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCachedFormulaResultType() == CellType.STRING) {
                return parseDecimalSafe(cell.getStringCellValue());
            }
        }
        return parseDecimalSafe(formatter.formatCellValue(cell));
    }

    private BigDecimal parseDecimalSafe(String raw) {
        try {
            return XlsxTransformUtil.parseDecimal(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseIntegerSafe(String raw) {
        try {
            return XlsxTransformUtil.parseInteger(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeTrendFrequency(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        if ("NA".equalsIgnoreCase(cleaned)) {
            return "";
        }
        return cleaned;
    }

    private Integer parseSortInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    private record HeaderMatch(int rowIndex, Map<String, Integer> headerIndex) {
    }

    private String normalizeIndicator(String raw) {
        if (raw == null) {
            return "OTHER";
        }
        String key = raw.trim();
        return INDICATOR_NORM_MAP.getOrDefault(key, "OTHER");
    }

    private String normalizeTrend(String desc) {
        if (desc == null) {
            return "INSUFFICIENT_DATA";
        }
        String d = desc.trim().toLowerCase();
        if (d.contains("improving")) {
            return "IMPROVING";
        }
        if (d.contains("degrading")) {
            return "DEGRADING";
        }
        if (d.contains("no change") || d.contains("no trend")) {
            return "NO_CHANGE";
        }
        return "INSUFFICIENT_DATA";
    }

    private static final Map<String, String> INDICATOR_NORM_MAP = createIndicatorMap();

    private static Map<String, String> createIndicatorMap() {
        Map<String, String> m = new HashMap<>();
        m.put("E.coli", "ECOLI");
        m.put("Clarity", "CLARITY");
        m.put("Dissolved reactive phosphorus", "DRP");
        m.put("Nitrate nitrogen", "NITRATE_N");
        m.put("Total nitrogen", "TOTAL_N");
        m.put("Ammoniacal nitrogen", "AMMONIACAL_N");
        return m;
    }
}
