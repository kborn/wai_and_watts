package nz.waiwatts.explanations.capabilities.types;

import java.util.Arrays;
import java.util.Optional;

public enum DatasetSource {
    MBIE_GENERATION_ANNUAL("mbie.generation.annual"),
    MBIE_GENERATION_QUARTERLY("mbie.generation.quarterly"),
    LAWA_WATER_QUALITY_STATE_MULTI_YEAR("lawa.water_quality.state.multi_year"),
    LAWA_WATER_QUALITY_TREND_MULTI_YEAR("lawa.water_quality.trend.multi_year");

    private final String wireValue;

    DatasetSource(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<DatasetSource> fromWireValue(String wireValue) {
        if (wireValue == null || wireValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = wireValue.trim();
        return Arrays.stream(values())
            .filter(value -> value.wireValue.equals(normalized))
            .findFirst();
    }
}
