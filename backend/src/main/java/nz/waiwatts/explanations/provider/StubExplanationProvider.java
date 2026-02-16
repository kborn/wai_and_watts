package nz.waiwatts.explanations.provider;

import nz.waiwatts.explanations.dto.ClassificationFact;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
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
            case "water_quality_overview" ->
                    generateWaterQualityOverviewExplanation(factPack);
            case "excellent_sites_trend" ->
                    generateExcellentSitesTrendExplanation(factPack);
            case "regional_water_quality" ->
                    generateRegionalWaterQualityExplanation(factPack);
            case "water_quality_trends" ->
                    generateWaterQualityTrendsExplanation(factPack);
            case "improving_sites_trend" ->
                    generateImprovingSitesTrendExplanation(factPack);
            case "regional_trend_comparison" ->
                    generateRegionalTrendComparisonExplanation(factPack);
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

    private Explanation generateWaterQualityOverviewExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Based on the available water quality data, ");
        
        if (!factPack.getFacts().getClassifications().isEmpty()) {
            explanation.append("the distribution of water quality states shows ");
            
            // Summarize classification data
            var classifications = factPack.getFacts().getClassifications();
            explanation.append("multiple classification categories across monitoring sites. ");
            
            // Add metrics if available
            if (!factPack.getFacts().getMetrics().isEmpty()) {
                var metrics = factPack.getFacts().getMetrics();
                explanation.append("Approximately ");
                // Find excellent percentage metric
                var excellentMetric = metrics.stream()
                    .filter(m -> m.getId().contains("excellent"))
                    .findFirst();

                if (excellentMetric.isPresent()) {
                    explanation.append(excellentMetric.get().getValue())
                            .append("% of sites are classified as having excellent water quality, while ");
                }

                // Find poor percentage metric
                var poorMetric = metrics.stream()
                    .filter(m -> m.getId().contains("poor"))
                    .findFirst();
                
                if (poorMetric.isPresent()) {
                    explanation.append(poorMetric.get().getValue())
                        .append("% are classified as having poor water quality.");
                }
            }
            
            // Return explanation with citations
            List<String> citations = classifications.stream()
                .limit(3)
                .map(ClassificationFact::getId)
                .toList();
            
            return new Explanation(explanation.toString(), citations);
        }
        
        return Explanation.refusal("Insufficient water quality data for overview analysis");
    }

    private Explanation generateExcellentSitesTrendExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Excellent water quality sites show ");
        
        // Check if we have time series data
        if (!factPack.getFacts().getTimeSeries().isEmpty()) {
            var timeSeries = factPack.getFacts().getTimeSeries().getFirst();
            var points = timeSeries.getPoints();
            
            if (points.size() >= 2) {
                var first = points.getFirst();
                var last = points.getLast();

                if (last.getValue().compareTo(first.getValue()) > 0) {
                    explanation.append("an increasing trend ");
                } else {
                    explanation.append("a decreasing trend ");
                }
                
                explanation.append("from ")
                    .append(first.getValue().intValue())
                    .append(" sites in ")
                    .append(first.getPeriod())
                    .append(" to ")
                    .append(last.getValue().intValue())
                    .append(" sites in ")
                    .append(last.getPeriod())
                    .append(".");
            } else {
                explanation.append("limited trend data available.");
            }
            
            // Check for comparison data
            if (!factPack.getFacts().getComparisons().isEmpty()) {
                var comparison = factPack.getFacts().getComparisons().getFirst();
                explanation.append(" This represents a change of ")
                    .append(comparison.getDeltaAbsolute().intValue())
                    .append(" sites (")
                    .append(comparison.getDeltaPercent())
                    .append("%).");
            }
            
            return new Explanation(
                explanation.toString(),
                List.of(timeSeries.getId())
            );
        }
        
        return Explanation.refusal("Insufficient trend data for excellent sites analysis");
    }

    private Explanation generateRegionalWaterQualityExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Regional water quality analysis shows ");
        
        if (!factPack.getFacts().getClassifications().isEmpty()) {
            var classifications = factPack.getFacts().getClassifications();
            explanation.append("variations in water quality states across different regions. ");
            
            // Summarize metrics by region if available
            if (!factPack.getFacts().getMetrics().isEmpty()) {
                var metrics = factPack.getFacts().getMetrics();
                explanation.append("Several regions show differences in the percentage of sites with excellent water quality.");
                
                // Add citation for regional analysis
                var regionalMetrics = metrics.stream()
                    .filter(m -> m.getId().contains("excellent") && m.getId().split(":").length > 4)
                    .toList();
                
                if (!regionalMetrics.isEmpty()) {
                    explanation.append(" Regional analysis is available for ")
                        .append(regionalMetrics.size())
                        .append(" regions.");
                }
            }
            
            // Return explanation with regional citations
            List<String> citations = classifications.stream()
                .filter(c -> c.getDimensions() != null && c.getDimensions().containsKey("region"))
                .limit(3)
                .map(ClassificationFact::getId)
                .toList();
            
            return new Explanation(explanation.toString(), citations);
        }
        
        return Explanation.refusal("Insufficient regional water quality data for analysis");
    }

    private Explanation generateWaterQualityTrendsExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Water quality trend analysis shows ");
        
        if (!factPack.getFacts().getClassifications().isEmpty()) {
            var classifications = factPack.getFacts().getClassifications();
            explanation.append("the distribution of trend directions across monitoring sites. ");
            
            // Summarize metrics if available
            if (!factPack.getFacts().getMetrics().isEmpty()) {
                var metrics = factPack.getFacts().getMetrics();
                
                var improvingMetric = metrics.stream()
                    .filter(m -> m.getId().contains("improving"))
                    .findFirst();
                var degradingMetric = metrics.stream()
                    .filter(m -> m.getId().contains("degrading"))
                    .findFirst();
                
                if (improvingMetric.isPresent() && degradingMetric.isPresent()) {
                    explanation.append("Approximately ")
                        .append(improvingMetric.get().getValue())
                        .append("% of sites show improving trends, while ")
                        .append(degradingMetric.get().getValue())
                        .append("% show degrading trends.");
                }
                
                var avgScoreMetric = metrics.stream()
                    .filter(m -> m.getId().contains("average_trend_score"))
                    .findFirst();
                
                if (avgScoreMetric.isPresent()) {
                    explanation.append(" The average trend score is ")
                        .append(avgScoreMetric.get().getValue())
                        .append(".");
                }
            }
            
            // Return explanation with citations
            List<String> citations = classifications.stream()
                .limit(3)
                .map(ClassificationFact::getId)
                .toList();
            
            return new Explanation(explanation.toString(), citations);
        }
        
        return Explanation.refusal("Insufficient water quality trend data for analysis");
    }

    private Explanation generateImprovingSitesTrendExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Improving water quality sites show ");
        
        // Check if we have time series data
        if (!factPack.getFacts().getTimeSeries().isEmpty()) {
            var timeSeries = factPack.getFacts().getTimeSeries().getFirst();
            var points = timeSeries.getPoints();
            
            if (points.size() >= 2) {
                var first = points.getFirst();
                var last = points.getLast();

                if (last.getValue().compareTo(first.getValue()) > 0) {
                    explanation.append("an encouraging increase ");
                } else {
                    explanation.append("a concerning decrease ");
                }
                
                explanation.append("from ")
                    .append(first.getValue().intValue())
                    .append(" sites in ")
                    .append(first.getPeriod())
                    .append(" to ")
                    .append(last.getValue().intValue())
                    .append(" sites in ")
                    .append(last.getPeriod())
                    .append(".");
            } else {
                explanation.append("limited trend data available for improving sites.");
            }
            
            // Check for comparison data
            if (!factPack.getFacts().getComparisons().isEmpty()) {
                var comparison = factPack.getFacts().getComparisons().getFirst();
                explanation.append(" This represents a change of ")
                    .append(comparison.getDeltaAbsolute().intValue())
                    .append(" sites (")
                    .append(comparison.getDeltaPercent())
                    .append("%).");
            }
            
            return new Explanation(
                explanation.toString(),
                List.of(timeSeries.getId())
            );
        }
        
        return Explanation.refusal("Insufficient trend data for improving sites analysis");
    }

    private Explanation generateRegionalTrendComparisonExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Regional trend comparison shows ");
        
        if (!factPack.getFacts().getClassifications().isEmpty()) {
            var classifications = factPack.getFacts().getClassifications();
            explanation.append("variations in water quality trend patterns across different regions. ");
            
            // Summarize metrics by region if available
            if (!factPack.getFacts().getMetrics().isEmpty()) {
                var metrics = factPack.getFacts().getMetrics();
                explanation.append("Several regions show different percentages of improving water quality sites.");
                
                // Add citation for regional trend analysis
                var regionalMetrics = metrics.stream()
                    .filter(m -> m.getId().contains("improving") && m.getId().split(":").length > 4)
                    .toList();
                
                if (!regionalMetrics.isEmpty()) {
                    explanation.append(" Regional trend analysis is available for ")
                        .append(regionalMetrics.size())
                        .append(" regions.");
                }
            }
            
            // Return explanation with regional citations
            List<String> citations = classifications.stream()
                .filter(c -> c.getDimensions() != null && c.getDimensions().containsKey("region"))
                .limit(3)
                .map(ClassificationFact::getId)
                .toList();
            
            return new Explanation(explanation.toString(), citations);
        }
        
        return Explanation.refusal("Insufficient regional trend data for comparison analysis");
    }
}
