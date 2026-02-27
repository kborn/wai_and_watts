package nz.waiwatts.explanations.dto;

import java.util.Map;

/**
 * ClassificationFact represents categorical classifications (LAWA-style).
 * <p>
 * Stable ID format: cls:{dataset}:{classification_type}:{subject}:{start_year}_{end_year}:{dimensions_hash}
 */
public record ClassificationFact(
    String id,
    String subject,
    String classificationType,
    String classification,
    Integer periodStartYear,
    Integer periodEndYear,
    Map<String, Object> dimensions
) {
    // Compatibility getters for existing call sites.
    public String getId() { return id; }
    public String getSubject() { return subject; }
    public String getClassificationType() { return classificationType; }
    public String getClassification() { return classification; }
    public Integer getPeriodStartYear() { return periodStartYear; }
    public Integer getPeriodEndYear() { return periodEndYear; }
    public Map<String, Object> getDimensions() { return dimensions; }
}
