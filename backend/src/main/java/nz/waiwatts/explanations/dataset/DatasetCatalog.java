package nz.waiwatts.explanations.dataset;

import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
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
            DatasetSource.MBIE_GENERATION_ANNUAL.wireValue(),
            "MBIE Generation (Annual)",
            "MBIE",
            "annual",
            List.of(
                QuestionType.RENEWABLE_GENERATION_TREND.wireValue(),
                QuestionType.FUEL_GENERATION_TREND.wireValue(),
                QuestionType.FUEL_TYPE_COMPARISON.wireValue(),
                QuestionType.GENERATION_MIX_OVERVIEW.wireValue()
            ),
            Set.of(
                FilterKey.FUEL_TYPE.wireValue(),
                FilterKey.FUEL_TYPE_B.wireValue(),
                FilterKey.START_YEAR.wireValue(),
                FilterKey.END_YEAR.wireValue()
            )
        ),
        new DatasetDescriptor(
            DatasetSource.MBIE_GENERATION_QUARTERLY.wireValue(),
            "MBIE Generation (Quarterly)",
            "MBIE",
            "quarterly",
            List.of(
                QuestionType.RENEWABLE_GENERATION_TREND.wireValue(),
                QuestionType.FUEL_GENERATION_TREND.wireValue(),
                QuestionType.FUEL_TYPE_COMPARISON.wireValue(),
                QuestionType.GENERATION_MIX_OVERVIEW.wireValue()
            ),
            Set.of(
                FilterKey.FUEL_TYPE.wireValue(),
                FilterKey.FUEL_TYPE_B.wireValue(),
                FilterKey.START_YEAR.wireValue(),
                FilterKey.END_YEAR.wireValue()
            )
        ),
        new DatasetDescriptor(
            DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue(),
            "LAWA Water Quality State (Multi-Year)",
            "LAWA",
            "multi_year",
            List.of(
                QuestionType.WATER_QUALITY_OVERVIEW.wireValue(),
                QuestionType.WATER_QUALITY_STATE_SITES_TREND.wireValue(),
                QuestionType.GUIDELINE_EXCEEDANCE_SITES.wireValue(),
                QuestionType.REGIONAL_WATER_QUALITY.wireValue()
            ),
            Set.of(
                FilterKey.STATE_CATEGORY.wireValue(),
                FilterKey.INDICATOR.wireValue(),
                FilterKey.REGION.wireValue(),
                FilterKey.START_YEAR.wireValue(),
                FilterKey.END_YEAR.wireValue()
            )
        ),
        new DatasetDescriptor(
            DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue(),
            "LAWA Water Quality Trend (Multi-Year)",
            "LAWA",
            "multi_year",
            List.of(
                QuestionType.WATER_QUALITY_TRENDS.wireValue(),
                QuestionType.IMPROVING_SITES_TREND.wireValue(),
                QuestionType.REGIONAL_TREND_COMPARISON.wireValue()
            ),
            Set.of(
                FilterKey.INDICATOR.wireValue(),
                FilterKey.REGION.wireValue(),
                FilterKey.TREND.wireValue(),
                FilterKey.START_YEAR.wireValue(),
                FilterKey.END_YEAR.wireValue()
            )
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
