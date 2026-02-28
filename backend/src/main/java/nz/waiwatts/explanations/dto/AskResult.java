package nz.waiwatts.explanations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Ask endpoint envelope response (always returned for /ask).
 */
public class AskResult {
    @JsonProperty("isRefusal")
    private boolean isRefusal;
    private Refusal refusal;
    private ExplanationRequest parsedRequest;
    private String selectedDatasetSource;
    private DatasetSelection datasetSelection;
    private String explanation;
    private List<Citation> citations;
    private Map<String, Object> dataSummary;
    private Debug debug;

    public AskResult() {}

        public static Builder builder() {
        return new Builder();
    }

    public static AskResult success(
        ExplanationRequest parsedRequest,
        String selectedDatasetSource,
        DatasetSelection datasetSelection,
        String explanation,
        List<Citation> citations,
        Debug debug
    ) {
        return builder()
            .refusal(false)
            .refusal(new Refusal(null, null, null))
            .parsedRequest(parsedRequest)
            .selectedDatasetSource(selectedDatasetSource)
            .datasetSelection(datasetSelection)
            .explanation(explanation != null ? explanation : "")
            .citations(citations != null ? citations : List.of())
            .debug(debug)
            .build();
    }

    public static AskResult refusal(
        String code,
        String message,
        Map<String, Object> details,
        ExplanationRequest parsedRequest,
        String selectedDatasetSource,
        DatasetSelection datasetSelection,
        Debug debug
    ) {
        return builder()
            .refusal(true)
            .refusal(new Refusal(code, message, details))
            .parsedRequest(parsedRequest)
            .selectedDatasetSource(selectedDatasetSource)
            .datasetSelection(datasetSelection)
            .explanation("")
            .citations(List.of())
            .debug(debug)
            .build();
    }

    public boolean isRefusal() {
        return isRefusal;
    }

    public void setRefusal(boolean refusal) {
        isRefusal = refusal;
    }

    public Refusal getRefusal() {
        return refusal;
    }

    public void setRefusal(Refusal refusal) {
        this.refusal = refusal;
    }

    public ExplanationRequest getParsedRequest() {
        return parsedRequest;
    }

    public void setParsedRequest(ExplanationRequest parsedRequest) {
        this.parsedRequest = parsedRequest;
    }

    public String getSelectedDatasetSource() {
        return selectedDatasetSource;
    }

    public void setSelectedDatasetSource(String selectedDatasetSource) {
        this.selectedDatasetSource = selectedDatasetSource;
    }

    public DatasetSelection getDatasetSelection() {
        return datasetSelection;
    }

    public void setDatasetSelection(DatasetSelection datasetSelection) {
        this.datasetSelection = datasetSelection;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<Citation> getCitations() {
        return citations;
    }

    public void setCitations(List<Citation> citations) {
        this.citations = citations;
    }

    public Map<String, Object> getDataSummary() {
        return dataSummary;
    }

    public void setDataSummary(Map<String, Object> dataSummary) {
        this.dataSummary = dataSummary;
    }

    public Debug getDebug() {
        return debug;
    }

    public void setDebug(Debug debug) {
        this.debug = debug;
    }

    public static class Refusal {
        private String code;
        private String message;
        private Map<String, Object> details;

        public Refusal() {}

        public Refusal(String code, String message, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }

    public static class DatasetSelection {
        private String strategy;
        private String reason;

        public DatasetSelection() {}

        public DatasetSelection(String strategy, String reason) {
            this.strategy = strategy;
            this.reason = reason;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class Debug {
        private String parserUsed;
        private Long parseDurationMs;
        private List<String> selectionCandidates;
        private String refusalTrigger;

        public Debug() {}

        public Debug(String parserUsed, Long parseDurationMs, List<String> selectionCandidates, String refusalTrigger) {
            this.parserUsed = parserUsed;
            this.parseDurationMs = parseDurationMs;
            this.selectionCandidates = selectionCandidates;
            this.refusalTrigger = refusalTrigger;
        }

        public String getParserUsed() {
            return parserUsed;
        }

        public void setParserUsed(String parserUsed) {
            this.parserUsed = parserUsed;
        }

        public Long getParseDurationMs() {
            return parseDurationMs;
        }

        public void setParseDurationMs(Long parseDurationMs) {
            this.parseDurationMs = parseDurationMs;
        }

        public List<String> getSelectionCandidates() {
            return selectionCandidates;
        }

        public void setSelectionCandidates(List<String> selectionCandidates) {
            this.selectionCandidates = selectionCandidates;
        }

        public String getRefusalTrigger() {
            return refusalTrigger;
        }

        public void setRefusalTrigger(String refusalTrigger) {
            this.refusalTrigger = refusalTrigger;
        }
    }

    public static final class Builder {
        private boolean isRefusal;
        private Refusal refusal;
        private ExplanationRequest parsedRequest;
        private String selectedDatasetSource;
        private DatasetSelection datasetSelection;
        private String explanation;
        private List<Citation> citations;
        private Map<String, Object> dataSummary;
        private Debug debug;

        private Builder() {}

        public Builder refusal(boolean refusal) {
            this.isRefusal = refusal;
            return this;
        }

        public Builder refusal(Refusal refusal) {
            this.refusal = refusal;
            return this;
        }

        public Builder parsedRequest(ExplanationRequest parsedRequest) {
            this.parsedRequest = parsedRequest;
            return this;
        }

        public Builder selectedDatasetSource(String selectedDatasetSource) {
            this.selectedDatasetSource = selectedDatasetSource;
            return this;
        }

        public Builder datasetSelection(DatasetSelection datasetSelection) {
            this.datasetSelection = datasetSelection;
            return this;
        }

        public Builder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        public Builder citations(List<Citation> citations) {
            this.citations = citations;
            return this;
        }

        public Builder dataSummary(Map<String, Object> dataSummary) {
            this.dataSummary = dataSummary;
            return this;
        }

        public Builder debug(Debug debug) {
            this.debug = debug;
            return this;
        }

        public AskResult build() {
            AskResult result = new AskResult();
            result.setRefusal(isRefusal);
            result.setRefusal(refusal);
            result.setParsedRequest(parsedRequest);
            result.setSelectedDatasetSource(selectedDatasetSource);
            result.setDatasetSelection(datasetSelection);
            result.setExplanation(explanation);
            result.setCitations(citations);
            result.setDataSummary(dataSummary);
            result.setDebug(debug);
            return result;
        }
    }
}
