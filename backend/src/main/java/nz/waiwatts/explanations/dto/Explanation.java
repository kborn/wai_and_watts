package nz.waiwatts.explanations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Explanation response from LLM provider.
 * 
 * Includes stable citations to Fact Pack facts and refusal handling.
 */
public class Explanation {
    private String explanationText;
    private List<String> citations;
    @JsonProperty("isRefusal")
    private boolean isRefusal;
    @JsonProperty("refusalReason")
    private String refusalReason;

    public Explanation() {}

    public Explanation(String explanationText, List<String> citations) {
        this.explanationText = explanationText;
        this.citations = citations;
        this.isRefusal = false;
    }

    public static Explanation refusal(String reason) {
        Explanation explanation = new Explanation();
        explanation.isRefusal = true;
        explanation.refusalReason = reason;
        explanation.explanationText = "I can't answer that using the available dataset facts. If you want, I can explain what facts would be needed.";
        return explanation;
    }

    // Getters and setters
    public String getExplanationText() {
        return explanationText;
    }

    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
    }

    public List<String> getCitations() {
        return citations;
    }

    public void setCitations(List<String> citations) {
        this.citations = citations;
    }

    public boolean isRefusal() {
        return isRefusal;
    }

    public void setRefusal(boolean refusal) {
        isRefusal = refusal;
    }

    public String getRefusalReason() {
        return refusalReason;
    }

    public void setRefusalReason(String refusalReason) {
        this.refusalReason = refusalReason;
    }
}