package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
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
    private static final int REGIONAL_SAMPLE_K = 2;
    private static final String LAWA_TREND_DATASET = DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue();

    public LawaTrendMultiYearFactPackBuilder(LawaTrendMultiYearRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public FactPack buildFactPack(ExplanationRequest request) {
        FactPack factPack = new FactPack();
        
        // Set request context
        FactPack.RequestContext requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType(request.getQuestionType());
        requestContext.setDatasetScope(List.of(LAWA_TREND_DATASET));
        requestContext.setFiltersApplied(request.getFilters());
        factPack.setRequestContext(requestContext);

        // Build provenance
        FactPack.Provenance provenance = new FactPack.Provenance();
        List<FactPack.DatasetSourceProvenance> sources = new ArrayList<>();

        // Get records for facts and derive provenance from them (Phase 11 acceptable)
        List<LawaTrendMultiYearRecord> records = pinToCanonicalRelease(getRecordsForRequest(request));
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
                    source.setDatasetSourceCode(LAWA_TREND_DATASET);
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
        // Check top-level datasetSource field (Phase 12+)
        String ds = request.getDatasetSource();
        if (ds != null) {
            return LAWA_TREND_DATASET.equals(ds);
        }
        
        // Backward compatibility: check filters
        Map<String, Object> filters = request.getFilters();
        if (filters != null) {
            Object dsFilter = filters.get(FilterKey.DATASET_SOURCE.wireValue());
            return LAWA_TREND_DATASET.equals(String.valueOf(dsFilter));
        }
        
        return false;
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
                indicatorFilter = str.trim().toLowerCase(Locale.ROOT);
            }
        }

        String regionFilter = null;
        if (filters != null && !filters.isEmpty()) {
            Object regObj = filters.get(FilterKey.REGION.wireValue());
            if (regObj instanceof String str && !str.isBlank()) {
                regionFilter = str.trim().toLowerCase(Locale.ROOT);
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

    private List<LawaTrendMultiYearRecord> pinToCanonicalRelease(List<LawaTrendMultiYearRecord> records) {
        if (records == null || records.isEmpty()) {
            return records == null ? List.of() : records;
        }

        // Deterministic release pinning for ask: choose one canonical release.
        Optional<DatasetRelease> canonicalRelease = records.stream()
            .map(LawaTrendMultiYearRecord::getDatasetRelease)
            .filter(Objects::nonNull)
            .max(
                Comparator.comparing(
                        DatasetRelease::getImportedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                    )
                    .thenComparing(
                        DatasetRelease::getRetrievedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                    )
                    .thenComparing(
                        DatasetRelease::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                    )
                    .thenComparing(
                        DatasetRelease::getContentHash,
                        Comparator.nullsLast(Comparator.naturalOrder())
                    )
                    .thenComparing(
                        release -> release.getId() != null ? release.getId().toString() : "",
                        Comparator.naturalOrder()
                    )
            );

        if (canonicalRelease.isEmpty()) {
            return records;
        }

        DatasetRelease target = canonicalRelease.get();
        return records.stream()
            .filter(record -> isSameRelease(record.getDatasetRelease(), target))
            .toList();
    }

    private boolean isSameRelease(DatasetRelease left, DatasetRelease right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getId() != null && right.getId() != null) {
            return left.getId().equals(right.getId());
        }
        return Objects.equals(left.getContentHash(), right.getContentHash());
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
                    factPack.getGuardrails().setRequiredCitations(stableRequiredCitations(
                        factPack.getFacts().getClassifications().stream().map(ClassificationFact::getId).toList(),
                        3
                    ));
                } else if (!factPack.getFacts().getTimeSeries().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(stableRequiredCitations(
                        factPack.getFacts().getTimeSeries().stream().map(TimeSeriesFact::getId).toList(),
                        1
                    ));
                }
                break;
            case REGIONAL_TREND_COMPARISON:
                boolean hasRegionalFacts = !(factPack.getFacts().getClassifications().isEmpty()
                        && factPack.getFacts().getMetrics().isEmpty()
                        && factPack.getFacts().getTimeSeries().isEmpty());
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

    private List<String> stableRequiredCitations(List<String> ids, int limit) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return ids.stream()
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .sorted()
            .limit(limit)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Set<String> selectDeterministicRegionalSample(
        Map<String, Long> totalSitesByRegion,
        Map<String, Long> improvingSitesByRegion
    ) {
        List<Map.Entry<String, BigDecimal>> ranked = totalSitesByRegion.entrySet().stream()
            .filter(e -> e.getValue() != null && e.getValue() > 0)
            .map(e -> {
                String region = e.getKey();
                long total = e.getValue();
                long improving = improvingSitesByRegion.getOrDefault(region, 0L);
                BigDecimal percent = new BigDecimal(improving)
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
                return Map.entry(region, percent);
            })
            .toList();

        Comparator<Map.Entry<String, BigDecimal>> highToLow = Comparator
            .comparing(Map.Entry<String, BigDecimal>::getValue, Comparator.reverseOrder())
            .thenComparing(Map.Entry::getKey);
        Comparator<Map.Entry<String, BigDecimal>> lowToHigh = Comparator
            .comparing(Map.Entry<String, BigDecimal>::getValue)
            .thenComparing(Map.Entry::getKey);

        LinkedHashSet<String> selected = new LinkedHashSet<>();
        ranked.stream().sorted(highToLow).limit(REGIONAL_SAMPLE_K).forEach(e -> selected.add(e.getKey()));
        ranked.stream().sorted(lowToHigh).limit(REGIONAL_SAMPLE_K).forEach(e -> selected.add(e.getKey()));
        return selected;
    }

    private List<String> buildRegionalTrendRequiredCitations(FactPack factPack) {
        List<String> classFamilies = factPack.getFacts().getClassifications().stream()
            .map(ClassificationFact::getId)
            .map(id -> id.replaceFirst(":[^:]+$", ":*"))
            .toList();
        List<String> metricFamilies = factPack.getFacts().getMetrics().stream()
            .map(MetricFact::getId)
            .filter(id -> id.startsWith("metric:lawa:improving_sites_percentage:"))
            .map(id -> "metric:lawa:improving_sites_percentage:*")
            .toList();
        List<String> families = new ArrayList<>();
        families.addAll(classFamilies);
        families.addAll(metricFamilies);
        return stableRequiredCitations(families, Integer.MAX_VALUE);
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
