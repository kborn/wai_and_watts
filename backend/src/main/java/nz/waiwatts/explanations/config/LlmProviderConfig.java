package nz.waiwatts.explanations.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.generator.ExplanationGenerator;
import nz.waiwatts.explanations.generator.OpenAiExplanationGenerator;
import nz.waiwatts.explanations.generator.DemoExplanationGenerator;
import nz.waiwatts.explanations.llm.OpenAiApiClient;
import nz.waiwatts.explanations.parser.IntentParser;
import nz.waiwatts.explanations.parser.OpenAiIntentParser;
import nz.waiwatts.explanations.parser.DemoIntentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderConfig.class);

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public HttpClient llmHttpClient(LlmProperties properties) {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .build();
    }

    @Bean
    public ExplanationGenerator explanationGenerator(
        LlmProperties properties,
        ObjectMapper objectMapper,
        OpenAiApiClient client
    ) {
        if (!properties.isConfigured()) {
            log.info("LLM not configured (model/apiKey missing). Using DemoExplanationGenerator.");
            return new DemoExplanationGenerator();
        }

        if (properties.getProvider() == LlmProvider.OPENAI) {
            log.info("LLM configured: provider=OPENAI model={}", properties.getModel());
            return new OpenAiExplanationGenerator(client, objectMapper, properties.getModel());
        }

        log.warn("LLM provider '{}' not recognized. Falling back to DemoExplanationGenerator.", properties.getProvider());
        return new DemoExplanationGenerator();
    }

    @Bean
    public IntentParser intentParser(
        LlmProperties properties,
        ObjectMapper objectMapper,
        OpenAiApiClient client,
        CapabilityRegistry capabilityRegistry
    ) {
        if (!properties.isConfigured()) {
            log.info("LLM not configured (model/apiKey missing). Using DemoIntentParser.");
            return new DemoIntentParser();
        }

        if (properties.getProvider() == LlmProvider.OPENAI) {
            log.info("Intent parser configured: provider=OPENAI model={}", properties.getModel());
            return new OpenAiIntentParser(client, objectMapper, properties.getModel(), capabilityRegistry);
        }

        log.warn("LLM provider '{}' not recognized. Using DemoIntentParser.", properties.getProvider());
        return new DemoIntentParser();
    }

    @Bean
    public OpenAiApiClient openAiApiClient(
        LlmProperties properties,
        ObjectMapper objectMapper,
        HttpClient llmHttpClient
    ) {
        return new OpenAiApiClient(llmHttpClient, objectMapper, properties);
    }
}
