package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dataset.DatasetDescriptor;
import nz.waiwatts.explanations.dto.CapabilitiesResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CapabilitiesService {

    private final DatasetCatalog datasetCatalog;
    private final QuestionTypeCatalog questionTypeCatalog;

    public CapabilitiesService(DatasetCatalog datasetCatalog, QuestionTypeCatalog questionTypeCatalog) {
        this.datasetCatalog = datasetCatalog;
        this.questionTypeCatalog = questionTypeCatalog;
    }

    public CapabilitiesResponse buildCapabilitiesResponse() {
        CapabilitiesResponse response = new CapabilitiesResponse();
        response.setSupportedQuestionTypes(questionTypeCatalog.supportedDescriptions());
        response.setUnsupportedQuestionTypes(questionTypeCatalog.unsupportedDescriptions());

        Map<String, String> supportedDatasetSources = new LinkedHashMap<>();
        for (DatasetDescriptor descriptor : datasetCatalog.getDatasets()) {
            supportedDatasetSources.put(descriptor.datasetSource(), descriptor.displayName());
        }
        response.setSupportedDatasetSources(supportedDatasetSources);

        response.setRequiredFilters(Map.of(
            "datasetSource", "Must specify the data source (e.g., 'mbie.generation.annual', 'lawa.water_quality.state.multi_year')"
        ));
        response.setFilterStructure(Map.of(
            "datasetSource", "string (required)",
            "fuelType", "string (optional, for MBIE data)",
            "fuelTypeB", "string (optional second fuel for MBIE comparisons)",
            "indicator", "string (optional, for LAWA data)",
            "region", "string (optional, for LAWA data)",
            "trend", "string (optional, for LAWA trend data)",
            "startYear", "integer (optional)",
            "endYear", "integer (optional)"
        ));

        var datasets = new ArrayList<CapabilitiesResponse.DatasetCapabilities>();
        for (DatasetDescriptor descriptor : datasetCatalog.getDatasets()) {
            CapabilitiesResponse.DatasetCapabilities datasetCapabilities = new CapabilitiesResponse.DatasetCapabilities();
            datasetCapabilities.setDatasetSource(descriptor.datasetSource());
            datasetCapabilities.setDisplayName(descriptor.displayName());
            datasetCapabilities.setDescription(descriptor.displayName());
            datasetCapabilities.setSupportedQuestionTypes(descriptor.supportedQuestionTypes());
            datasetCapabilities.setSupportedFilters(descriptor.supportedFilters());
            datasets.add(datasetCapabilities);
        }
        response.setDatasets(datasets);
        return response;
    }
}
