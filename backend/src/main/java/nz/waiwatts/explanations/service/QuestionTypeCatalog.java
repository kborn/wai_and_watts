package nz.waiwatts.explanations.service;

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
        DESCRIPTION_OVERRIDES.put("renewable_generation_trend", "Explain renewable generation trends between years");
        DESCRIPTION_OVERRIDES.put("fuel_type_comparison", "Compare two fuel types (e.g., hydro vs geothermal)");
        DESCRIPTION_OVERRIDES.put("generation_mix_overview", "Summarize main sources of electricity generation");
        DESCRIPTION_OVERRIDES.put("water_quality_overview", "Provide overview of water quality state distribution");
        DESCRIPTION_OVERRIDES.put("regional_water_quality", "Compare water quality across regions");
        DESCRIPTION_OVERRIDES.put("water_quality_trends", "Explain overall water quality trend distribution");
        DESCRIPTION_OVERRIDES.put("improving_sites_trend", "Explain trends in improving water quality sites");
        DESCRIPTION_OVERRIDES.put("regional_trend_comparison", "Compare water quality trends across regions");
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
        boolean allMbie = datasetSources.stream().allMatch(ds -> ds.startsWith("mbie."));
        if (allMbie) {
            return QuestionTypeGroup.MBIE;
        }

        boolean allLawaState = datasetSources.stream()
            .allMatch(ds -> ds.startsWith("lawa.") && ds.contains(".state."));
        if (allLawaState) {
            return QuestionTypeGroup.LAWA_STATE;
        }

        boolean allLawaTrend = datasetSources.stream()
            .allMatch(ds -> ds.startsWith("lawa.") && ds.contains(".trend."));
        if (allLawaTrend) {
            return QuestionTypeGroup.LAWA_TREND;
        }
        return QuestionTypeGroup.UNKNOWN;
    }
}
