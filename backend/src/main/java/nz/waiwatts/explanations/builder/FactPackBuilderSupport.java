package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.dto.ClassificationFact;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.dto.MetricFact;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class FactPackBuilderSupport {

    private FactPackBuilderSupport() {
    }

    static FactPack initializeFactPack(ExplanationRequest request, String datasetSourceCode) {
        FactPack factPack = new FactPack();

        FactPack.RequestContext requestContext = new FactPack.RequestContext();
        requestContext.setQuestionType(request.getQuestionType());
        requestContext.setDatasetScope(List.of(datasetSourceCode));
        requestContext.setFiltersApplied(request.getFilters());
        factPack.setRequestContext(requestContext);

        FactPack.Provenance provenance = new FactPack.Provenance();
        provenance.setDatasetSources(new ArrayList<>());
        factPack.setProvenance(provenance);

        return factPack;
    }

    static boolean supportsDatasetSource(ExplanationRequest request, String datasetSourceCode) {
        String topLevelDatasetSource = request.getDatasetSource();
        if (topLevelDatasetSource != null) {
            return datasetSourceCode.equals(topLevelDatasetSource);
        }

        Map<String, Object> filters = request.getFilters();
        if (filters != null) {
            Object datasetSourceFilter = filters.get(FilterKey.DATASET_SOURCE.wireValue());
            return datasetSourceCode.equals(String.valueOf(datasetSourceFilter));
        }

        return false;
    }

    static <T> List<T> pinToCanonicalRelease(List<T> records, Function<T, DatasetRelease> releaseExtractor) {
        if (records == null || records.isEmpty()) {
            return records == null ? List.of() : records;
        }

        Optional<DatasetRelease> canonicalRelease = records.stream()
            .map(releaseExtractor)
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
            .filter(record -> isSameRelease(releaseExtractor.apply(record), target))
            .toList();
    }

    static <T> List<FactPack.DatasetSourceProvenance> buildGroupedProvenance(
        List<T> records,
        String datasetSourceCode,
        Function<T, DatasetRelease> releaseExtractor,
        Function<List<T>, String> periodCoverageResolver
    ) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<T>> byReleaseKey = records.stream()
            .map(record -> Map.entry(record, releaseExtractor.apply(record)))
            .filter(entry -> entry.getValue() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                entry -> releaseKey(entry.getValue()),
                java.util.stream.Collectors.mapping(Map.Entry::getKey, java.util.stream.Collectors.toList())
            ));

        return byReleaseKey.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                List<T> group = entry.getValue();
                DatasetRelease release = releaseExtractor.apply(group.getFirst());
                FactPack.DatasetSourceProvenance source = new FactPack.DatasetSourceProvenance();
                source.setDatasetSourceCode(datasetSourceCode);
                source.setDatasetReleaseId(entry.getKey());
                source.setContentHash(release != null ? release.getContentHash() : null);
                source.setPeriodCoverage(periodCoverageResolver.apply(group));
                return source;
            })
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    static String releaseKey(DatasetRelease release) {
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

    static boolean hasAnyFacts(FactPack factPack) {
        return !(factPack.getFacts().getClassifications().isEmpty()
            && factPack.getFacts().getMetrics().isEmpty()
            && factPack.getFacts().getTimeSeries().isEmpty());
    }

    static List<String> stableRequiredCitations(List<String> ids, int limit) {
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

    static String resolveMetricType(ExplanationRequest request, String fallback) {
        if (request == null || request.getFilters() == null) {
            return fallback;
        }
        Object metricType = request.getFilters().get(FilterKey.METRIC_TYPE.wireValue());
        if (metricType instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return fallback;
    }

    static String resolveUppercaseFilter(ExplanationRequest request, FilterKey filterKey) {
        if (request == null || request.getFilters() == null) {
            return null;
        }
        Object filterValue = request.getFilters().get(filterKey.wireValue());
        if (filterValue instanceof String s && !s.isBlank()) {
            return s.trim().toUpperCase();
        }
        return null;
    }

    static Set<String> selectDeterministicRegionalSample(
        Map<String, Long> totalSitesByRegion,
        Map<String, Long> selectedSitesByRegion,
        int topK,
        int bottomK
    ) {
        List<Map.Entry<String, BigDecimal>> ranked = totalSitesByRegion.entrySet().stream()
            .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
            .map(entry -> {
                String region = entry.getKey();
                long total = entry.getValue();
                long selected = selectedSitesByRegion.getOrDefault(region, 0L);
                BigDecimal percent = new BigDecimal(selected)
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
        ranked.stream().sorted(highToLow).limit(topK).forEach(entry -> selected.add(entry.getKey()));
        ranked.stream().sorted(lowToHigh).limit(bottomK).forEach(entry -> selected.add(entry.getKey()));
        return selected;
    }

    static List<String> buildRegionalRequiredCitations(FactPack factPack, String metricFamilyPrefix) {
        List<String> classFamilies = factPack.getFacts().getClassifications().stream()
            .map(ClassificationFact::getId)
            .map(id -> id.replaceFirst(":[^:]+$", ":*"))
            .toList();
        List<String> metricFamilies = factPack.getFacts().getMetrics().stream()
            .map(MetricFact::getId)
            .filter(id -> id.startsWith(metricFamilyPrefix))
            .map(id -> metricFamilyPrefix + "*")
            .toList();
        List<String> families = new ArrayList<>();
        families.addAll(classFamilies);
        families.addAll(metricFamilies);
        return stableRequiredCitations(families, Integer.MAX_VALUE);
    }

    private static boolean isSameRelease(DatasetRelease left, DatasetRelease right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getId() != null && right.getId() != null) {
            return left.getId().equals(right.getId());
        }
        return Objects.equals(left.getContentHash(), right.getContentHash());
    }
}
