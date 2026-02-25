package nz.waiwatts.explanations.capabilities;

import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dataset.DatasetDescriptor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry of supported NL explanation capabilities.
 *
 * This is the single source of truth for:
 * - question types
 * - dataset compatibility
 * - per-question filters
 * - per-question metric types
 * - examples
 */
@Component
public class CapabilityRegistry {

    private static final String DATASET_SOURCE_FILTER = "datasetSource";
    private static final String METRIC_TYPE_FILTER = "metricType";

    private final DatasetCatalog datasetCatalog;
    private final Map<String, CapabilityDefinition> byQuestionType;
    private final Map<String, String> unsupportedQuestionTypes;

    public CapabilityRegistry(DatasetCatalog datasetCatalog) {
        this.datasetCatalog = datasetCatalog;
        this.byQuestionType = buildCapabilities();
        this.unsupportedQuestionTypes = Map.ofEntries(
            Map.entry("forecasting", "Predicting future values"),
            Map.entry("causation", "Claiming cause-and-effect relationships"),
            Map.entry("policy_recommendation", "Recommending policies"),
            Map.entry("site_specific_advice", "Providing site-specific environmental advice"),
            Map.entry("hypothetical", "What-if scenarios or counterfactuals")
        );
    }

    public boolean isSupportedQuestionType(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return false;
        }
        return byQuestionType.containsKey(questionType.trim());
    }

    public boolean isSupportedDatasetSource(String datasetSource) {
        return datasetCatalog.findBySource(datasetSource).isPresent();
    }

    public boolean isDatasetSupportedForQuestion(String questionType, String datasetSource) {
        CapabilityDefinition capability = get(questionType);
        if (capability == null || datasetSource == null || datasetSource.isBlank()) {
            return false;
        }
        return capability.datasetSources().contains(datasetSource.trim());
    }

    public boolean isFilterSupportedForQuestion(String questionType, String filterKey) {
        CapabilityDefinition capability = get(questionType);
        if (capability == null || filterKey == null || filterKey.isBlank()) {
            return false;
        }
        if (METRIC_TYPE_FILTER.equals(filterKey)) {
            return true;
        }
        return capability.supportedFilters().contains(filterKey.trim());
    }

    public boolean isMetricTypeSupportedForQuestion(String questionType, String metricType) {
        CapabilityDefinition capability = get(questionType);
        if (capability == null || metricType == null || metricType.isBlank()) {
            return false;
        }
        return capability.metricTypes().contains(metricType.trim());
    }

    public boolean isMetricTypeSupportedForQuestionAndDataset(
        String questionType,
        String datasetSource,
        String metricType
    ) {
        if (!isDatasetSupportedForQuestion(questionType, datasetSource)) {
            return false;
        }
        return isMetricTypeSupportedForQuestion(questionType, metricType);
    }

    public Optional<String> defaultMetricTypeForQuestion(String questionType) {
        CapabilityDefinition capability = get(questionType);
        if (capability == null) {
            return Optional.empty();
        }
        return Optional.of(capability.defaultMetricType());
    }

    public Set<String> supportedFiltersForQuestion(String questionType) {
        CapabilityDefinition capability = get(questionType);
        if (capability == null) {
            return Set.of();
        }
        return capability.supportedFilters();
    }

    public Set<String> supportedMetricTypesForQuestion(String questionType) {
        CapabilityDefinition capability = get(questionType);
        if (capability == null) {
            return Set.of();
        }
        return capability.metricTypes();
    }

    public Set<String> supportedDatasetSourcesForQuestion(String questionType) {
        CapabilityDefinition capability = get(questionType);
        if (capability == null) {
            return Set.of();
        }
        return capability.datasetSources();
    }

    public List<String> examplesForQuestion(String questionType) {
        CapabilityDefinition capability = get(questionType);
        if (capability == null) {
            return List.of();
        }
        return capability.examples();
    }

    public Set<String> getSupportedQuestionTypes() {
        return Set.copyOf(byQuestionType.keySet());
    }

    public Set<String> getSupportedDatasetSources() {
        return datasetCatalog.getDatasets().stream()
            .map(DatasetDescriptor::datasetSource)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getAllowedFilterKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>(byQuestionType.values().stream()
                .map(CapabilityDefinition::supportedFilters)
                .flatMap(Collection::stream)
                .toList());
        keys.add(METRIC_TYPE_FILTER);
        return keys;
    }

    public Set<String> getAllSupportedMetricTypes() {
        return byQuestionType.values().stream()
            .map(CapabilityDefinition::metricTypes)
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Map<String, String> supportedQuestionTypeDescriptions() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        byQuestionType.values().forEach(cap -> out.put(cap.questionType(), cap.description()));
        return out;
    }

    public Map<String, String> unsupportedQuestionTypeDescriptions() {
        return unsupportedQuestionTypes;
    }

    public Map<String, String> supportedDatasetSourceDescriptions() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        datasetCatalog.getDatasets().forEach(ds -> out.put(ds.datasetSource(), ds.displayName()));
        return out;
    }

    public Map<String, Object> toCapabilitiesResponse() {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("supportedQuestionTypes", supportedQuestionTypeDescriptions());
        response.put("unsupportedQuestionTypes", unsupportedQuestionTypeDescriptions());
        response.put("supportedDatasetSources", supportedDatasetSourceDescriptions());
        response.put("requiredFilters", Map.of(
            DATASET_SOURCE_FILTER,
            "Must specify a dataset source compatible with the selected question type."
        ));
        response.put("filterStructure", Map.of(
            DATASET_SOURCE_FILTER, "string (required)",
            "fuelType", "string (optional)",
            "fuelTypeB", "string (optional)",
            "indicator", "string (optional)",
            "stateCategory", "string (optional)",
            "region", "string (optional)",
            "trend", "string (optional)",
            "startYear", "integer (optional)",
            "endYear", "integer (optional)",
            METRIC_TYPE_FILTER, "string (optional)"
        ));

        response.put("metricTypes", byQuestionType.values().stream()
            .collect(Collectors.toMap(
                CapabilityDefinition::questionType,
                CapabilityDefinition::metricTypes,
                (a, b) -> a,
                LinkedHashMap::new
            )));

        response.put("examples", byQuestionType.values().stream()
            .collect(Collectors.toMap(
                CapabilityDefinition::questionType,
                CapabilityDefinition::examples,
                (a, b) -> a,
                LinkedHashMap::new
            )));

        List<Map<String, Object>> capabilities = byQuestionType.values().stream()
            .map(capability -> {
                List<String> requiredFilters = requiredFiltersForQuestion(capability.questionType());
                List<String> optionalFilters = capability.supportedFilters().stream()
                    .filter(filter -> !requiredFilters.contains(filter))
                    .sorted()
                    .toList();
                Map<String, Object> row = new LinkedHashMap<>();
                // Canonical UI-oriented capability contract
                row.put("intentId", capability.questionType());
                row.put("displayName", displayNameForQuestionType(capability.questionType()));
                row.put("questionType", capability.questionType());
                row.put("description", capability.description());
                row.put("supportedDatasets", capability.datasetSources());
                row.put("datasetSources", capability.datasetSources());
                row.put("requiredFilters", requiredFilters);
                row.put("optionalFilters", optionalFilters);
                row.put("supportedFilters", capability.supportedFilters());
                row.put("metricTypes", capability.metricTypes());
                row.put("defaultMetricType", capability.defaultMetricType());
                row.put("exampleTemplates", exampleTemplatesForQuestion(capability.questionType()));
                row.put("examples", capability.examples());
                return row;
            })
            .toList();
        response.put("capabilities", capabilities);

        List<Map<String, Object>> datasets = new ArrayList<>();
        for (DatasetDescriptor descriptor : datasetCatalog.getDatasets()) {
            datasets.add(Map.of(
                "datasetSource", descriptor.datasetSource(),
                "displayName", descriptor.displayName(),
                "description", descriptor.displayName(),
                "supportedQuestionTypes", descriptor.supportedQuestionTypes(),
                "supportedFilters", descriptor.supportedFilters()
            ));
        }
        response.put("datasets", datasets);

        return response;
    }

    private List<String> requiredFiltersForQuestion(String questionType) {
        if ("fuel_generation_trend".equals(questionType)) {
            return List.of("fuelType");
        }
        if ("fuel_type_comparison".equals(questionType)) {
            return List.of("fuelType", "fuelTypeB");
        }
        if ("water_quality_state_sites_trend".equals(questionType)) {
            return List.of("stateCategory");
        }
        return List.of();
    }

    private String displayNameForQuestionType(String questionType) {
        return switch (questionType) {
            case "renewable_generation_trend" -> "Renewable Generation Trend";
            case "fuel_generation_trend" -> "Fuel Generation Trend";
            case "fuel_type_comparison" -> "Fuel Type Comparison";
            case "generation_mix_overview" -> "Generation Mix Overview";
            case "water_quality_overview" -> "Water Quality Overview";
            case "water_quality_state_sites_trend" -> "Water Quality State Sites Trend";
            case "regional_water_quality" -> "Regional Water Quality Comparison";
            case "water_quality_trends" -> "Water Quality Trends";
            case "improving_sites_trend" -> "Improving Sites Trend";
            case "regional_trend_comparison" -> "Regional Trend Comparison";
            default -> questionType;
        };
    }

    private List<String> exampleTemplatesForQuestion(String questionType) {
        return switch (questionType) {
            case "fuel_generation_trend" -> List.of(
                "How has {fuelType} generation changed since {startYear}?",
                "Show {fuelType} generation trend between {startYear} and {endYear}."
            );
            case "fuel_type_comparison" -> List.of(
                "Compare {fuelType} and {fuelTypeB} generation between {startYear} and {endYear}.",
                "Which was higher in {endYear}: {fuelType} or {fuelTypeB}?"
            );
            case "water_quality_state_sites_trend" -> List.of(
                "How has the number of {stateCategory} sites changed over time?",
                "Track {stateCategory} sites for {indicator} in {region}."
            );
            case "renewable_generation_trend" -> List.of(
                "Explain renewable generation trends between {startYear} and {endYear}.",
                "What is the renewable share trend over time?"
            );
            case "generation_mix_overview" -> List.of(
                "Summarize generation mix for {endYear}.",
                "Show generation share by fuel type."
            );
            case "water_quality_overview" -> List.of(
                "Give a water quality overview for {region}.",
                "Summarize state distribution for {indicator}."
            );
            case "regional_water_quality" -> List.of(
                "Compare water quality states across regions for {indicator}.",
                "Which regions have the highest share of excellent sites?"
            );
            case "water_quality_trends" -> List.of(
                "Summarize water quality trends for {indicator}.",
                "What is the trend distribution in {region}?"
            );
            case "improving_sites_trend" -> List.of(
                "How has the number of improving sites changed in {region}?",
                "Track improving sites trend for {indicator}."
            );
            case "regional_trend_comparison" -> List.of(
                "Compare trend outcomes across regions for {indicator}.",
                "Which regions show the strongest improving trend?"
            );
            default -> List.of();
        };
    }

    public List<String> suggestedQuestionTypes(String currentQuestionType, String datasetSource) {
        return byQuestionType.values().stream()
            .filter(capability -> {
                if (datasetSource == null || datasetSource.isBlank()) {
                    return true;
                }
                return capability.datasetSources().contains(datasetSource);
            })
            .sorted(Comparator.comparing(CapabilityDefinition::questionType))
            .map(CapabilityDefinition::questionType)
            .filter(qt -> !qt.equals(currentQuestionType))
            .limit(4)
            .toList();
    }

    private CapabilityDefinition get(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return null;
        }
        return byQuestionType.get(questionType.trim());
    }

    private Map<String, CapabilityDefinition> buildCapabilities() {
        LinkedHashMap<String, CapabilityDefinition> capabilities = new LinkedHashMap<>();

        capabilities.put("renewable_generation_trend", new CapabilityDefinition(
            "renewable_generation_trend",
            "Explain renewable electricity trends over time",
            Set.of("mbie.generation.annual", "mbie.generation.quarterly"),
            Set.of("startYear", "endYear"),
            Set.of("generation_gwh", "renewable_share_pct"),
            "generation_gwh",
            List.of(
                "Explain renewable generation trends between 2020 and 2023.",
                "How has renewable share changed in quarterly data?"
            )
        ));
        capabilities.put("fuel_generation_trend", new CapabilityDefinition(
            "fuel_generation_trend",
            "Explain electricity generation trends for any fuel type",
            Set.of("mbie.generation.annual", "mbie.generation.quarterly"),
            Set.of("startYear", "endYear", "fuelType"),
            Set.of("generation_gwh"),
            "generation_gwh",
            List.of(
                "Explain hydro generation trends between 2018 and 2023.",
                "Show geothermal generation trend from 2015 to 2024."
            )
        ));
        capabilities.put("fuel_type_comparison", new CapabilityDefinition(
            "fuel_type_comparison",
            "Compare two MBIE fuel types",
            Set.of("mbie.generation.annual", "mbie.generation.quarterly"),
            Set.of("startYear", "endYear", "fuelType", "fuelTypeB"),
            Set.of("generation_gwh", "generation_share_pct"),
            "generation_gwh",
            List.of(
                "Compare hydro and geothermal generation patterns.",
                "Compare hydro and wind shares in the latest quarter."
            )
        ));
        capabilities.put("generation_mix_overview", new CapabilityDefinition(
            "generation_mix_overview",
            "Summarize generation mix by fuel type",
            Set.of("mbie.generation.annual", "mbie.generation.quarterly"),
            Set.of("startYear", "endYear"),
            Set.of("generation_gwh", "generation_share_pct"),
            "generation_gwh",
            List.of(
                "What are the main sources of electricity generation in New Zealand?",
                "Show the generation mix by share."
            )
        ));

        capabilities.put("water_quality_overview", new CapabilityDefinition(
            "water_quality_overview",
            "Summarize LAWA state distribution",
            Set.of("lawa.water_quality.state.multi_year"),
            Set.of("indicator", "region", "startYear", "endYear"),
            Set.of("site_percentage"),
            "site_percentage",
            List.of(
                "Give me an overview of current water quality state by indicator.",
                "How does water quality state look for Auckland?"
            )
        ));
        capabilities.put("water_quality_state_sites_trend", new CapabilityDefinition(
            "water_quality_state_sites_trend",
            "Track sites over time for any water quality state category",
            Set.of("lawa.water_quality.state.multi_year"),
            Set.of("stateCategory", "indicator", "region", "startYear", "endYear"),
            Set.of("site_count"),
            "site_count",
            List.of(
                "How has the count of excellent sites changed over time?",
                "Track POOR state-category sites for E. coli in Auckland."
            )
        ));
        capabilities.put("regional_water_quality", new CapabilityDefinition(
            "regional_water_quality",
            "Compare water quality state across regions",
            Set.of("lawa.water_quality.state.multi_year"),
            Set.of("indicator", "region", "startYear", "endYear"),
            Set.of("site_percentage"),
            "site_percentage",
            List.of(
                "Compare water quality state across regions for E. coli."
            )
        ));

        capabilities.put("water_quality_trends", new CapabilityDefinition(
            "water_quality_trends",
            "Summarize LAWA trend distribution",
            Set.of("lawa.water_quality.trend.multi_year"),
            Set.of("indicator", "region", "trend", "startYear", "endYear"),
            Set.of("site_percentage", "average_trend_score"),
            "site_percentage",
            List.of(
                "Explain overall water quality trend distribution.",
                "What is the average trend score for this region?"
            )
        ));
        capabilities.put("improving_sites_trend", new CapabilityDefinition(
            "improving_sites_trend",
            "Track improving sites over time",
            Set.of("lawa.water_quality.trend.multi_year"),
            Set.of("indicator", "region", "trend", "startYear", "endYear"),
            Set.of("site_count"),
            "site_count",
            List.of(
                "How has the number of improving sites changed?"
            )
        ));
        capabilities.put("regional_trend_comparison", new CapabilityDefinition(
            "regional_trend_comparison",
            "Compare trend outcomes across regions",
            Set.of("lawa.water_quality.trend.multi_year"),
            Set.of("indicator", "region", "trend", "startYear", "endYear"),
            Set.of("site_percentage"),
            "site_percentage",
            List.of(
                "Compare trend outcomes across regions for nitrate."
            )
        ));

        return capabilities;
    }

    public record CapabilityDefinition(
        String questionType,
        String description,
        Set<String> datasetSources,
        Set<String> supportedFilters,
        Set<String> metricTypes,
        String defaultMetricType,
        List<String> examples
    ) {}
}
