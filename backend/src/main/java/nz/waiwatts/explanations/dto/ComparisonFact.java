package nz.waiwatts.explanations.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ComparisonFact represents a comparison between two time periods.
 * <p>
 * Stable ID format: cmp:{dataset}:{metric}:{dimensions}:{baseline_vs_comparison}
 */
public record ComparisonFact(
    String id,
    String metricName,
    String baselinePeriod,
    String comparisonPeriod,
    BigDecimal deltaAbsolute,
    BigDecimal deltaPercent,
    String unit,
    Map<String, Object> dimensions
) {
    // Compatibility getters for existing call sites.
    public String getId() { return id; }
    public String getMetricName() { return metricName; }
    public String getBaselinePeriod() { return baselinePeriod; }
    public String getComparisonPeriod() { return comparisonPeriod; }
    public BigDecimal getDeltaAbsolute() { return deltaAbsolute; }
    public BigDecimal getDeltaPercent() { return deltaPercent; }
    public String getUnit() { return unit; }
    public Map<String, Object> getDimensions() { return dimensions; }
}
