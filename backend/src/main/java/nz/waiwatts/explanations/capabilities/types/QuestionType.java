package nz.waiwatts.explanations.capabilities.types;

import java.util.Arrays;
import java.util.Optional;

public enum QuestionType {
    RENEWABLE_GENERATION_TREND("renewable_generation_trend"),
    FUEL_GENERATION_TREND("fuel_generation_trend"),
    FUEL_TYPE_COMPARISON("fuel_type_comparison"),
    GENERATION_MIX_OVERVIEW("generation_mix_overview"),
    WATER_QUALITY_OVERVIEW("water_quality_overview"),
    WATER_QUALITY_STATE_SITES_TREND("water_quality_state_sites_trend"),
    REGIONAL_WATER_QUALITY("regional_water_quality"),
    WATER_QUALITY_TRENDS("water_quality_trends"),
    IMPROVING_SITES_TREND("improving_sites_trend"),
    REGIONAL_TREND_COMPARISON("regional_trend_comparison");

    private final String wireValue;

    QuestionType(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<QuestionType> fromWireValue(String wireValue) {
        if (wireValue == null || wireValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = wireValue.trim();
        return Arrays.stream(values())
            .filter(value -> value.wireValue.equals(normalized))
            .findFirst();
    }
}
