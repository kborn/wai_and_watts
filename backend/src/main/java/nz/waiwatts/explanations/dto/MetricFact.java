package nz.waiwatts.explanations.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * MetricFact represents a single metric value at a specific point in time.
 * <p>
 * Stable ID format: metric:{dataset}:{metric}:{period}:{dimensions_hash}
 */
public record MetricFact(
    String id,
    String metricName,
    BigDecimal value,
    String unit,
    String period,
    Map<String, Object> dimensions
) {
    // Compatibility getters for existing call sites.
    public String getId() { return id; }
    public String getMetricName() { return metricName; }
    public BigDecimal getValue() { return value; }
    public String getUnit() { return unit; }
    public String getPeriod() { return period; }
    public Map<String, Object> getDimensions() { return dimensions; }
}
