package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.explanations.dto.*;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fact Pack Builder for MBIE Annual Generation data
 */
public class MbieGenerationAnnualFactPackBuilder implements FactPackBuilder {

    private final MbieGenerationAnnualRecordRepository repository;

    // Centralized renewable types to avoid drift
    private static final Set<String> RENEWABLE_FUEL_TYPES = Set.of("HYDRO", "WIND", "GEOTHERMAL", "BIOMASS", "SOLAR");
    private static final String METRIC_GENERATION_GWH = "generation_gwh";
    private static final String METRIC_RENEWABLE_SHARE_PCT = "renewable_share_pct";
    private static final String METRIC_GENERATION_SHARE_PCT = "generation_share_pct";

    public MbieGenerationAnnualFactPackBuilder(MbieGenerationAnnualRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public FactPack buildFactPack(ExplanationRequest request) {
        FactPack factPack = new FactPack();
        
        // Set request context
        FactPack.RequestContext requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType(request.getQuestionType());
        requestContext.setDatasetScope(List.of("mbie.generation.annual"));
        requestContext.setFiltersApplied(request.getFilters());
        factPack.setRequestContext(requestContext);

        // Build provenance
        FactPack.Provenance provenance = new FactPack.Provenance();
        List<FactPack.DatasetSourceProvenance> sources = new ArrayList<>();

        // Get records for facts and derive provenance from them (Phase 11 acceptable)
        // TODO(Phase 12+): Replace with distinct release lookup to avoid heavy loads
        List<MbieGenerationAnnualRecord> records = pinToCanonicalRelease(getRecordsForRequest(request));
        if (!records.isEmpty()) {
            // Group by dataset release to ensure correct contentHash and per-release coverage.
            // Use a stable key: UUID string when available, else hash fallback.
            Map<String, List<MbieGenerationAnnualRecord>> byReleaseKey = records.stream()
                .filter(r -> r.getDatasetRelease() != null)
                .collect(Collectors.groupingBy(r -> releaseKey(r.getDatasetRelease())));

            byReleaseKey.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String releaseKey = entry.getKey();
                    List<MbieGenerationAnnualRecord> group = entry.getValue();
                    FactPack.DatasetSourceProvenance source = new FactPack.DatasetSourceProvenance();
                    source.setDatasetSourceCode("mbie.generation.annual");
                    source.setDatasetReleaseId(releaseKey);
                    // contentHash from this release specifically
                    if (!group.isEmpty() && group.getFirst().getDatasetRelease() != null) {
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
            return "mbie.generation.annual".equals(ds);
        }
        
        // Backward compatibility: check filters
        Map<String, Object> filters = request.getFilters();
        if (filters != null) {
            Object dsFilter = filters.get("datasetSource");
            return "mbie.generation.annual".equals(String.valueOf(dsFilter));
        }
        
        return false;
    }

    @Override
    public String getSupportedDatasetSourceCode() {
        return "mbie.generation.annual";
    }

    private List<MbieGenerationAnnualRecord> getRecordsForRequest(ExplanationRequest request) {
        Map<String, Object> filters = request != null ? request.getFilters() : null;
        Integer startYear = null;
        Integer endYear = null;
        if (filters != null && !filters.isEmpty()) {
            try {
                Object s = filters.get("startYear");
                Object e = filters.get("endYear");
                if (s != null) startYear = Integer.parseInt(s.toString());
                if (e != null) endYear = Integer.parseInt(e.toString());
            } catch (NumberFormatException ignore) {
                // Leave bounds null; service-level validation already handles bad inputs
            }
        }

        // Only apply a fuelType filter for question types that need a single-fuel trend focus.
        String questionType = (request != null) ? request.getQuestionType() : null;
        boolean applyFuelType = "fuel_generation_trend".equals(questionType);
        String fuelType = null;
        if (applyFuelType && filters != null) {
            Object ftObj = filters.get("fuelType");
            if (ftObj instanceof String str && !str.isBlank()) {
                fuelType = str.trim().toLowerCase(Locale.ROOT);
            }
        }

        String fuelTypeForQuery = applyFuelType ? fuelType : null;
        return repository.findForReadApi(startYear, endYear, fuelTypeForQuery);
    }

    private List<MbieGenerationAnnualRecord> pinToCanonicalRelease(List<MbieGenerationAnnualRecord> records) {
        if (records == null || records.isEmpty()) {
            return records == null ? List.of() : records;
        }

        // Deterministic release pinning for ask: choose one canonical release.
        Optional<DatasetRelease> canonicalRelease = records.stream()
            .map(MbieGenerationAnnualRecord::getDatasetRelease)
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

    private String releaseKey(DatasetRelease release) {
        if (release == null) {
            return "unknown";
        }
        if (release.getId() != null) {
            return release.getId().toString();
        }
        if (release.getContentHash() != null && !release.getContentHash().isBlank()) {
            return "hash:" + release.getContentHash();
        }
        return "unknown";
    }

    private void buildFacts(FactPack factPack, ExplanationRequest request, List<MbieGenerationAnnualRecord> records) {
        String questionType = request.getQuestionType();
        
        switch (questionType) {
            case "renewable_generation_trend":
                buildRenewableGenerationTrendFacts(factPack, records, resolveMetricType(request, METRIC_GENERATION_GWH));
                break;
            case "fuel_generation_trend":
                buildFuelGenerationTrendFacts(factPack, request, records);
                break;
            case "fuel_type_comparison":
                buildFuelTypeComparisonFacts(factPack, request, records);
                break;
            case "generation_mix_overview":
                buildGenerationMixOverviewFacts(factPack, request, records);
                break;
            default:
                // Build basic facts for unsupported question types (will result in refusal)
                buildBasicFacts(factPack, records);
                break;
        }
    }

    private void buildRenewableGenerationTrendFacts(
        FactPack factPack,
        List<MbieGenerationAnnualRecord> records,
        String metricType
    ) {
        // Filter for renewable fuel types
        Map<Integer, BigDecimal> yearlyRenewableTotals = records.stream()
            .filter(record -> RENEWABLE_FUEL_TYPES.contains(record.getFuelTypeNorm()))
            .collect(Collectors.groupingBy(
                MbieGenerationAnnualRecord::getPeriodYear,
                Collectors.mapping(MbieGenerationAnnualRecord::getGenerationGwh, 
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        // Only create time series if we have data
        if (!yearlyRenewableTotals.isEmpty()) {
            // Create time series fact
            // Compute coverage dynamically from the years present
            IntSummaryStatistics stats = yearlyRenewableTotals.keySet().stream().mapToInt(Integer::intValue).summaryStatistics();
            String coverage = stats.getCount() > 0 ? stats.getMin() + "_" + stats.getMax() : "all_time";

            TimeSeriesFact timeSeries;
            List<TimeSeriesFact.DataPoint> points;
            if (METRIC_RENEWABLE_SHARE_PCT.equals(metricType)) {
                Map<Integer, BigDecimal> yearlyTotals = records.stream()
                    .collect(Collectors.groupingBy(
                        MbieGenerationAnnualRecord::getPeriodYear,
                        Collectors.mapping(MbieGenerationAnnualRecord::getGenerationGwh,
                            Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                    ));

                timeSeries = new TimeSeriesFact(
                    "ts:mbie:renewable_share_pct:" + coverage,
                    METRIC_RENEWABLE_SHARE_PCT,
                    "%",
                    Map.of("scope", "NZ")
                );

                points = yearlyRenewableTotals.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        BigDecimal total = yearlyTotals.get(entry.getKey());
                        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
                            return null;
                        }
                        BigDecimal pct = entry.getValue()
                            .multiply(new BigDecimal("100"))
                            .divide(total, 2, RoundingMode.HALF_UP);
                        return new TimeSeriesFact.DataPoint(entry.getKey().toString(), pct);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            } else {
                timeSeries = new TimeSeriesFact(
                    "ts:mbie:renewable_generation_gwh:" + coverage,
                    "renewable_generation_gwh",
                    "GWh",
                    Map.of("scope", "NZ")
                );

                points = yearlyRenewableTotals.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new TimeSeriesFact.DataPoint(entry.getKey().toString(), entry.getValue()))
                    .collect(Collectors.toList());
            }

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);
        }
    }

    private void buildFuelGenerationTrendFacts(
        FactPack factPack,
        ExplanationRequest request,
        List<MbieGenerationAnnualRecord> records
    ) {
        String fuelType = resolveRequestedFuelType(request);
        if (fuelType == null || fuelType.isBlank()) {
            return;
        }

        Map<Integer, BigDecimal> yearlyFuelTotals = records.stream()
            .filter(record -> fuelType.equalsIgnoreCase(record.getFuelTypeNorm()))
            .collect(Collectors.groupingBy(
                MbieGenerationAnnualRecord::getPeriodYear,
                Collectors.mapping(MbieGenerationAnnualRecord::getGenerationGwh, 
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        // Only create time series if we have data
        if (!yearlyFuelTotals.isEmpty()) {
            // Create time series fact
            IntSummaryStatistics stats = yearlyFuelTotals.keySet().stream().mapToInt(Integer::intValue).summaryStatistics();
            String coverage = stats.getCount() > 0 ? stats.getMin() + "_" + stats.getMax() : "all_time";

            TimeSeriesFact timeSeries = new TimeSeriesFact(
                "ts:mbie:fuel_generation_gwh:" + fuelType + ":" + coverage,
                "fuel_generation_gwh",
                "GWh",
                Map.of("scope", "NZ", "fuel_type", fuelType)
            );

            List<TimeSeriesFact.DataPoint> points = yearlyFuelTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TimeSeriesFact.DataPoint(entry.getKey().toString(), entry.getValue()))
                .collect(Collectors.toList());

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);
        }

        // Add comparison between most recent and previous year if we have data
        List<Integer> years = yearlyFuelTotals.keySet().stream()
            .sorted()
            .toList();
        
        if (years.size() >= 2) {
            int latestYear = years.getLast();
            int previousYear = years.get(years.size() - 2);
            
            BigDecimal latestValue = yearlyFuelTotals.get(latestYear);
            BigDecimal previousValue = yearlyFuelTotals.get(previousYear);
            // Guard against division by zero; skip comparison if previous is zero or null
            if (previousValue != null && previousValue.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal delta = latestValue.subtract(previousValue);
                BigDecimal deltaPercent = delta.divide(previousValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

                ComparisonFact comparison = new ComparisonFact(
                    "cmp:mbie:generation_gwh:" + fuelType + ":" + latestYear + "_vs_" + previousYear,
                    "generation_gwh",
                    String.valueOf(previousYear),
                    String.valueOf(latestYear),
                    delta,
                    deltaPercent,
                    "GWh",
                    Map.of("fuel_type", fuelType)
                );
                
                factPack.getFacts().getComparisons().add(comparison);
            }
        }
    }

    private void buildFuelTypeComparisonFacts(FactPack factPack, ExplanationRequest request, List<MbieGenerationAnnualRecord> records) {
        String metricType = resolveMetricType(request, METRIC_GENERATION_GWH);
        List<String> fuels = extractFuelTypeFilters(request);
        if (fuels.size() >= 2) {
            buildFuelTypeTimeSeriesFacts(factPack, records, fuels);
            buildFuelTypeLatestMetrics(factPack, records, fuels, metricType);
            return;
        }
        buildFuelTypeLatestMetrics(factPack, records, null, metricType);
    }

    private void buildGenerationMixOverviewFacts(FactPack factPack, ExplanationRequest request, List<MbieGenerationAnnualRecord> records) {
        // Mix overview is a metric snapshot of the latest period in scope
        String metricType = resolveMetricType(request, METRIC_GENERATION_GWH);
        buildFuelTypeLatestMetrics(factPack, records, null, metricType);
    }

    private void buildFuelTypeTimeSeriesFacts(FactPack factPack, List<MbieGenerationAnnualRecord> records, List<String> fuels) {
        for (String fuel : fuels) {
            Map<Integer, BigDecimal> yearlyTotals = records.stream()
                .filter(record -> fuel.equalsIgnoreCase(record.getFuelTypeNorm()))
                .collect(Collectors.groupingBy(
                    MbieGenerationAnnualRecord::getPeriodYear,
                    Collectors.mapping(MbieGenerationAnnualRecord::getGenerationGwh,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

            if (yearlyTotals.isEmpty()) {
                continue;
            }

            IntSummaryStatistics stats = yearlyTotals.keySet().stream().mapToInt(Integer::intValue).summaryStatistics();
            String coverage = stats.getCount() > 0 ? stats.getMin() + "_" + stats.getMax() : "all_time";

            TimeSeriesFact timeSeries = new TimeSeriesFact(
                "ts:mbie:generation_gwh:" + fuel + ":" + coverage,
                "generation_gwh",
                "GWh",
                Map.of("scope", "NZ", "fuel_type", fuel)
            );

            List<TimeSeriesFact.DataPoint> points = yearlyTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TimeSeriesFact.DataPoint(entry.getKey().toString(), entry.getValue()))
                .collect(Collectors.toList());

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);
        }
    }

    private void buildFuelTypeLatestMetrics(
        FactPack factPack,
        List<MbieGenerationAnnualRecord> records,
        List<String> fuels,
        String metricType
    ) {
        OptionalInt latestYear = records.stream()
            .mapToInt(MbieGenerationAnnualRecord::getPeriodYear)
            .max();

        if (latestYear.isEmpty()) {
            return;
        }

        int year = latestYear.getAsInt();
        Map<String, BigDecimal> fuelTypeTotals = records.stream()
            .filter(record -> record.getPeriodYear() == year)
            .filter(record -> fuels == null || fuels.isEmpty() || fuels.stream().anyMatch(f -> f.equalsIgnoreCase(record.getFuelTypeNorm())))
            .collect(Collectors.groupingBy(
                MbieGenerationAnnualRecord::getFuelTypeNorm,
                Collectors.mapping(MbieGenerationAnnualRecord::getGenerationGwh,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        BigDecimal totalForYear = fuelTypeTotals.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        fuelTypeTotals.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String fuelType = entry.getKey();
                BigDecimal total = entry.getValue();

                BigDecimal value = total;
                String metricIdPrefix = "metric:mbie:generation_gwh";
                String metricName = "generation_gwh";
                String unit = "GWh";
                if (METRIC_GENERATION_SHARE_PCT.equals(metricType)) {
                    if (totalForYear.compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    }
                    value = total
                        .multiply(new BigDecimal("100"))
                        .divide(totalForYear, 2, RoundingMode.HALF_UP);
                    metricIdPrefix = "metric:mbie:generation_share_pct";
                    metricName = METRIC_GENERATION_SHARE_PCT;
                    unit = "%";
                }

                MetricFact metric = new MetricFact(
                    metricIdPrefix + ":" + year + ":" + fuelType,
                    metricName,
                    value,
                    unit,
                    String.valueOf(year),
                    Map.of("fuel_type", fuelType)
                );
                factPack.getFacts().getMetrics().add(metric);
            });
    }

    private List<String> extractFuelTypeFilters(ExplanationRequest request) {
        if (request == null || request.getFilters() == null) {
            return List.of();
        }
        Object fuelA = request.getFilters().get("fuelType");
        Object fuelB = request.getFilters().get("fuelTypeB");
        List<String> fuels = new ArrayList<>();
        if (fuelA instanceof String s && !s.isBlank()) {
            fuels.add(s.trim().toUpperCase());
        }
        if (fuelB instanceof String s && !s.isBlank()) {
            String norm = s.trim().toUpperCase();
            if (!fuels.contains(norm)) {
                fuels.add(norm);
            }
        }
        return fuels;
    }

    private void buildBasicFacts(FactPack factPack, List<MbieGenerationAnnualRecord> records) {
        // Add basic summary statistics for unsupported question types
        if (!records.isEmpty()) {
            BigDecimal totalGeneration = records.stream()
                .map(MbieGenerationAnnualRecord::getGenerationGwh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            MetricFact totalMetric = new MetricFact(
                "metric:mbie:total_generation_gwh:all",
                "total_generation_gwh",
                totalGeneration,
                "GWh",
                "all_time",
                Map.of("scope", "NZ")
            );
            
            factPack.getFacts().getMetrics().add(totalMetric);
        }
    }

    private void setGuardrails(FactPack factPack, ExplanationRequest request) {
        String questionType = request.getQuestionType();
        
        switch (questionType) {
            case "renewable_generation_trend":
            case "fuel_generation_trend":
                factPack.getGuardrails().setAllowedClaims(Arrays.asList("trend_increase", "trend_decrease", "trend_summary"));
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation"));
                if (!factPack.getFacts().getTimeSeries().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(stableRequiredCitations(
                        factPack.getFacts().getTimeSeries().stream().map(TimeSeriesFact::getId).toList(),
                        1
                    ));
                }
                break;
            case "fuel_type_comparison":
            case "generation_mix_overview":
                factPack.getGuardrails().setAllowedClaims(Arrays.asList("comparison", "largest_contributor", "relative_proportion"));
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation"));
                if (!factPack.getFacts().getTimeSeries().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(stableRequiredCitations(
                        factPack.getFacts().getTimeSeries().stream().map(TimeSeriesFact::getId).toList(),
                        Integer.MAX_VALUE
                    ));
                } else if (!factPack.getFacts().getMetrics().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(stableRequiredCitations(
                        factPack.getFacts().getMetrics().stream().map(MetricFact::getId).toList(),
                        Integer.MAX_VALUE
                    ));
                }
                break;
            default:
                // For unsupported question types, set empty guardrails to trigger refusal
                factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation"));
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

    private String getPeriodCoverage(List<MbieGenerationAnnualRecord> records) {
        if (records.isEmpty()) {
            return "unknown";
        }
        
        IntSummaryStatistics stats = records.stream()
            .mapToInt(MbieGenerationAnnualRecord::getPeriodYear)
            .summaryStatistics();
        
        // Keep hyphen format to preserve existing contract/tests for provenance coverage (min-max)
        // Note: time-series IDs currently use underscore (min_max). Consider harmonizing in a future phase.
        return stats.getMin() + "-" + stats.getMax();
    }

    private String resolveMetricType(ExplanationRequest request, String fallback) {
        if (request == null || request.getFilters() == null) {
            return fallback;
        }
        Object metricType = request.getFilters().get("metricType");
        if (metricType instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return fallback;
    }

    private String resolveRequestedFuelType(ExplanationRequest request) {
        if (request == null || request.getFilters() == null) {
            return null;
        }
        Object fuelType = request.getFilters().get("fuelType");
        if (fuelType instanceof String s && !s.isBlank()) {
            return s.trim().toUpperCase(Locale.ROOT);
        }
        return null;
    }
}
