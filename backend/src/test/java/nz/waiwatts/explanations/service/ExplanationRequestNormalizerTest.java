package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationRequestNormalizerTest {

    private final ExplanationRequestNormalizer normalizer =
        new ExplanationRequestNormalizer(new CapabilityRegistry(new DatasetCatalog()));

    @Test
    void normalizesLawaTrendBindingsAndAppliesImplicitTrend() {
        ExplanationRequest normalized = normalizer.normalize(new ExplanationRequest(
            "improving_sites_trend",
            "lawa.water_quality.trend.multi_year",
            Map.of(
                "indicator", "E. coli",
                "region", "Auckland"
            )
        ));

        assertNotNull(normalized.getFilters());
        assertEquals("ecoli", normalized.getFilters().get("indicator"));
        assertEquals("auckland", normalized.getFilters().get("region"));
        assertEquals("improving", normalized.getFilters().get("trend"));
    }

    @Test
    void removesUnknownMetricTypeAndPromotesFuelComparison() {
        ExplanationRequest normalized = normalizer.normalize(new ExplanationRequest(
            "fuel_generation_trend",
            "mbie.generation.annual",
            Map.of(
                "fuelType", "hydro",
                "fuelTypeB", "wind",
                "metricType", "unknown"
            )
        ));

        assertEquals("fuel_type_comparison", normalized.getQuestionType());
        assertNotNull(normalized.getFilters());
        assertFalse(normalized.getFilters().containsKey("metricType"));
        assertEquals("hydro", normalized.getFilters().get("fuelType"));
        assertEquals("wind", normalized.getFilters().get("fuelTypeB"));
    }

    @Test
    void removesNullishCategoricalBindings() {
        ExplanationRequest normalized = normalizer.normalize(new ExplanationRequest(
            "water_quality_trends",
            "lawa.water_quality.trend.multi_year",
            Map.of(
                "indicator", "unknown",
                "region", "NULL",
                "trend", "null",
                "startYear", 2015
            )
        ));

        assertNotNull(normalized.getFilters());
        assertEquals(1, normalized.getFilters().size());
        assertEquals(2015, normalized.getFilters().get("startYear"));
        assertTrue(normalized.getFilters().containsKey("startYear"));
    }
}
