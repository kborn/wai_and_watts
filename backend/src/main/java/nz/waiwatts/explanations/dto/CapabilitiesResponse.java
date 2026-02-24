package nz.waiwatts.explanations.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CapabilitiesResponse {

    private Map<String, String> supportedQuestionTypes;
    private Map<String, String> unsupportedQuestionTypes;
    private Map<String, String> supportedDatasetSources;
    private Map<String, String> requiredFilters;
    private Map<String, String> filterStructure;
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
        private Set<String> supportedFilters;

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

        public Set<String> getSupportedFilters() {
            return supportedFilters;
        }

        public void setSupportedFilters(Set<String> supportedFilters) {
            this.supportedFilters = supportedFilters;
        }
    }
}

