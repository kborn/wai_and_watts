package nz.waiwatts.ingestion.transform;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public final class XlsxTransformUtil {

    private XlsxTransformUtil() {
    }

    public static Map<String, Integer> readHeaderMap(Row row, DataFormatter formatter) {
        Map<String, Integer> index = new HashMap<>();
        if (row == null) {
            return index;
        }
        for (Cell cell : row) {
            String raw = formatter.formatCellValue(cell);
            String key = normalizeHeader(raw);
            if (!key.isEmpty() && !index.containsKey(key)) {
                index.put(key, cell.getColumnIndex());
            }
        }
        return index;
    }

    public static String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String cleaned = header.trim().toLowerCase();
        cleaned = cleaned.replaceAll("[^a-z0-9]+", "");
        return cleaned;
    }

    public static String cellString(Row row, Integer idx, DataFormatter formatter) {
        if (row == null || idx == null) {
            return "";
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    public static Integer parseInteger(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replace("\u00A0", "")
                .replace(",", "")
                .replace("\t", "")
                .trim();
        if (cleaned.isEmpty() || "-".equals(cleaned)) {
            return null;
        }
        return Integer.parseInt(cleaned);
    }

    public static BigDecimal parseDecimal(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replace("\u00A0", "")
                .replace(",", "")
                .replace("\t", "")
                .trim();
        if (cleaned.isEmpty() || "-".equals(cleaned) || "NA".equalsIgnoreCase(cleaned)) {
            return null;
        }
        return new BigDecimal(cleaned);
    }

    public static Integer parseQuarterFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDate date = cell.getDateCellValue().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            int month = date.getMonthValue();
            return ((month - 1) / 3) + 1;
        }
        String raw = new DataFormatter().formatCellValue(cell);
        return parseQuarterFromString(raw);
    }

    public static Integer parseQuarterFromString(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().toUpperCase();
        int idx = cleaned.indexOf('Q');
        if (idx >= 0 && idx + 1 < cleaned.length()) {
            char q = cleaned.charAt(idx + 1);
            if (q >= '1' && q <= '4') {
                return q - '0';
            }
        }
        return null;
    }
}
