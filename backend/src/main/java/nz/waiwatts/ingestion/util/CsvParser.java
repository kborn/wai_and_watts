package nz.waiwatts.ingestion.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing CSV files with proper handling of quoted strings containing commas.
 * This addresses the limitation of simple string.split() which cannot handle quoted fields.
 */
public class CsvParser {

    /**
     * Parses a CSV line into an array of fields, properly handling quoted strings.
     * Supports:
     * - Fields enclosed in double quotes
     * - Commas inside quoted fields
     * - Double quotes escaped as "" within quoted fields
     * - Empty fields
     *
     * @param line the CSV line to parse
     * @return array of field values
     * @throws IllegalArgumentException if the CSV line has malformed quotes
     */
    public static String[] parseLine(String line) {
        if (line == null) {
            return new String[0];
        }

        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped double quote within quotes (""), add one quote to field
                    currentField.append('"');
                    i++; // Skip the next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Field separator
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                // Regular character
                currentField.append(c);
            }
        }

        // Add the last field
        fields.add(currentField.toString());

        // Validate that we're not still in quotes (malformed CSV)
        if (inQuotes) {
            throw new IllegalArgumentException("Malformed CSV line: unclosed quotes");
        }

        return fields.toArray(new String[0]);
    }

    /**
     * Parses a CSV line and trims whitespace from each field.
     * This is a convenience method that combines parseLine with field trimming.
     *
     * @param line the CSV line to parse
     * @return array of trimmed field values
     * @throws IllegalArgumentException if the CSV line has malformed quotes
     */
    public static String[] parseLineTrimmed(String line) {
        String[] fields = parseLine(line);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }
        return fields;
    }
}