package nz.waiwatts.explanations.dataset;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Small in-memory dataset catalog for dataset selection.
 */
@Component
public class DatasetCatalog {

    private final List<DatasetDescriptor> datasets = List.of(
        new DatasetDescriptor(
            "mbie.generation.annual",
            "MBIE Generation (Annual)",
            "MBIE",
            "annual",
            List.of(
                "renewable_generation_trend",
                "fuel_generation_trend",
                "fuel_type_comparison",
                "generation_mix_overview"
            ),
            Set.of("fuelType", "fuelTypeB", "startYear", "endYear")
        ),
        new DatasetDescriptor(
            "mbie.generation.quarterly",
            "MBIE Generation (Quarterly)",
            "MBIE",
            "quarterly",
            List.of(
                "renewable_generation_trend",
                "fuel_generation_trend",
                "fuel_type_comparison",
                "generation_mix_overview"
            ),
            Set.of("fuelType", "fuelTypeB", "startYear", "endYear")
        ),
        new DatasetDescriptor(
            "lawa.water_quality.state.multi_year",
            "LAWA Water Quality State (Multi-Year)",
            "LAWA",
            "multi_year",
            List.of(
                "water_quality_overview",
                "water_quality_state_sites_trend",
                "regional_water_quality"
            ),
            Set.of("stateCategory", "indicator", "region", "startYear", "endYear")
        ),
        new DatasetDescriptor(
            "lawa.water_quality.trend.multi_year",
            "LAWA Water Quality Trend (Multi-Year)",
            "LAWA",
            "multi_year",
            List.of(
                "water_quality_trends",
                "improving_sites_trend",
                "regional_trend_comparison"
            ),
            Set.of("indicator", "region", "trend", "startYear", "endYear")
        )
    );

    public List<DatasetDescriptor> getDatasets() {
        return datasets;
    }

    public Optional<DatasetDescriptor> findBySource(String datasetSource) {
        if (datasetSource == null || datasetSource.isBlank()) {
            return Optional.empty();
        }
        String normalized = datasetSource.trim();
        return datasets.stream()
            .filter(ds -> ds.datasetSource().equalsIgnoreCase(normalized))
            .findFirst();
    }
}
