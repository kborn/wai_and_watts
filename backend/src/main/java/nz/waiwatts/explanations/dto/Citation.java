package nz.waiwatts.explanations.dto;

public record Citation(
    String id,
    String type,
    String datasetSource,
    String field,
    String fuelType,
    Integer periodYear,
    Period period
) {
    public static Builder builder() {
        return new Builder();
    }

    // Compatibility getters for existing call sites.
    public String getId() { return id; }
    public String getType() { return type; }
    public String getDatasetSource() { return datasetSource; }
    public String getField() { return field; }
    public String getFuelType() { return fuelType; }
    public Integer getPeriodYear() { return periodYear; }
    public Period getPeriod() { return period; }

    public record Period(
        Integer startYear,
        Integer endYear
    ) {
        // Compatibility getters for existing call sites.
        public Integer getStartYear() { return startYear; }
        public Integer getEndYear() { return endYear; }
    }

    public static final class Builder {
        private String id;
        private String type;
        private String datasetSource;
        private String field;
        private String fuelType;
        private Integer periodYear;
        private Period period;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder datasetSource(String datasetSource) {
            this.datasetSource = datasetSource;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder fuelType(String fuelType) {
            this.fuelType = fuelType;
            return this;
        }

        public Builder periodYear(Integer periodYear) {
            this.periodYear = periodYear;
            return this;
        }

        public Builder period(Period period) {
            this.period = period;
            return this;
        }

        public Citation build() {
            return new Citation(
                id,
                type,
                datasetSource,
                field,
                fuelType,
                periodYear,
                period
            );
        }
    }
}
