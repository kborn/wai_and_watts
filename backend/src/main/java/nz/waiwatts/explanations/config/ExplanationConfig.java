package nz.waiwatts.explanations.config;

import nz.waiwatts.explanations.builder.FactPackBuilder;
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
        return new LawaStateMultiYearFactPackBuilder(repository, lawaStateCategoryProperties);
    }

    @Bean
    @Order(40)
    public FactPackBuilder lawaTrendMultiYearFactPackBuilder(LawaTrendMultiYearRecordRepository repository) {
        return new LawaTrendMultiYearFactPackBuilder(repository);
    }

}
