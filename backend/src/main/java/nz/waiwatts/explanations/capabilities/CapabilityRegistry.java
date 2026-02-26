package nz.waiwatts.explanations.capabilities;

import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.MetricType;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
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
import java.util.function.Function;
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

    private static final FilterKey DATASET_SOURCE_FILTER = FilterKey.DATASET_SOURCE;
    private static final FilterKey METRIC_TYPE_FILTER = FilterKey.METRIC_TYPE;

    private final DatasetCatalog datasetCatalog;
    private final Map<QuestionType, CapabilityDefinition> byQuestionType;
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

    public Optional<QuestionType> parseQuestionType(String wireValue) {
        return QuestionType.fromWireValue(wireValue);
    }

    public Optional<DatasetSource> parseDatasetSource(String wireValue) {
        return DatasetSource.fromWireValue(wireValue);
    }

    public boolean isSupportedQuestionType(String questionType) {
        return parseQuestionType(questionType).isPresent();
    }

    public boolean isSupportedDatasetSource(String datasetSource) {
        if (datasetSource == null || datasetSource.isBlank()) {
            return false;
        }
        return parseDatasetSource(datasetSource).isPresent()
            && datasetCatalog.findBySource(datasetSource.trim()).isPresent();
    }

    public boolean isDatasetSupportedForQuestion(String questionType, String datasetSource) {
        CapabilityDefinition capability = get(questionType);
        Optional<DatasetSource> datasetSourceEnum = parseDatasetSource(datasetSource);
        return datasetSourceEnum.filter(source -> capability.datasetSources().contains(source)).isPresent();
    }

    public boolean isFilterSupportedForQuestion(String questionType, String filterKey) {
        CapabilityDefinition capability = get(questionType);
        Optional<FilterKey> filterKeyEnum = FilterKey.fromWireValue(filterKey);
        if (filterKeyEnum.isEmpty()) {
            return false;
        }
        if (METRIC_TYPE_FILTER == filterKeyEnum.get()) {
            return true;
        }
        return capability.supportedFilters().contains(filterKeyEnum.get());
    }

    public boolean isMetricTypeSupportedForQuestion(String questionType, String metricType) {
        CapabilityDefinition capability = get(questionType);
        Optional<MetricType> metricTypeEnum = MetricType.fromWireValue(metricType);
        return metricTypeEnum.filter(type -> capability.metricTypes().contains(type)).isPresent();
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
        return Optional.of(capability.defaultMetricType().wireValue());
    }

    public Set<String> supportedMetricTypesForQuestion(String questionType) {
        CapabilityDefinition capability = get(questionType);
        return toWireSet(capability.metricTypes(), MetricType::wireValue);
    }

    public Set<String> supportedDatasetSourcesForQuestion(String questionType) {
        CapabilityDefinition capability = get(questionType);
        return toWireSet(capability.datasetSources(), DatasetSource::wireValue);
    }

    public List<String> examplesForQuestion(String questionType) {
        CapabilityDefinition capability = get(questionType);
        return capability.examples();
    }

    public Set<String> getSupportedQuestionTypes() {
        return byQuestionType.keySet().stream()
            .map(QuestionType::wireValue)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getSupportedDatasetSources() {
        return byQuestionType.values().stream()
            .map(CapabilityDefinition::datasetSources)
            .flatMap(Collection::stream)
            .map(DatasetSource::wireValue)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getAllowedFilterKeys() {
        LinkedHashSet<String> keys = byQuestionType.values().stream()
            .map(CapabilityDefinition::supportedFilters)
            .flatMap(Collection::stream)
            .map(FilterKey::wireValue)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        keys.add(METRIC_TYPE_FILTER.wireValue());
        return keys;
    }

    public Set<String> getAllSupportedMetricTypes() {
        return byQuestionType.values().stream()
            .map(CapabilityDefinition::metricTypes)
            .flatMap(Collection::stream)
            .map(MetricType::wireValue)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Map<String, String> supportedQuestionTypeDescriptions() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        byQuestionType.values().forEach(cap -> out.put(cap.questionType().wireValue(), cap.description()));
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
            DATASET_SOURCE_FILTER.wireValue(),
            "Must specify a dataset source compatible with the selected question type."
        ));

        LinkedHashMap<String, String> filterStructure = getStringStringLinkedHashMap();
        response.put("filterStructure", filterStructure);
        response.put("suggestedValuesByToken", suggestedValuesByToken());

        response.put("metricTypes", byQuestionType.values().stream()
            .collect(Collectors.toMap(
                cap -> cap.questionType().wireValue(),
                cap -> toWireSet(cap.metricTypes(), MetricType::wireValue),
                (a, b) -> a,
                LinkedHashMap::new
            )));

        response.put("examples", byQuestionType.values().stream()
            .collect(Collectors.toMap(
                cap -> cap.questionType().wireValue(),
                CapabilityDefinition::examples,
                (a, b) -> a,
                LinkedHashMap::new
            )));

        List<Map<String, Object>> capabilities = byQuestionType.values().stream()
            .map(capability -> {
                List<String> requiredFilters = toWireList(capability.requiredFilters(), FilterKey::wireValue);
                List<String> optionalFilters = capability.supportedFilters().stream()
                    .filter(filter -> !capability.requiredFilters().contains(filter))
                    .map(FilterKey::wireValue)
                    .sorted()
                    .toList();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("intentId", capability.questionType().wireValue());
                row.put("displayName", capability.displayName());
                row.put("questionType", capability.questionType().wireValue());
                row.put("description", capability.description());
                row.put("supportedDatasets", toWireSet(capability.datasetSources(), DatasetSource::wireValue));
                row.put("datasetSources", toWireSet(capability.datasetSources(), DatasetSource::wireValue));
                row.put("requiredFilters", requiredFilters);
                row.put("optionalFilters", optionalFilters);
                row.put("supportedFilters", toWireSet(capability.supportedFilters(), FilterKey::wireValue));
                row.put("metricTypes", toWireSet(capability.metricTypes(), MetricType::wireValue));
                row.put("defaultMetricType", capability.defaultMetricType().wireValue());
                row.put("exampleTemplates", capability.exampleTemplates());
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

    private static LinkedHashMap<String, String> getStringStringLinkedHashMap() {
        LinkedHashMap<String, String> filterStructure = new LinkedHashMap<>();
        filterStructure.put(DATASET_SOURCE_FILTER.wireValue(), "string (required)");
        filterStructure.put(FilterKey.FUEL_TYPE.wireValue(), "string (optional)");
        filterStructure.put(FilterKey.FUEL_TYPE_B.wireValue(), "string (optional)");
        filterStructure.put(FilterKey.INDICATOR.wireValue(), "string (optional)");
        filterStructure.put(FilterKey.STATE_CATEGORY.wireValue(), "string (optional)");
        filterStructure.put(FilterKey.REGION.wireValue(), "string (optional)");
        filterStructure.put(FilterKey.TREND.wireValue(), "string (optional)");
        filterStructure.put(FilterKey.START_YEAR.wireValue(), "integer (optional)");
        filterStructure.put(FilterKey.END_YEAR.wireValue(), "integer (optional)");
        filterStructure.put(METRIC_TYPE_FILTER.wireValue(), "string (optional)");
        return filterStructure;
    }

    private Map<String, List<String>> suggestedValuesByToken() {
        LinkedHashMap<String, List<String>> out = new LinkedHashMap<>();
        out.put(FilterKey.FUEL_TYPE.wireValue(), List.of("wind", "solar", "hydro", "geothermal"));
        out.put(FilterKey.FUEL_TYPE_B.wireValue(), List.of("hydro", "wind", "solar", "coal"));
        out.put(FilterKey.STATE_CATEGORY.wireValue(), List.of("EXCELLENT", "GOOD", "FAIR", "POOR"));
        out.put(FilterKey.REGION.wireValue(), List.of("Auckland", "Canterbury", "Otago", "Waikato"));
        out.put(FilterKey.INDICATOR.wireValue(), List.of("E. coli", "Nitrate", "Ammoniacal nitrogen"));
        out.put(FilterKey.TREND.wireValue(), List.of("improving", "declining", "stable"));
        return out;
    }

    public List<String> suggestedQuestionTypes(String currentQuestionType, String datasetSource) {
        Optional<QuestionType> current = parseQuestionType(currentQuestionType);
        Optional<DatasetSource> dataset = parseDatasetSource(datasetSource);
        return byQuestionType.values().stream()
            .filter(capability -> {
                if (datasetSource == null || datasetSource.isBlank()) {
                    return true;
                }
                return dataset.filter(source -> capability.datasetSources().contains(source)).isPresent();
            })
            .sorted(Comparator.comparing(cap -> cap.questionType().wireValue()))
            .map(cap -> cap.questionType().wireValue())
            .filter(qt -> current.isEmpty() || !qt.equals(current.get().wireValue()))
            .limit(4)
            .toList();
    }

    private CapabilityDefinition get(String questionType) {
        return parseQuestionType(questionType)
            .map(byQuestionType::get)
            .orElse(null);
    }

    private Map<QuestionType, CapabilityDefinition> buildCapabilities() {
        LinkedHashMap<QuestionType, CapabilityDefinition> capabilities = new LinkedHashMap<>();

        capabilities.put(QuestionType.RENEWABLE_GENERATION_TREND, new CapabilityDefinition(
            QuestionType.RENEWABLE_GENERATION_TREND,
            "Renewable Generation Trend",
            "Explain renewable electricity trends over time",
            orderedSet(DatasetSource.MBIE_GENERATION_ANNUAL, DatasetSource.MBIE_GENERATION_QUARTERLY),
            orderedSet(FilterKey.START_YEAR, FilterKey.END_YEAR),
            List.of(),
            orderedSet(MetricType.GENERATION_GWH, MetricType.RENEWABLE_SHARE_PCT),
            MetricType.GENERATION_GWH,
            List.of(
                "Explain renewable generation trends between {startYear} and {endYear}.",
                "What is the renewable share trend over time?"
            ),
            List.of(
                "Explain renewable generation trends between 2020 and 2023.",
                "How has renewable share changed in quarterly data?"
            )
        ));
        capabilities.put(QuestionType.FUEL_GENERATION_TREND, new CapabilityDefinition(
            QuestionType.FUEL_GENERATION_TREND,
            "Fuel Generation Trend",
            "Explain electricity generation trends for any fuel type",
            orderedSet(DatasetSource.MBIE_GENERATION_ANNUAL, DatasetSource.MBIE_GENERATION_QUARTERLY),
            orderedSet(FilterKey.START_YEAR, FilterKey.END_YEAR, FilterKey.FUEL_TYPE),
            List.of(FilterKey.FUEL_TYPE),
            orderedSet(MetricType.GENERATION_GWH),
            MetricType.GENERATION_GWH,
            List.of(
                "How has {fuelType} generation changed since {startYear}?",
                "Show {fuelType} generation trend between {startYear} and {endYear}."
            ),
            List.of(
                "Explain hydro generation trends between 2018 and 2023.",
                "Show geothermal generation trend from 2015 to 2024."
            )
        ));
        capabilities.put(QuestionType.FUEL_TYPE_COMPARISON, new CapabilityDefinition(
            QuestionType.FUEL_TYPE_COMPARISON,
            "Fuel Type Comparison",
            "Compare two MBIE fuel types",
            orderedSet(DatasetSource.MBIE_GENERATION_ANNUAL, DatasetSource.MBIE_GENERATION_QUARTERLY),
            orderedSet(FilterKey.START_YEAR, FilterKey.END_YEAR, FilterKey.FUEL_TYPE, FilterKey.FUEL_TYPE_B),
            List.of(FilterKey.FUEL_TYPE, FilterKey.FUEL_TYPE_B),
            orderedSet(MetricType.GENERATION_GWH, MetricType.GENERATION_SHARE_PCT),
            MetricType.GENERATION_GWH,
            List.of(
                "Compare {fuelType} and {fuelTypeB} generation between {startYear} and {endYear}.",
                "Which was higher in {endYear}: {fuelType} or {fuelTypeB}?"
            ),
            List.of(
                "Compare hydro and geothermal generation patterns.",
                "Compare hydro and wind shares in the latest quarter."
            )
        ));
        capabilities.put(QuestionType.GENERATION_MIX_OVERVIEW, new CapabilityDefinition(
            QuestionType.GENERATION_MIX_OVERVIEW,
            "Generation Mix Overview",
            "Summarize generation mix by fuel type",
            orderedSet(DatasetSource.MBIE_GENERATION_ANNUAL, DatasetSource.MBIE_GENERATION_QUARTERLY),
            orderedSet(FilterKey.START_YEAR, FilterKey.END_YEAR),
            List.of(),
            orderedSet(MetricType.GENERATION_GWH, MetricType.GENERATION_SHARE_PCT),
            MetricType.GENERATION_GWH,
            List.of(
                "Summarize generation mix for {endYear}.",
                "Show generation share by fuel type."
            ),
            List.of(
                "What are the main sources of electricity generation in New Zealand?",
                "Show the generation mix by share."
            )
        ));

        capabilities.put(QuestionType.WATER_QUALITY_OVERVIEW, new CapabilityDefinition(
            QuestionType.WATER_QUALITY_OVERVIEW,
            "Water Quality Overview",
            "Summarize LAWA state distribution",
            orderedSet(DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR),
            orderedSet(FilterKey.INDICATOR, FilterKey.REGION, FilterKey.START_YEAR, FilterKey.END_YEAR),
            List.of(),
            orderedSet(MetricType.SITE_PERCENTAGE),
            MetricType.SITE_PERCENTAGE,
            List.of(
                "Give a water quality overview for {region}.",
                "Summarize state distribution for {indicator}."
            ),
            List.of(
                "Give me an overview of current water quality state by indicator.",
                "How does water quality state look for Auckland?"
            )
        ));
        capabilities.put(QuestionType.WATER_QUALITY_STATE_SITES_TREND, new CapabilityDefinition(
            QuestionType.WATER_QUALITY_STATE_SITES_TREND,
            "Water Quality State Sites Trend",
            "Track sites over time for any water quality state category",
            orderedSet(DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR),
            orderedSet(FilterKey.STATE_CATEGORY, FilterKey.INDICATOR, FilterKey.REGION, FilterKey.START_YEAR, FilterKey.END_YEAR),
            List.of(FilterKey.STATE_CATEGORY),
            orderedSet(MetricType.SITE_COUNT),
            MetricType.SITE_COUNT,
            List.of(
                "How has the number of {stateCategory} sites changed over time?",
                "Track {stateCategory} sites for {indicator} in {region}."
            ),
            List.of(
                "How has the count of excellent sites changed over time?",
                "Track POOR state-category sites for E. coli in Auckland."
            )
        ));
        capabilities.put(QuestionType.REGIONAL_WATER_QUALITY, new CapabilityDefinition(
            QuestionType.REGIONAL_WATER_QUALITY,
            "Regional Water Quality Comparison",
            "Compare water quality state across regions",
            orderedSet(DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR),
            orderedSet(FilterKey.INDICATOR, FilterKey.REGION, FilterKey.START_YEAR, FilterKey.END_YEAR),
            List.of(),
            orderedSet(MetricType.SITE_PERCENTAGE),
            MetricType.SITE_PERCENTAGE,
            List.of(
                "Compare water quality states across regions for {indicator}.",
                "Which regions have the highest share of excellent sites?"
            ),
            List.of(
                "Compare water quality state across regions for E. coli."
            )
        ));

        capabilities.put(QuestionType.WATER_QUALITY_TRENDS, new CapabilityDefinition(
            QuestionType.WATER_QUALITY_TRENDS,
            "Water Quality Trends",
            "Summarize LAWA trend distribution",
            orderedSet(DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR),
            orderedSet(FilterKey.INDICATOR, FilterKey.REGION, FilterKey.TREND, FilterKey.START_YEAR, FilterKey.END_YEAR),
            List.of(),
            orderedSet(MetricType.SITE_PERCENTAGE, MetricType.AVERAGE_TREND_SCORE),
            MetricType.SITE_PERCENTAGE,
            List.of(
                "Summarize water quality trends for {indicator}.",
                "What is the trend distribution in {region}?"
            ),
            List.of(
                "Explain overall water quality trend distribution.",
                "What is the average trend score for this region?"
            )
        ));
        capabilities.put(QuestionType.IMPROVING_SITES_TREND, new CapabilityDefinition(
            QuestionType.IMPROVING_SITES_TREND,
            "Improving Sites Trend",
            "Track improving sites over time",
            orderedSet(DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR),
            orderedSet(FilterKey.INDICATOR, FilterKey.REGION, FilterKey.TREND, FilterKey.START_YEAR, FilterKey.END_YEAR),
            List.of(),
            orderedSet(MetricType.SITE_COUNT),
            MetricType.SITE_COUNT,
            List.of(
                "How has the number of improving sites changed in {region}?",
                "Track improving sites trend for {indicator}."
            ),
            List.of(
                "How has the number of improving sites changed?"
            )
        ));
        capabilities.put(QuestionType.REGIONAL_TREND_COMPARISON, new CapabilityDefinition(
            QuestionType.REGIONAL_TREND_COMPARISON,
            "Regional Trend Comparison",
            "Compare trend outcomes across regions",
            orderedSet(DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR),
            orderedSet(FilterKey.INDICATOR, FilterKey.REGION, FilterKey.TREND, FilterKey.START_YEAR, FilterKey.END_YEAR),
            List.of(),
            orderedSet(MetricType.SITE_PERCENTAGE),
            MetricType.SITE_PERCENTAGE,
            List.of(
                "Compare trend outcomes across regions for {indicator}.",
                "Which regions show the strongest improving trend?"
            ),
            List.of(
                "Compare trend outcomes across regions for nitrate."
            )
        ));

        return capabilities;
    }

    @SafeVarargs
    private static <T> LinkedHashSet<T> orderedSet(T... values) {
        return new LinkedHashSet<>(List.of(values));
    }

    private static <T> Set<String> toWireSet(Collection<T> values, Function<T, String> mapper) {
        return values.stream()
            .map(mapper)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <T> List<String> toWireList(List<T> values, Function<T, String> mapper) {
        return values.stream().map(mapper).toList();
    }

    public record CapabilityDefinition(
        QuestionType questionType,
        String displayName,
        String description,
        Set<DatasetSource> datasetSources,
        Set<FilterKey> supportedFilters,
        List<FilterKey> requiredFilters,
        Set<MetricType> metricTypes,
        MetricType defaultMetricType,
        List<String> exampleTemplates,
        List<String> examples
    ) {}
}
