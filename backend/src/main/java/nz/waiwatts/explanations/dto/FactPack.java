package nz.waiwatts.explanations.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Fact Pack - the only allowed data interface between Wai & Watts domain data and any LLM.
 * 
 * A Fact Pack is a deterministic, structured, provenance-safe bundle of facts derived 
 * from the database that the LLM may use to generate natural-language explanations.
 * 
 * Implements deterministic ordering and stable IDs for citation requirements.
 */
public class FactPack {

    private String factPackVersion = "1.0";
    private OffsetDateTime generatedAtUtc;
    private RequestContext requestContext;
    private Provenance provenance;
    private Facts facts;
    private Guardrails guardrails;

    public FactPack() {
        // Keep unset by default so FactPack construction remains deterministic.
        // Callers can set this explicitly if a stable timestamp source is required.
        this.generatedAtUtc = null;
        this.facts = new Facts();
        this.guardrails = new Guardrails();
    }

    // Getters and setters
    public String getFactPackVersion() {
        return factPackVersion;
    }

    public void setFactPackVersion(String factPackVersion) {
        this.factPackVersion = factPackVersion;
    }

    public OffsetDateTime getGeneratedAtUtc() {
        return generatedAtUtc;
    }

    public void setGeneratedAtUtc(OffsetDateTime generatedAtUtc) {
        this.generatedAtUtc = generatedAtUtc;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }

    public Facts getFacts() {
        return facts;
    }

    public void setFacts(Facts facts) {
        this.facts = facts;
    }

    public Guardrails getGuardrails() {
        return guardrails;
    }

    public void setGuardrails(Guardrails guardrails) {
        this.guardrails = guardrails;
    }

    /**
     * Request context for the Fact Pack
     */
    public static class RequestContext {
        private String questionType;
        private List<String> datasetScope;
        private Map<String, Object> filtersApplied;

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

        public List<String> getDatasetScope() {
            return datasetScope;
        }

        public void setDatasetScope(List<String> datasetScope) {
            this.datasetScope = datasetScope;
        }

        public Map<String, Object> getFiltersApplied() {
            return filtersApplied;
        }

        public void setFiltersApplied(Map<String, Object> filtersApplied) {
            this.filtersApplied = filtersApplied;
        }
    }

    /**
     * Provenance information for the Fact Pack
     */
    public static class Provenance {
        private List<DatasetSourceProvenance> datasetSources;

        public List<DatasetSourceProvenance> getDatasetSources() {
            return datasetSources;
        }

        public void setDatasetSources(List<DatasetSourceProvenance> datasetSources) {
            this.datasetSources = datasetSources;
        }
    }

    /**
     * Dataset source provenance with stable identifiers for citation
     */
    public static class DatasetSourceProvenance {
        private String datasetSourceCode;
        private String datasetReleaseId;
        private String contentHash;
        private String periodCoverage;

        public String getDatasetSourceCode() {
            return datasetSourceCode;
        }

        public void setDatasetSourceCode(String datasetSourceCode) {
            this.datasetSourceCode = datasetSourceCode;
        }

        public String getDatasetReleaseId() {
            return datasetReleaseId;
        }

        public void setDatasetReleaseId(String datasetReleaseId) {
            this.datasetReleaseId = datasetReleaseId;
        }

        public String getContentHash() {
            return contentHash;
        }

        public void setContentHash(String contentHash) {
            this.contentHash = contentHash;
        }

        public String getPeriodCoverage() {
            return periodCoverage;
        }

        public void setPeriodCoverage(String periodCoverage) {
            this.periodCoverage = periodCoverage;
        }
    }

    /**
     * Facts bundle with deterministic ordering
     */
    public static class Facts {
        private List<MetricFact> metrics = new java.util.ArrayList<>();
        private List<ComparisonFact> comparisons = new java.util.ArrayList<>();
        private List<TimeSeriesFact> timeSeries = new java.util.ArrayList<>();
        private List<ClassificationFact> classifications = new java.util.ArrayList<>();

        public List<MetricFact> getMetrics() {
            return metrics;
        }

        public void setMetrics(List<MetricFact> metrics) {
            this.metrics = metrics;
        }

        public List<ComparisonFact> getComparisons() {
            return comparisons;
        }

        public void setComparisons(List<ComparisonFact> comparisons) {
            this.comparisons = comparisons;
        }

        public List<TimeSeriesFact> getTimeSeries() {
            return timeSeries;
        }

        public void setTimeSeries(List<TimeSeriesFact> timeSeries) {
            this.timeSeries = timeSeries;
        }

        public List<ClassificationFact> getClassifications() {
            return classifications;
        }

        public void setClassifications(List<ClassificationFact> classifications) {
            this.classifications = classifications;
        }
    }

    /**
     * Guardrails for the LLM with stable citation requirements
     */
    public static class Guardrails {
        private List<String> allowedClaims = new java.util.ArrayList<>();
        private List<String> forbiddenClaims = new java.util.ArrayList<>();
        private List<String> requiredCitations = new java.util.ArrayList<>();

        public List<String> getAllowedClaims() {
            return allowedClaims;
        }

        public void setAllowedClaims(List<String> allowedClaims) {
            this.allowedClaims = allowedClaims;
        }

        public List<String> getForbiddenClaims() {
            return forbiddenClaims;
        }

        public void setForbiddenClaims(List<String> forbiddenClaims) {
            this.forbiddenClaims = forbiddenClaims;
        }

        public List<String> getRequiredCitations() {
            return requiredCitations;
        }

        public void setRequiredCitations(List<String> requiredCitations) {
            this.requiredCitations = requiredCitations;
        }
    }
}
