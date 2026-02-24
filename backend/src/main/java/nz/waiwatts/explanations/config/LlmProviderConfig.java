package nz.waiwatts.explanations.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.provider.ExplanationProvider;
import nz.waiwatts.explanations.provider.OpenAiExplanationProvider;
import nz.waiwatts.explanations.provider.OpenAiResponseClient;
import nz.waiwatts.explanations.provider.StubExplanationProvider;
import nz.waiwatts.explanations.parser.IntentParser;
import nz.waiwatts.explanations.parser.OpenAiIntentParser;
import nz.waiwatts.explanations.parser.HardcodedDemoIntentParser;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public HttpClient llmHttpClient(LlmProperties properties) {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .build();
    }

    @Bean
    public ExplanationProvider explanationProvider(
        LlmProperties properties,
        ObjectMapper objectMapper,
        OpenAiResponseClient client
    ) {
        if (!properties.isConfigured()) {
            log.info("LLM not configured (model/apiKey missing). Using StubExplanationProvider.");
            return new StubExplanationProvider();
        }

        if (properties.getProvider() == LlmProvider.OPENAI) {
            log.info("LLM configured: provider=OPENAI model={}", properties.getModel());
            return new OpenAiExplanationProvider(client, objectMapper, properties.getModel());
        }

        log.warn("LLM provider '{}' not recognized. Falling back to StubExplanationProvider.", properties.getProvider());
        return new StubExplanationProvider();
    }

    @Bean
    public IntentParser intentParser(
        LlmProperties properties,
        ObjectMapper objectMapper,
        OpenAiResponseClient client,
        DatasetCatalog datasetCatalog
    ) {
        if (!properties.isConfigured()) {
            log.info("LLM not configured (model/apiKey missing). Using HardcodedDemoIntentParser.");
            return new HardcodedDemoIntentParser();
        }

        if (properties.getProvider() == LlmProvider.OPENAI) {
            log.info("Intent parser configured: provider=OPENAI model={}", properties.getModel());
            return new OpenAiIntentParser(client, objectMapper, properties.getModel(), datasetCatalog);
        }

        log.warn("LLM provider '{}' not recognized. Using HardcodedDemoIntentParser.", properties.getProvider());
        return new HardcodedDemoIntentParser();
    }

    @Bean
    public OpenAiResponseClient openAiResponseClient(
        LlmProperties properties,
        ObjectMapper objectMapper,
        HttpClient llmHttpClient
    ) {
        return new OpenAiResponseClient(llmHttpClient, objectMapper, properties);
    }
}
