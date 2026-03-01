package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.dto.*;
import nz.waiwatts.lawa.LawaBindingNormalization;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fact Pack Builder for LAWA Trend Multi-Year data
 */
public class LawaTrendMultiYearFactPackBuilder implements FactPackBuilder {

    private final LawaTrendMultiYearRecordRepository repository;

    // Centralized improving trend classifications for LAWA water quality
    private static final Set<String> IMPROVING_TRENDS = Set.of("IMPROVING");
    private static final Set<String> DEGRADING_TRENDS = Set.of("DEGRADING");
    private static final int REGIONAL_SAMPLE_K = 2;
    private static final String LAWA_TREND_DATASET = DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue();

    public LawaTrendMultiYearFactPackBuilder(LawaTrendMultiYearRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public FactPack buildFactPack(ExplanationRequest request) {
        FactPack factPack = FactPackBuilderSupport.initializeFactPack(request, LAWA_TREND_DATASET);

        // Get records for facts and derive provenance from them (Phase 11 acceptable)
        List<LawaTrendMultiYearRecord> records = FactPackBuilderSupport.pinToCanonicalRelease(
            getRecordsForRequest(request),
            LawaTrendMultiYearRecord::getDatasetRelease
        );
        factPack.getProvenance().setDatasetSources(
            FactPackBuilderSupport.buildGroupedProvenance(
                records,
                LAWA_TREND_DATASET,
                LawaTrendMultiYearRecord::getDatasetRelease,
                this::getPeriodCoverage
            )
        );

        // Build facts based on question type
        buildFacts(factPack, request, records);

        // Set guardrails based on question type
        setGuardrails(factPack, request);

        return factPack;
    }

    @Override
    public String getSupportedDatasetSourceCode() {
        return LAWA_TREND_DATASET;
    }

    private List<LawaTrendMultiYearRecord> getRecordsForRequest(ExplanationRequest request) {
        Map<String, Object> filters = request != null ? request.getFilters() : null;
        Integer startYear = null;
        Integer endYear = null;
        if (filters != null && !filters.isEmpty()) {
            try {
                Object s = filters.get(FilterKey.START_YEAR.wireValue());
                Object e = filters.get(FilterKey.END_YEAR.wireValue());
                if (s != null) startYear = Integer.parseInt(s.toString());
                if (e != null) endYear = Integer.parseInt(e.toString());
            } catch (NumberFormatException ignore) {
                // Leave bounds null; service-level validation already handles bad inputs
            }
        }

        String indicatorFilter = null;
        if (filters != null && !filters.isEmpty()) {
            Object indObj = filters.get(FilterKey.INDICATOR.wireValue());
            if (indObj instanceof String str && !str.isBlank()) {
                indicatorFilter = LawaBindingNormalization.normalizeTrendIndicatorForQuery(str);
            }
        }

        String regionFilter = null;
        if (filters != null && !filters.isEmpty()) {
            Object regObj = filters.get(FilterKey.REGION.wireValue());
            if (regObj instanceof String str && !str.isBlank()) {
                regionFilter = LawaBindingNormalization.normalizeRegionForQuery(str);
            }
        }

        String trendFilter = null;
        if (filters != null && !filters.isEmpty()) {
            Object trendObj = filters.get(FilterKey.TREND.wireValue());
            if (trendObj instanceof String str && !str.isBlank()) {
                trendFilter = str.trim().toLowerCase(Locale.ROOT);
            }
        }

        return repository.findForAsk(startYear, endYear, indicatorFilter, regionFilter, trendFilter);
    }

    private void buildFacts(FactPack factPack, ExplanationRequest request, List<LawaTrendMultiYearRecord> records) {
        QuestionType questionType = QuestionType.fromWireValue(request.getQuestionType()).orElse(null);
        if (questionType == null) {
            buildBasicFacts(factPack, records);
            return;
        }

        switch (questionType) {
            case WATER_QUALITY_TRENDS:
                buildWaterQualityTrendsFacts(factPack, records);
                break;
            case IMPROVING_SITES_TREND:
                buildImprovingSitesTrendFacts(factPack, records);
                break;
            case REGIONAL_TREND_COMPARISON:
                buildRegionalTrendComparisonFacts(factPack, records);
                break;
            default:
                // Build basic facts for unsupported question types (will result in refusal)
                buildBasicFacts(factPack, records);
                break;
        }
    }

    private void buildWaterQualityTrendsFacts(FactPack factPack, List<LawaTrendMultiYearRecord> records) {
        // Overall trend distribution across all sites and indicators
        Map<String, Long> trendDistribution = records.stream()
            .collect(Collectors.groupingBy(
                LawaTrendMultiYearRecord::getTrendNorm,
                Collectors.counting()
            ));

        // Create classification facts for trend categories
        trendDistribution.entrySet().stream()
            .sorted(Map.Entry.comparingByKey()) // deterministic ordering
            .forEach(entry -> {
                String trend = entry.getKey();

                ClassificationFact classification = new ClassificationFact(
                    "class:lawa:water_quality_trend:" + trend,
                    "water_quality_trend",
                    "water_quality_trend",
                    trend,
                    null, // periodStartYear
                    null, // periodEndYear
                    Map.of("scope", "NZ", "dataset", "trend_multi_year")
                );
                factPack.getFacts().getClassifications().add(classification);
            });

        // Calculate percentages of improving vs degrading sites
        long totalSites = records.stream()
            .map(LawaTrendMultiYearRecord::getLawaSiteId)
            .distinct()
            .count();

        long improvingSites = records.stream()
            .filter(r -> IMPROVING_TRENDS.contains(r.getTrendNorm()))
            .map(LawaTrendMultiYearRecord::getLawaSiteId)
            .distinct()
            .count();

        long degradingSites = records.stream()
            .filter(r -> DEGRADING_TRENDS.contains(r.getTrendNorm()))
            .map(LawaTrendMultiYearRecord::getLawaSiteId)
            .distinct()
            .count();

        if (totalSites > 0) {
            BigDecimal improvingPercent = new BigDecimal(improvingSites)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(totalSites), 2, RoundingMode.HALF_UP);

            BigDecimal degradingPercent = new BigDecimal(degradingSites)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(totalSites), 2, RoundingMode.HALF_UP);

            MetricFact improvingMetric = new MetricFact(
                "metric:lawa:improving_sites_percentage",
                "improving_sites_percentage",
                improvingPercent,
                "%",
                "current_period",
                Map.of("scope", "NZ", "trend_direction", "improving")
            );

            MetricFact degradingMetric = new MetricFact(
                "metric:lawa:degrading_sites_percentage",
                "degrading_sites_percentage",
                degradingPercent,
                "%",
                "current_period",
                Map.of("scope", "NZ", "trend_direction", "degrading")
            );

            factPack.getFacts().getMetrics().add(improvingMetric);
            factPack.getFacts().getMetrics().add(degradingMetric);
        }

        // Average trend score analysis
        OptionalDouble avgTrendScoreOpt = records.stream()
            .mapToInt(LawaTrendMultiYearRecord::getTrendScore)
            .average();

        if (avgTrendScoreOpt.isPresent()) {
            BigDecimal avgTrendScore = BigDecimal.valueOf(avgTrendScoreOpt.getAsDouble())
                .setScale(2, RoundingMode.HALF_UP);

            MetricFact avgScoreMetric = new MetricFact(
                "metric:lawa:average_trend_score",
                "average_trend_score",
                avgTrendScore,
                "score",
                "current_period",
                Map.of("scope", "NZ", "dataset", "trend_multi_year")
            );

            factPack.getFacts().getMetrics().add(avgScoreMetric);
        }
    }

    private void buildImprovingSitesTrendFacts(FactPack factPack, List<LawaTrendMultiYearRecord> records) {
        // Track improving sites over time periods (by period end year)
        Map<Integer, Long> improvingSitesByEndYear = records.stream()
            .filter(r -> IMPROVING_TRENDS.contains(r.getTrendNorm()))
            .collect(Collectors.groupingBy(
                LawaTrendMultiYearRecord::getPeriodEndYear,
                Collectors.mapping(LawaTrendMultiYearRecord::getLawaSiteId, Collectors.counting())
            ));

        if (!improvingSitesByEndYear.isEmpty()) {
            List<Integer> years = improvingSitesByEndYear.keySet().stream()
                .sorted()
                .toList();
            
            String coverage = !years.isEmpty() ? years.getFirst() + "_to_" + years.getLast() : "all_time";

            TimeSeriesFact timeSeries = new TimeSeriesFact(
                "ts:lawa:improving_sites_count:" + coverage,
                "improving_sites_count",
                "sites",
                Map.of("scope", "NZ", "trend_direction", "improving")
            );

            List<TimeSeriesFact.DataPoint> points = years.stream()
                .sorted()
                .map(year -> new TimeSeriesFact.DataPoint(
                    year.toString(), 
                    new BigDecimal(improvingSitesByEndYear.get(year))
                ))
                .collect(Collectors.toList());

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);

            // Add comparison between most recent and previous period if we have data
            if (years.size() >= 2) {
                int latestYear = years.getLast();
                int previousYear = years.get(years.size() - 2);
                
                Long latestCount = improvingSitesByEndYear.get(latestYear);
                Long previousCount = improvingSitesByEndYear.get(previousYear);
                
                if (previousCount > 0) {
                    BigDecimal delta = new BigDecimal(latestCount - previousCount);
                    BigDecimal deltaPercent = delta
                        .divide(new BigDecimal(previousCount), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);

                    ComparisonFact comparison = new ComparisonFact(
                        "cmp:lawa:improving_sites:" + latestYear + "_vs_" + previousYear,
                        "improving_sites_count",
                        String.valueOf(previousYear),
                        String.valueOf(latestYear),
                        delta,
                        deltaPercent,
                        "sites",
                        Map.of("trend_direction", "improving")
                    );
                    
                    factPack.getFacts().getComparisons().add(comparison);
                }
            }
        }
    }

    private void buildRegionalTrendComparisonFacts(FactPack factPack, List<LawaTrendMultiYearRecord> records) {
        // Trend distribution by region
        Map<String, Map<String, Long>> regionalTrendDistribution = records.stream()
            .collect(Collectors.groupingBy(
                LawaTrendMultiYearRecord::getRegion,
                Collectors.groupingBy(
                    LawaTrendMultiYearRecord::getTrendNorm,
                    Collectors.counting()
                )
            ));

        // Create classification facts for each region
        regionalTrendDistribution.entrySet().stream()
            .sorted(Map.Entry.comparingByKey()) // deterministic ordering by region
            .forEach(regionEntry -> {
                String region = regionEntry.getKey();
                Map<String, Long> trendCounts = regionEntry.getValue();
                
                trendCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()) // deterministic ordering by trend
                    .forEach(trendEntry -> {
                        String trend = trendEntry.getKey();

                        ClassificationFact classification = new ClassificationFact(
                            "class:lawa:water_quality_trend:" + region + ":" + trend,
                            "water_quality_trend",
                            "water_quality_trend",
                            trend,
                            null, // periodStartYear
                            null, // periodEndYear
                            Map.of("region", region, "dataset", "trend_multi_year")
                        );
                        factPack.getFacts().getClassifications().add(classification);
                    });
            });

        // Calculate percentage of improving sites by region
        Map<String, Long> totalSitesByRegion = records.stream()
            .collect(Collectors.groupingBy(
                LawaTrendMultiYearRecord::getRegion,
                Collectors.mapping(LawaTrendMultiYearRecord::getLawaSiteId, Collectors.counting())
            ));

        Map<String, Long> improvingSitesByRegion = records.stream()
            .filter(r -> IMPROVING_TRENDS.contains(r.getTrendNorm()))
            .collect(Collectors.groupingBy(
                LawaTrendMultiYearRecord::getRegion,
                Collectors.mapping(LawaTrendMultiYearRecord::getLawaSiteId, Collectors.counting())
            ));

        Set<String> selectedRegions = selectDeterministicRegionalSample(
            totalSitesByRegion,
            improvingSitesByRegion
        );

        totalSitesByRegion.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(regionEntry -> {
                String region = regionEntry.getKey();
                if (!selectedRegions.contains(region)) {
                    return;
                }
                Long totalSites = totalSitesByRegion.get(region);
                Long improvingSites = improvingSitesByRegion.getOrDefault(region, 0L);
                
                if (totalSites > 0) {
                    MetricFact metric = getMetricFact(improvingSites, totalSites, region);

                    factPack.getFacts().getMetrics().add(metric);
                }
            });

        // Keep only classifications for selected regions so regional claims stay within visible facts.
        factPack.getFacts().setClassifications(
            factPack.getFacts().getClassifications().stream()
                .filter(c -> selectedRegions.contains((String) c.getDimensions().get("region")))
                .toList()
        );
    }

    private static MetricFact getMetricFact(Long improvingSites, Long totalSites, String region) {
        BigDecimal improvingPercent = new BigDecimal(improvingSites)
            .multiply(new BigDecimal("100"))
            .divide(new BigDecimal(totalSites), 2, RoundingMode.HALF_UP);

        return new MetricFact(
            "metric:lawa:improving_sites_percentage:" + region,
            "improving_sites_percentage",
            improvingPercent,
            "%",
            "current_period",
            Map.of("region", region, "trend_direction", "improving")
        );
    }

    private void buildBasicFacts(FactPack factPack, List<LawaTrendMultiYearRecord> records) {
        // Add basic summary statistics for unsupported question types
        if (!records.isEmpty()) {
            long totalRecords = records.size();
            long uniqueSites = records.stream()
                .map(LawaTrendMultiYearRecord::getLawaSiteId)
                .distinct()
                .count();

            MetricFact totalRecordsMetric = new MetricFact(
                "metric:lawa:total_trend_records:all",
                "total_trend_records",
                new BigDecimal(totalRecords),
                "records",
                "all_time",
                Map.of("scope", "NZ", "dataset", "trend_multi_year")
            );

            MetricFact uniqueSitesMetric = new MetricFact(
                "metric:lawa:unique_sites:all",
                "unique_sites",
                new BigDecimal(uniqueSites),
                "sites",
                "all_time",
                Map.of("scope", "NZ", "dataset", "trend_multi_year")
            );
            
            factPack.getFacts().getMetrics().add(totalRecordsMetric);
            factPack.getFacts().getMetrics().add(uniqueSitesMetric);
        }
    }

    private void setGuardrails(FactPack factPack, ExplanationRequest request) {
        QuestionType questionType = QuestionType.fromWireValue(request.getQuestionType()).orElse(null);
        if (questionType == null) {
            factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
            factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
            factPack.getGuardrails().setRequiredCitations(new ArrayList<>());
            return;
        }

        switch (questionType) {
            case WATER_QUALITY_TRENDS:
            case IMPROVING_SITES_TREND:
                // If there are no facts, keep allowed claims empty to trigger refusal as per tests
                boolean hasAnyFacts = FactPackBuilderSupport.hasAnyFacts(factPack);
                if (hasAnyFacts) {
                    factPack.getGuardrails().setAllowedClaims(Arrays.asList("trend_distribution", "improvement_rate", "regional_comparison", "trend_summary"));
                } else {
                    factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                }
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
                if (!factPack.getFacts().getClassifications().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(FactPackBuilderSupport.stableRequiredCitations(
                        factPack.getFacts().getClassifications().stream().map(ClassificationFact::getId).toList(),
                        3
                    ));
                } else if (!factPack.getFacts().getTimeSeries().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(FactPackBuilderSupport.stableRequiredCitations(
                        factPack.getFacts().getTimeSeries().stream().map(TimeSeriesFact::getId).toList(),
                        1
                    ));
                }
                break;
            case REGIONAL_TREND_COMPARISON:
                boolean hasRegionalFacts = FactPackBuilderSupport.hasAnyFacts(factPack);
                if (hasRegionalFacts) {
                    factPack.getGuardrails().setAllowedClaims(Arrays.asList("trend_distribution", "improvement_rate", "regional_comparison", "trend_summary"));
                    factPack.getGuardrails().setRequiredCitations(
                        buildRegionalTrendRequiredCitations(factPack)
                    );
                } else {
                    factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                    factPack.getGuardrails().setRequiredCitations(new ArrayList<>());
                }
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
                break;
            default:
                // For unsupported question types, set empty guardrails to trigger refusal
                factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
                factPack.getGuardrails().setRequiredCitations(new ArrayList<>());
                break;
        }
    }

    private Set<String> selectDeterministicRegionalSample(
        Map<String, Long> totalSitesByRegion,
        Map<String, Long> improvingSitesByRegion
    ) {
        return FactPackBuilderSupport.selectDeterministicRegionalSample(
            totalSitesByRegion,
            improvingSitesByRegion,
            REGIONAL_SAMPLE_K,
            REGIONAL_SAMPLE_K
        );
    }

    private List<String> buildRegionalTrendRequiredCitations(FactPack factPack) {
        return FactPackBuilderSupport.buildRegionalRequiredCitations(
            factPack,
            "metric:lawa:improving_sites_percentage:"
        );
    }

    private String getPeriodCoverage(List<LawaTrendMultiYearRecord> records) {
        if (records.isEmpty()) {
            return "unknown";
        }
        
        IntSummaryStatistics startStats = records.stream()
            .mapToInt(LawaTrendMultiYearRecord::getPeriodStartYear)
            .summaryStatistics();
        
        IntSummaryStatistics endStats = records.stream()
            .mapToInt(LawaTrendMultiYearRecord::getPeriodEndYear)
            .summaryStatistics();
        
        return startStats.getMin() + "-" + endStats.getMax();
    }
}
