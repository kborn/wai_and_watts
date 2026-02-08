package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.explanations.dto.*;
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

    public LawaTrendMultiYearFactPackBuilder(LawaTrendMultiYearRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public FactPack buildFactPack(ExplanationRequest request) {
        FactPack factPack = new FactPack();
        
        // Set request context
        FactPack.RequestContext requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType(request.getQuestionType());
        requestContext.setDatasetScope(List.of("lawa.water_quality.trend.multi_year"));
        requestContext.setFiltersApplied(request.getFilters());
        factPack.setRequestContext(requestContext);

        // Build provenance
        FactPack.Provenance provenance = new FactPack.Provenance();
        List<FactPack.DatasetSourceProvenance> sources = new ArrayList<>();

        // Get records for facts and derive provenance from them (Phase 11 acceptable)
        List<LawaTrendMultiYearRecord> records = getRecordsForRequest(request);
        if (!records.isEmpty()) {
            // Group by dataset release to ensure correct contentHash and per-release coverage
            // Use a stable string key: prefer UUID string, else contentHash, else "unknown"
            Map<String, List<LawaTrendMultiYearRecord>> byReleaseKey = records.stream()
                .filter(r -> r.getDatasetRelease() != null)
                .collect(Collectors.groupingBy(r -> {
                    var rel = r.getDatasetRelease();
                    if (rel.getId() != null) return rel.getId().toString();
                    if (rel.getContentHash() != null && !rel.getContentHash().isBlank()) return "hash:" + rel.getContentHash();
                    return "unknown";
                }));

            byReleaseKey.entrySet().stream()
                // Deterministic ordering by key for stability
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String releaseKey = entry.getKey();
                    List<LawaTrendMultiYearRecord> group = entry.getValue();
                    FactPack.DatasetSourceProvenance source = new FactPack.DatasetSourceProvenance();
                    source.setDatasetSourceCode("lawa.water_quality.trend.multi_year");
                    source.setDatasetReleaseId(releaseKey);
                    // contentHash from this release specifically if present
                    if (!group.isEmpty()
                        && group.getFirst().getDatasetRelease() != null
                        && group.getFirst().getDatasetRelease().getContentHash() != null) {
                        source.setContentHash(group.getFirst().getDatasetRelease().getContentHash());
                    }
                    // periodCoverage computed for this release's records only
                    source.setPeriodCoverage(getPeriodCoverage(group));
                    sources.add(source);
                });
        }
        // Always set a (possibly empty) list to avoid nulls upstream
        provenance.setDatasetSources(sources);
        factPack.setProvenance(provenance);

        // Build facts based on question type
        buildFacts(factPack, request, records);

        // Set guardrails based on question type
        setGuardrails(factPack, request);

        return factPack;
    }

    @Override
    public boolean canHandle(ExplanationRequest request) {
        Map<String, Object> filters = request.getFilters();
        if (filters == null) return false;
        Object ds = filters.get("datasetSource");
        return "lawa.water_quality.trend.multi_year".equals(String.valueOf(ds));
    }

    @Override
    public String getSupportedDatasetSourceCode() {
        return "lawa.water_quality.trend.multi_year";
    }

    private List<LawaTrendMultiYearRecord> getRecordsForRequest(ExplanationRequest request) {
        // Phase 11: apply basic in-memory filtering for determinism without expanding repo surface
        List<LawaTrendMultiYearRecord> all = repository.findAll();

        Map<String, Object> filters = request != null ? request.getFilters() : null;
        if (filters == null || filters.isEmpty()) {
            return all;
        }

        Integer startYear = null;
        Integer endYear = null;
        try {
            Object s = filters.get("startYear");
            Object e = filters.get("endYear");
            if (s != null) startYear = Integer.parseInt(s.toString());
            if (e != null) endYear = Integer.parseInt(e.toString());
        } catch (NumberFormatException ignore) {
            // Leave bounds null; service-level validation already handles bad inputs
        }

        String indicatorFilter = null;
        Object indObj = filters.get("indicator");
        if (indObj instanceof String str && !str.isBlank()) {
            indicatorFilter = str.trim().toUpperCase();
        }

        String regionFilter = null;
        Object regObj = filters.get("region");
        if (regObj instanceof String str && !str.isBlank()) {
            regionFilter = str.trim();
        }

        String trendFilter = null;
        Object trendObj = filters.get("trend");
        if (trendObj instanceof String str && !str.isBlank()) {
            trendFilter = str.trim().toUpperCase();
        }

        final Integer fStart = startYear;
        final Integer fEnd = endYear;
        final String fIndicator = indicatorFilter;
        final String fRegion = regionFilter;
        final String fTrend = trendFilter;

        return all.stream()
            .filter(r -> fStart == null || r.getPeriodEndYear() >= fStart)
            .filter(r -> fEnd == null || r.getPeriodStartYear() <= fEnd)
            .filter(r -> fIndicator == null || fRegion == null || 
                      (r.getIndicatorNorm() != null && r.getIndicatorNorm().equalsIgnoreCase(fIndicator)))
            .filter(r -> fRegion == null || (r.getRegion() != null && r.getRegion().equalsIgnoreCase(fRegion)))
            .filter(r -> fTrend == null || (r.getTrendNorm() != null && r.getTrendNorm().equalsIgnoreCase(fTrend)))
            .toList();
    }

    private void buildFacts(FactPack factPack, ExplanationRequest request, List<LawaTrendMultiYearRecord> records) {
        String questionType = request.getQuestionType();
        
        switch (questionType) {
            case "water_quality_trends":
                buildWaterQualityTrendsFacts(factPack, records);
                break;
            case "improving_sites_trend":
                buildImprovingSitesTrendFacts(factPack, records);
                break;
            case "regional_trend_comparison":
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

        totalSitesByRegion.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(regionEntry -> {
                String region = regionEntry.getKey();
                Long totalSites = totalSitesByRegion.get(region);
                Long improvingSites = improvingSitesByRegion.getOrDefault(region, 0L);
                
                if (totalSites > 0) {
                    BigDecimal improvingPercent = new BigDecimal(improvingSites)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(totalSites), 2, RoundingMode.HALF_UP);

                    MetricFact metric = new MetricFact(
                        "metric:lawa:improving_sites_percentage:" + region,
                        "improving_sites_percentage",
                        improvingPercent,
                        "%",
                        "current_period",
                        Map.of("region", region, "trend_direction", "improving")
                    );
                    
                    factPack.getFacts().getMetrics().add(metric);
                }
            });
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
        String questionType = request.getQuestionType();
        
        switch (questionType) {
            case "water_quality_trends":
            case "improving_sites_trend":
            case "regional_trend_comparison":
                // If there are no facts, keep allowed claims empty to trigger refusal as per tests
                boolean hasAnyFacts = !(factPack.getFacts().getClassifications().isEmpty()
                        && factPack.getFacts().getMetrics().isEmpty()
                        && factPack.getFacts().getTimeSeries().isEmpty());
                if (hasAnyFacts) {
                    factPack.getGuardrails().setAllowedClaims(Arrays.asList("trend_distribution", "improvement_rate", "regional_comparison", "trend_summary"));
                } else {
                    factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                }
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
                if (!factPack.getFacts().getClassifications().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(
                        factPack.getFacts().getClassifications().stream()
                            .limit(3) // Limit to first 3 for reasonable citation list
                            .map(ClassificationFact::getId)
                            .collect(Collectors.toList())
                    );
                } else if (!factPack.getFacts().getTimeSeries().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(Collections.singletonList(factPack.getFacts().getTimeSeries().getFirst().getId()));
                }
                break;
            default:
                // For unsupported question types, set empty guardrails to trigger refusal
                factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
                factPack.getGuardrails().setRequiredCitations(new ArrayList<>());
                break;
        }
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