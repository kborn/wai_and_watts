package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dataset.DatasetDescriptor;
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

    public Map<String, Object> buildCapabilitiesResponse() {
        Map<String, Object> supportedTypes = new LinkedHashMap<>();
        supportedTypes.put("supportedQuestionTypes", questionTypeCatalog.supportedDescriptions());
        supportedTypes.put("unsupportedQuestionTypes", questionTypeCatalog.unsupportedDescriptions());

        Map<String, String> supportedDatasetSources = new LinkedHashMap<>();
        for (DatasetDescriptor descriptor : datasetCatalog.getDatasets()) {
            supportedDatasetSources.put(descriptor.datasetSource(), descriptor.displayName());
        }
        supportedTypes.put("supportedDatasetSources", supportedDatasetSources);

        supportedTypes.put("requiredFilters", Map.of(
            "datasetSource", "Must specify the data source (e.g., 'mbie.generation.annual', 'lawa.water_quality.state.multi_year')"
        ));
        supportedTypes.put("filterStructure", Map.of(
            "datasetSource", "string (required)",
            "fuelType", "string (optional, for MBIE data)",
            "fuelTypeB", "string (optional second fuel for MBIE comparisons)",
            "indicator", "string (optional, for LAWA data)",
            "region", "string (optional, for LAWA data)",
            "trend", "string (optional, for LAWA trend data)",
            "startYear", "integer (optional)",
            "endYear", "integer (optional)"
        ));

        var datasets = new ArrayList<Map<String, Object>>();
        for (DatasetDescriptor descriptor : datasetCatalog.getDatasets()) {
            datasets.add(Map.of(
                "datasetSource", descriptor.datasetSource(),
                "displayName", descriptor.displayName(),
                "description", descriptor.displayName(),
                "supportedQuestionTypes", descriptor.supportedQuestionTypes(),
                "supportedFilters", descriptor.supportedFilters()
            ));
        }
        supportedTypes.put("datasets", datasets);
        return supportedTypes;
    }
}
