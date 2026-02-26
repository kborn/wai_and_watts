package nz.waiwatts.explanations.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * TimeSeriesFact represents a series of data points over time.
 * 
 * Stable ID format: ts:{dataset}:{metric}:{start_year}_{end_year}:{dimensions_hash}
 * 
 * Data points are sorted deterministically by period for stable serialization.
 */
public class TimeSeriesFact {
    private String id;
    private String metricName;
    private String unit;
    private Map<String, Object> dimensions;
    private List<DataPoint> points = new ArrayList<>();

    public TimeSeriesFact() {}

    public TimeSeriesFact(String id, String metricName, String unit, Map<String, Object> dimensions) {
        this.id = id;
        this.metricName = metricName;
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

    public List<DataPoint> getPoints() {
        return points;
    }

    public void setPoints(List<DataPoint> points) {
        this.points = new ArrayList<>(points);
        // Ensure deterministic ordering for stable serialization
        this.points.sort(Comparator.comparing(DataPoint::getPeriod));
    }

    /**
     * Data point in a time series with stable ordering by period
     */
    public static class DataPoint {
        private String period;
        private BigDecimal value;

        public DataPoint() {}

        public DataPoint(String period, BigDecimal value) {
            this.period = period;
            this.value = value;
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }
    }
}