package nz.waiwatts.explanations.provider;

import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.dto.ClassificationFact;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.dto.MetricFact;
import nz.waiwatts.explanations.dto.TimeSeriesFact;
import nz.waiwatts.explanations.service.CitationValidationUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stubbed/deterministic Explanation Provider for Phase 11 testing.
 * <p>
 * This provider returns deterministic responses based on the question type and Fact Pack content
 * without calling a real LLM. This allows testing the architecture, grounding, citations, 
 * and refusal behavior in Phase 11. Citation validation uses the shared validation layer
 * so behavior stays aligned with live providers.
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
        return CitationValidationUtil.validateRequiredCitations(requiredCitations, actualCitations);
    }

    private boolean hasMissingFacts(FactPack factPack) {
        List<String> requiredCitations = factPack.getGuardrails().getRequiredCitations();
        
        // If we have required citations but no facts, that's missing facts
        return !requiredCitations.isEmpty() && 
               factPack.getFacts().getTimeSeries().isEmpty() && 
               factPack.getFacts().getMetrics().isEmpty() &&
               factPack.getFacts().getComparisons().isEmpty() &&
               factPack.getFacts().getClassifications().isEmpty();
    }

    private Explanation generateDeterministicExplanation(String questionType, FactPack factPack) {
        QuestionType parsedQuestionType = QuestionType.fromWireValue(questionType).orElse(null);
        if (parsedQuestionType == null) {
            return Explanation.refusal("Question type not supported in Phase 11: " + questionType);
        }
        return switch (parsedQuestionType) {
            case RENEWABLE_GENERATION_TREND ->
                    generateRenewableGenerationTrendExplanation(factPack);
            case FUEL_GENERATION_TREND ->
                    generateHydroGenerationTrendExplanation(factPack);
            case FUEL_TYPE_COMPARISON ->
                    generateFuelTypeComparisonExplanation(factPack);
            case GENERATION_MIX_OVERVIEW ->
                    generateGenerationMixOverviewExplanation(factPack);
            case WATER_QUALITY_OVERVIEW ->
                    generateWaterQualityOverviewExplanation(factPack);
            case WATER_QUALITY_STATE_SITES_TREND ->
                    generateExcellentSitesTrendExplanation(factPack);
            case REGIONAL_WATER_QUALITY ->
                    generateRegionalWaterQualityExplanation(factPack);
            case WATER_QUALITY_TRENDS ->
                    generateWaterQualityTrendsExplanation(factPack);
            case IMPROVING_SITES_TREND ->
                    generateImprovingSitesTrendExplanation(factPack);
            case REGIONAL_TREND_COMPARISON ->
                    generateRegionalTrendComparisonExplanation(factPack);
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
                    .append(" ")
                    .append(timeSeries.getUnit())
                    .append(" in ")
                    .append(first.getPeriod())
                    .append(" to ")
                    .append(last.getValue())
                    .append(" ")
                    .append(timeSeries.getUnit())
                    .append(" in ")
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

        // If time series exist (typically for specific fuel comparisons), compare trends
        if (!factPack.getFacts().getTimeSeries().isEmpty()) {
            var timeSeriesList = factPack.getFacts().getTimeSeries().stream()
                .sorted(Comparator.comparing(TimeSeriesFact::getId))
                .toList();

            if (timeSeriesList.size() >= 2) {
                var a = timeSeriesList.getFirst();
                var b = timeSeriesList.get(1);
                String fuelA = (String) a.getDimensions().getOrDefault("fuel_type", "FUEL_A");
                String fuelB = (String) b.getDimensions().getOrDefault("fuel_type", "FUEL_B");

                String trendA = trendDirection(a);
                String trendB = trendDirection(b);

                explanation.append(fuelA)
                    .append(" shows an ")
                    .append(trendA)
                    .append(" trend, while ")
                    .append(fuelB)
                    .append(" shows an ")
                    .append(trendB)
                    .append(" trend over the available period.");

                return new Explanation(
                    explanation.toString(),
                    List.of(a.getId(), b.getId())
                );
            }
        }
        
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
                    .append(" ")
                    .append(top.getUnit())
                    .append(" in ")
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

    private Explanation generateGenerationMixOverviewExplanation(FactPack factPack) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("In the most recent period available, ");

        var metrics = factPack.getFacts().getMetrics();
        if (metrics == null || metrics.isEmpty()) {
            return Explanation.refusal("Insufficient fuel type data for generation mix overview");
        }

        var sorted = metrics.stream()
            .sorted(Comparator.comparing(MetricFact::getValue).reversed())
            .toList();

        var top = sorted.getFirst();
        String topFuel = (String) top.getDimensions().get("fuel_type");
        explanation.append(topFuel)
            .append(" is the largest contributor at ")
            .append(top.getValue())
            .append(" ")
            .append(top.getUnit())
            .append(" in ")
            .append(top.getPeriod());

        if (sorted.size() >= 2) {
            var second = sorted.get(1);
            String secondFuel = (String) second.getDimensions().get("fuel_type");
            explanation.append(", followed by ")
                .append(secondFuel)
                .append(" at ")
                .append(second.getValue())
                .append(" ")
                .append(second.getUnit())
                .append(".");
        } else {
            explanation.append(".");
        }

        List<String> citations = sorted.stream()
            .map(MetricFact::getId)
            .toList();

        return new Explanation(
            explanation.toString(),
            citations
        );
    }

    private String trendDirection(TimeSeriesFact series) {
        var points = series.getPoints();
        if (points == null || points.size() < 2) {
            return "unclear";
        }
        var first = points.getFirst().getValue();
        var last = points.getLast().getValue();
        if (last.compareTo(first) > 0) {
            return "increasing";
        }
        if (last.compareTo(first) < 0) {
            return "decreasing";
        }
        return "stable";
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
            List<String> metricCitations = new ArrayList<>();
            if (!factPack.getFacts().getMetrics().isEmpty()) {
                var metrics = factPack.getFacts().getMetrics();
                explanation.append("Approximately ");
                // Find excellent percentage metric
                var excellentMetric = metrics.stream()
                    .filter(m -> m.getId().contains("excellent"))
                    .findFirst();

                excellentMetric.ifPresent(metricFact -> {
                    explanation.append(metricFact.getValue())
                        .append("% of sites are classified as having excellent water quality, while ");
                    metricCitations.add(metricFact.getId());
                });

                // Find poor percentage metric
                var poorMetric = metrics.stream()
                    .filter(m -> m.getId().contains("poor"))
                    .findFirst();

                poorMetric.ifPresent(metricFact -> {
                    explanation.append(metricFact.getValue())
                        .append("% are classified as having poor water quality.");
                    metricCitations.add(metricFact.getId());
                });
            }
            
            // Return explanation with citations
            List<String> citations = new ArrayList<>(classifications.stream()
                .limit(3)
                .map(ClassificationFact::getId)
                .toList());
            citations.addAll(metricCitations);
            
            return new Explanation(explanation.toString(), citations.stream().distinct().toList());
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

                avgScoreMetric.ifPresent(metricFact -> explanation.append(" The average trend score is ")
                        .append(metricFact.getValue())
                        .append("."));
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
