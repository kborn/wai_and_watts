package nz.waiwatts.ingestion.mbie;

import nz.waiwatts.ingestion.util.AbstractCsvParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class MbieGenerationAnnualCsvParser extends AbstractCsvParser implements MbieGenerationAnnualParser {

    @Override
    public List<MbieGenerationAnnualParsedRecord> parse(InputStream input) throws IOException {
        List<MbieGenerationAnnualParsedRecord> result = new ArrayList<>();
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
                String[] parts = splitCsvLine(line);
                if (isRowBlank(parts)) {
                    continue;
                }
                int year = Integer.parseInt(getRequired(parts, headerIndex, "period_year", lineNo));
                String raw = getRequired(parts, headerIndex, "fuel_type_raw", lineNo);
                String norm = getOptional(parts, headerIndex, "fuel_type_norm");
                if (norm.isEmpty()) {
                    norm = MbieFuelNormalizer.normalizeFuel(raw);
                }
                norm = MbieFuelNormalizer.mapToKnown(norm);
                BigDecimal gwh = new BigDecimal(getRequired(parts, headerIndex, "generation_gwh", lineNo));
                result.add(new MbieGenerationAnnualParsedRecord(year, raw, norm, gwh));
            }
        }
        if (result.isEmpty()) {
            throw new IOException("CSV contains header but no data rows");
        }
        return result;
    }

    @Override
    protected String[] splitCsvLine(String line) {
        // Simple split, fixture has no embedded commas/quotes
        String[] parts = line.split(",", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private static final List<String> REQUIRED_COLUMNS = List.of(
            "period_year",
            "fuel_type_raw",
            "fuel_type_norm",
            "generation_gwh"
    );
}
