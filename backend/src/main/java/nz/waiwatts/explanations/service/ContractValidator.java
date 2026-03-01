package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ContractValidator {

    private final CapabilityRegistry capabilityRegistry;
    private final Map<QuestionType, Function<Map<String, Object>, Result>> crossFieldValidators;

    public ContractValidator(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
        this.crossFieldValidators = buildCrossFieldValidators();
    }

    public Result validate(ExplanationRequest request) {
        return validate(request, request != null ? request.getDatasetSource() : null);
    }

    public Result validateForDatasetCandidate(ExplanationRequest request, String datasetSource) {
        return validate(request, datasetSource);
    }

    private Result validate(ExplanationRequest request, String datasetSourceOverride) {
        if (request == null) {
            return Result.failure("VALIDATION_FAILED", "request cannot be null");
        }

        String questionTypeValue = request.getQuestionType();
        if (questionTypeValue == null || questionTypeValue.trim().isEmpty()) {
            return Result.failure("VALIDATION_FAILED", "questionType is required");
        }

        String datasetSourceValue = datasetSourceOverride != null ? datasetSourceOverride : request.getDatasetSource();
        if (datasetSourceValue == null || datasetSourceValue.trim().isEmpty()) {
            return Result.failure("VALIDATION_FAILED", "datasetSource is required");
        }

        if (!capabilityRegistry.isSupportedQuestionType(questionTypeValue)) {
            return Result.failure("UNSUPPORTED_QUESTION_TYPE", "Unsupported question type: " + questionTypeValue);
        }

        if (!capabilityRegistry.isSupportedDatasetSource(datasetSourceValue)) {
            return Result.failure("VALIDATION_FAILED", "Unsupported dataset source: " + datasetSourceValue);
        }

        CapabilityRegistry.QuestionContract questionContract = capabilityRegistry.questionContract(questionTypeValue).orElse(null);
        CapabilityRegistry.DatasetContract datasetContract = capabilityRegistry.datasetContract(datasetSourceValue).orElse(null);
        if (questionContract == null || datasetContract == null) {
            return Result.failure("VALIDATION_FAILED", "Question or dataset contract was not found.");
        }

        if (!questionContract.supportedDatasets().contains(datasetContract.datasetSource())) {
            return Result.failure("DATASET_MISMATCH", mismatchMessage(questionContract.questionType(), datasetContract.datasetSource()));
        }

        Map<String, Object> filters = request.getFilters();
        if (filters == null || filters.isEmpty()) {
            return validateRequiredBindings(Map.of(), questionContract);
        }

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            CapabilityRegistry.BindingDefinition binding = capabilityRegistry.bindingDefinition(key).orElse(null);
            if (binding == null) {
                return Result.failure("VALIDATION_FAILED", "Unknown filter key: " + key);
            }
            if (!questionContract.allowedBindings().contains(binding.key())) {
                return Result.failure("UNSUPPORTED_CAPABILITY",
                    "Question type " + questionTypeValue + " does not support filter: " + key);
            }
            if (binding.key() != FilterKey.METRIC_TYPE && !datasetContract.supportedBindings().contains(binding.key())) {
                return Result.failure("UNSUPPORTED_CAPABILITY",
                    "Dataset " + datasetSourceValue + " does not support filter: " + key);
            }
            Result typeResult = validateBindingType(binding, entry.getValue());
            if (!typeResult.valid()) {
                return typeResult;
            }
        }

        Integer startYear = filters.get(FilterKey.START_YEAR.wireValue()) instanceof Integer year ? year : null;
        Integer endYear = filters.get(FilterKey.END_YEAR.wireValue()) instanceof Integer year ? year : null;
        if (startYear != null && endYear != null && startYear > endYear) {
            return Result.failure("VALIDATION_FAILED", "startYear must be less than or equal to endYear");
        }

        String metricType = filters.get(FilterKey.METRIC_TYPE.wireValue()) instanceof String s ? s.trim() : null;
        if (metricType != null && !metricType.isBlank()
            && !capabilityRegistry.isMetricTypeSupportedForQuestionAndDataset(questionTypeValue, datasetSourceValue, metricType)) {
            return Result.failure(
                "UNSUPPORTED_CAPABILITY",
                "Question type " + questionTypeValue
                    + " with dataset " + datasetSourceValue
                    + " does not support metricType: " + metricType
            );
        }

        return validateRequiredBindings(filters, questionContract);
    }

    private Result validateRequiredBindings(
        Map<String, Object> filters,
        CapabilityRegistry.QuestionContract questionContract
    ) {
        List<FilterKey> missingBindings = questionContract.requiredBindings().stream()
            .filter(required -> !hasRequiredBindingValue(required, filters))
            .toList();
        if (!missingBindings.isEmpty()) {
            String missing = missingBindings.stream()
                .map(FilterKey::wireValue)
                .collect(Collectors.joining(", "));
            return Result.failure("MISSING_REQUIRED_FILTERS", "Missing required filters: " + missing);
        }

        Function<Map<String, Object>, Result> crossFieldValidator = crossFieldValidators.get(questionContract.questionType());
        if (crossFieldValidator != null) {
            return crossFieldValidator.apply(filters);
        }
        return Result.success();
    }

    private boolean hasRequiredBindingValue(FilterKey required, Map<String, Object> filters) {
        CapabilityRegistry.BindingDefinition binding = capabilityRegistry.bindingDefinitions().get(required);
        if (binding == null) {
            return false;
        }
        Object value = filters.get(required.wireValue());
        return switch (binding.type()) {
            case STRING, METRIC_TYPE -> value instanceof String s && !s.trim().isEmpty();
            case INTEGER -> value instanceof Integer;
        };
    }

    private Result validateBindingType(CapabilityRegistry.BindingDefinition binding, Object value) {
        return switch (binding.type()) {
            case STRING, METRIC_TYPE -> (value instanceof String)
                ? Result.success()
                : Result.failure("VALIDATION_FAILED", binding.key().wireValue() + " must be a string");
            case INTEGER -> (value instanceof Integer)
                ? Result.success()
                : Result.failure("VALIDATION_FAILED", binding.key().wireValue() + " must be an integer");
        };
    }

    private String mismatchMessage(QuestionType questionType, DatasetSource datasetSource) {
        if (questionType == null || datasetSource == null) {
            return "Question type and dataset source are not a supported combination.";
        }

        boolean mbieQuestion = questionType == QuestionType.RENEWABLE_GENERATION_TREND
            || questionType == QuestionType.FUEL_GENERATION_TREND
            || questionType == QuestionType.FUEL_TYPE_COMPARISON
            || questionType == QuestionType.GENERATION_MIX_OVERVIEW;
        boolean lawaQuestion = questionType == QuestionType.WATER_QUALITY_OVERVIEW
            || questionType == QuestionType.WATER_QUALITY_STATE_SITES_TREND
            || questionType == QuestionType.REGIONAL_WATER_QUALITY
            || questionType == QuestionType.WATER_QUALITY_TRENDS
            || questionType == QuestionType.IMPROVING_SITES_TREND
            || questionType == QuestionType.REGIONAL_TREND_COMPARISON;
        boolean mbieDataset = datasetSource == DatasetSource.MBIE_GENERATION_ANNUAL
            || datasetSource == DatasetSource.MBIE_GENERATION_QUARTERLY;
        boolean lawaDataset = datasetSource == DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR
            || datasetSource == DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR;

        if (lawaQuestion && mbieDataset) {
            return "Parsed a LAWA water quality question, but selected an MBIE dataset.";
        }
        if (mbieQuestion && lawaDataset) {
            return "Parsed an MBIE generation question, but selected a LAWA dataset.";
        }
        if ((questionType == QuestionType.WATER_QUALITY_OVERVIEW
            || questionType == QuestionType.WATER_QUALITY_STATE_SITES_TREND
            || questionType == QuestionType.REGIONAL_WATER_QUALITY)
            && datasetSource == DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR) {
            return "Parsed a LAWA state question, but selected a trend dataset.";
        }
        if ((questionType == QuestionType.WATER_QUALITY_TRENDS
            || questionType == QuestionType.IMPROVING_SITES_TREND
            || questionType == QuestionType.REGIONAL_TREND_COMPARISON)
            && datasetSource == DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR) {
            return "Parsed a LAWA trend question, but selected a state dataset.";
        }
        return "Question type and dataset source are not a supported combination.";
    }

    public record Result(boolean valid, String refusalCategory, String refusalMessage) {
        public static Result success() {
            return new Result(true, null, null);
        }

        public static Result failure(String category, String message) {
            return new Result(false, category, message);
        }
    }

    private Map<QuestionType, Function<Map<String, Object>, Result>> buildCrossFieldValidators() {
        LinkedHashMap<QuestionType, Function<Map<String, Object>, Result>> validators = new LinkedHashMap<>();
        validators.put(QuestionType.FUEL_TYPE_COMPARISON, this::validateDistinctFuelComparison);
        return validators;
    }

    private Result validateDistinctFuelComparison(Map<String, Object> filters) {
        String fuelA = filters.get(FilterKey.FUEL_TYPE.wireValue()) instanceof String s ? s.trim() : null;
        String fuelB = filters.get(FilterKey.FUEL_TYPE_B.wireValue()) instanceof String s ? s.trim() : null;
        if (fuelA != null && fuelA.equalsIgnoreCase(fuelB)) {
            return Result.failure("VALIDATION_FAILED", "fuelType and fuelTypeB must be different");
        }
        return Result.success();
    }
}
