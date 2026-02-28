package nz.waiwatts.explanations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Explanation response from LLM provider.
 * <p>
 * Includes stable citations to Fact Pack facts and refusal handling.
 */
public class Explanation {
    private String explanationText;
    private List<String> citations;
    @JsonProperty("isRefusal")
    private boolean isRefusal;
    @JsonProperty("refusalReason")
    private String refusalReason;
    @JsonProperty("selectedDatasetSource")
    private String selectedDatasetSource;
    @JsonProperty("datasetSelectionReason")
    private String datasetSelectionReason;
    @JsonProperty("parsedRequest")
    private ExplanationRequest parsedRequest;
    @JsonProperty("debug")
    private DebugInfo debug;

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

    public String getSelectedDatasetSource() {
        return selectedDatasetSource;
    }

    public void setSelectedDatasetSource(String selectedDatasetSource) {
        this.selectedDatasetSource = selectedDatasetSource;
    }

    public String getDatasetSelectionReason() {
        return datasetSelectionReason;
    }

    public void setDatasetSelectionReason(String datasetSelectionReason) {
        this.datasetSelectionReason = datasetSelectionReason;
    }

    public ExplanationRequest getParsedRequest() {
        return parsedRequest;
    }

    public void setParsedRequest(ExplanationRequest parsedRequest) {
        this.parsedRequest = parsedRequest;
    }

    public DebugInfo getDebug() {
        return debug;
    }

    public void setDebug(DebugInfo debug) {
        this.debug = debug;
    }

    public static class DebugInfo {
        @JsonProperty("parserUsed")
        private String parserUsed;
        @JsonProperty("datasetSelectionStrategy")
        private String datasetSelectionStrategy;

        public DebugInfo() {}

        public DebugInfo(String parserUsed, String datasetSelectionStrategy) {
            this.parserUsed = parserUsed;
            this.datasetSelectionStrategy = datasetSelectionStrategy;
        }

        public String getParserUsed() {
            return parserUsed;
        }

        public void setParserUsed(String parserUsed) {
            this.parserUsed = parserUsed;
        }

        public String getDatasetSelectionStrategy() {
            return datasetSelectionStrategy;
        }

        public void setDatasetSelectionStrategy(String datasetSelectionStrategy) {
            this.datasetSelectionStrategy = datasetSelectionStrategy;
        }
    }
}
