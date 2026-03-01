package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dataset.DatasetDescriptor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class QuestionTypeCatalog {

    public enum QuestionTypeGroup {
        MBIE,
        LAWA_STATE,
        LAWA_TREND,
        UNKNOWN
    }

    private static final Map<String, String> DESCRIPTION_OVERRIDES = new LinkedHashMap<>();

    static {
        DESCRIPTION_OVERRIDES.put(QuestionType.RENEWABLE_GENERATION_TREND.wireValue(), "Explain renewable generation trends between years");
        DESCRIPTION_OVERRIDES.put(QuestionType.FUEL_TYPE_COMPARISON.wireValue(), "Compare two fuel types (e.g., hydro vs geothermal)");
        DESCRIPTION_OVERRIDES.put(QuestionType.GENERATION_MIX_OVERVIEW.wireValue(), "Summarize main sources of electricity generation");
        DESCRIPTION_OVERRIDES.put(QuestionType.WATER_QUALITY_OVERVIEW.wireValue(), "Provide overview of water quality state distribution");
        DESCRIPTION_OVERRIDES.put(QuestionType.GUIDELINE_EXCEEDANCE_SITES.wireValue(), "List sites with guideline exceedances in the latest available year");
        DESCRIPTION_OVERRIDES.put(QuestionType.REGIONAL_WATER_QUALITY.wireValue(), "Compare water quality across regions");
        DESCRIPTION_OVERRIDES.put(QuestionType.WATER_QUALITY_TRENDS.wireValue(), "Explain overall water quality trend distribution");
        DESCRIPTION_OVERRIDES.put(QuestionType.IMPROVING_SITES_TREND.wireValue(), "Explain trends in improving water quality sites");
        DESCRIPTION_OVERRIDES.put(QuestionType.REGIONAL_TREND_COMPARISON.wireValue(), "Compare water quality trends across regions");
    }

    private final Set<String> supportedQuestionTypes;
    private final Map<String, QuestionTypeGroup> groupsByQuestionType;

    public QuestionTypeCatalog(DatasetCatalog datasetCatalog) {
        List<DatasetDescriptor> datasets = datasetCatalog.getDatasets();
        this.supportedQuestionTypes = datasetCatalog.getDatasets().stream()
            .flatMap(ds -> ds.supportedQuestionTypes().stream())
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        this.groupsByQuestionType = deriveGroups(datasets);
    }

    public Map<String, String> supportedDescriptions() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String questionType : supportedQuestionTypes.stream().sorted().toList()) {
            out.put(questionType, descriptionFor(questionType));
        }
        return out;
    }

    public QuestionTypeGroup groupFor(String questionType) {
        if (questionType == null) {
            return QuestionTypeGroup.UNKNOWN;
        }
        return groupsByQuestionType.getOrDefault(questionType, QuestionTypeGroup.UNKNOWN);
    }

    private String descriptionFor(String questionType) {
        String override = DESCRIPTION_OVERRIDES.get(questionType);
        if (override != null && !override.isBlank()) {
            return override;
        }
        return "Explain " + humanize(questionType) + ".";
    }

    private String humanize(String questionType) {
        return questionType.replace('_', ' ');
    }

    private Map<String, QuestionTypeGroup> deriveGroups(List<DatasetDescriptor> datasets) {
        Map<String, Set<String>> sourcesByQuestionType = new LinkedHashMap<>();
        for (DatasetDescriptor descriptor : datasets) {
            for (String questionType : descriptor.supportedQuestionTypes()) {
                sourcesByQuestionType
                    .computeIfAbsent(questionType, k -> new LinkedHashSet<>())
                    .add(descriptor.datasetSource());
            }
        }

        Map<String, QuestionTypeGroup> groups = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : sourcesByQuestionType.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList()) {
            groups.put(entry.getKey(), classifyGroup(entry.getValue()));
        }
        return groups;
    }

    private QuestionTypeGroup classifyGroup(Set<String> datasetSources) {
        if (datasetSources == null || datasetSources.isEmpty()) {
            return QuestionTypeGroup.UNKNOWN;
        }
        boolean allMbie = datasetSources.stream().allMatch(ds ->
            ds.equals(DatasetSource.MBIE_GENERATION_ANNUAL.wireValue())
                || ds.equals(DatasetSource.MBIE_GENERATION_QUARTERLY.wireValue())
        );
        if (allMbie) {
            return QuestionTypeGroup.MBIE;
        }

        boolean allLawaState = datasetSources.stream()
            .allMatch(ds -> ds.equals(DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue()));
        if (allLawaState) {
            return QuestionTypeGroup.LAWA_STATE;
        }

        boolean allLawaTrend = datasetSources.stream()
            .allMatch(ds -> ds.equals(DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue()));
        if (allLawaTrend) {
            return QuestionTypeGroup.LAWA_TREND;
        }
        return QuestionTypeGroup.UNKNOWN;
    }
}
