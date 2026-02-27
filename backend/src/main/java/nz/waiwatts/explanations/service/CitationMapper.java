package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dto.Citation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CitationMapper {

    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");

    public List<Citation> map(List<String> citationIds, String datasetSource) {
        if (citationIds == null || citationIds.isEmpty()) {
            return List.of();
        }
        List<String> canonicalIds = citationIds.stream()
            .filter(id -> id != null && !id.isBlank())
            .map(String::trim)
            .distinct()
            .sorted(Comparator.comparing(id -> id.toLowerCase(Locale.ROOT)))
            .toList();

        List<Citation> citations = new ArrayList<>(canonicalIds.size());
        for (String id : canonicalIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            citations.add(parseCitation(id.trim(), datasetSource));
        }
        return citations;
    }

    private Citation parseCitation(String id, String datasetSource) {
        String[] parts = id.split(":");
        String prefix = parts.length > 0 ? parts[0].toLowerCase(Locale.ROOT) : "";
        String type = switch (prefix) {
            case "ts" -> "TIME_SERIES";
            case "metric" -> "METRIC";
            case "class" -> "CLASSIFICATION";
            case "cmp" -> "COMPARISON";
            default -> "UNKNOWN";
        };

        String field = null;
        String fuelType = null;
        Integer periodYear = null;
        Citation.Period period = null;

        switch (prefix) {
            case "ts" -> {
                // Examples:
                // ts:mbie:generation_gwh:HYDRO:1974_2024
                // ts:mbie:renewable_generation_gwh_quarterly:2020-Q1_to_2024-Q4
                if (parts.length >= 3) {
                    field = parts[2];
                }
                if (parts.length >= 4 && parts[3] != null && !parts[3].contains("_to_") && !parts[3].contains("_")) {
                    fuelType = parts[3];
                }
                String coverage = parts.length >= 5 ? parts[4] : (parts.length == 4 ? parts[3] : null);
                period = parseCoverage(coverage);
            }
            case "metric" -> {
                // Examples:
                // metric:mbie:generation_gwh:2024:COAL
                if (parts.length >= 3) {
                    field = parts[2];
                }
                if (parts.length >= 4) {
                    periodYear = parseYear(parts[3]).orElse(null);
                }
                if (parts.length >= 5) {
                    fuelType = parts[4];
                }
            }
            case "class" -> {
                // Examples:
                // class:lawa:water_quality_trend:DEGRADING
                if (parts.length >= 3) {
                    field = parts[2];
                }
            }
            case "cmp" -> {
                // Examples:
                // cmp:mbie:generation_gwh:HYDRO:2024_vs_2023
                if (parts.length >= 3) {
                    field = parts[2];
                }
                if (parts.length >= 4) {
                    fuelType = parts[3];
                }
                if (parts.length >= 5) {
                    period = parseCoverage(parts[4]);
                }
            }
        }

        return Citation.builder()
            .id(id)
            .type(type)
            .datasetSource(datasetSource)
            .field(field)
            .fuelType(fuelType)
            .periodYear(periodYear)
            .period(period)
            .build();
    }

    private Optional<Integer> parseYear(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher matcher = YEAR_PATTERN.matcher(value);
        if (matcher.find()) {
            try {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Citation.Period parseCoverage(String coverage) {
        if (coverage == null || coverage.isBlank() || "all_time".equalsIgnoreCase(coverage)) {
            return null;
        }

        if (coverage.contains("_to_")) {
            String[] parts = coverage.split("_to_");
            Integer start = parseYear(parts[0]).orElse(null);
            Integer end = parts.length > 1 ? parseYear(parts[1]).orElse(null) : null;
            return (start == null && end == null) ? null : new Citation.Period(start, end);
        }

        if (coverage.contains("_")) {
            String[] parts = coverage.split("_");
            Integer start = parseYear(parts[0]).orElse(null);
            Integer end = parts.length > 1 ? parseYear(parts[1]).orElse(null) : null;
            return (start == null && end == null) ? null : new Citation.Period(start, end);
        }

        return parseYear(coverage).map(year -> new Citation.Period(year, year)).orElse(null);
    }
}
