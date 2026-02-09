package nz.waiwatts.explanations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request for an explanation with structured question typing.
 * 
 * Uses question_type enumeration to enforce supported question classes
 * and prevent freeform chat prompts.
 */
public class ExplanationRequest {
    @JsonProperty("question_type")
    private String questionType;
    private Map<String, Object> filters;

    public ExplanationRequest() {}

    public ExplanationRequest(String questionType, Map<String, Object> filters) {
        this.questionType = questionType;
        this.filters = filters;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }
}