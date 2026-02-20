package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.explanations.dto.*;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fact Pack Builder for MBIE Quarterly Generation data
 */
public class MbieGenerationQuarterlyFactPackBuilder implements FactPackBuilder {

    private final MbieGenerationQuarterlyRecordRepository repository;

    // Centralized renewable types to avoid drift
    private static final Set<String> RENEWABLE_FUEL_TYPES = Set.of("HYDRO", "WIND", "GEOTHERMAL", "BIOMASS", "SOLAR");

    public MbieGenerationQuarterlyFactPackBuilder(MbieGenerationQuarterlyRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public FactPack buildFactPack(ExplanationRequest request) {
        FactPack factPack = new FactPack();
        
        // Set request context
        FactPack.RequestContext requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType(request.getQuestionType());
        requestContext.setDatasetScope(List.of("mbie.generation.quarterly"));
        requestContext.setFiltersApplied(request.getFilters());
        factPack.setRequestContext(requestContext);

        // Build provenance
        FactPack.Provenance provenance = new FactPack.Provenance();
        List<FactPack.DatasetSourceProvenance> sources = new ArrayList<>();

        // Get records for facts and derive provenance from them (Phase 11 acceptable)
        List<MbieGenerationQuarterlyRecord> records = pinToCanonicalRelease(getRecordsForRequest(request));
        if (!records.isEmpty()) {
            // Group by dataset release to ensure correct contentHash and per-release coverage
            // Use a stable string key: prefer UUID string, else contentHash, else "unknown"
            Map<String, List<MbieGenerationQuarterlyRecord>> byReleaseKey = records.stream()
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
                    List<MbieGenerationQuarterlyRecord> group = entry.getValue();
                    FactPack.DatasetSourceProvenance source = new FactPack.DatasetSourceProvenance();
                    source.setDatasetSourceCode("mbie.generation.quarterly");
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
            return "mbie.generation.quarterly".equals(ds);
        }
        
        // Backward compatibility: check filters
        Map<String, Object> filters = request.getFilters();
        if (filters != null) {
            Object dsFilter = filters.get("datasetSource");
            return "mbie.generation.quarterly".equals(String.valueOf(dsFilter));
        }
        
        return false;
    }

    @Override
    public String getSupportedDatasetSourceCode() {
        return "mbie.generation.quarterly";
    }

    private List<MbieGenerationQuarterlyRecord> getRecordsForRequest(ExplanationRequest request) {
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

        // Only apply a fuelType filter for question types that actually need a single-fuel focus
        String questionType = (request != null) ? request.getQuestionType() : null;
        boolean applyFuelType = "hydro_generation_trend".equals(questionType);
        String fuelType = null;
        if (applyFuelType && filters != null) {
            Object ftObj = filters.get("fuelType");
            if (ftObj instanceof String str && !str.isBlank()) {
                fuelType = str.trim().toLowerCase(Locale.ROOT);
            }
        }

        String fuelTypeForQuery = applyFuelType ? fuelType : null;
        return repository.findForReadApi(startYear, endYear, null, fuelTypeForQuery);
    }

    private List<MbieGenerationQuarterlyRecord> pinToCanonicalRelease(List<MbieGenerationQuarterlyRecord> records) {
        if (records == null || records.isEmpty()) {
            return records == null ? List.of() : records;
        }

        // Deterministic release pinning for ask: choose one canonical release.
        Optional<DatasetRelease> canonicalRelease = records.stream()
            .map(MbieGenerationQuarterlyRecord::getDatasetRelease)
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

    private void buildFacts(FactPack factPack, ExplanationRequest request, List<MbieGenerationQuarterlyRecord> records) {
        String questionType = request.getQuestionType();
        
        switch (questionType) {
            case "renewable_generation_trend":
                buildRenewableGenerationTrendFacts(factPack, records);
                break;
            case "hydro_generation_trend":
                buildHydroGenerationTrendFacts(factPack, records);
                break;
            case "fuel_type_comparison":
                buildFuelTypeComparisonFacts(factPack, request, records);
                break;
            case "generation_mix_overview":
                buildGenerationMixOverviewFacts(factPack, records);
                break;
            default:
                // Build basic facts for unsupported question types (will result in refusal)
                buildBasicFacts(factPack, records);
                break;
        }
    }

    private void buildRenewableGenerationTrendFacts(FactPack factPack, List<MbieGenerationQuarterlyRecord> records) {
        // Filter for renewable fuel types and group by quarter
        Map<String, BigDecimal> quarterlyRenewableTotals = records.stream()
            .filter(record -> RENEWABLE_FUEL_TYPES.contains(record.getFuelTypeNorm()))
            .collect(Collectors.groupingBy(
                r -> r.getPeriodYear() + "-Q" + r.getPeriodQuarter(),
                Collectors.mapping(MbieGenerationQuarterlyRecord::getGenerationGwh, 
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        // Only create time series if we have data
        if (!quarterlyRenewableTotals.isEmpty()) {
            // Create time series fact
            List<String> quarters = quarterlyRenewableTotals.keySet().stream()
                .sorted()
                .toList();
            String coverage = !quarters.isEmpty() ? quarters.getFirst() + "_to_" + quarters.getLast() : "all_time";

            TimeSeriesFact timeSeries = new TimeSeriesFact(
                "ts:mbie:renewable_generation_gwh_quarterly:" + coverage,
                "renewable_generation_gwh_quarterly",
                "GWh",
                Map.of("scope", "NZ", "granularity", "quarterly")
            );

            List<TimeSeriesFact.DataPoint> points = quarterlyRenewableTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TimeSeriesFact.DataPoint(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);
        }
    }

    private void buildHydroGenerationTrendFacts(FactPack factPack, List<MbieGenerationQuarterlyRecord> records) {
        // Filter for HYDRO only and group by quarter
        Map<String, BigDecimal> quarterlyHydroTotals = records.stream()
            .filter(record -> "HYDRO".equals(record.getFuelTypeNorm()))
            .collect(Collectors.groupingBy(
                r -> r.getPeriodYear() + "-Q" + r.getPeriodQuarter(),
                Collectors.mapping(MbieGenerationQuarterlyRecord::getGenerationGwh, 
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        // Only create time series if we have data
        if (!quarterlyHydroTotals.isEmpty()) {
            List<String> quarters = quarterlyHydroTotals.keySet().stream()
                .sorted()
                .toList();
            String coverage = !quarters.isEmpty() ? quarters.getFirst() + "_to_" + quarters.getLast() : "all_time";

            TimeSeriesFact timeSeries = new TimeSeriesFact(
                "ts:mbie:hydro_generation_gwh_quarterly:" + coverage,
                "hydro_generation_gwh_quarterly",
                "GWh",
                Map.of("scope", "NZ", "fuel_type", "HYDRO", "granularity", "quarterly")
            );

            List<TimeSeriesFact.DataPoint> points = quarterlyHydroTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TimeSeriesFact.DataPoint(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);
        }

        // Add comparison between most recent and previous quarter if we have data
        List<String> quarters = quarterlyHydroTotals.keySet().stream()
            .sorted()
            .toList();
        
        if (quarters.size() >= 2) {
            String latestQuarter = quarters.getLast();
            String previousQuarter = quarters.get(quarters.size() - 2);
            
            BigDecimal latestValue = quarterlyHydroTotals.get(latestQuarter);
            BigDecimal previousValue = quarterlyHydroTotals.get(previousQuarter);
            // Guard against division by zero; skip comparison if previous is zero or null
            if (previousValue != null && previousValue.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal delta = latestValue.subtract(previousValue);
                BigDecimal deltaPercent = delta.divide(previousValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

                ComparisonFact comparison = new ComparisonFact(
                    "cmp:mbie:generation_gwh_quarterly:HYDRO:" + latestQuarter + "_vs_" + previousQuarter,
                    "generation_gwh_quarterly",
                    previousQuarter,
                    latestQuarter,
                    delta,
                    deltaPercent,
                    "GWh",
                    Map.of("fuel_type", "HYDRO", "granularity", "quarterly")
                );
                
                factPack.getFacts().getComparisons().add(comparison);
            }
        }
    }

    private void buildFuelTypeComparisonFacts(FactPack factPack, ExplanationRequest request, List<MbieGenerationQuarterlyRecord> records) {
        List<String> fuels = extractFuelTypeFilters(request);
        if (fuels.size() >= 2) {
            buildFuelTypeTimeSeriesFacts(factPack, records, fuels);
            buildFuelTypeLatestMetrics(factPack, records, fuels);
            return;
        }
        buildFuelTypeLatestMetrics(factPack, records, null);
    }

    private void buildGenerationMixOverviewFacts(FactPack factPack, List<MbieGenerationQuarterlyRecord> records) {
        buildFuelTypeLatestMetrics(factPack, records, null);
    }

    private void buildFuelTypeTimeSeriesFacts(FactPack factPack, List<MbieGenerationQuarterlyRecord> records, List<String> fuels) {
        for (String fuel : fuels) {
            Map<String, BigDecimal> quarterlyTotals = records.stream()
                .filter(record -> fuel.equalsIgnoreCase(record.getFuelTypeNorm()))
                .collect(Collectors.groupingBy(
                    r -> r.getPeriodYear() + "-Q" + r.getPeriodQuarter(),
                    Collectors.mapping(MbieGenerationQuarterlyRecord::getGenerationGwh,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

            if (quarterlyTotals.isEmpty()) {
                continue;
            }

            List<String> quarters = quarterlyTotals.keySet().stream()
                .sorted()
                .toList();
            String coverage = !quarters.isEmpty() ? quarters.getFirst() + "_to_" + quarters.getLast() : "all_time";

            TimeSeriesFact timeSeries = new TimeSeriesFact(
                "ts:mbie:generation_gwh_quarterly:" + fuel + ":" + coverage,
                "generation_gwh_quarterly",
                "GWh",
                Map.of("scope", "NZ", "fuel_type", fuel, "granularity", "quarterly")
            );

            List<TimeSeriesFact.DataPoint> points = quarterlyTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TimeSeriesFact.DataPoint(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);
        }
    }

    private void buildFuelTypeLatestMetrics(
        FactPack factPack,
        List<MbieGenerationQuarterlyRecord> records,
        List<String> fuels
    ) {
        // Get most recent quarter's data
        Optional<String> latestQuarterOpt = records.stream()
            .map(r -> r.getPeriodYear() + "-Q" + r.getPeriodQuarter())
            .max(String::compareTo);

        if (latestQuarterOpt.isPresent()) {
            String latestQuarter = latestQuarterOpt.get();
            int year = Integer.parseInt(latestQuarter.split("-Q")[0]);
            int quarter = Integer.parseInt(latestQuarter.split("-Q")[1]);
            
            Map<String, BigDecimal> fuelTypeTotals = records.stream()
                .filter(record -> record.getPeriodYear() == year && record.getPeriodQuarter() == quarter)
                .filter(record -> fuels == null || fuels.isEmpty() || fuels.stream().anyMatch(f -> f.equalsIgnoreCase(record.getFuelTypeNorm())))
                .collect(Collectors.groupingBy(
                    MbieGenerationQuarterlyRecord::getFuelTypeNorm,
                    Collectors.mapping(MbieGenerationQuarterlyRecord::getGenerationGwh, 
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

            // Create metric facts for each fuel type with deterministic ordering
            fuelTypeTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // deterministic ordering
                .forEach(entry -> {
                    String fuelType = entry.getKey();
                    BigDecimal total = entry.getValue();
                    
                    MetricFact metric = new MetricFact(
                        "metric:mbie:generation_gwh_quarterly:" + latestQuarter + ":" + fuelType,
                        "generation_gwh_quarterly",
                        total,
                        "GWh",
                        latestQuarter,
                        Map.of("fuel_type", fuelType, "granularity", "quarterly")
                    );
                    factPack.getFacts().getMetrics().add(metric);
                });
        }
    }

    private void buildBasicFacts(FactPack factPack, List<MbieGenerationQuarterlyRecord> records) {
        // Add basic summary statistics for unsupported question types
        if (!records.isEmpty()) {
            BigDecimal totalGeneration = records.stream()
                .map(MbieGenerationQuarterlyRecord::getGenerationGwh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            MetricFact totalMetric = new MetricFact(
                "metric:mbie:total_generation_gwh_quarterly:all",
                "total_generation_gwh_quarterly",
                totalGeneration,
                "GWh",
                "all_time",
                Map.of("scope", "NZ", "granularity", "quarterly")
            );
            
            factPack.getFacts().getMetrics().add(totalMetric);
        }
    }

    private void setGuardrails(FactPack factPack, ExplanationRequest request) {
        String questionType = request.getQuestionType();
        
        switch (questionType) {
            case "renewable_generation_trend":
            case "hydro_generation_trend":
                // If no facts, keep allowedClaims empty to trigger refusal as per tests
                boolean hasAnyFactsTrend = !(factPack.getFacts().getClassifications().isEmpty()
                        && factPack.getFacts().getMetrics().isEmpty()
                        && factPack.getFacts().getTimeSeries().isEmpty());
                if (hasAnyFactsTrend) {
                    factPack.getGuardrails().setAllowedClaims(Arrays.asList("trend_increase", "trend_decrease", "trend_summary"));
                } else {
                    factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                }
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
                if (!factPack.getFacts().getTimeSeries().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(stableRequiredCitations(
                        factPack.getFacts().getTimeSeries().stream().map(TimeSeriesFact::getId).toList(),
                        1
                    ));
                }
                break;
            case "fuel_type_comparison":
            case "generation_mix_overview":
                // If no facts, keep allowedClaims empty to trigger refusal as per tests
                boolean hasAnyFactsComp = !(factPack.getFacts().getClassifications().isEmpty()
                        && factPack.getFacts().getMetrics().isEmpty()
                        && factPack.getFacts().getTimeSeries().isEmpty());
                if (hasAnyFactsComp) {
                    factPack.getGuardrails().setAllowedClaims(Arrays.asList("comparison", "largest_contributor", "relative_proportion"));
                } else {
                    factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                }
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
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

    private String getPeriodCoverage(List<MbieGenerationQuarterlyRecord> records) {
        if (records.isEmpty()) {
            return "unknown";
        }
        
        // Find earliest and latest quarter
        String earliestQuarter = records.stream()
            .map(r -> r.getPeriodYear() + "-Q" + r.getPeriodQuarter())
            .min(String::compareTo)
            .orElse("unknown");
        
        String latestQuarter = records.stream()
            .map(r -> r.getPeriodYear() + "-Q" + r.getPeriodQuarter())
            .max(String::compareTo)
            .orElse("unknown");
        
        return earliestQuarter + "-" + latestQuarter;
    }
}
