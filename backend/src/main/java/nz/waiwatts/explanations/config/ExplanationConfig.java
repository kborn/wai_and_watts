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

/**
 * Spring configuration for Phase 11 explanation components
 */
@Configuration
public class ExplanationConfig {

    @Bean
    public FactPackBuilder mbieGenerationAnnualFactPackBuilder(MbieGenerationAnnualRecordRepository repository) {
        return new MbieGenerationAnnualFactPackBuilder(repository);
    }

    @Bean
    public FactPackBuilder mbieGenerationQuarterlyFactPackBuilder(MbieGenerationQuarterlyRecordRepository repository) {
        return new MbieGenerationQuarterlyFactPackBuilder(repository);
    }

    @Bean
    public FactPackBuilder lawaStateMultiYearFactPackBuilder(LawaStateMultiYearRecordRepository repository) {
        return new LawaStateMultiYearFactPackBuilder(repository);
    }

    @Bean
    public FactPackBuilder lawaTrendMultiYearFactPackBuilder(LawaTrendMultiYearRecordRepository repository) {
        return new LawaTrendMultiYearFactPackBuilder(repository);
    }

}