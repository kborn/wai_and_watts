package nz.waiwatts.explanations.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ComparisonFact represents a comparison between two time periods.
 * 
 * Stable ID format: cmp:{dataset}:{metric}:{dimensions}:{baseline_vs_comparison}
 */
public class ComparisonFact {
    private String id;
    private String metricName;
    private String baselinePeriod;
    private String comparisonPeriod;
    private BigDecimal deltaAbsolute;
    private BigDecimal deltaPercent;
    private String unit;
    private Map<String, Object> dimensions;

    public ComparisonFact() {}

    public ComparisonFact(String id, String metricName, String baselinePeriod, String comparisonPeriod, 
                         BigDecimal deltaAbsolute, BigDecimal deltaPercent, String unit, Map<String, Object> dimensions) {
        this.id = id;
        this.metricName = metricName;
        this.baselinePeriod = baselinePeriod;
        this.comparisonPeriod = comparisonPeriod;
        this.deltaAbsolute = deltaAbsolute;
        this.deltaPercent = deltaPercent;
        this.unit = unit;
        this.dimensions = dimensions;
    }

    // Getters and setters
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

    public String getBaselinePeriod() {
        return baselinePeriod;
    }

    public void setBaselinePeriod(String baselinePeriod) {
        this.baselinePeriod = baselinePeriod;
    }

    public String getComparisonPeriod() {
        return comparisonPeriod;
    }

    public void setComparisonPeriod(String comparisonPeriod) {
        this.comparisonPeriod = comparisonPeriod;
    }

    public BigDecimal getDeltaAbsolute() {
        return deltaAbsolute;
    }

    public void setDeltaAbsolute(BigDecimal deltaAbsolute) {
        this.deltaAbsolute = deltaAbsolute;
    }

    public BigDecimal getDeltaPercent() {
        return deltaPercent;
    }

    public void setDeltaPercent(BigDecimal deltaPercent) {
        this.deltaPercent = deltaPercent;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Map<String, Object> getDimensions() {
        return dimensions;
    }

    public void setDimensions(Map<String, Object> dimensions) {
        this.dimensions = dimensions;
    }
}