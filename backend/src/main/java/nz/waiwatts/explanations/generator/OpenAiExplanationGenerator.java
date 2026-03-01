package nz.waiwatts.explanations.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.llm.OpenAiApiClient;
import nz.waiwatts.explanations.service.CitationValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * OpenAI-backed explanation generator.
 * <p>
 * Uses the Responses API in JSON mode and returns a structured Explanation.
 * All facts must come from the provided FactPack.
 * Citation validation uses the shared validation layer to stay aligned with stub behavior.
 */
public class OpenAiExplanationGenerator implements ExplanationGenerator {

    private static final Logger log = LoggerFactory.getLogger(OpenAiExplanationGenerator.class);

    private final OpenAiApiClient client;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiExplanationGenerator(OpenAiApiClient client, ObjectMapper objectMapper, String model) {
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
        if (explanation == null) {
            return false;
        }
        List<String> required = factPack.getGuardrails() != null && factPack.getGuardrails().getRequiredCitations() != null
            ? factPack.getGuardrails().getRequiredCitations()
            : List.of();
        return CitationValidationUtil.validateRequiredCitations(required, explanation.getCitations());
    }

    private Explanation parseExplanation(String output) {
        try {
            Explanation parsed = objectMapper.readValue(output, Explanation.class);
            if (parsed == null) {
                return null;
            }

            // Non-refusal responses must contain a non-empty explanation text.
            // This keeps provider/stub contracts aligned and avoids silent malformed payloads.
            if (!parsed.isRefusal()) {
                String explanationText = parsed.getExplanationText();
                if (explanationText == null || explanationText.isBlank()) {
                    log.warn("LLM JSON output missing explanationText for non-refusal response");
                    return null;
                }
            }

            return parsed;
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
