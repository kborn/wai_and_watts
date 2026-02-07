package nz.waiwatts.explanations.dto;

import java.util.Map;

/**
 * ClassificationFact represents categorical classifications (LAWA-style).
 * 
 * Stable ID format: cls:{dataset}:{classification_type}:{subject}:{start_year}_{end_year}:{dimensions_hash}
 */
public class ClassificationFact {
    private String id;
    private String subject;
    private String classificationType;
    private String classification;
    private Integer periodStartYear;
    private Integer periodEndYear;
    private Map<String, Object> dimensions;

    public ClassificationFact() {}

    public ClassificationFact(String id, String subject, String classificationType, String classification, 
                             Integer periodStartYear, Integer periodEndYear, Map<String, Object> dimensions) {
        this.id = id;
        this.subject = subject;
        this.classificationType = classificationType;
        this.classification = classification;
        this.periodStartYear = periodStartYear;
        this.periodEndYear = periodEndYear;
        this.dimensions = dimensions;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getClassificationType() {
        return classificationType;
    }

    public void setClassificationType(String classificationType) {
        this.classificationType = classificationType;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Integer getPeriodStartYear() {
        return periodStartYear;
    }

    public void setPeriodStartYear(Integer periodStartYear) {
        this.periodStartYear = periodStartYear;
    }

    public Integer getPeriodEndYear() {
        return periodEndYear;
    }

    public void setPeriodEndYear(Integer periodEndYear) {
        this.periodEndYear = periodEndYear;
    }

    public Map<String, Object> getDimensions() {
        return dimensions;
    }

    public void setDimensions(Map<String, Object> dimensions) {
        this.dimensions = dimensions;
    }
}