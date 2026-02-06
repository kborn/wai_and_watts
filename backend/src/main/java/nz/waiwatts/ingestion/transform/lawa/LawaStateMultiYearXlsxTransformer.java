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

public class LawaStateMultiYearXlsxTransformer {

    private static final String SHEET_NAME = "State Attribute Band";

    private static final String H_REGION = "region";
    private static final String H_LAWA_SITE_ID = "lawasiteid";
    private static final String H_SITE_ID = "siteid";
    private static final String H_LATITUDE = "latitude";
    private static final String H_LONGITUDE = "longitude";
    private static final String H_HYEAR = "hyear";
    private static final String H_INDICATOR = "indicatorattribute";
    private static final String H_UNITS = "unitsofmeasure";
    private static final String H_ATTRIBUTE_BAND = "attributeband";
    private static final String H_MEDIAN = "median";
    private static final String H_P95 = "95thpercentile";
    private static final String H_REC_260 = "rechealthexceedancesover260numericattributestatistic";
    private static final String H_REC_540 = "rechealthexceedancesover540numericattributestatistic";

    public byte[] transform(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IOException("Sheet not found: " + SHEET_NAME);
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
                String lawaSiteId = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_LAWA_SITE_ID), formatter);
                String indicatorRaw = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_INDICATOR), formatter);
                if (lawaSiteId.isEmpty() || indicatorRaw.isEmpty()) {
                    continue;
                }
                String siteName = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_SITE_ID), formatter);
                String region = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_REGION), formatter);
                String latitude = toPlain(parseDecimalCell(row, headerMatch.headerIndex().get(H_LATITUDE), formatter));
                String longitude = toPlain(parseDecimalCell(row, headerMatch.headerIndex().get(H_LONGITUDE), formatter));
                String units = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_UNITS), formatter);
                String attributeBand = XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_ATTRIBUTE_BAND), formatter);
                Integer hYear = XlsxTransformUtil.parseInteger(XlsxTransformUtil.cellString(row, headerMatch.headerIndex().get(H_HYEAR), formatter));
                if (hYear == null) {
                    continue;
                }

                String indicatorNorm = normalizeIndicator(indicatorRaw);
                String stateNorm = normalizeState(attributeBand);
                BigDecimal median = parseDecimalCell(row, headerMatch.headerIndex().get(H_MEDIAN), formatter);
                BigDecimal p95 = parseDecimalCell(row, headerMatch.headerIndex().get(H_P95), formatter);
                BigDecimal rec260 = parseDecimalCell(row, headerMatch.headerIndex().get(H_REC_260), formatter);
                BigDecimal rec540 = parseDecimalCell(row, headerMatch.headerIndex().get(H_REC_540), formatter);

                int periodStart = hYear - 5;
                int periodEnd = hYear;

                rows.add(List.of(
                        lawaSiteId,
                        siteName,
                        region,
                        latitude,
                        longitude,
                        indicatorRaw,
                        indicatorNorm,
                        units,
                        attributeBand,
                        stateNorm,
                        toPlain(median),
                        toPlain(p95),
                        toPlain(rec260),
                        toPlain(rec540),
                        "HYDRO_5YR_ROLLING",
                        String.valueOf(periodStart),
                        String.valueOf(periodEnd)
                ));
            }
            rows.sort(Comparator
                    .comparing((List<String> r) -> r.get(2), Comparator.nullsLast(String::compareTo))
                    .thenComparing(r -> r.get(0))
                    .thenComparing(r -> r.get(6))
                    .thenComparing(r -> r.get(16)));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String header = CsvTransformUtil.toCsvLine(List.of(
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
            ));
            out.write((header + "\n").getBytes(StandardCharsets.UTF_8));
            for (List<String> row : rows) {
                String line = CsvTransformUtil.toCsvLine(row);
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return out.toByteArray();
        }
    }

    private HeaderMatch findHeaderRow(Sheet sheet, DataFormatter formatter) throws IOException {
        int maxScan = Math.min(30, sheet.getLastRowNum());
        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            Map<String, Integer> headerIndex = XlsxTransformUtil.readHeaderMap(row, formatter);
            if (headerIndex.containsKey(H_LAWA_SITE_ID) && headerIndex.containsKey(H_INDICATOR) && headerIndex.containsKey(H_HYEAR)) {
                return new HeaderMatch(r, headerIndex);
            }
        }
        throw new IOException("Failed to locate header row in sheet: " + SHEET_NAME);
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

    private String normalizeIndicator(String raw) {
        if (raw == null) {
            return "OTHER";
        }
        String key = raw.trim();
        return INDICATOR_NORM_MAP.getOrDefault(key, "OTHER");
    }

    private String normalizeState(String band) {
        if (band == null) {
            return "UNKNOWN";
        }
        String key = band.trim().toUpperCase();
        return BAND_TO_STATE.getOrDefault(key, "UNKNOWN");
    }

    private record HeaderMatch(int rowIndex, Map<String, Integer> headerIndex) {
    }

    private static final Map<String, String> INDICATOR_NORM_MAP = createIndicatorMap();

    private static Map<String, String> createIndicatorMap() {
        Map<String, String> m = new HashMap<>();
        m.put("E.coli", "ECOLI");
        m.put("Clarity / Suspended fine sediment", "CLARITY");
        m.put("Dissolved reactive phosphorus", "DRP");
        m.put("NO3N", "NO3N");
        m.put("TON", "TON");
        m.put("Ammonical nitrogen / Ammonia (toxicity)", "AMMONIA_TOXICITY");
        m.put("Nitrate nitrogen / Nitrate (toxicity)", "NITRATE_TOXICITY");
        return m;
    }

    private static final Map<String, String> BAND_TO_STATE = createBandMap();

    private static Map<String, String> createBandMap() {
        Map<String, String> m = new HashMap<>();
        m.put("A", "EXCELLENT");
        m.put("B", "GOOD");
        m.put("C", "FAIR");
        m.put("D", "POOR");
        m.put("E", "VERY_POOR");
        return m;
    }
}
