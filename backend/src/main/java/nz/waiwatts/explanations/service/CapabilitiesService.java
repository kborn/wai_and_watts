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

    public CapabilitiesService(DatasetCatalog datasetCatalog) {
        this.datasetCatalog = datasetCatalog;
    }

    public Map<String, Object> buildCapabilitiesResponse() {
        Map<String, Object> supportedTypes = new LinkedHashMap<>();
        supportedTypes.put("supportedQuestionTypes", Map.of(
            "renewable_generation_trend", "Explain renewable generation trends between years",
            "hydro_generation_trend", "Explain hydro generation trends between years",
            "fuel_type_comparison", "Compare two fuel types (e.g., hydro vs geothermal)",
            "generation_mix_overview", "Summarize main sources of electricity generation",
            "water_quality_overview", "Provide overview of water quality state distribution",
            "excellent_sites_trend", "Explain trends in excellent water quality sites",
            "regional_water_quality", "Compare water quality across regions",
            "water_quality_trends", "Explain overall water quality trend distribution",
            "improving_sites_trend", "Explain trends in improving water quality sites",
            "regional_trend_comparison", "Compare water quality trends across regions"
        ));
        supportedTypes.put("unsupportedQuestionTypes", Map.of(
            "forecasting", "Predicting future values",
            "causation", "Claiming cause-and-effect relationships",
            "policy_recommendation", "Recommending policies",
            "site_specific_advice", "Providing site-specific water quality advice",
            "hypothetical", "What-if scenarios or counterfactuals"
        ));

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

