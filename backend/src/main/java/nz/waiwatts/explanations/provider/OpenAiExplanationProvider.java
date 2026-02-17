package nz.waiwatts.explanations.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * OpenAI-backed ExplanationProvider.
 *
 * Uses the Responses API in JSON mode and returns a structured Explanation.
 * All facts must come from the provided FactPack.
 */
public class OpenAiExplanationProvider implements ExplanationProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiExplanationProvider.class);

    private final OpenAiResponseClient client;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiExplanationProvider(OpenAiResponseClient client, ObjectMapper objectMapper, String model) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public Explanation generateExplanation(String questionType, FactPack factPack) {
        String factPackJson;
        try {
            factPackJson = objectMapper.writeValueAsString(factPack);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize FactPack: {}", e.getMessage());
            return Explanation.refusal("Unable to serialize FactPack for LLM");
        }

        String instructions = buildInstructions();
        String input = buildInput(questionType, factPackJson);

        String output = client.createResponse(model, instructions, input);
        if (output == null || output.isBlank()) {
            return Explanation.refusal("LLM provider returned no response");
        }

        Explanation parsed = parseExplanation(output);
        if (parsed == null) {
            return Explanation.refusal("LLM response parse failure");
        }

        if (parsed.getCitations() == null) {
            parsed.setCitations(List.of());
        }
        return parsed;
    }

    @Override
    public boolean validateCitations(Explanation explanation, FactPack factPack) {
        if (explanation == null || explanation.getCitations() == null) {
            return false;
        }
        List<String> required = factPack.getGuardrails() != null && factPack.getGuardrails().getRequiredCitations() != null
            ? factPack.getGuardrails().getRequiredCitations()
            : List.of();
        List<String> actual = explanation.getCitations();
        return required.stream().allMatch(req -> hasMatchingCitation(req, actual));
    }

    private boolean hasMatchingCitation(String requiredId, List<String> actualIds) {
        if (requiredId == null || requiredId.isBlank()) return true;
        if (actualIds == null || actualIds.isEmpty()) return false;
        String req = requiredId.trim().toLowerCase(Locale.ROOT);
        List<String> normalizedActualIds = actualIds.stream()
            .filter(id -> id != null && !id.isBlank())
            .map(id -> id.trim().toLowerCase(Locale.ROOT))
            .toList();
        if (normalizedActualIds.contains(req)) return true;

        if (req.endsWith(":*")) {
            String wildcardPrefix = req.substring(0, req.length() - 1);
            return normalizedActualIds.stream().anyMatch(act -> act.startsWith(wildcardPrefix));
        }

        boolean isLawaMetricOrClass = req.startsWith("metric:lawa:") || req.startsWith("class:lawa:");
        if (!isLawaMetricOrClass) return false;
        int lastColon = req.lastIndexOf(':');
        if (lastColon <= 0) return false;
        String familyPrefix = req.substring(0, lastColon + 1);
        return normalizedActualIds.stream().anyMatch(act -> act.startsWith(familyPrefix));
    }

    private Explanation parseExplanation(String output) {
        try {
            return objectMapper.readValue(output, Explanation.class);
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON output: {}", e.getMessage());
            return null;
        }
    }

    private String buildInstructions() {
        return """
            You are the Wai & Watts explanation engine.
            Use ONLY the provided Fact Pack. Do NOT use external knowledge.
            Do NOT mention any region name or numeric value unless that exact region/value appears in the provided facts.
            If facts are a regional subset, do not imply coverage beyond those regions.
            Always respond as a single JSON object with keys:
            explanationText (string), citations (array of fact IDs), isRefusal (boolean), refusalReason (string or null).
            If the Fact Pack is insufficient, set isRefusal=true and provide a brief refusalReason.
            Citations MUST reference Fact Pack fact IDs and MUST include all requiredCitations from guardrails.
            """;
    }

    private String buildInput(String questionType, String factPackJson) {
        return "Question type: " + questionType + "\nFact Pack JSON:\n" + factPackJson;
    }
}
