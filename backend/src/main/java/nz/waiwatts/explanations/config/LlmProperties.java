package nz.waiwatts.explanations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM configuration properties.
 *
 * When model + apiKey are provided, the LLM provider is enabled.
 * Otherwise, the system falls back to stub implementations.
 */
@ConfigurationProperties(prefix = "waiwatts.llm")
public class LlmProperties {

    private LlmProvider provider;
    private String model;
    private String apiKey;
    private String baseUrl;
    private int timeoutSeconds = 30;
    private double temperature = 0.2;
    private int maxOutputTokens = 500;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public boolean isConfigured() {
        return provider != null && hasText(model) && hasText(apiKey) && hasText(baseUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public LlmProvider getProvider() {
        return provider;
    }

    public void setProvider(LlmProvider provider) {
        this.provider = provider;
    }
}

