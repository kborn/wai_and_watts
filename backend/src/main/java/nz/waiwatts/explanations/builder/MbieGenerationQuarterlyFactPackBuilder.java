package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.MetricType;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
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
    private static final String MBIE_QUARTERLY_DATASET = DatasetSource.MBIE_GENERATION_QUARTERLY.wireValue();
    private static final String METRIC_GENERATION_GWH = MetricType.GENERATION_GWH.wireValue();
    private static final String METRIC_RENEWABLE_SHARE_PCT = MetricType.RENEWABLE_SHARE_PCT.wireValue();
    private static final String METRIC_GENERATION_SHARE_PCT = MetricType.GENERATION_SHARE_PCT.wireValue();

    public MbieGenerationQuarterlyFactPackBuilder(MbieGenerationQuarterlyRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public FactPack buildFactPack(ExplanationRequest request) {
        FactPack factPack = FactPackBuilderSupport.initializeFactPack(request, MBIE_QUARTERLY_DATASET);

        // Get records for facts and derive provenance from them (Phase 11 acceptable)
        List<MbieGenerationQuarterlyRecord> records = FactPackBuilderSupport.pinToCanonicalRelease(
            getRecordsForRequest(request),
            MbieGenerationQuarterlyRecord::getDatasetRelease
        );
        factPack.getProvenance().setDatasetSources(
            FactPackBuilderSupport.buildGroupedProvenance(
                records,
                MBIE_QUARTERLY_DATASET,
                MbieGenerationQuarterlyRecord::getDatasetRelease,
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
    public boolean canHandle(ExplanationRequest request) {
        return FactPackBuilderSupport.supportsDatasetSource(request, MBIE_QUARTERLY_DATASET);
    }

    @Override
    public String getSupportedDatasetSourceCode() {
        return MBIE_QUARTERLY_DATASET;
    }

    private List<MbieGenerationQuarterlyRecord> getRecordsForRequest(ExplanationRequest request) {
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

        // Only apply a fuelType filter for single-fuel trend questions.
        String questionType = (request != null) ? request.getQuestionType() : null;
        boolean applyFuelType = QuestionType.FUEL_GENERATION_TREND.wireValue().equals(questionType);
        String fuelType = null;
        if (applyFuelType && filters != null) {
            Object ftObj = filters.get(FilterKey.FUEL_TYPE.wireValue());
            if (ftObj instanceof String str && !str.isBlank()) {
                fuelType = str.trim().toLowerCase(Locale.ROOT);
            }
        }

        String fuelTypeForQuery = applyFuelType ? fuelType : null;
        return repository.findForReadApi(startYear, endYear, null, fuelTypeForQuery);
    }

    private void buildFacts(FactPack factPack, ExplanationRequest request, List<MbieGenerationQuarterlyRecord> records) {
        QuestionType questionType = QuestionType.fromWireValue(request.getQuestionType()).orElse(null);
        if (questionType == null) {
            buildBasicFacts(factPack, records);
            return;
        }

        switch (questionType) {
            case RENEWABLE_GENERATION_TREND:
                buildRenewableGenerationTrendFacts(
                    factPack,
                    records,
                    FactPackBuilderSupport.resolveMetricType(request, METRIC_GENERATION_GWH)
                );
                break;
            case FUEL_GENERATION_TREND:
                buildFuelGenerationTrendFacts(factPack, request, records);
                break;
            case FUEL_TYPE_COMPARISON:
                buildFuelTypeComparisonFacts(factPack, request, records);
                break;
            case GENERATION_MIX_OVERVIEW:
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
        List<MbieGenerationQuarterlyRecord> records,
        String metricType
    ) {
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

            TimeSeriesFact timeSeries;
            List<TimeSeriesFact.DataPoint> points;
            if (METRIC_RENEWABLE_SHARE_PCT.equals(metricType)) {
                Map<String, BigDecimal> quarterlyTotals = records.stream()
                    .collect(Collectors.groupingBy(
                        r -> r.getPeriodYear() + "-Q" + r.getPeriodQuarter(),
                        Collectors.mapping(MbieGenerationQuarterlyRecord::getGenerationGwh,
                            Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                    ));
                timeSeries = new TimeSeriesFact(
                    "ts:mbie:renewable_share_pct_quarterly:" + coverage,
                    "renewable_share_pct_quarterly",
                    "%",
                    Map.of("scope", "NZ", "granularity", "quarterly")
                );
                points = quarterlyRenewableTotals.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        BigDecimal total = quarterlyTotals.get(entry.getKey());
                        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
                            return null;
                        }
                        BigDecimal pct = entry.getValue()
                            .multiply(new BigDecimal("100"))
                            .divide(total, 2, RoundingMode.HALF_UP);
                        return new TimeSeriesFact.DataPoint(entry.getKey(), pct);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            } else {
                timeSeries = new TimeSeriesFact(
                    "ts:mbie:renewable_generation_gwh_quarterly:" + coverage,
                    "renewable_generation_gwh_quarterly",
                    "GWh",
                    Map.of("scope", "NZ", "granularity", "quarterly")
                );
                points = quarterlyRenewableTotals.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new TimeSeriesFact.DataPoint(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            }

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);
        }
    }

    private void buildFuelGenerationTrendFacts(
        FactPack factPack,
        ExplanationRequest request,
        List<MbieGenerationQuarterlyRecord> records
    ) {
        String fuelType = FactPackBuilderSupport.resolveUppercaseFilter(request, FilterKey.FUEL_TYPE);
        if (fuelType == null || fuelType.isBlank()) {
            return;
        }

        Map<String, BigDecimal> quarterlyFuelTotals = records.stream()
            .filter(record -> fuelType.equalsIgnoreCase(record.getFuelTypeNorm()))
            .collect(Collectors.groupingBy(
                r -> r.getPeriodYear() + "-Q" + r.getPeriodQuarter(),
                Collectors.mapping(MbieGenerationQuarterlyRecord::getGenerationGwh, 
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        // Only create time series if we have data
        if (!quarterlyFuelTotals.isEmpty()) {
            List<String> quarters = quarterlyFuelTotals.keySet().stream()
                .sorted()
                .toList();
            String coverage = !quarters.isEmpty() ? quarters.getFirst() + "_to_" + quarters.getLast() : "all_time";

            TimeSeriesFact timeSeries = new TimeSeriesFact(
                "ts:mbie:fuel_generation_gwh_quarterly:" + fuelType + ":" + coverage,
                "fuel_generation_gwh_quarterly",
                "GWh",
                Map.of("scope", "NZ", "fuel_type", fuelType, "granularity", "quarterly")
            );

            List<TimeSeriesFact.DataPoint> points = quarterlyFuelTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TimeSeriesFact.DataPoint(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

            timeSeries.setPoints(points);
            factPack.getFacts().getTimeSeries().add(timeSeries);
        }

        // Add comparison between most recent and previous quarter if we have data
        List<String> quarters = quarterlyFuelTotals.keySet().stream()
            .sorted()
            .toList();
        
        if (quarters.size() >= 2) {
            String latestQuarter = quarters.getLast();
            String previousQuarter = quarters.get(quarters.size() - 2);
            
            BigDecimal latestValue = quarterlyFuelTotals.get(latestQuarter);
            BigDecimal previousValue = quarterlyFuelTotals.get(previousQuarter);
            // Guard against division by zero; skip comparison if previous is zero or null
            if (previousValue != null && previousValue.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal delta = latestValue.subtract(previousValue);
                BigDecimal deltaPercent = delta.divide(previousValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

                ComparisonFact comparison = new ComparisonFact(
                    "cmp:mbie:generation_gwh_quarterly:" + fuelType + ":" + latestQuarter + "_vs_" + previousQuarter,
                    "generation_gwh_quarterly",
                    previousQuarter,
                    latestQuarter,
                    delta,
                    deltaPercent,
                    "GWh",
                    Map.of("fuel_type", fuelType, "granularity", "quarterly")
                );
                
                factPack.getFacts().getComparisons().add(comparison);
            }
        }
    }

    private void buildFuelTypeComparisonFacts(FactPack factPack, ExplanationRequest request, List<MbieGenerationQuarterlyRecord> records) {
        String metricType = FactPackBuilderSupport.resolveMetricType(request, METRIC_GENERATION_GWH);
        List<String> fuels = extractFuelTypeFilters(request);
        if (fuels.size() >= 2) {
            buildFuelTypeTimeSeriesFacts(factPack, records, fuels);
            buildFuelTypeLatestMetrics(factPack, records, fuels, metricType);
            return;
        }
        buildFuelTypeLatestMetrics(factPack, records, null, metricType);
    }

    private void buildGenerationMixOverviewFacts(
        FactPack factPack,
        ExplanationRequest request,
        List<MbieGenerationQuarterlyRecord> records
    ) {
        buildFuelTypeLatestMetrics(
            factPack,
            records,
            null,
            FactPackBuilderSupport.resolveMetricType(request, METRIC_GENERATION_GWH)
        );
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
        List<String> fuels,
        String metricType
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

            BigDecimal totalForQuarter = fuelTypeTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Create metric facts for each fuel type with deterministic ordering
            fuelTypeTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // deterministic ordering
                .forEach(entry -> {
                    String fuelType = entry.getKey();
                    BigDecimal total = entry.getValue();

                    BigDecimal value = total;
                    String metricName = "generation_gwh_quarterly";
                    String metricIdPrefix = "metric:mbie:generation_gwh_quarterly";
                    String unit = "GWh";
                    if (METRIC_GENERATION_SHARE_PCT.equals(metricType)) {
                        if (totalForQuarter.compareTo(BigDecimal.ZERO) == 0) {
                            return;
                        }
                        value = total
                            .multiply(new BigDecimal("100"))
                            .divide(totalForQuarter, 2, RoundingMode.HALF_UP);
                        metricName = "generation_share_pct_quarterly";
                        metricIdPrefix = "metric:mbie:generation_share_pct_quarterly";
                        unit = "%";
                    }
                    
                    MetricFact metric = new MetricFact(
                        metricIdPrefix + ":" + latestQuarter + ":" + fuelType,
                        metricName,
                        value,
                        unit,
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
        QuestionType questionType = QuestionType.fromWireValue(request.getQuestionType()).orElse(null);
        if (questionType == null) {
            factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
            factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
            factPack.getGuardrails().setRequiredCitations(new ArrayList<>());
            return;
        }

        switch (questionType) {
            case RENEWABLE_GENERATION_TREND:
            case FUEL_GENERATION_TREND:
                // If no facts, keep allowedClaims empty to trigger refusal as per tests
                boolean hasAnyFactsTrend = FactPackBuilderSupport.hasAnyFacts(factPack);
                if (hasAnyFactsTrend) {
                    factPack.getGuardrails().setAllowedClaims(Arrays.asList("trend_increase", "trend_decrease", "trend_summary"));
                } else {
                    factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                }
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
                if (!factPack.getFacts().getTimeSeries().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(FactPackBuilderSupport.stableRequiredCitations(
                        factPack.getFacts().getTimeSeries().stream().map(TimeSeriesFact::getId).toList(),
                        1
                    ));
                }
                break;
            case FUEL_TYPE_COMPARISON:
            case GENERATION_MIX_OVERVIEW:
                // If no facts, keep allowedClaims empty to trigger refusal as per tests
                boolean hasAnyFactsComp = FactPackBuilderSupport.hasAnyFacts(factPack);
                if (hasAnyFactsComp) {
                    factPack.getGuardrails().setAllowedClaims(Arrays.asList("comparison", "largest_contributor", "relative_proportion"));
                } else {
                    factPack.getGuardrails().setAllowedClaims(new ArrayList<>());
                }
                factPack.getGuardrails().setForbiddenClaims(Arrays.asList("forecast", "causation", "policy_recommendation", "site_specific_advice"));
                if (!factPack.getFacts().getTimeSeries().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(FactPackBuilderSupport.stableRequiredCitations(
                        factPack.getFacts().getTimeSeries().stream().map(TimeSeriesFact::getId).toList(),
                        Integer.MAX_VALUE
                    ));
                } else if (!factPack.getFacts().getMetrics().isEmpty()) {
                    factPack.getGuardrails().setRequiredCitations(FactPackBuilderSupport.stableRequiredCitations(
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

    private List<String> extractFuelTypeFilters(ExplanationRequest request) {
        if (request == null || request.getFilters() == null) {
            return List.of();
        }
        Object fuelA = request.getFilters().get(FilterKey.FUEL_TYPE.wireValue());
        Object fuelB = request.getFilters().get(FilterKey.FUEL_TYPE_B.wireValue());
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
