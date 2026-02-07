package nz.waiwatts.explanations.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * MetricFact represents a single metric value at a specific point in time.
 * 
 * Stable ID format: metric:{dataset}:{metric}:{period}:{dimensions_hash}
 */
public class MetricFact {
    private String id;
    private String metricName;
    private BigDecimal value;
    private String unit;
    private String period;
    private Map<String, Object> dimensions;

    public MetricFact() {}

    public MetricFact(String id, String metricName, BigDecimal value, String unit, String period, Map<String, Object> dimensions) {
        this.id = id;
        this.metricName = metricName;
        this.value = value;
        this.unit = unit;
        this.period = period;
        this.dimensions = dimensions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Map<String, Object> getDimensions() {
        return dimensions;
    }

    public void setDimensions(Map<String, Object> dimensions) {
        this.dimensions = dimensions;
    }
}