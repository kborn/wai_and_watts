package nz.waiwatts.explanations.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nz.waiwatts.explanations.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OpenAiApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiApiClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final LlmProperties properties;

    public OpenAiApiClient(HttpClient httpClient, ObjectMapper objectMapper, LlmProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String createResponse(String model, String instructions, String input) {
        ObjectNode payload = basePayload(model, instructions, input);
        applyJsonObjectFormat(payload);
        return send(payload);
    }

    public String createResponseWithSchema(
        String model,
        String instructions,
        String input,
        JsonNode schema,
        String schemaName
    ) {
        ObjectNode payload = basePayload(model, instructions, input);
        applyJsonSchemaFormat(payload, schema, schemaName);
        return send(payload);
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        if (root.hasNonNull("output_text")) {
            return root.get("output_text").asText();
        }

        if (!root.has("output")) {
            return null;
        }

        StringBuilder output = new StringBuilder();
        for (JsonNode item : root.get("output")) {
            JsonNode content = item.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                String type = contentItem.hasNonNull("type") ? contentItem.get("type").asText() : "";
                if ("output_text".equals(type) || "text".equals(type)) {
                    output.append(contentItem.get("text").asText());
                }
            }
        }

        String result = output.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private ObjectNode basePayload(String model, String instructions, String input) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("input", input);
        payload.put("instructions", instructions);
        payload.put("temperature", properties.getTemperature());
        payload.put("max_output_tokens", properties.getMaxOutputTokens());
        return payload;
    }

    private void applyJsonObjectFormat(ObjectNode payload) {
        ObjectNode text = payload.putObject("text");
        ObjectNode format = text.putObject("format");
        format.put("type", "json_object");
    }

    private void applyJsonSchemaFormat(ObjectNode payload, JsonNode schema, String schemaName) {
        ObjectNode text = payload.putObject("text");
        ObjectNode format = text.putObject("format");
        format.put("type", "json_schema");
        format.put("name", schemaName);
        format.put("strict", true);
        format.set("schema", schema);
    }

    private String send(ObjectNode payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);

            String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
            if (baseUrl == null) {
                log.warn("OpenAI base URL not configured");
                return null;
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/responses"))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI response error: status={} body={}", response.statusCode(), response.body());
                return null;
            }

            return extractOutputText(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("OpenAI response call interrupted: {}", e.getMessage());
            return null;
        } catch (IOException e) {
            log.warn("OpenAI response call failed: {}", e.getMessage());
            return null;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
