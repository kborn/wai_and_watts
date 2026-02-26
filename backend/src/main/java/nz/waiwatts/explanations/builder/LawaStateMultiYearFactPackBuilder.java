package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.config.LawaStateCategoryProperties;
import nz.waiwatts.explanations.dto.*;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fact Pack Builder for LAWA State Multi-Year data
 */
public class LawaStateMultiYearFactPackBuilder implements FactPackBuilder {

    private final LawaStateMultiYearRecordRepository repository;
    private final Map<String, Set<String>> stateCategoryBands;
    private final int regionalTopK;
    private final int regionalBottomK;
    private static final String LAWA_STATE_DATASET = DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue();

    public LawaStateMultiYearFactPackBuilder(LawaStateMultiYearRecordRepository repository) {
        this(repository, null);
    }

    public LawaStateMultiYearFactPackBuilder(
        LawaStateMultiYearRecordRepository repository,
        LawaStateCategoryProperties lawaProperties
    ) {
        this.repository = repository;
        this.stateCategoryBands = buildStateCategoryBands(lawaProperties);
        this.regionalTopK = lawaProperties != null && lawaProperties.getRegionalSample() != null
            ? lawaProperties.getRegionalSample().getTopK()
            : 2;
        this.regionalBottomK = lawaProperties != null && lawaProperties.getRegionalSample() != null
            ? lawaProperties.getRegionalSample().getBottomK()
            : 2;
    }

    @Override
    public FactPack buildFactPack(ExplanationRequest request) {
        FactPack factPack = new FactPack();
        
        // Set request context
        FactPack.RequestContext requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType(request.getQuestionType());
        requestContext.setDatasetScope(List.of(LAWA_STATE_DATASET));
        requestContext.setFiltersApplied(request.getFilters());
        factPack.setRequestContext(requestContext);

        // Build provenance
        FactPack.Provenance provenance = new FactPack.Provenance();
        List<FactPack.DatasetSourceProvenance> sources = new ArrayList<>();

        // Get records for facts and derive provenance from them (Phase 11 acceptable)
        List<LawaStateMultiYearRecord> records = pinToCanonicalRelease(getRecordsForRequest(request));
        if (!records.isEmpty()) {
            // Group by dataset release to ensure correct contentHash and per-release coverage
            // Use a stable string key: prefer UUID string, else contentHash, else "unknown"
            Map<String, List<LawaStateMultiYearRecord>> byReleaseKey = records.stream()
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
                    List<LawaStateMultiYearRecord> group = entry.getValue();
                    FactPack.DatasetSourceProvenance source = new FactPack.DatasetSourceProvenance();
                    source.setDatasetSourceCode(LAWA_STATE_DATASET);
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
            return LAWA_STATE_DATASET.equals(ds);
        }
        
        // Backward compatibility: check filters
        Map<String, Object> filters = request.getFilters();
        if (filters != null) {
            Object dsFilter = filters.get(FilterKey.DATASET_SOURCE.wireValue());
            return LAWA_STATE_DATASET.equals(String.valueOf(dsFilter));
        }
        
        return false;
    }

    @Override
    public String getSupportedDatasetSourceCode() {
        return LAWA_STATE_DATASET;
    }

    private List<LawaStateMultiYearRecord> getRecordsForRequest(ExplanationRequest request) {
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

        return repository.findForReadApi(startYear, endYear, indicatorFilter, regionFilter);
    }

    private List<LawaStateMultiYearRecord> pinToCanonicalRelease(List<LawaStateMultiYearRecord> records) {
        if (records == null || records.isEmpty()) {
            return records == null ? List.of() : records;
        }

        // Deterministic release pinning for ask: choose one canonical release.
        Optional<DatasetRelease> canonicalRelease = records.stream()
            .map(LawaStateMultiYearRecord::getDatasetRelease)
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

    private void buildFacts(FactPack factPack, ExplanationRequest request, List<LawaStateMultiYearRecord> records) {
        QuestionType questionType = QuestionType.fromWireValue(request.getQuestionType()).orElse(null);
        if (questionType == null) {
            buildBasicFacts(factPack, records);
            return;
        }

        switch (questionType) {
            case WATER_QUALITY_OVERVIEW:
                buildWaterQualityOverviewFacts(factPack, records);
                break;
            case WATER_QUALITY_STATE_SITES_TREND:
                buildStateCategorySitesTrendFacts(factPack, request, records);
                break;
            case REGIONAL_WATER_QUALITY:
                buildRegionalWaterQualityFacts(factPack, records);
                break;
            default:
                // Build basic facts for unsupported question types (will result in refusal)
                buildBasicFacts(factPack, records);
                break;
        }
    }

    private void buildWaterQualityOverviewFacts(FactPack factPack, List<LawaStateMultiYearRecord> records) {
        // Overall water quality distribution across all sites and indicators
        Map<String, Long> stateDistribution = records.stream()
            .collect(Collectors.groupingBy(
                LawaStateMultiYearRecord::getStateNorm,
                Collectors.counting()
            ));

        // Create classification facts for water quality states
        stateDistribution.entrySet().stream()
            .sorted(Map.Entry.comparingByKey()) // deterministic ordering
            .forEach(entry -> {
                String state = entry.getKey();

                ClassificationFact classification = new ClassificationFact(
                    "class:lawa:water_quality_state:" + state,
                    "water_quality_state",
                    "water_quality_state",
                    state,
                    null, // periodStartYear
                    null, // periodEndYear
                    Map.of("scope", "NZ", "dataset", "state_multi_year")
                );
                factPack.getFacts().getClassifications().add(classification);
            });

        // Calculate percentage of excellent vs poor sites
        long totalSites = records.stream()
            .map(LawaStateMultiYearRecord::getLawaSiteId)
            .distinct()
            .count();

        Set<String> excellentBands = bandsForCategory("EXCELLENT");
        Set<String> poorBands = bandsForCategory("POOR");

        long excellentSites = records.stream()
            .filter(r -> excellentBands.contains(normalizeBand(r.getAttributeBand())))
            .map(LawaStateMultiYearRecord::getLawaSiteId)
            .distinct()
            .count();

        long poorSites = records.stream()
            .filter(r -> poorBands.contains(normalizeBand(r.getAttributeBand())))
            .map(LawaStateMultiYearRecord::getLawaSiteId)
            .distinct()
            .count();

        if (totalSites > 0) {
            BigDecimal excellentPercent = new BigDecimal(excellentSites)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(totalSites), 2, RoundingMode.HALF_UP);

            BigDecimal poorPercent = new BigDecimal(poorSites)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(totalSites), 2, RoundingMode.HALF_UP);

            MetricFact excellentMetric = new MetricFact(
                "metric:lawa:excellent_sites_percentage",
                "excellent_sites_percentage",
                excellentPercent,
                "%",
                "current_period",
                Map.of("scope", "NZ", "quality_band", "excellent")
            );

            MetricFact poorMetric = new MetricFact(
                "metric:lawa:poor_sites_percentage",
                "poor_sites_percentage",
                poorPercent,
                "%",
                "current_period",
                Map.of("scope", "NZ", "quality_band", "poor")
            );

            factPack.getFacts().getMetrics().add(excellentMetric);
            factPack.getFacts().getMetrics().add(poorMetric);
        }
    }

    private void buildStateCategorySitesTrendFacts(
        FactPack factPack,
        ExplanationRequest request,
        List<LawaStateMultiYearRecord> records
    ) {
        String stateCategory = resolveStateCategory(request);
        if (stateCategory == null || stateCategory.isBlank()) {
            return;
        }
        Set<String> targetBands = bandsForCategory(stateCategory);
        if (targetBands.isEmpty()) {
            return;
        }

        Map<Integer, Long> categorySitesByEndYear = records.stream()
            .filter(r -> targetBands.contains(normalizeBand(r.getAttributeBand())))
            .collect(Collectors.groupingBy(
                LawaStateMultiYearRecord::getPeriodEndYear,
                Collectors.mapping(LawaStateMultiYearRecord::getLawaSiteId, Collectors.counting())
            ));

        if (!categorySitesByEndYear.isEmpty()) {
            List<Integer> years = categorySitesByEndYear.keySet().stream()
                .sorted()
                .toList();
            
            String coverage = !years.isEmpty() ? years.getFirst() + "_to_" + years.getLast() : "all_time";

            TimeSeriesFact timeSeries = new TimeSeriesFact(
                "ts:lawa:state_category_sites_count:" + stateCategory + ":" + coverage,
                "state_category_sites_count",
                "sites",
                Map.of("scope", "NZ", "state_category", stateCategory)
            );

            List<TimeSeriesFact.DataPoint> points = years.stream()
                .sorted()
                .map(year -> new TimeSeriesFact.DataPoint(
                    year.toString(), 
                    new BigDecimal(categorySitesByEndYear.get(year))
                ))
                .collect(Collectors.toList());

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);

            // Add comparison between most recent and previous period if we have data
            if (years.size() >= 2) {
                int latestYear = years.getLast();
                int previousYear = years.get(years.size() - 2);
                
                Long latestCount = categorySitesByEndYear.get(latestYear);
                Long previousCount = categorySitesByEndYear.get(previousYear);
                
                if (previousCount > 0) {
                    BigDecimal delta = new BigDecimal(latestCount - previousCount);
                    BigDecimal deltaPercent = delta
                        .divide(new BigDecimal(previousCount), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);

                    ComparisonFact comparison = new ComparisonFact(
                        "cmp:lawa:state_category_sites:" + stateCategory + ":" + latestYear + "_vs_" + previousYear,
                        "state_category_sites_count",
                        String.valueOf(previousYear),
                        String.valueOf(latestYear),
                        delta,
                        deltaPercent,
                        "sites",
                        Map.of("state_category", stateCategory)
                    );
                    
                    factPack.getFacts().getComparisons().add(comparison);
                }
            }
        }
    }

    private void buildRegionalWaterQualityFacts(FactPack factPack, List<LawaStateMultiYearRecord> records) {
        // Water quality distribution by region
        Map<String, Map<String, Long>> regionalStateDistribution = records.stream()
            .collect(Collectors.groupingBy(
                LawaStateMultiYearRecord::getRegion,
                Collectors.groupingBy(
                    LawaStateMultiYearRecord::getStateNorm,
                    Collectors.counting()
                )
            ));

        // Create classification facts for each region
        regionalStateDistribution.entrySet().stream()
            .sorted(Map.Entry.comparingByKey()) // deterministic ordering by region
            .forEach(regionEntry -> {
                String region = regionEntry.getKey();
                Map<String, Long> stateCounts = regionEntry.getValue();
                
                stateCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()) // deterministic ordering by state
                    .forEach(stateEntry -> {
                        String state = stateEntry.getKey();

                        ClassificationFact classification = new ClassificationFact(
                            "class:lawa:water_quality_state:" + region + ":" + state,
                            "water_quality_state",
                            "water_quality_state",
                            state,
                            null, // periodStartYear
                            null, // periodEndYear
                            Map.of("region", region, "dataset", "state_multi_year")
                        );
                        factPack.getFacts().getClassifications().add(classification);
                    });
            });

        // Calculate percentage of excellent sites by region
        Map<String, Long> totalSitesByRegion = records.stream()
            .collect(Collectors.groupingBy(
                LawaStateMultiYearRecord::getRegion,
                Collectors.mapping(LawaStateMultiYearRecord::getLawaSiteId, Collectors.counting())
            ));

        Set<String> excellentBands = bandsForCategory("EXCELLENT");
        Map<String, Long> excellentSitesByRegion = records.stream()
            .filter(r -> excellentBands.contains(normalizeBand(r.getAttributeBand())))
            .collect(Collectors.groupingBy(
                LawaStateMultiYearRecord::getRegion,
                Collectors.mapping(LawaStateMultiYearRecord::getLawaSiteId, Collectors.counting())
            ));

        Set<String> selectedRegions = selectDeterministicRegionalSample(
            totalSitesByRegion,
            excellentSitesByRegion
        );

        totalSitesByRegion.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(regionEntry -> {
                String region = regionEntry.getKey();
                if (!selectedRegions.contains(region)) {
                    return;
                }
                Long totalSites = totalSitesByRegion.get(region);
                Long excellentSites = excellentSitesByRegion.getOrDefault(region, 0L);
                
                if (totalSites > 0) {
                    BigDecimal excellentPercent = new BigDecimal(excellentSites)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(totalSites), 2, RoundingMode.HALF_UP);

                    MetricFact metric = new MetricFact(
                        "metric:lawa:excellent_sites_percentage:" + region,
                        "excellent_sites_percentage",
                        excellentPercent,
                        "%",
                        "current_period",
                        Map.of("region", region, "quality_band", "excellent")
                    );
                    
                    factPack.getFacts().getMetrics().add(metric);
                }
            });

        // Keep only classifications for selected regions so the provider can only cite available subset.
        factPack.getFacts().setClassifications(
            factPack.getFacts().getClassifications().stream()
                .filter(c -> selectedRegions.contains((String) c.getDimensions().get("region")))
                .toList()
        );
    }

    private void buildBasicFacts(FactPack factPack, List<LawaStateMultiYearRecord> records) {
        // Add basic summary statistics for unsupported question types
        if (!records.isEmpty()) {
            long totalRecords = records.size();
            long uniqueSites = records.stream()
                .map(LawaStateMultiYearRecord::getLawaSiteId)
                .distinct()
                .count();

            MetricFact totalRecordsMetric = new MetricFact(
                "metric:lawa:total_state_records:all",
                "total_state_records",
                new BigDecimal(totalRecords),
                "records",
                "all_time",
                Map.of("scope", "NZ", "dataset", "state_multi_year")
            );

            MetricFact uniqueSitesMetric = new MetricFact(
                "metric:lawa:unique_sites:all",
                "unique_sites",
                new BigDecimal(uniqueSites),
                "sites",
                "all_time",
                Map.of("scope", "NZ", "dataset", "state_multi_year")
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
            case WATER_QUALITY_OVERVIEW:
            case WATER_QUALITY_STATE_SITES_TREND:
                // If there are no facts, keep allowed claims empty to trigger refusal as per tests
                boolean hasAnyFacts = !(factPack.getFacts().getClassifications().isEmpty()
                        && factPack.getFacts().getMetrics().isEmpty()
                        && factPack.getFacts().getTimeSeries().isEmpty());
                if (hasAnyFacts) {
                    factPack.getGuardrails().setAllowedClaims(Arrays.asList("distribution", "trend", "percentage", "regional_comparison"));
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
            case REGIONAL_WATER_QUALITY:
                boolean hasRegionalFacts = !(factPack.getFacts().getClassifications().isEmpty()
                        && factPack.getFacts().getMetrics().isEmpty()
                        && factPack.getFacts().getTimeSeries().isEmpty());
                if (hasRegionalFacts) {
                    factPack.getGuardrails().setAllowedClaims(Arrays.asList("distribution", "trend", "percentage", "regional_comparison"));
                    factPack.getGuardrails().setRequiredCitations(
                        buildRegionalWaterQualityRequiredCitations(factPack)
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
        Map<String, Long> excellentSitesByRegion
    ) {
        List<Map.Entry<String, BigDecimal>> ranked = totalSitesByRegion.entrySet().stream()
            .filter(e -> e.getValue() != null && e.getValue() > 0)
            .map(e -> {
                String region = e.getKey();
                long total = e.getValue();
                long excellent = excellentSitesByRegion.getOrDefault(region, 0L);
                BigDecimal percent = new BigDecimal(excellent)
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
        ranked.stream().sorted(highToLow).limit(regionalTopK).forEach(e -> selected.add(e.getKey()));
        ranked.stream().sorted(lowToHigh).limit(regionalBottomK).forEach(e -> selected.add(e.getKey()));
        return selected;
    }

    private List<String> buildRegionalWaterQualityRequiredCitations(FactPack factPack) {
        List<String> classFamilies = factPack.getFacts().getClassifications().stream()
            .map(ClassificationFact::getId)
            .map(id -> id.replaceFirst(":[^:]+$", ":*"))
            .toList();
        List<String> metricFamilies = factPack.getFacts().getMetrics().stream()
            .map(MetricFact::getId)
            .filter(id -> id.startsWith("metric:lawa:excellent_sites_percentage:"))
            .map(id -> "metric:lawa:excellent_sites_percentage:*")
            .toList();
        List<String> families = new ArrayList<>();
        families.addAll(classFamilies);
        families.addAll(metricFamilies);
        return stableRequiredCitations(families, Integer.MAX_VALUE);
    }

    private String getPeriodCoverage(List<LawaStateMultiYearRecord> records) {
        if (records.isEmpty()) {
            return "unknown";
        }
        
        IntSummaryStatistics startStats = records.stream()
            .mapToInt(LawaStateMultiYearRecord::getPeriodStartYear)
            .summaryStatistics();
        
        IntSummaryStatistics endStats = records.stream()
            .mapToInt(LawaStateMultiYearRecord::getPeriodEndYear)
            .summaryStatistics();
        
        return startStats.getMin() + "-" + endStats.getMax();
    }

    private Map<String, Set<String>> buildStateCategoryBands(LawaStateCategoryProperties properties) {
        Map<String, List<String>> raw = properties != null ? properties.getStateCategoryBands() : null;
        if (raw == null || raw.isEmpty()) {
            return Map.of(
                "EXCELLENT", Set.of("A"),
                "GOOD", Set.of("B"),
                "FAIR", Set.of("C"),
                "POOR", Set.of("D", "E")
            );
        }
        Map<String, Set<String>> normalized = new HashMap<>();
        raw.forEach((category, bands) -> {
            if (category == null || bands == null) {
                return;
            }
            Set<String> cleanedBands = bands.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeBand)
                .filter(b -> !b.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!cleanedBands.isEmpty()) {
                normalized.put(category.trim().toUpperCase(Locale.ROOT), cleanedBands);
            }
        });
        return normalized;
    }

    private String resolveStateCategory(ExplanationRequest request) {
        if (request == null || request.getFilters() == null) {
            return null;
        }
        Object stateCategory = request.getFilters().get("stateCategory");
        if (stateCategory instanceof String s && !s.isBlank()) {
            return s.trim().toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private Set<String> bandsForCategory(String stateCategory) {
        if (stateCategory == null) {
            return Set.of();
        }
        return stateCategoryBands.getOrDefault(stateCategory.trim().toUpperCase(Locale.ROOT), Set.of());
    }

    private String normalizeBand(String band) {
        if (band == null) {
            return "";
        }
        return band.trim().toUpperCase(Locale.ROOT);
    }
}
