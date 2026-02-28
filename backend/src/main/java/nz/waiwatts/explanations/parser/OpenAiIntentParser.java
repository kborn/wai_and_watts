package nz.waiwatts.explanations.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.provider.OpenAiResponseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * OpenAI-backed intent parser.
 * <p>
 * Produces a structured ExplanationRequest or null if parsing is ambiguous.
 */
public class OpenAiIntentParser implements IntentParser {

    private static final Logger log = LoggerFactory.getLogger(OpenAiIntentParser.class);

    private static final String UNKNOWN = "unknown";

    private final OpenAiResponseClient client;
    private final ObjectMapper objectMapper;
    private final String model;
    private final CapabilityRegistry capabilityRegistry;

    public OpenAiIntentParser(
        OpenAiResponseClient client,
        ObjectMapper objectMapper,
        String model,
        CapabilityRegistry capabilityRegistry
    ) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.model = model;
        this.capabilityRegistry = capabilityRegistry;
    }

    @Override
    public ExplanationRequest parseQuestion(String question) {
        String instructions = buildInstructions();
        String input = "User question: " + question;
        String output = client.createResponseWithSchema(model, instructions, input, buildSchema(), "intent_parse");

        if (output == null || output.isBlank()) {
            log.warn("OpenAI intent parser returned empty output");
            return null;
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(output);
        } catch (Exception e) {
            log.warn("OpenAI intent parser output not valid JSON: {}", e.getMessage());
            return null;
        }

        String questionType = textOrNull(node, "questionType");
        String datasetSource = textOrNull(node, "datasetSource");

        if (questionType == null) {
            return null;
        }

        if (UNKNOWN.equals(questionType)) {
            return null;
        }

        if (!capabilityRegistry.isSupportedQuestionType(questionType)) {
            return null;
        }

        if (datasetSource != null) {
            if (UNKNOWN.equals(datasetSource)) {
                datasetSource = null;
            } else if (!capabilityRegistry.isSupportedDatasetSource(datasetSource)) {
                return null;
            }
        }

        Map<String, Object> filters = parseFilters(node.get("filters"));
        return new ExplanationRequest(questionType, datasetSource, filters == null || filters.isEmpty() ? null : filters);
    }

    private Map<String, Object> parseFilters(JsonNode filtersNode) {
        if (filtersNode == null || !filtersNode.isObject()) {
            return null;
        }

        Map<String, Object> filters = new HashMap<>();
        filtersNode.properties().forEach(entry -> {
            String key = entry.getKey();
            if (!capabilityRegistry.getAllowedFilterKeys().contains(key)) {
                return;
            }
            JsonNode value = entry.getValue();
            if (value == null || value.isNull()) {
                return;
            }
            Object normalized = normalizeFilterValue(key, value);
            if (normalized != null) {
                filters.put(key, normalized);
            }
        });

        return filters.isEmpty() ? null : filters;
    }

    private Object normalizeFilterValue(String key, JsonNode value) {
        if (FilterKey.START_YEAR.wireValue().equals(key) || FilterKey.END_YEAR.wireValue().equals(key)) {
            if (value.isInt()) {
                return value.intValue();
            }
            if (value.isTextual()) {
                String text = value.asText().trim();
                if (text.equalsIgnoreCase("null") || text.isEmpty()) {
                    return null;
                }
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }

        if (value.isTextual()) {
            String text = value.asText().trim();
            if (text.equalsIgnoreCase("null")) {
                return null;
            }
            return text.isEmpty() ? null : text;
        }

        return null;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text != null ? text.trim() : null;
    }

    private ObjectNode buildSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
        properties.set("questionType", enumNode(capabilityRegistry.getSupportedQuestionTypes(), UNKNOWN));
        properties.set("datasetSource", enumNode(capabilityRegistry.getSupportedDatasetSources(), UNKNOWN));

        ObjectNode filters = properties.putObject("filters");
        filters.put("type", "object");
        filters.put("additionalProperties", false);

        ObjectNode filterProps = filters.putObject("properties");
        filterProps.set(FilterKey.FUEL_TYPE.wireValue(), nullableType("string"));
        filterProps.set(FilterKey.FUEL_TYPE_B.wireValue(), nullableType("string"));
        filterProps.set(FilterKey.INDICATOR.wireValue(), nullableType("string"));
        filterProps.set(FilterKey.STATE_CATEGORY.wireValue(), nullableType("string"));
        filterProps.set(FilterKey.REGION.wireValue(), nullableType("string"));
        filterProps.set(FilterKey.TREND.wireValue(), nullableType("string"));
        filterProps.set(FilterKey.START_YEAR.wireValue(), nullableType("integer"));
        filterProps.set(FilterKey.END_YEAR.wireValue(), nullableType("integer"));
        filterProps.set(FilterKey.METRIC_TYPE.wireValue(), nullableEnumNode(
            capabilityRegistry.getAllSupportedMetricTypes(),
            UNKNOWN
        ));

        ArrayNode filterRequired = filters.putArray("required");
        filterRequired.add(FilterKey.FUEL_TYPE.wireValue());
        filterRequired.add(FilterKey.FUEL_TYPE_B.wireValue());
        filterRequired.add(FilterKey.INDICATOR.wireValue());
        filterRequired.add(FilterKey.STATE_CATEGORY.wireValue());
        filterRequired.add(FilterKey.REGION.wireValue());
        filterRequired.add(FilterKey.TREND.wireValue());
        filterRequired.add(FilterKey.START_YEAR.wireValue());
        filterRequired.add(FilterKey.END_YEAR.wireValue());
        filterRequired.add(FilterKey.METRIC_TYPE.wireValue());

        ArrayNode required = schema.putArray("required");
        required.add("questionType");
        required.add("datasetSource");
        required.add("filters");

        return schema;
    }

    private ObjectNode enumNode(Set<String> allowed, String extra) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "string");
        ArrayNode enums = node.putArray("enum");
        for (String value : allowed) {
            enums.add(value);
        }
        enums.add(extra);
        return node;
    }

    private ObjectNode nullableEnumNode(Set<String> allowed, String extra) {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode types = node.putArray("type");
        types.add("string");
        types.add("null");
        ArrayNode enums = node.putArray("enum");
        for (String value : new LinkedHashSet<>(allowed)) {
            enums.add(value);
        }
        enums.add(extra);
        enums.addNull();
        return node;
    }

    private ObjectNode nullableType(String type) {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode types = node.putArray("type");
        types.add(type);
        types.add("null");
        return node;
    }

    private String buildInstructions() {
        StringBuilder examples = new StringBuilder();
        capabilityRegistry.supportedQuestionTypeDescriptions().forEach((questionType, description) -> {
            var samples = capabilityRegistry.examplesForQuestion(questionType);
            if (!samples.isEmpty()) {
                examples.append("- ")
                    .append(questionType)
                    .append(" (")
                    .append(description)
                    .append("): ")
                    .append(String.join(" | ", samples.stream().limit(2).toList()))
                    .append("\n");
            }
        });

        return """
            You are an intent parser for Wai & Watts.
            Map the user question to a structured ExplanationRequest.
            Only use the supported questionType and datasetSource values provided in the schema.
            If you cannot confidently map the question, set questionType or datasetSource to "unknown".
            Use filters only when explicitly stated (startYear, endYear, fuelType, fuelTypeB, indicator, stateCategory, region, trend, metricType).
            Do not invent filters or values.
            Capability examples:
            """ + examples + """
            Return JSON only, matching the schema exactly.
            """;
    }
}
