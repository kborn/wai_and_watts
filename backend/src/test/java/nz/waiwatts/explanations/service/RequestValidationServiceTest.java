package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationServiceTest {

    private final RequestValidationService service =
        new RequestValidationService(new CapabilityRegistry(new DatasetCatalog()));

    @Test
    void validatesHappyPathForMbieRequest() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("startYear", 2020, "endYear", 2024)
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertTrue(result.isValid());
    }

    @Test
    void failsWhenQuestionTypeMissing() {
        ExplanationRequest request = new ExplanationRequest(
            null,
            "mbie.generation.annual",
            null
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("VALIDATION_FAILED", result.getRefusalCategory());
        assertEquals("questionType is required", result.getRefusalMessage());
    }

    @Test
    void failsWhenDatasetSourceMissing() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            null,
            null
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("VALIDATION_FAILED", result.getRefusalCategory());
        assertEquals("datasetSource is required", result.getRefusalMessage());
    }

    @Test
    void failsForUnsupportedQuestionType() {
        ExplanationRequest request = new ExplanationRequest(
            "unsupported_type",
            "mbie.generation.annual",
            null
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("UNSUPPORTED_QUESTION_TYPE", result.getRefusalCategory());
        assertEquals("Unsupported question type: unsupported_type", result.getRefusalMessage());
    }

    @Test
    void failsForUnsupportedDatasetSource() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.future",
            null
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("VALIDATION_FAILED", result.getRefusalCategory());
        assertEquals("Unsupported dataset source: mbie.generation.future", result.getRefusalMessage());
    }

    @Test
    void failsForMbieQuestionWithLawaDataset() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            "lawa.water_quality.state.multi_year",
            null
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("DATASET_MISMATCH", result.getRefusalCategory());
        assertEquals("Parsed an MBIE generation question, but selected a LAWA dataset.", result.getRefusalMessage());
    }

    @Test
    void failsForLawaStateQuestionWithTrendDataset() {
        ExplanationRequest request = new ExplanationRequest(
            "water_quality_overview",
            "lawa.water_quality.trend.multi_year",
            null
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("DATASET_MISMATCH", result.getRefusalCategory());
        assertEquals("Parsed a LAWA state question, but selected a trend dataset.", result.getRefusalMessage());
    }

    @Test
    void failsForUnknownFilterKey() {
        ExplanationRequest request = new ExplanationRequest(
            "water_quality_overview",
            "lawa.water_quality.state.multi_year",
            Map.of("unknownFilter", "value")
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("VALIDATION_FAILED", result.getRefusalCategory());
        assertEquals("Unknown filter key: unknownFilter", result.getRefusalMessage());
    }

    @Test
    void failsWhenFilterIsNotSupportedByQuestionType() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("trend", "IMPROVING")
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("UNSUPPORTED_CAPABILITY", result.getRefusalCategory());
        assertEquals(
            "Question type renewable_generation_trend does not support filter: trend",
            result.getRefusalMessage()
        );
    }

    @Test
    void failsForInvalidYearRange() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("startYear", 2024, "endYear", 2020)
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("VALIDATION_FAILED", result.getRefusalCategory());
        assertEquals("startYear must be less than or equal to endYear", result.getRefusalMessage());
    }

    @Test
    void failsWhenStartYearIsNotInteger() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("startYear", "2020")
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("VALIDATION_FAILED", result.getRefusalCategory());
        assertEquals("startYear must be an integer", result.getRefusalMessage());
    }

    @Test
    void failsWhenEndYearIsNotInteger() {
        ExplanationRequest request = new ExplanationRequest(
            "renewable_generation_trend",
            "mbie.generation.annual",
            Map.of("endYear", 2024.0)
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("VALIDATION_FAILED", result.getRefusalCategory());
        assertEquals("endYear must be an integer", result.getRefusalMessage());
    }

    @Test
    void failsFuelComparisonWhenRequiredFiltersMissing() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_type_comparison",
            "mbie.generation.annual",
            Map.of("fuelType", "HYDRO")
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("MISSING_REQUIRED_FILTERS", result.getRefusalCategory());
        assertEquals("fuel_type_comparison requires fuelType and fuelTypeB", result.getRefusalMessage());
    }

    @Test
    void failsFuelComparisonWhenBothFuelsAreSameIgnoringCase() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_type_comparison",
            "mbie.generation.annual",
            Map.of("fuelType", "Hydro", "fuelTypeB", "hydro")
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("VALIDATION_FAILED", result.getRefusalCategory());
        assertEquals("fuelType and fuelTypeB must be different", result.getRefusalMessage());
    }

    @Test
    void passesFuelComparisonWhenBothRequiredDistinctFiltersProvided() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_type_comparison",
            "mbie.generation.annual",
            Map.of("fuelType", "Hydro", "fuelTypeB", "Wind")
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertTrue(result.isValid());
    }

    @Test
    void acceptsSupportedMetricTypeForQuestionType() {
        ExplanationRequest request = new ExplanationRequest(
            "generation_mix_overview",
            "mbie.generation.annual",
            Map.of("metricType", "generation_share_pct")
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertTrue(result.isValid());
    }

    @Test
    void rejectsUnsupportedMetricTypeForQuestionType() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            "mbie.generation.annual",
            Map.of("metricType", "generation_share_pct")
        );

        RequestValidationService.ValidationResult result = service.validateRequest(request);

        assertEquals("UNSUPPORTED_CAPABILITY", result.getRefusalCategory());
        assertEquals(
            "Question type fuel_generation_trend with dataset mbie.generation.annual does not support metricType: generation_share_pct",
            result.getRefusalMessage()
        );
    }
}
