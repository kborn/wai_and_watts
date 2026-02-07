package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.provider.ExplanationProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of the Explanation Service that orchestrates the explanation generation process.
 */
@Service
public class ExplanationServiceImpl implements ExplanationService {

    private final List<FactPackBuilder> factPackBuilders;
    private final ExplanationProvider explanationProvider;

    public ExplanationServiceImpl(List<FactPackBuilder> factPackBuilders, ExplanationProvider explanationProvider) {
        if (factPackBuilders == null) {
            throw new NullPointerException("FactPack builders list cannot be null");
        }
        if (explanationProvider == null) {
            throw new NullPointerException("ExplanationProvider cannot be null");
        }
        this.factPackBuilders = factPackBuilders;
        this.explanationProvider = explanationProvider;
    }

    @Override
    public Explanation generateExplanation(ExplanationRequest request) {
        // Select appropriate Fact Pack Builder
        FactPackBuilder builder = selectFactPackBuilder(request);

        if (builder == null) {
            return Explanation.refusal("No data source available for this request");
        }

        // Generate Fact Pack
        FactPack factPack = builder.buildFactPack(request);

        // Handle null FactPack from builder
        if (factPack == null) {
            throw new NullPointerException("FactPack builder returned null");
        }

        // Generate explanation using provider (question derived from questionType)
        Explanation explanation = explanationProvider.generateExplanation(request.getQuestionType(), factPack);

        // Handle null explanation from provider
        if (explanation == null) {
            throw new NullPointerException("ExplanationProvider returned null");
        }

        // Validate citations
        if (!explanation.isRefusal() && !explanationProvider.validateCitations(explanation, factPack)) {
            return Explanation.refusal("Generated explanation missing required citations");
        }

        return explanation;
    }

    private FactPackBuilder selectFactPackBuilder(ExplanationRequest request) {
        return factPackBuilders.stream()
            .filter(builder -> builder.canHandle(request))
            .findFirst()
            .orElse(null);
    }
}