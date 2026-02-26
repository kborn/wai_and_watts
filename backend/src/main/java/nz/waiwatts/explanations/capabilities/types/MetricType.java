package nz.waiwatts.explanations.capabilities.types;

import java.util.Arrays;
import java.util.Optional;

public enum MetricType {
    GENERATION_GWH("generation_gwh"),
    RENEWABLE_SHARE_PCT("renewable_share_pct"),
    GENERATION_SHARE_PCT("generation_share_pct"),
    SITE_PERCENTAGE("site_percentage"),
    SITE_COUNT("site_count"),
    AVERAGE_TREND_SCORE("average_trend_score");

    private final String wireValue;

    MetricType(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<MetricType> fromWireValue(String wireValue) {
        if (wireValue == null || wireValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = wireValue.trim();
        return Arrays.stream(values())
            .filter(value -> value.wireValue.equals(normalized))
            .findFirst();
    }
}
