package nz.waiwatts.explanations.capabilities.types;

import java.util.Arrays;
import java.util.Optional;

public enum FilterKey {
    DATASET_SOURCE("datasetSource"),
    FUEL_TYPE("fuelType"),
    FUEL_TYPE_B("fuelTypeB"),
    INDICATOR("indicator"),
    STATE_CATEGORY("stateCategory"),
    REGION("region"),
    TREND("trend"),
    START_YEAR("startYear"),
    END_YEAR("endYear"),
    METRIC_TYPE("metricType");

    private final String wireValue;

    FilterKey(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<FilterKey> fromWireValue(String wireValue) {
        if (wireValue == null || wireValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = wireValue.trim();
        return Arrays.stream(values())
            .filter(value -> value.wireValue.equals(normalized))
            .findFirst();
    }
}
