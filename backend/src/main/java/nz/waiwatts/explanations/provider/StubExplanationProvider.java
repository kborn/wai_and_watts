package nz.waiwatts.explanations.provider;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import org.springframework.stereotype.Component;
import nz.waiwatts.explanations.dto.MetricFact;

import java.util.Comparator;
import java.util.List;

/**
 * Stubbed/deterministic Explanation Provider for Phase 11 testing.
 * 
 * This provider returns deterministic responses based on the question type and Fact Pack content
 * without calling a real LLM. This allows testing the architecture, grounding, citations, 
 * and refusal behavior in Phase 11.
 */
@Component
public class StubExplanationProvider implements ExplanationProvider {

    @Override
    public Explanation generateExplanation(String questionType, FactPack factPack) {
        
        // Check if question type is supported
        if (factPack.getGuardrails().getAllowedClaims().isEmpty()) {
            return Explanation.refusal("Unsupported question type: " + questionType);
        }
        
        // Check if required facts are missing
        if (hasMissingFacts(factPack)) {
            return Explanation.refusal("Insufficient data to answer the question");
        }
        
        // Generate deterministic explanation based on question type
        return generateDeterministicExplanation(questionType, factPack);
    }

    public boolean validateCitations(Explanation explanation, FactPack factPack) {
        List<String> requiredCitations = factPack.getGuardrails().getRequiredCitations();
        List<String> actualCitations = explanation.getCitations();
        
        // Check if all required citations are present
        return requiredCitations.stream().allMatch(actualCitations::contains);
    }

    private boolean hasMissingFacts(FactPack factPack) {
        List<String> requiredCitations = factPack.getGuardrails().getRequiredCitations();
        
        // If we have required citations but no facts, that's missing facts
        return !requiredCitations.isEmpty() && 
               factPack.getFacts().getTimeSeries().isEmpty() && 
               factPack.getFacts().getMetrics().isEmpty() &&
               factPack.getFacts().getComparisons().isEmpty();
    }

    private Explanation generateDeterministicExplanation(String questionType, FactPack factPack) {
        return switch (questionType) {
            case "renewable_generation_trend" ->
                    generateRenewableGenerationTrendExplanation(factPack);
            case "hydro_generation_trend" ->
                    generateHydroGenerationTrendExplanation(factPack);
            case "fuel_type_comparison" ->
                    generateFuelTypeComparisonExplanation(factPack);
            default ->
                    Explanation.refusal("Question type not supported in Phase 11: " + questionType);
        };
    }

    private Explanation generateRenewableGenerationTrendExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Based on the available data, renewable electricity generation has shown ");
        
        if (!factPack.getFacts().getTimeSeries().isEmpty()) {
            var timeSeries = factPack.getFacts().getTimeSeries().getFirst();
            var points = timeSeries.getPoints();
            
            if (points.size() >= 2) {
                var first = points.getFirst();
                var last = points.getLast();

                if (last.getValue().compareTo(first.getValue()) > 0) {
                    explanation.append("an overall increasing trend ");
                } else {
                    explanation.append("an overall decreasing trend ");
                }
                
                explanation.append("from ")
                    .append(first.getValue())
                    .append(" GWh in ")
                    .append(first.getPeriod())
                    .append(" to ")
                    .append(last.getValue())
                    .append(" GWh in ")
                    .append(last.getPeriod())
                    .append(".");
            } else {
                explanation.append("limited trend data available.");
            }
            
            // Return explanation with citation
            return new Explanation(
                explanation.toString(),
                List.of(timeSeries.getId())
            );
        }
        
        return Explanation.refusal("Insufficient time series data for trend analysis");
    }

    private Explanation generateHydroGenerationTrendExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Hydroelectric generation data shows ");
        
        // Check if we have comparison data
        if (!factPack.getFacts().getComparisons().isEmpty()) {
            var comparison = factPack.getFacts().getComparisons().getFirst();
            explanation.append("a change of ")
                .append(comparison.getDeltaAbsolute())
                .append(" GWh (")
                .append(comparison.getDeltaPercent())
                .append("%) from ")
                .append(comparison.getBaselinePeriod())
                .append(" to ")
                .append(comparison.getComparisonPeriod())
                .append(".");
                
            return new Explanation(
                explanation.toString(),
                List.of(comparison.getId())
            );
        }
        
        // Fall back to time series if no comparison
        if (!factPack.getFacts().getTimeSeries().isEmpty()) {
            var timeSeries = factPack.getFacts().getTimeSeries().getFirst();
            explanation.append("the generation pattern over the available period.");
            
            return new Explanation(
                explanation.toString(),
                List.of(timeSeries.getId())
            );
        }
        
        return Explanation.refusal("Insufficient hydro generation data for analysis");
    }

    private Explanation generateFuelTypeComparisonExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Among the fuel types analyzed, ");
        
        if (!factPack.getFacts().getMetrics().isEmpty()) {
            var metrics = factPack.getFacts().getMetrics();
            
            // Find the fuel type with highest generation
            var maxMetric = metrics.stream()
                .max(Comparator.comparing(MetricFact::getValue));

            if (maxMetric.isPresent()) {
                var top = maxMetric.get();
                String fuelType = (String) top.getDimensions().get("fuel_type");
                
                explanation.append(fuelType)
                    .append(" had the highest electricity generation at ")
                    .append(top.getValue())
                    .append(" GWh in ")
                    .append(top.getPeriod())
                    .append(".");
                    
                // Create citations for all fuel types
                List<String> citations = metrics.stream()
                    .map(MetricFact::getId)
                    .toList();
                    
                return new Explanation(
                    explanation.toString(),
                    citations
                );
            }
        }
        
        return Explanation.refusal("Insufficient fuel type data for comparison");
    }
}