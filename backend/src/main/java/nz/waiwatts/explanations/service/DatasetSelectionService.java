package nz.waiwatts.explanations.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.capabilities.types.DatasetSource;
import nz.waiwatts.explanations.capabilities.types.QuestionType;
import nz.waiwatts.explanations.config.LlmProvider;
import nz.waiwatts.explanations.config.LlmProperties;
import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dataset.DatasetDescriptor;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.llm.OpenAiApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class DatasetSelectionService {

    private static final Logger log = LoggerFactory.getLogger(DatasetSelectionService.class);
    private static final Pattern QUARTER_SIGNAL_PATTERN = Pattern.compile(
        "(?i)\\bquarter(?:ly)?\\b|\\bq[1-4]\\b"
    );

    private final DatasetCatalog datasetCatalog;
    private final OpenAiApiClient client;
    private final ObjectMapper objectMapper;
    private final LlmProperties llmProperties;
    private final QuestionTypeCatalog questionTypeCatalog;
    private final CapabilityRegistry capabilityRegistry;
    private final ContractValidator contractValidator;

    public DatasetSelectionService(
        DatasetCatalog datasetCatalog,
        OpenAiApiClient client,
        ObjectMapper objectMapper,
        LlmProperties llmProperties,
        QuestionTypeCatalog questionTypeCatalog,
        CapabilityRegistry capabilityRegistry,
        ContractValidator contractValidator
    ) {
        this.datasetCatalog = datasetCatalog;
        this.client = client;
        this.objectMapper = objectMapper;
        this.llmProperties = llmProperties;
        this.questionTypeCatalog = questionTypeCatalog;
        this.capabilityRegistry = capabilityRegistry;
        this.contractValidator = contractValidator;
    }

    public DatasetSelectionResult selectDataset(String question, ExplanationRequest request) {
        String questionType = request != null ? request.getQuestionType() : null;
        QuestionTypeCatalog.QuestionTypeGroup group = questionTypeCatalog.groupFor(questionType);
        List<String> allowedSources = allowedSourcesFor(group);

        String explicit = request != null ? request.getDatasetSource() : null;
        if (explicit != null && !explicit.isBlank()) {
            return verifyExplicitDataset(request, explicit, group);
        }

        if (!isLlmEnabled()) {
            return DatasetSelectionResult.refusal(
                "LLM_REQUIRED",
                "Configure LLM API key to enable dataset selection.",
                DatasetSelectionStrategy.NONE,
                List.of()
            );
        }

        if (group == QuestionTypeCatalog.QuestionTypeGroup.LAWA_STATE) {
            return DatasetSelectionResult.selected(
                DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue(),
                "Question type is LAWA state; dataset is fixed.",
                DatasetSelectionStrategy.HEURISTIC
            );
        }

        if (group == QuestionTypeCatalog.QuestionTypeGroup.LAWA_TREND) {
            return DatasetSelectionResult.selected(
                DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue(),
                "Question type is LAWA trend; dataset is fixed.",
                DatasetSelectionStrategy.HEURISTIC
            );
        }

        List<String> candidates = proposeCandidates(question, request);
        if (candidates.isEmpty()) {
            return DatasetSelectionResult.refusal(
                "UNSUPPORTED_CAPABILITY",
                "Unable to select a dataset for this question.",
                DatasetSelectionStrategy.LLM_CANDIDATES,
                candidates
            );
        }

        List<String> clamped = clampCandidates(candidates, allowedSources);
        if (allowedSources != null && clamped.isEmpty()) {
            if (isCrossDomainMismatch(group, candidates)) {
                return DatasetSelectionResult.refusal(
                    "DATASET_MISMATCH",
                    mismatchMessage(group, candidates.isEmpty() ? null : candidates.getFirst()),
                    DatasetSelectionStrategy.LLM_CANDIDATES,
                    candidates
                );
            }
            return DatasetSelectionResult.refusal(
                "UNSUPPORTED_CAPABILITY",
                "No candidate datasets match the parsed question type.",
                DatasetSelectionStrategy.LLM_CANDIDATES,
                candidates
            );
        }

        List<String> evaluatedCandidates = clamped.isEmpty() ? candidates : clamped;
        List<DatasetSelectionResult> verifiedSelections = new ArrayList<>();
        for (String candidate : evaluatedCandidates) {
            DatasetSelectionResult verified = verifyCandidate(request, candidate);
            if (verified.isSelected()) {
                verifiedSelections.add(verified);
            }
        }

        if (verifiedSelections.size() == 1) {
            DatasetSelectionResult selected = verifiedSelections.getFirst();
            selected.setCandidates(candidates);
            return selected;
        }
        if (verifiedSelections.size() > 1) {
            DatasetSelectionResult selected = selectDeterministicCandidate(verifiedSelections, group, question);
            selected.setCandidates(candidates);
            return selected;
        }

        return DatasetSelectionResult.refusal(
            "UNSUPPORTED_CAPABILITY",
            "No available dataset supports the requested question and filters.",
            DatasetSelectionStrategy.LLM_CANDIDATES,
            candidates
        );
    }

    private DatasetSelectionResult verifyExplicitDataset(
        ExplanationRequest request,
        String datasetSource,
        QuestionTypeCatalog.QuestionTypeGroup group
    ) {
        if (group != QuestionTypeCatalog.QuestionTypeGroup.UNKNOWN && !isAllowedForGroup(datasetSource, group)) {
            return DatasetSelectionResult.refusal(
                "DATASET_MISMATCH",
                mismatchMessage(group, datasetSource),
                DatasetSelectionStrategy.EXPLICIT,
                List.of(datasetSource)
            );
        }

        DatasetSelectionResult verified = verifyCandidate(request, datasetSource);
        if (!verified.isSelected()) {
            return DatasetSelectionResult.refusal(
                verified.getRefusalCategory(),
                verified.getRefusalMessage(),
                DatasetSelectionStrategy.EXPLICIT,
                List.of(datasetSource)
            );
        }
        return DatasetSelectionResult.selected(
            datasetSource,
            "Dataset source explicitly provided in parsed intent.",
            DatasetSelectionStrategy.EXPLICIT
        );
    }

    private DatasetSelectionResult verifyCandidate(ExplanationRequest request, String datasetSource) {
        ContractValidator.Result validation = contractValidator.validateForDatasetCandidate(request, datasetSource);
        if (!validation.valid()) {
            return DatasetSelectionResult.refusal(
                validation.refusalCategory(),
                validation.refusalMessage(),
                DatasetSelectionStrategy.LLM_CANDIDATES,
                List.of(datasetSource)
            );
        }

        return DatasetSelectionResult.selected(
            datasetSource,
            "Selected from LLM candidates after verifying question type and filters.",
            DatasetSelectionStrategy.LLM_CANDIDATES
        );
    }

    private List<String> proposeCandidates(String question, ExplanationRequest request) {
        String output = client.createResponseWithSchema(
            llmProperties.getModel(),
            buildInstructions(),
            buildInput(question, request),
            buildSchema(),
            "dataset_selection"
        );

        if (output == null || output.isBlank()) {
            log.warn("Dataset selection LLM returned empty output");
            return Collections.emptyList();
        }

        try {
            JsonNode node = objectMapper.readTree(output);
            JsonNode candidatesNode = node.get("candidates");
            if (candidatesNode == null || !candidatesNode.isArray()) {
                return Collections.emptyList();
            }
            List<String> candidates = new ArrayList<>();
            for (JsonNode candidate : candidatesNode) {
                if (candidate != null && candidate.isTextual()) {
                    candidates.add(candidate.asText().trim());
                }
            }
            return candidates;
        } catch (Exception e) {
            log.warn("Dataset selection LLM output not valid JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildInstructions() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are selecting datasetSource codes for Wai & Watts.\n")
            .append("Return up to three candidates, ranked best first.\n")
            .append("Only use datasetSource codes from the provided catalog.\n")
            .append("Return JSON only, matching the schema exactly.\n\n")
            .append("Catalog:\n");

        for (DatasetDescriptor descriptor : datasetCatalog.getDatasets()) {
            sb.append("- ")
                .append(descriptor.datasetSource())
                .append(" | ")
                .append(descriptor.displayName())
                .append(" | domain=")
                .append(descriptor.domain())
                .append(" | grain=")
                .append(descriptor.grain())
                .append(" | questionTypes=")
                .append(capabilityRegistry.datasetContract(descriptor.datasetSource())
                    .map(contract -> contract.supportedQuestionTypes().stream().map(QuestionType::wireValue).toList())
                    .orElse(descriptor.supportedQuestionTypes()))
                .append("\n");
        }

        return sb.toString();
    }

    private String buildInput(String question, ExplanationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(question).append("\n");
        if (request != null) {
            sb.append("Parsed questionType: ").append(request.getQuestionType()).append("\n");
            sb.append("Parsed filters: ").append(request.getFilters() != null ? request.getFilters() : "{}").append("\n");
        }
        return sb.toString();
    }

    private ObjectNode buildSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
        ObjectNode candidates = properties.putObject("candidates");
        candidates.put("type", "array");
        candidates.put("maxItems", 3);
        ObjectNode items = candidates.putObject("items");
        items.put("type", "string");
        ArrayNode enums = items.putArray("enum");
        for (DatasetDescriptor descriptor : datasetCatalog.getDatasets()) {
            enums.add(descriptor.datasetSource());
        }

        ArrayNode required = schema.putArray("required");
        required.add("candidates");
        return schema;
    }

    private boolean isLlmEnabled() {
        return llmProperties != null
            && llmProperties.isConfigured()
            && llmProperties.getProvider() == LlmProvider.OPENAI;
    }

    private List<String> allowedSourcesFor(QuestionTypeCatalog.QuestionTypeGroup group) {
        return switch (group) {
            case MBIE -> List.of(
                DatasetSource.MBIE_GENERATION_ANNUAL.wireValue(),
                DatasetSource.MBIE_GENERATION_QUARTERLY.wireValue()
            );
            case LAWA_STATE -> List.of(DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue());
            case LAWA_TREND -> List.of(DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue());
            case UNKNOWN -> null;
        };
    }

    private List<String> clampCandidates(List<String> candidates, List<String> allowed) {
        if (allowed == null || allowed.isEmpty()) {
            return candidates;
        }
        return candidates.stream()
            .filter(candidate -> allowed.stream().anyMatch(candidate::equalsIgnoreCase))
            .toList();
    }

    private boolean isAllowedForGroup(String datasetSource, QuestionTypeCatalog.QuestionTypeGroup group) {
        List<String> allowed = allowedSourcesFor(group);
        if (allowed == null) {
            return true;
        }
        return allowed.stream().anyMatch(ds -> ds.equalsIgnoreCase(datasetSource));
    }

    private boolean isCrossDomainMismatch(QuestionTypeCatalog.QuestionTypeGroup group, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        boolean hasMbie = candidates.stream().anyMatch(this::isMbieDataset);
        boolean hasLawa = candidates.stream().anyMatch(this::isLawaDataset);
        return (group == QuestionTypeCatalog.QuestionTypeGroup.MBIE && hasLawa)
            || ((group == QuestionTypeCatalog.QuestionTypeGroup.LAWA_STATE
                    || group == QuestionTypeCatalog.QuestionTypeGroup.LAWA_TREND) && hasMbie);
    }

    private boolean isMbieDataset(String datasetSource) {
        return DatasetSource.MBIE_GENERATION_ANNUAL.wireValue().equalsIgnoreCase(datasetSource)
            || DatasetSource.MBIE_GENERATION_QUARTERLY.wireValue().equalsIgnoreCase(datasetSource);
    }

    private boolean isLawaDataset(String datasetSource) {
        return DatasetSource.LAWA_WATER_QUALITY_STATE_MULTI_YEAR.wireValue().equalsIgnoreCase(datasetSource)
            || DatasetSource.LAWA_WATER_QUALITY_TREND_MULTI_YEAR.wireValue().equalsIgnoreCase(datasetSource);
    }

    private String mismatchMessage(QuestionTypeCatalog.QuestionTypeGroup group, String datasetSource) {
        return switch (group) {
            case MBIE -> "Parsed an MBIE generation question, but selected a LAWA dataset.";
            case LAWA_STATE -> "Parsed a LAWA state question, but selected a non-state dataset.";
            case LAWA_TREND -> "Parsed a LAWA trend question, but selected a non-trend dataset.";
            default -> "Parsed question is incompatible with dataset " + datasetSource + ".";
        };
    }

    private DatasetSelectionResult selectDeterministicCandidate(
        List<DatasetSelectionResult> verifiedSelections,
        QuestionTypeCatalog.QuestionTypeGroup group,
        String question
    ) {
        if (group == QuestionTypeCatalog.QuestionTypeGroup.MBIE) {
            boolean quarterSignal = hasQuarterSignal(question);
            String preferred = quarterSignal
                ? DatasetSource.MBIE_GENERATION_QUARTERLY.wireValue()
                : DatasetSource.MBIE_GENERATION_ANNUAL.wireValue();
            Optional<DatasetSelectionResult> preferredMatch = verifiedSelections.stream()
                .filter(result -> preferred.equalsIgnoreCase(result.getDatasetSource()))
                .findFirst();
            if (preferredMatch.isPresent()) {
                DatasetSelectionResult result = preferredMatch.get();
                return DatasetSelectionResult.selected(
                    result.getDatasetSource(),
                    "Deterministic MBIE tie-breaker applied: "
                        + (quarterSignal ? "quarter signal -> quarterly dataset." : "no quarter signal -> annual dataset."),
                    DatasetSelectionStrategy.LLM_CANDIDATES
                );
            }
        }

        DatasetSelectionResult fallback = verifiedSelections.stream().min(Comparator.comparing(result -> result.getDatasetSource().toLowerCase(Locale.ROOT)))
            .orElseThrow();

        return DatasetSelectionResult.selected(
            fallback.getDatasetSource(),
            "Deterministic tie-breaker applied: lexicographic datasetSource order.",
            DatasetSelectionStrategy.LLM_CANDIDATES
        );
    }

    private boolean hasQuarterSignal(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return QUARTER_SIGNAL_PATTERN.matcher(question).find();
    }

    public enum DatasetSelectionStrategy {
        EXPLICIT,
        LLM_CANDIDATES,
        HEURISTIC,
        NONE
    }

    public static class DatasetSelectionResult {
        private final boolean selected;
        private final String datasetSource;
        private final String reason;
        private final String refusalCategory;
        private final String refusalMessage;
        private final DatasetSelectionStrategy strategy;
        private List<String> candidates;

        private DatasetSelectionResult(
            boolean selected,
            String datasetSource,
            String reason,
            DatasetSelectionStrategy strategy,
            String refusalCategory,
            String refusalMessage
        ) {
            this.selected = selected;
            this.datasetSource = datasetSource;
            this.reason = reason;
            this.strategy = strategy;
            this.refusalCategory = refusalCategory;
            this.refusalMessage = refusalMessage;
            this.candidates = List.of();
        }

        public static DatasetSelectionResult selected(String datasetSource, String reason, DatasetSelectionStrategy strategy) {
            return new DatasetSelectionResult(true, datasetSource, reason, strategy, null, null);
        }

        public static DatasetSelectionResult refusal(
            String category,
            String message,
            DatasetSelectionStrategy strategy,
            List<String> candidates
        ) {
            DatasetSelectionResult result = new DatasetSelectionResult(false, null, null, strategy, category, message);
            result.candidates = candidates != null ? candidates : List.of();
            return result;
        }

        public boolean isSelected() {
            return selected;
        }

        public String getDatasetSource() {
            return datasetSource;
        }

        public String getReason() {
            return reason;
        }

        public String getRefusalCategory() {
            return refusalCategory;
        }

        public String getRefusalMessage() {
            return refusalMessage;
        }

        public DatasetSelectionStrategy getStrategy() {
            return strategy;
        }

        public List<String> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<String> candidates) {
            this.candidates = candidates != null ? candidates : List.of();
        }
    }
}
