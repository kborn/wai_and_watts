package nz.waiwatts.explanations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request for an explanation with structured question typing.
 * <p>
 * Phase 12 contract: Must include questionType, datasetSource, and optional filters.
 * Enforces supported question classes and prevents freeform chat prompts.
 */
public class ExplanationRequest {
    @JsonProperty("questionType")
    private String questionType;
    
    @JsonProperty("datasetSource")
    private String datasetSource;
    
    private Map<String, Object> filters;

    public ExplanationRequest() {}

    public ExplanationRequest(String questionType, String datasetSource, Map<String, Object> filters) {
        this.questionType = questionType;
        this.datasetSource = datasetSource;
        this.filters = filters;
    }

    /**
     * Backward compatibility constructor for existing tests.
     * datasetSource must be provided via filters.
     */
    public ExplanationRequest(String questionType, Map<String, Object> filters) {
        this.questionType = questionType;
        this.filters = filters;
        // Extract datasetSource from filters for backward compatibility
        if (filters != null && filters.containsKey("datasetSource")) {
            this.datasetSource = (String) filters.get("datasetSource");
        }
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getDatasetSource() {
        return datasetSource;
    }

    public void setDatasetSource(String datasetSource) {
        this.datasetSource = datasetSource;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }
}