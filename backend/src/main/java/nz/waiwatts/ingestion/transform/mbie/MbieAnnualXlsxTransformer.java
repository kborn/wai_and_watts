package nz.waiwatts.ingestion.transform.mbie;

import nz.waiwatts.ingestion.mbie.MbieFuelNormalizer;
import nz.waiwatts.ingestion.transform.CsvTransformUtil;
import nz.waiwatts.ingestion.transform.XlsxTransformUtil;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MbieAnnualXlsxTransformer {

    private static final String SHEET_NAME = "6 - Fuel type (GWh)";

    private static final String HEADER_YEAR = "calendaryear";
    private static final String HEADER_YEAR_ALT = "year";

    private static final Map<String, String> FUEL_HEADERS = new LinkedHashMap<>();

    static {
        FUEL_HEADERS.put("hydro", "Hydro");
        FUEL_HEADERS.put("geothermal", "Geothermal");
        FUEL_HEADERS.put("biogas", "Biogas");
        FUEL_HEADERS.put("wind", "Wind");
        FUEL_HEADERS.put("solarpv", "Solar PV");
        FUEL_HEADERS.put("solar", "Solar");
        FUEL_HEADERS.put("oil1", "Oil");
        FUEL_HEADERS.put("oil", "Oil");
        FUEL_HEADERS.put("coal", "Coal");
        FUEL_HEADERS.put("gas", "Gas");
        FUEL_HEADERS.put("naturalgas", "Natural gas");
    }

    public byte[] transform(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IOException("Sheet not found: " + SHEET_NAME);
            }
            DataFormatter formatter = new DataFormatter();
            HeaderMatch headerMatch = findHeaderRow(sheet, formatter);
            int yearColIndex = headerMatch.yearColIndex();
            Row fuelHeaderRow = sheet.getRow(headerMatch.fuelHeaderRowIndex());
            Map<String, Integer> headerIndex = XlsxTransformUtil.readHeaderMap(fuelHeaderRow, formatter);
            Map<Integer, String> fuelColumns = resolveFuelColumns(fuelHeaderRow, headerIndex, formatter);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String header = CsvTransformUtil.toCsvLine(List.of(
                    "period_year",
                    "fuel_type_raw",
                    "fuel_type_norm",
                    "generation_gwh"
            ));
            out.write((header + "\n").getBytes(StandardCharsets.UTF_8));

            int lastRow = sheet.getLastRowNum();
            for (int r = headerMatch.fuelHeaderRowIndex() + 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Integer year = parseYearSafe(XlsxTransformUtil.cellString(row, yearColIndex, formatter));
                if (year == null) {
                    continue;
                }
                for (Map.Entry<Integer, String> entry : fuelColumns.entrySet()) {
                    Integer colIdx = entry.getKey();
                    String fuelRaw = entry.getValue();
                    BigDecimal gwh = parseDecimalCell(row, colIdx, formatter);
                    if (gwh == null) {
                        continue;
                    }
                    String fuelNorm = MbieFuelNormalizer.mapToKnown(fuelRaw);
                    String line = CsvTransformUtil.toCsvLine(List.of(
                            String.valueOf(year),
                            fuelRaw,
                            fuelNorm,
                            formatGeneration(gwh)
                    ));
                    out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                }
            }
            return out.toByteArray();
        }
    }

    private HeaderMatch findHeaderRow(Sheet sheet, DataFormatter formatter) throws IOException {
        int maxScan = Math.min(25, sheet.getLastRowNum());
        int yearHeaderRow = -1;
        Integer yearColIndex = null;
        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Map<String, Integer> headerIndex = XlsxTransformUtil.readHeaderMap(row, formatter);
            if (headerIndex.containsKey(HEADER_YEAR)) {
                yearHeaderRow = r;
                yearColIndex = headerIndex.get(HEADER_YEAR);
                break;
            }
            if (headerIndex.containsKey(HEADER_YEAR_ALT)) {
                yearHeaderRow = r;
                yearColIndex = headerIndex.get(HEADER_YEAR_ALT);
                break;
            }
        }
        if (yearHeaderRow < 0 || yearColIndex == null) {
            throw new IOException("Failed to locate Calendar Year header in sheet: " + SHEET_NAME);
        }
        int fuelHeaderRowIndex = yearHeaderRow + 1;
        Row fuelHeaderRow = sheet.getRow(fuelHeaderRowIndex);
        Map<String, Integer> fuelHeaderIndex = XlsxTransformUtil.readHeaderMap(fuelHeaderRow, formatter);
        if (!hasAnyFuelHeader(fuelHeaderIndex)) {
            fuelHeaderRowIndex = yearHeaderRow;
        }
        return new HeaderMatch(yearColIndex, fuelHeaderRowIndex);
    }

    private String formatGeneration(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private Integer parseYearSafe(String raw) {
        try {
            return XlsxTransformUtil.parseInteger(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimalSafe(String raw) {
        try {
            return XlsxTransformUtil.parseDecimal(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimalCell(Row row, int colIdx, DataFormatter formatter) {
        if (row == null) {
            return null;
        }
        var cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
            if (cell.getCachedFormulaResultType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCachedFormulaResultType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                return parseDecimalSafe(cell.getStringCellValue());
            }
        }
        return parseDecimalSafe(formatter.formatCellValue(cell));
    }

    private boolean hasAnyFuelHeader(Map<String, Integer> headerIndex) {
        for (String key : FUEL_HEADERS.keySet()) {
            if (headerIndex.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private Map<Integer, String> resolveFuelColumns(Row headerRow,
                                                    Map<String, Integer> headerIndex,
                                                    DataFormatter formatter) throws IOException {
        Map<Integer, String> result = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> entry : FUEL_HEADERS.entrySet()) {
            Integer idx = headerIndex.get(entry.getKey());
            if (idx != null) {
                String raw = XlsxTransformUtil.cellString(headerRow, idx, formatter);
                result.put(idx, raw.isEmpty() ? entry.getValue() : raw);
            }
        }
        if (!result.keySet().isEmpty()) {
            return result;
        }
        missing.addAll(FUEL_HEADERS.keySet());
        throw new IOException("Missing fuel columns: " + String.join(", ", missing));
    }

    private record HeaderMatch(int yearColIndex, int fuelHeaderRowIndex) {
    }
}
