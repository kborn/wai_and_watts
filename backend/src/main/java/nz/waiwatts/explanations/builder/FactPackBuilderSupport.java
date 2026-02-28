package nz.waiwatts.explanations.builder;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

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
