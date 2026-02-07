package nz.waiwatts.explanations.config;

import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.builder.MbieGenerationAnnualFactPackBuilder;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
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


}