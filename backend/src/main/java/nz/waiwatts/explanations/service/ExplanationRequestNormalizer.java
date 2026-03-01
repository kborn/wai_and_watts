package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.lawa.LawaBindingNormalization;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ExplanationRequestNormalizer {

    private static final Set<String> NULLISH_TOKENS = Set.of("unknown", "null");
    private static final Set<String> NULLABLE_CATEGORICAL_FILTERS = Set.of(
        FilterKey.FUEL_TYPE.wireValue(),
        FilterKey.FUEL_TYPE_B.wireValue(),
        FilterKey.INDICATOR.wireValue(),
        FilterKey.STATE_CATEGORY.wireValue(),
        FilterKey.REGION.wireValue(),
        FilterKey.TREND.wireValue(),
        FilterKey.METRIC_TYPE.wireValue()
    );

    private final CapabilityRegistry capabilityRegistry;

    public ExplanationRequestNormalizer(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    public ExplanationRequest normalize(ExplanationRequest request) {
        return normalizeForDataset(request, request != null ? request.getDatasetSource() : null);
    }

    public ExplanationRequest normalizeForDataset(ExplanationRequest request, String datasetSourceOverride) {
        if (request == null) {
            return null;
        }

        String questionType = request.getQuestionType();
        String datasetSource = datasetSourceOverride != null ? datasetSourceOverride : request.getDatasetSource();
        Map<String, Object> filters = copyFilters(request.getFilters());

        removeNullishCategoricalFilters(filters);

        questionType = normalizeQuestionType(questionType, filters);

        datasetSource = normalizeDatasetForQuestionType(questionType, datasetSource);
        applyImplicitBindings(questionType, filters);
        removeUnsupportedMetricType(questionType, datasetSource, filters);
        normalizeBindingsForDataset(datasetSource, filters);

        return new ExplanationRequest(
            questionType,
            datasetSource,
            filters.isEmpty() ? null : filters
        );
    }

    private Map<String, Object> copyFilters(Map<String, Object> filters) {
        return filters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(filters);
    }

    private void removeNullishCategoricalFilters(Map<String, Object> filters) {
        NULLABLE_CATEGORICAL_FILTERS.forEach(filterKey -> {
            Object value = filters.get(filterKey);
            if (value instanceof String s && NULLISH_TOKENS.contains(s.trim().toLowerCase())) {
                filters.remove(filterKey);
            }
        });
    }

    private String normalizeQuestionType(String questionType, Map<String, Object> filters) {
        CapabilityRegistry.QuestionContract questionContract = capabilityRegistry.questionContract(questionType).orElse(null);
        if (questionContract == null) {
            return questionType;
        }
        return questionContract.promotionRules().stream()
            .filter(rule -> rule.requiredBindings().stream().allMatch(binding -> hasBindingValue(binding, filters)))
            .map(rule -> rule.targetQuestionType().wireValue())
            .findFirst()
            .orElse(questionType);
    }

    private String normalizeDatasetForQuestionType(String questionType, String datasetSource) {
        if (questionType == null || datasetSource == null) {
            return datasetSource;
        }

        CapabilityRegistry.QuestionContract questionContract = capabilityRegistry.questionContract(questionType).orElse(null);
        CapabilityRegistry.DatasetContract datasetContract = capabilityRegistry.datasetContract(datasetSource).orElse(null);
        if (questionContract == null || datasetContract == null) {
            return datasetSource;
        }
        if (questionContract.supportedDatasets().contains(datasetContract.datasetSource())) {
            return datasetSource;
        }
        CapabilityRegistry.DatasetKind expectedKind = questionContract.expectedDatasetKind();
        if (expectedKind == null || expectedKind == CapabilityRegistry.DatasetKind.MIXED) {
            return datasetSource;
        }
        return questionContract.supportedDatasets().stream()
            .filter(source -> capabilityRegistry.datasetContract(source.wireValue())
                .map(contract -> contract.datasetKind() == expectedKind)
                .orElse(false))
            .findFirst()
            .map(DatasetSource::wireValue)
            .orElse(datasetSource);
    }

    private void applyImplicitBindings(String questionType, Map<String, Object> filters) {
        capabilityRegistry.questionContract(questionType)
            .ifPresent(contract -> contract.implicitBindings().forEach((filterKey, value) ->
                filters.putIfAbsent(filterKey.wireValue(), value)
            ));
    }

    private void removeUnsupportedMetricType(String questionType, String datasetSource, Map<String, Object> filters) {
        Object metricType = filters.get(FilterKey.METRIC_TYPE.wireValue());
        if (!(metricType instanceof String metricTypeValue) || metricTypeValue.isBlank()) {
            return;
        }

        boolean supported = datasetSource == null || datasetSource.isBlank()
            ? capabilityRegistry.isMetricTypeSupportedForQuestion(questionType, metricTypeValue)
            : capabilityRegistry.isMetricTypeSupportedForQuestionAndDataset(questionType, datasetSource, metricTypeValue);
        if (!supported) {
            filters.remove(FilterKey.METRIC_TYPE.wireValue());
        }
    }

    private void normalizeBindingsForDataset(String datasetSource, Map<String, Object> filters) {
        if (datasetSource == null || datasetSource.isBlank() || filters.isEmpty()) {
            return;
        }

        if (datasetSource.equals(DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue())) {
            normalizeStringBinding(filters, FilterKey.INDICATOR, LawaBindingNormalization::normalizeStateIndicatorForQuery);
            normalizeStringBinding(filters, FilterKey.REGION, LawaBindingNormalization::normalizeRegionForQuery);
            normalizeStringBinding(filters, FilterKey.STATE_CATEGORY, value -> value.trim().toUpperCase());
            return;
        }

        if (datasetSource.equals(DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue())) {
            normalizeStringBinding(filters, FilterKey.INDICATOR, LawaBindingNormalization::normalizeTrendIndicatorForQuery);
            normalizeStringBinding(filters, FilterKey.REGION, LawaBindingNormalization::normalizeRegionForQuery);
            normalizeStringBinding(filters, FilterKey.TREND, value -> value.trim().toLowerCase());
        }
    }

    private void normalizeStringBinding(
        Map<String, Object> filters,
        FilterKey filterKey,
        java.util.function.Function<String, String> normalizer
    ) {
        Object value = filters.get(filterKey.wireValue());
        if (value instanceof String s && !s.isBlank()) {
            filters.put(filterKey.wireValue(), normalizer.apply(s));
        }
    }

    private boolean hasBindingValue(FilterKey filterKey, Map<String, Object> filters) {
        Object value = filters.get(filterKey.wireValue());
        if (value instanceof String s) {
            return !s.isBlank();
        }
        return value != null;
    }
}
