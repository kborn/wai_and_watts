package nz.waiwatts.explanations.config;

import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.builder.LawaStateFactPackSettings;
import nz.waiwatts.explanations.builder.MbieGenerationAnnualFactPackBuilder;
import nz.waiwatts.explanations.builder.MbieGenerationQuarterlyFactPackBuilder;
import nz.waiwatts.explanations.builder.LawaStateMultiYearFactPackBuilder;
import nz.waiwatts.explanations.builder.LawaTrendMultiYearFactPackBuilder;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring configuration for Phase 11 explanation components
 */
@Configuration
public class ExplanationConfig {

    @Bean
    @Order(10)
    public FactPackBuilder mbieGenerationAnnualFactPackBuilder(MbieGenerationAnnualRecordRepository repository) {
        return new MbieGenerationAnnualFactPackBuilder(repository);
    }

    @Bean
    @Order(20)
    public FactPackBuilder mbieGenerationQuarterlyFactPackBuilder(MbieGenerationQuarterlyRecordRepository repository) {
        return new MbieGenerationQuarterlyFactPackBuilder(repository);
    }

    @Bean
    @Order(30)
    public FactPackBuilder lawaStateMultiYearFactPackBuilder(
        LawaStateMultiYearRecordRepository repository,
        LawaStateCategoryProperties lawaStateCategoryProperties
    ) {
        return new LawaStateMultiYearFactPackBuilder(
            repository,
            new LawaStateFactPackSettings(
                buildStateCategoryBands(lawaStateCategoryProperties),
                lawaStateCategoryProperties.getRegionalSample().getTopK(),
                lawaStateCategoryProperties.getRegionalSample().getBottomK()
            )
        );
    }

    @Bean
    @Order(40)
    public FactPackBuilder lawaTrendMultiYearFactPackBuilder(LawaTrendMultiYearRecordRepository repository) {
        return new LawaTrendMultiYearFactPackBuilder(repository);
    }

    private Map<String, Set<String>> buildStateCategoryBands(LawaStateCategoryProperties properties) {
        Map<String, List<String>> raw = properties != null ? properties.getStateCategoryBands() : null;
        if (raw == null || raw.isEmpty()) {
            return Map.of(
                "EXCELLENT", Set.of("A"),
                "GOOD", Set.of("B"),
                "FAIR", Set.of("C"),
                "POOR", Set.of("D", "E")
            );
        }

        Map<String, Set<String>> normalized = new LinkedHashMap<>();
        raw.forEach((category, bands) -> {
            if (category == null || bands == null) {
                return;
            }
            Set<String> cleanedBands = bands.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeBand)
                .filter(b -> !b.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!cleanedBands.isEmpty()) {
                normalized.put(category.trim().toUpperCase(Locale.ROOT), cleanedBands);
            }
        });
        return normalized;
    }

    private String normalizeBand(String band) {
        if (band == null) {
            return "";
        }
        return band.trim().toUpperCase(Locale.ROOT);
    }
}
