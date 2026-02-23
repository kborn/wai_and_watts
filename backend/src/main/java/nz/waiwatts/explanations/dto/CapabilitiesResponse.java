package nz.waiwatts.explanations.dto;

import java.util.List;

public class CapabilitiesResponse {


    private List<CapabilityDefinition> capabilities;
    private List<DatasetCapabilities> datasets;


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

        public String getDatasetSource() {
            return datasetSource;
        }

        public void setDatasetSource(String datasetSource) {
            this.datasetSource = datasetSource;
        }

    }

    public static class CapabilityDefinition {
        private String questionType;

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

    }
}
