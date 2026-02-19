package nz.waiwatts.explanations.dto;

public class Citation {
    private String id;
    private String type;
    private String datasetSource;
    private String field;
    private String fuelType;
    private Integer periodYear;
    private Period period;

    public Citation() {}

    public Citation(
        String id,
        String type,
        String datasetSource,
        String field,
        String fuelType,
        Integer periodYear,
        Period period
    ) {
        this.id = id;
        this.type = type;
        this.datasetSource = datasetSource;
        this.field = field;
        this.fuelType = fuelType;
        this.periodYear = periodYear;
        this.period = period;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDatasetSource() {
        return datasetSource;
    }

    public void setDatasetSource(String datasetSource) {
        this.datasetSource = datasetSource;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public static class Period {
        private Integer startYear;
        private Integer endYear;

        public Period() {}

        public Period(Integer startYear, Integer endYear) {
            this.startYear = startYear;
            this.endYear = endYear;
        }

        public Integer getStartYear() {
            return startYear;
        }

        public void setStartYear(Integer startYear) {
            this.startYear = startYear;
        }

        public Integer getEndYear() {
            return endYear;
        }

        public void setEndYear(Integer endYear) {
            this.endYear = endYear;
        }
    }
}
