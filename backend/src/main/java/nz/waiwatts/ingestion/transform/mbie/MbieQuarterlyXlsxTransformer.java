package nz.waiwatts.ingestion.transform.mbie;

import nz.waiwatts.ingestion.mbie.MbieFuelNormalizer;
import nz.waiwatts.ingestion.transform.CsvTransformUtil;
import nz.waiwatts.ingestion.transform.XlsxTransformUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MbieQuarterlyXlsxTransformer {

    private static final String SHEET_NAME = "1 - Quarterly GWh";

    private static final String HEADER_CALENDAR_QUARTER = "calendarquarter";
    private static final Set<String> ALLOWED_FUELS = Set.of(
            "Hydro",
            "Geothermal",
            "Biogas",
            "Wood",
            "Wind",
            "Solar",
            "Oil",
            "Coal",
            "Gas"
    );

    public byte[] transform(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IOException("Sheet not found: " + SHEET_NAME);
            }
            DataFormatter formatter = new DataFormatter();
            HeaderMatch headerMatch = findHeaderRow(sheet, formatter);
            Map<Integer, QuarterPeriod> periods = resolvePeriods(headerMatch.headerRow(), headerMatch.headerColIndex());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String header = CsvTransformUtil.toCsvLine(List.of(
                    "period_year",
                    "period_quarter",
                    "fuel_type_raw",
                    "fuel_type_norm",
                    "generation_gwh"
            ));
            out.write((header + "\n").getBytes(StandardCharsets.UTF_8));

            int lastRow = sheet.getLastRowNum();
            for (int r = headerMatch.rowIndex() + 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String fuelRaw = XlsxTransformUtil.cellString(row, headerMatch.headerColIndex(), formatter);
                if (fuelRaw.isEmpty() || isNetGenerationRow(fuelRaw)) {
                    continue;
                }
                String cleanedFuel = cleanFuelRaw(fuelRaw);
                if (!ALLOWED_FUELS.contains(cleanedFuel)) {
                    continue;
                }
                for (Map.Entry<Integer, QuarterPeriod> entry : periods.entrySet()) {
                    Integer colIdx = entry.getKey();
                    BigDecimal gwh = parseDecimalCell(row, colIdx, formatter);
                    if (gwh == null) {
                        continue;
                    }
                    String fuelNorm = MbieFuelNormalizer.mapToKnown(cleanedFuel);
                    String line = CsvTransformUtil.toCsvLine(List.of(
                            String.valueOf(entry.getValue().year()),
                            String.valueOf(entry.getValue().quarter()),
                            cleanedFuel,
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
        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                String normalized = XlsxTransformUtil.normalizeHeader(formatter.formatCellValue(cell));
                if (HEADER_CALENDAR_QUARTER.equals(normalized)) {
                    return new HeaderMatch(r, row, cell.getColumnIndex());
                }
            }
        }
        throw new IOException("Failed to locate header row in sheet: " + SHEET_NAME);
    }

    private Map<Integer, QuarterPeriod> resolvePeriods(Row headerRow, int headerColIndex) throws IOException {
        Map<Integer, QuarterPeriod> result = new LinkedHashMap<>();
        short lastCol = headerRow.getLastCellNum();
        for (int c = headerColIndex + 1; c < lastCol; c++) {
            Cell cell = headerRow.getCell(c);
            Integer year = parseYearFromCell(cell);
            Integer quarter = XlsxTransformUtil.parseQuarterFromCell(cell);
            if (year == null || quarter == null) {
                continue;
            }
            result.put(c, new QuarterPeriod(year, quarter));
        }
        if (result.isEmpty()) {
            throw new IOException("Failed to locate calendar quarters in sheet: " + SHEET_NAME);
        }
        return result;
    }

    private Integer parseYearFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDate date = cell.getDateCellValue().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            return date.getYear();
        }
        String raw = new DataFormatter().formatCellValue(cell);
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[^0-9]", " ").trim();
        String[] parts = cleaned.split("\\s+");
        for (String part : parts) {
            if (part.length() == 4) {
                try {
                    return XlsxTransformUtil.parseInteger(part);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isNetGenerationRow(String fuelRaw) {
        String normalized = XlsxTransformUtil.normalizeHeader(fuelRaw);
        return normalized.startsWith("netgeneration");
    }

    private String cleanFuelRaw(String fuelRaw) {
        if (fuelRaw == null) {
            return "";
        }
        String cleaned = fuelRaw.replaceAll("\\d+$", "").trim();
        return cleaned.replaceAll("\\s+", " ");
    }

    private String formatGeneration(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal parseDecimalCell(Row row, int colIdx, DataFormatter formatter) {
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

    private record HeaderMatch(int rowIndex, Row headerRow, int headerColIndex) {
    }

    private record QuarterPeriod(int year, int quarter) {
    }
}
