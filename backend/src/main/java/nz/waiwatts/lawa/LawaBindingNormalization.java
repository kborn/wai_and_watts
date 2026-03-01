package nz.waiwatts.lawa;

import java.util.Locale;
import java.util.Map;

/**
 * Shared normalization for LAWA query bindings so request labels and stored normalized
 * codes resolve consistently across explanation and context flows.
 */
public final class LawaBindingNormalization {

    private static final Map<String, String> STATE_INDICATOR_NORMALIZATION = Map.ofEntries(
        Map.entry("e. coli", "ecoli"),
        Map.entry("e.coli", "ecoli"),
        Map.entry("ecoli", "ecoli"),
        Map.entry("clarity / suspended fine sediment", "clarity"),
        Map.entry("clarity", "clarity"),
        Map.entry("dissolved reactive phosphorus", "drp"),
        Map.entry("drp", "drp"),
        Map.entry("no3n", "no3n"),
        Map.entry("ton", "ton"),
        Map.entry("ammonical nitrogen / ammonia (toxicity)", "ammonia_toxicity"),
        Map.entry("ammoniacal nitrogen / ammonia (toxicity)", "ammonia_toxicity"),
        Map.entry("ammonia toxicity", "ammonia_toxicity"),
        Map.entry("nitrate nitrogen / nitrate (toxicity)", "nitrate_toxicity"),
        Map.entry("nitrate toxicity", "nitrate_toxicity"),
        Map.entry("nitrate", "nitrate_toxicity")
    );

    private static final Map<String, String> TREND_INDICATOR_NORMALIZATION = Map.ofEntries(
        Map.entry("e. coli", "ecoli"),
        Map.entry("e.coli", "ecoli"),
        Map.entry("ecoli", "ecoli"),
        Map.entry("clarity", "clarity"),
        Map.entry("dissolved reactive phosphorus", "drp"),
        Map.entry("drp", "drp"),
        Map.entry("nitrate nitrogen", "nitrate_n"),
        Map.entry("nitrate", "nitrate_n"),
        Map.entry("total nitrogen", "total_n"),
        Map.entry("ammoniacal nitrogen", "ammoniacal_n")
    );

    private LawaBindingNormalization() {
    }

    public static String normalizeRegionForQuery(String rawRegion) {
        return normalizeFallback(rawRegion);
    }

    public static String normalizeStateIndicatorForQuery(String rawIndicator) {
        return normalizeFromMap(rawIndicator, STATE_INDICATOR_NORMALIZATION);
    }

    public static String normalizeTrendIndicatorForQuery(String rawIndicator) {
        return normalizeFromMap(rawIndicator, TREND_INDICATOR_NORMALIZATION);
    }

    public static boolean matchesNormalizedStateIndicator(String recordIndicatorNorm, String requestedIndicator) {
        return matchesNormalized(recordIndicatorNorm, requestedIndicator, true);
    }

    public static boolean matchesNormalizedTrendIndicator(String recordIndicatorNorm, String requestedIndicator) {
        return matchesNormalized(recordIndicatorNorm, requestedIndicator, false);
    }

    private static boolean matchesNormalized(String recordIndicatorNorm, String requestedIndicator, boolean state) {
        if (requestedIndicator == null || requestedIndicator.isBlank()) {
            return true;
        }
        if (recordIndicatorNorm == null || recordIndicatorNorm.isBlank()) {
            return false;
        }
        String normalizedRequested = state
            ? normalizeStateIndicatorForQuery(requestedIndicator)
            : normalizeTrendIndicatorForQuery(requestedIndicator);
        return recordIndicatorNorm.trim().toLowerCase(Locale.ROOT).equals(normalizedRequested);
    }

    private static String normalizeFromMap(String rawValue, Map<String, String> normalization) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String trimmed = rawValue.trim();
        String normalized = normalization.get(trimmed.toLowerCase(Locale.ROOT));
        return normalized != null ? normalized : normalizeFallback(trimmed);
    }

    private static String normalizeFallback(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return rawValue.trim().toLowerCase(Locale.ROOT);
    }
}
