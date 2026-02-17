package nz.waiwatts.explanations.dataset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatasetCatalogTest {

    @Test
    void mbieAnnualSupportsRenewableGenerationTrend() {
        DatasetCatalog catalog = new DatasetCatalog();

        DatasetDescriptor descriptor = catalog.findBySource("mbie.generation.annual")
            .orElseThrow(() -> new AssertionError("Missing mbie.generation.annual in catalog"));

        assertTrue(descriptor.supportedQuestionTypes().contains("renewable_generation_trend"));
        assertTrue(descriptor.supportedFilters().contains("startYear"));
        assertTrue(descriptor.supportedFilters().contains("endYear"));
    }
}
