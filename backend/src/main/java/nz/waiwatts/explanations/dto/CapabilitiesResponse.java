package nz.waiwatts.explanations.dto;

import java.util.List;
import java.util.Map;

public class CapabilitiesResponse {

    private Map<String, String> supportedQuestionTypes;
    private Map<String, String> unsupportedQuestionTypes;
    private Map<String, String> supportedDatasetSources;
    private Map<String, String> requiredFilters;
    private Map<String, String> filterStructure;
    private Map<String, List<String>> metricTypes;
    private Map<String, List<String>> examples;
    private List<CapabilityDefinition> capabilities;
    private List<DatasetCapabilities> datasets;

    public Map<String, String> getSupportedQuestionTypes() {
        return supportedQuestionTypes;
    }

    public void setSupportedQuestionTypes(Map<String, String> supportedQuestionTypes) {
        this.supportedQuestionTypes = supportedQuestionTypes;
    }

    public Map<String, String> getUnsupportedQuestionTypes() {
        return unsupportedQuestionTypes;
    }

    public void setUnsupportedQuestionTypes(Map<String, String> unsupportedQuestionTypes) {
        this.unsupportedQuestionTypes = unsupportedQuestionTypes;
    }

    public Map<String, String> getSupportedDatasetSources() {
        return supportedDatasetSources;
    }

    public void setSupportedDatasetSources(Map<String, String> supportedDatasetSources) {
        this.supportedDatasetSources = supportedDatasetSources;
    }

    public Map<String, String> getRequiredFilters() {
        return requiredFilters;
    }

    public void setRequiredFilters(Map<String, String> requiredFilters) {
        this.requiredFilters = requiredFilters;
    }

    public Map<String, String> getFilterStructure() {
        return filterStructure;
    }

    public void setFilterStructure(Map<String, String> filterStructure) {
        this.filterStructure = filterStructure;
    }

    public Map<String, List<String>> getMetricTypes() {
        return metricTypes;
    }

    public void setMetricTypes(Map<String, List<String>> metricTypes) {
        this.metricTypes = metricTypes;
    }

    public Map<String, List<String>> getExamples() {
        return examples;
    }

    public void setExamples(Map<String, List<String>> examples) {
        this.examples = examples;
    }

    public List<CapabilityDefinition> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<CapabilityDefinition> capabilities) {
        this.capabilities = capabilities;
    }

    public List<DatasetCapabilities> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<DatasetCapabilities> datasets) {
        this.datasets = datasets;
    }

    public static class DatasetCapabilities {
        private String datasetSource;
        private String displayName;
        private String description;
        private List<String> supportedQuestionTypes;
        private List<String> supportedFilters;

        public String getDatasetSource() {
            return datasetSource;
        }

        public void setDatasetSource(String datasetSource) {
            this.datasetSource = datasetSource;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getSupportedQuestionTypes() {
            return supportedQuestionTypes;
        }

        public void setSupportedQuestionTypes(List<String> supportedQuestionTypes) {
            this.supportedQuestionTypes = supportedQuestionTypes;
        }

        public List<String> getSupportedFilters() {
            return supportedFilters;
        }

        public void setSupportedFilters(List<String> supportedFilters) {
            this.supportedFilters = supportedFilters;
        }
    }

    public static class CapabilityDefinition {
        private String intentId;
        private String displayName;
        private String questionType;
        private String description;
        private List<String> supportedDatasets;
        private List<String> datasetSources;
        private List<String> requiredFilters;
        private List<String> optionalFilters;
        private List<String> supportedFilters;
        private List<String> metricTypes;
        private String defaultMetricType;
        private List<String> exampleTemplates;
        private List<String> examples;

        public String getIntentId() {
            return intentId;
        }

        public void setIntentId(String intentId) {
            this.intentId = intentId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getSupportedDatasets() {
            return supportedDatasets;
        }

        public void setSupportedDatasets(List<String> supportedDatasets) {
            this.supportedDatasets = supportedDatasets;
        }

        public List<String> getDatasetSources() {
            return datasetSources;
        }

        public void setDatasetSources(List<String> datasetSources) {
            this.datasetSources = datasetSources;
        }

        public List<String> getRequiredFilters() {
            return requiredFilters;
        }

        public void setRequiredFilters(List<String> requiredFilters) {
            this.requiredFilters = requiredFilters;
        }

        public List<String> getOptionalFilters() {
            return optionalFilters;
        }

        public void setOptionalFilters(List<String> optionalFilters) {
            this.optionalFilters = optionalFilters;
        }

        public List<String> getSupportedFilters() {
            return supportedFilters;
        }

        public void setSupportedFilters(List<String> supportedFilters) {
            this.supportedFilters = supportedFilters;
        }

        public List<String> getMetricTypes() {
            return metricTypes;
        }

        public void setMetricTypes(List<String> metricTypes) {
            this.metricTypes = metricTypes;
        }

        public String getDefaultMetricType() {
            return defaultMetricType;
        }

        public void setDefaultMetricType(String defaultMetricType) {
            this.defaultMetricType = defaultMetricType;
        }

        public List<String> getExampleTemplates() {
            return exampleTemplates;
        }

        public void setExampleTemplates(List<String> exampleTemplates) {
            this.exampleTemplates = exampleTemplates;
        }

        public List<String> getExamples() {
            return examples;
        }

        public void setExamples(List<String> examples) {
            this.examples = examples;
        }
    }
}
