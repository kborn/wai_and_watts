package nz.waiwatts.explanations.parser;

import nz.waiwatts.explanations.dto.ExplanationRequest;

import java.util.Map;

/**
 * Hardcoded demo intent parser for no-LLM-key mode.
 *
 * Supports exactly the four Ask sample questions and refuses all others.
 */
public class HardcodedDemoIntentParser implements IntentParser {

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
                "renewable_generation_trend",
                "mbie.generation.annual",
                Map.of("startYear", 2020, "endYear", 2023)
            );
            case Q_GENERATION_MIX -> new ExplanationRequest(
                "generation_mix_overview",
                "mbie.generation.annual",
                null
            );
            case Q_FUEL_COMPARE -> new ExplanationRequest(
                "fuel_type_comparison",
                "mbie.generation.annual",
                Map.of("fuelType", "HYDRO", "fuelTypeB", "GEOTHERMAL")
            );
            case Q_HYDRO_TREND -> new ExplanationRequest(
                "hydro_generation_trend",
                "mbie.generation.annual",
                Map.of("startYear", 2018, "endYear", 2023, "fuelType", "HYDRO")
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
