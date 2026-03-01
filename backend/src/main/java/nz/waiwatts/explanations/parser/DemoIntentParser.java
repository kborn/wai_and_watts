package nz.waiwatts.explanations.parser;

import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.dto.ExplanationRequest;

import java.util.Map;

/**
 * Hardcoded demo intent parser for no-LLM-key mode.
 * <p>
 * Supports exactly the four Ask sample questions and refuses all others.
 */
public class DemoIntentParser implements IntentParser {

    private static final String Q_RENEWABLE_TREND = "explain renewable generation trends between 2020 and 2023";
    private static final String Q_GENERATION_MIX = "what are the main sources of electricity generation in new zealand";
    private static final String Q_FUEL_COMPARE = "compare hydro and geothermal generation patterns";
    private static final String Q_HYDRO_TREND = "explain hydro generation trends between 2018 and 2023";

    @Override
    public ExplanationRequest parseQuestion(String question) {
        if (question == null) {
            return null;
        }

        String normalized = normalizeQuestion(question);

        return switch (normalized) {
            case Q_RENEWABLE_TREND -> new ExplanationRequest(
                QuestionType.RENEWABLE_GENERATION_TREND.wireValue(),
                DatasetSource.MBIE_GENERATION_ANNUAL.wireValue(),
                Map.of(FilterKey.START_YEAR.wireValue(), 2020, FilterKey.END_YEAR.wireValue(), 2023)
            );
            case Q_GENERATION_MIX -> new ExplanationRequest(
                QuestionType.GENERATION_MIX_OVERVIEW.wireValue(),
                DatasetSource.MBIE_GENERATION_ANNUAL.wireValue(),
                null
            );
            case Q_FUEL_COMPARE -> new ExplanationRequest(
                QuestionType.FUEL_TYPE_COMPARISON.wireValue(),
                DatasetSource.MBIE_GENERATION_ANNUAL.wireValue(),
                Map.of(FilterKey.FUEL_TYPE.wireValue(), "HYDRO", FilterKey.FUEL_TYPE_B.wireValue(), "GEOTHERMAL")
            );
            case Q_HYDRO_TREND -> new ExplanationRequest(
                QuestionType.FUEL_GENERATION_TREND.wireValue(),
                DatasetSource.MBIE_GENERATION_ANNUAL.wireValue(),
                Map.of(
                    FilterKey.START_YEAR.wireValue(), 2018,
                    FilterKey.END_YEAR.wireValue(), 2023,
                    FilterKey.FUEL_TYPE.wireValue(), "HYDRO"
                )
            );
            default -> null;
        };
    }

    private String normalizeQuestion(String question) {
        String trimmed = question.trim().toLowerCase();
        String collapsed = trimmed.replaceAll("\\s+", " ");
        return collapsed.replaceAll("[\\?\\.]+$", "");
    }
}
