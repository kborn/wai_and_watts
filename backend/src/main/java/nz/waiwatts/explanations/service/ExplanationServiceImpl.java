package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.provider.ExplanationProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;

/**
 * Implementation of Explanation Service that orchestrates the explanation generation process.
 */
@Service
public class ExplanationServiceImpl implements ExplanationService {

    private final List<FactPackBuilder> factPackBuilders;
    private final ExplanationProvider explanationProvider;

    public ExplanationServiceImpl(List<FactPackBuilder> factPackBuilders, ExplanationProvider explanationProvider) {
        if (factPackBuilders == null) {
            throw new NullPointerException("FactPack builders list cannot be null");
        }
        if (explanationProvider == null) {
            throw new NullPointerException("ExplanationProvider cannot be null");
        }
        this.factPackBuilders = factPackBuilders;
        this.explanationProvider = explanationProvider;
    }

    @Override
    public Explanation generateExplanation(ExplanationRequest request) {
        // Validate request structure first
        String validationError = validateRequest(request);
        if (validationError != null) {
            return Explanation.refusal(validationError);
        }

        // Select appropriate Fact Pack Builder
        FactPackBuilder builder = selectFactPackBuilder(request);

        if (builder == null) {
            return Explanation.refusal("No data source available for this request");
        }

        // Generate Fact Pack
        FactPack factPack = builder.buildFactPack(request);

        // Handle null Fact Pack from builder
        if (factPack == null) {
            throw new NullPointerException("FactPack builder returned null");
        }

        // Generate explanation using provider (question derived from questionType)
        Explanation explanation = explanationProvider.generateExplanation(request.getQuestionType(), factPack);

        // Handle null explanation from provider
        if (explanation == null) {
            throw new NullPointerException("ExplanationProvider returned null");
        }

        // Validate citations
        if (!explanation.isRefusal() && !explanationProvider.validateCitations(explanation, factPack)) {
            return Explanation.refusal("Generated explanation missing required citations");
        }

        return explanation;
    }

    /**
     * Validate request structure and constraints
     */
    private String validateRequest(ExplanationRequest request) {
        if (request == null) {
            return "Invalid request: request cannot be null";
        }

        String questionType = request.getQuestionType();
        Map<String, Object> filters = request.getFilters();

        // Validate question type
        if (questionType == null || questionType.trim().isEmpty()) {
            return "Invalid request: questionType is required";
        }

        // Validate dataset source
        if (filters == null) {
            return "Invalid request: filters are required";
        }

        String datasetSource = (String) filters.get("datasetSource");
        if (datasetSource == null || datasetSource.trim().isEmpty()) {
            return "Invalid request: datasetSource filter is required";
        }

        // Validate question type and dataset source compatibility
        if (!isValidQuestionDatasetCombination(questionType, datasetSource)) {
            return String.format("Invalid combination: questionType '%s' is not supported with datasetSource '%s'", 
                questionType, datasetSource);
        }

        // Validate time range filters (if present)
        String timeRangeError = validateTimeRangeFilters(filters);
        if (timeRangeError != null) {
            return timeRangeError;
        }

        return null; // No validation errors
    }

    /**
     * Check if question type is compatible with dataset source
     */
    private boolean isValidQuestionDatasetCombination(String questionType, String datasetSource) {
        // Define valid combinations
        List<String> mbieAnnualQuestions = List.of(
            "renewable_generation_trend", 
            "hydro_generation_trend", 
            "fuel_type_comparison"
        );

        List<String> mbieQuarterlyQuestions = List.of(
            "renewable_generation_trend", 
            "hydro_generation_trend", 
            "fuel_type_comparison"
        );

        List<String> lawaQuestions = List.of(
            "water_quality_trend",
            "water_quality_comparison"
        );

        return switch (datasetSource) {
            case "mbie.generation.annual" -> mbieAnnualQuestions.contains(questionType);
            case "mbie.generation.quarterly" -> mbieQuarterlyQuestions.contains(questionType);
            case "lawa.water_quality.state.multi_year" -> lawaQuestions.contains(questionType);
            case "lawa.water_quality.trend.multi_year" -> lawaQuestions.contains(questionType);
            default -> false;
        };
    }

    /**
     * Validate time range filters (startYear, endYear)
     */
    private String validateTimeRangeFilters(Map<String, Object> filters) {
        Object startYear = filters.get("startYear");
        Object endYear = filters.get("endYear");

        if (startYear != null && endYear != null) {
            try {
                int start = Integer.parseInt(startYear.toString());
                int end = Integer.parseInt(endYear.toString());

                if (start >= end) {
                    return String.format("Invalid time range: startYear (%d) must be before endYear (%d)", start, end);
                }

                // Validate reasonable bounds (e.g., no future data, reasonable historical range)
                int currentYear = java.time.Year.now().getValue();
                if (end > currentYear) {
                    return String.format("Invalid time range: endYear (%d) cannot be in the future", end);
                }

                if (end < 1990 || start < 1990) {
                    return String.format("Invalid time range: years before 1990 are not supported");
                }

            } catch (NumberFormatException e) {
                return "Invalid time range: startYear and endYear must be valid integers";
            }
        }

        return null; // No time range validation errors
    }

    private FactPackBuilder selectFactPackBuilder(ExplanationRequest request) {
        return factPackBuilders.stream()
            .filter(builder -> builder.canHandle(request))
            .findFirst()
            .orElse(null);
    }
}