package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dataset.DatasetCatalog;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    private static final Map<String, String> SUPPORTED_DESCRIPTIONS = new LinkedHashMap<>();
    private static final Map<String, String> UNSUPPORTED_DESCRIPTIONS = Map.of(
        "forecasting", "Predicting future values",
        "causation", "Claiming cause-and-effect relationships",
        "policy_recommendation", "Recommending policies",
        "site_specific_advice", "Providing site-specific water quality advice",
        "hypothetical", "What-if scenarios or counterfactuals"
    );

    static {
        SUPPORTED_DESCRIPTIONS.put("renewable_generation_trend", "Explain renewable generation trends between years");
        SUPPORTED_DESCRIPTIONS.put("hydro_generation_trend", "Explain hydro generation trends between years");
        SUPPORTED_DESCRIPTIONS.put("fuel_type_comparison", "Compare two fuel types (e.g., hydro vs geothermal)");
        SUPPORTED_DESCRIPTIONS.put("generation_mix_overview", "Summarize main sources of electricity generation");
        SUPPORTED_DESCRIPTIONS.put("water_quality_overview", "Provide overview of water quality state distribution");
        SUPPORTED_DESCRIPTIONS.put("excellent_sites_trend", "Explain trends in excellent water quality sites");
        SUPPORTED_DESCRIPTIONS.put("regional_water_quality", "Compare water quality across regions");
        SUPPORTED_DESCRIPTIONS.put("water_quality_trends", "Explain overall water quality trend distribution");
        SUPPORTED_DESCRIPTIONS.put("improving_sites_trend", "Explain trends in improving water quality sites");
        SUPPORTED_DESCRIPTIONS.put("regional_trend_comparison", "Compare water quality trends across regions");
    }

    private final Set<String> supportedQuestionTypes;

    public QuestionTypeCatalog(DatasetCatalog datasetCatalog) {
        this.supportedQuestionTypes = datasetCatalog.getDatasets().stream()
            .flatMap(ds -> ds.supportedQuestionTypes().stream())
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        Set<String> missingDescriptions = new LinkedHashSet<>(supportedQuestionTypes);
        missingDescriptions.removeAll(SUPPORTED_DESCRIPTIONS.keySet());
        if (!missingDescriptions.isEmpty()) {
            throw new IllegalStateException(
                "Missing question type descriptions for: " + missingDescriptions
            );
        }
    }

    public boolean isSupported(String questionType) {
        return questionType != null && supportedQuestionTypes.contains(questionType);
    }

    public Map<String, String> supportedDescriptions() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String questionType : SUPPORTED_DESCRIPTIONS.keySet()) {
            if (supportedQuestionTypes.contains(questionType)) {
                out.put(questionType, SUPPORTED_DESCRIPTIONS.get(questionType));
            }
        }
        return out;
    }

    public Map<String, String> unsupportedDescriptions() {
        return UNSUPPORTED_DESCRIPTIONS;
    }

    public QuestionTypeGroup groupFor(String questionType) {
        if (questionType == null) {
            return QuestionTypeGroup.UNKNOWN;
        }
        return switch (questionType) {
            case "renewable_generation_trend",
                 "hydro_generation_trend",
                 "fuel_type_comparison",
                 "generation_mix_overview" -> QuestionTypeGroup.MBIE;
            case "water_quality_overview",
                 "excellent_sites_trend",
                 "regional_water_quality" -> QuestionTypeGroup.LAWA_STATE;
            case "water_quality_trends",
                 "improving_sites_trend",
                 "regional_trend_comparison" -> QuestionTypeGroup.LAWA_TREND;
            default -> QuestionTypeGroup.UNKNOWN;
        };
    }
}

