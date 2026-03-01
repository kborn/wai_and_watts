package nz.waiwatts.explanations.service;

import io.micrometer.core.instrument.Metrics;
import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.capabilities.types.FilterKey;
import nz.waiwatts.explanations.dto.*;
import nz.waiwatts.explanations.generator.ExplanationGenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Explanation Service that orchestrates the explanation generation process.
 */
@Service
public class ExplanationServiceImpl implements ExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ExplanationServiceImpl.class);

    private final List<FactPackBuilder> factPackBuilders;
    private final ExplanationGenerator explanationGenerator;
    private final RequestValidationService validationService;

    public ExplanationServiceImpl(
        List<FactPackBuilder> factPackBuilders,
        ExplanationGenerator explanationGenerator,
        RequestValidationService validationService
    ) {
        if (factPackBuilders == null) {
            throw new IllegalArgumentException("FactPack builders list cannot be null");
        }
        if (explanationGenerator == null) {
            throw new IllegalArgumentException("ExplanationGenerator cannot be null");
        }
        if (validationService == null) {
            throw new IllegalArgumentException("RequestValidationService cannot be null");
        }
        validateUniqueBuilderSources(factPackBuilders);
        this.factPackBuilders = factPackBuilders;
        this.explanationGenerator = explanationGenerator;
        this.validationService = validationService;
    }

    @Override
    public Explanation generateExplanation(ExplanationRequest request) {
        // Validate request structure first
        RequestValidationService.ValidationResult validationResult = validationService.validateRequest(request);
        if (!validationResult.isValid()) {
            return Explanation.refusal(validationResult.getRefusalMessage());
        }

        // Select appropriate Fact Pack Builder
        Optional<FactPackBuilder> builderSelection = selectFactPackBuilder(request);
        if (builderSelection.isEmpty()) {
            return Explanation.refusal("No data source available for this request");
        }
        FactPackBuilder builder = builderSelection.get();

        // Generate Fact Pack
        try {
            String ds = request.getDatasetSource();
            if (ds == null) {
                ds = request.getFilters() != null
                    ? (String) request.getFilters().get(FilterKey.DATASET_SOURCE.wireValue())
                    : null;
            }
            log.info("FactPackBuilder selected: {} for questionType={} datasetSource={}",
                    builder.getClass().getSimpleName(), request.getQuestionType(), ds);
        } catch (Exception ignore) {
            // do not fail due to logging
        }
        FactPack factPack;
        try {
            factPack = builder.buildFactPack(request);
        } catch (RuntimeException e) {
            log.error("FactPack builder failed for questionType={} datasetSource={}: {}",
                    request.getQuestionType(), request.getDatasetSource(), e.getMessage(), e);
            return Explanation.refusal("Unable to build FactPack for the requested question");
        }

        // Handle null Fact Pack from builder → refuse rather than crash
        if (factPack == null) {
            log.info("FactPack result: null - unable to build FactPack");
            return Explanation.refusal("Unable to build FactPack for the requested question");
        }

        // Log fact pack shape at debug level
        logFactPackShape(factPack);

        // Pre-provider safety gates
        if (factPack.getGuardrails() == null) {
            return Explanation.refusal("FactPack missing guardrails");
        }
        if (factPack.getGuardrails().getAllowedClaims() == null) {
            return Explanation.refusal("FactPack missing guardrails.allowedClaims");
        }
        if (factPack.getGuardrails().getRequiredCitations() == null) {
            return Explanation.refusal("FactPack missing guardrails.requiredCitations");
        }

        // Treat null lists as empty to avoid NPEs
        boolean noFacts = isNoFacts(factPack);
        if (noFacts) {
            return Explanation.refusal("No facts available to answer the question");
        }

        // Generate explanation using provider (question derived from questionType)
        Explanation explanation;
        try {
            long providerStart = System.nanoTime();
            explanation = explanationGenerator.generateExplanation(request.getQuestionType(), factPack);
            long providerDurationMs = (System.nanoTime() - providerStart) / 1_000_000;
            recordExplanationStageMetrics("provider", providerDurationMs);
        } catch (RuntimeException e) {
            log.error("Explanation provider failed for questionType={} datasetSource={}: {}",
                    request.getQuestionType(), request.getDatasetSource(), e.getMessage(), e);
            return Explanation.refusal("Explanation provider failed to generate an explanation");
        }

        // Handle null explanation from provider
        if (explanation == null) {
            return Explanation.refusal("Explanation provider failed to generate an explanation");
        }

        // Validate citations — service enforces; provider validation still called for redundancy in Phase 11
        if (!explanation.isRefusal()) {
            long citationValidationStart = System.nanoTime();
            boolean serviceCitationsOk = validateCitations(explanation, factPack);
            long citationValidationDurationMs = (System.nanoTime() - citationValidationStart) / 1_000_000;
            recordExplanationStageMetrics("citation_validation", citationValidationDurationMs);
            if (!serviceCitationsOk) {
                // Internal debug payload to assist development without leaking to clients
                logCitationFailureDebug(request, explanation, factPack);
                return Explanation.refusal("Generated explanation missing required citations");
            }
        }

        return explanation;
    }

    private static boolean isNoFacts(FactPack factPack) {
        boolean noFacts;
        if (factPack.getFacts() == null) {
            noFacts = true;
        } else {
            var facts = factPack.getFacts();
            var ts = facts.getTimeSeries();
            var mets = facts.getMetrics();
            var comps = facts.getComparisons();
            var classes = facts.getClassifications();
            boolean tsEmpty = (ts == null || ts.isEmpty());
            boolean metsEmpty = (mets == null || mets.isEmpty());
            boolean compsEmpty = (comps == null || comps.isEmpty());
            boolean classesEmpty = (classes == null || classes.isEmpty());
            noFacts = tsEmpty && metsEmpty && compsEmpty && classesEmpty;
        }
        return noFacts;
    }

    private void recordExplanationStageMetrics(String stage, long durationMs) {
        Metrics.globalRegistry
            .counter("waiwatts.explanation.stage.count", "stage", stage)
            .increment();
        Metrics.globalRegistry
            .timer("waiwatts.explanation.stage.duration", "stage", stage)
            .record(Math.max(durationMs, 0L), TimeUnit.MILLISECONDS);
    }

    @Override
    public Object buildFactPack(ExplanationRequest request) {
        // Validate request structure first
        RequestValidationService.ValidationResult validationResult = validationService.validateRequest(request);
        if (!validationResult.isValid()) {
            return Map.of(
                "error", validationResult.getRefusalMessage(),
                "request", request
            );
        }

        // Select appropriate Fact Pack Builder
        Optional<FactPackBuilder> builderSelection = selectFactPackBuilder(request);
        if (builderSelection.isEmpty()) {
            return Map.of(
                "error", "No data source available for this request",
                "request", request
            );
        }
        FactPackBuilder builder = builderSelection.get();

        // Generate Fact Pack
        try {
            FactPack factPack = builder.buildFactPack(request);
            if (factPack == null) {
                return Map.of(
                    "error", "FactPackBuilder returned null",
                    "request", request
                );
            }
            return factPack;
        } catch (Exception e) {
            return Map.of(
                "error", "Exception building FactPack: " + e.getMessage(),
                "request", request
            );
        }
    }

    /**
     * Service-owned citation validation: all required citations from guardrails must
     * be present among the explanation's citations. Empty required list implies no requirement.
     */
    private boolean validateCitations(Explanation explanation, FactPack factPack) {
        try {
            List<String> required = (factPack.getGuardrails() != null && factPack.getGuardrails().getRequiredCitations() != null)
                    ? factPack.getGuardrails().getRequiredCitations() : List.of();
            List<String> actual = (explanation.getCitations() != null) ? explanation.getCitations() : List.of();
            if (!CitationValidationUtil.hasNonEmptyCitations(actual)) {
                return false;
            }
            if (!CitationValidationUtil.validateRequiredCitations(required, actual)) {
                return false;
            }
            return CitationValidationUtil.validateActualCitationsAgainstFactIds(actual, collectFactIds(factPack));
        } catch (Exception e) {
            // Defensive: on unexpected structure, fail validation
            return false;
        }
    }

    private List<String> collectFactIds(FactPack factPack) {
        if (factPack == null || factPack.getFacts() == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        if (factPack.getFacts().getMetrics() != null) {
            ids.addAll(factPack.getFacts().getMetrics().stream().map(MetricFact::getId).toList());
        }
        if (factPack.getFacts().getComparisons() != null) {
            ids.addAll(factPack.getFacts().getComparisons().stream().map(ComparisonFact::getId).toList());
        }
        if (factPack.getFacts().getTimeSeries() != null) {
            ids.addAll(factPack.getFacts().getTimeSeries().stream().map(TimeSeriesFact::getId).toList());
        }
        if (factPack.getFacts().getClassifications() != null) {
            ids.addAll(factPack.getFacts().getClassifications().stream().map(ClassificationFact::getId).toList());
        }
        return ids;
    }

    private void logCitationFailureDebug(ExplanationRequest request, Explanation explanation, FactPack factPack) {
        try {
            List<String> required = factPack.getGuardrails() != null ? factPack.getGuardrails().getRequiredCitations() : List.of();
            List<String> actual = explanation.getCitations();
            String fpVer = factPack.getFactPackVersion();
            String provenance = (factPack.getProvenance() != null && factPack.getProvenance().getDatasetSources() != null)
                    ? factPack.getProvenance().getDatasetSources().stream()
                        .map(ds -> String.format("%s@%s#%s", ds.getDatasetSourceCode(), ds.getDatasetReleaseId(), ds.getContentHash()))
                        .toList()
                        .toString()
                    : "[]";
            log.debug("Citation validation failed. questionType={}, requiredCitations={}, actualCitations={}, factPackVersion={}, provenance={}",
                    request != null ? request.getQuestionType() : null, required, actual, fpVer, provenance);
        } catch (Exception e) {
            log.debug("Citation failure debug logging encountered an error: {}", e.getMessage());
        }
    }

    private Optional<FactPackBuilder> selectFactPackBuilder(ExplanationRequest request) {
        String datasetSource = request != null ? request.getDatasetSource() : null;
        return factPackBuilders.stream()
            .filter(builder -> datasetSource != null && datasetSource.equals(builder.getSupportedDatasetSourceCode()))
            .findFirst();
    }

    private void validateUniqueBuilderSources(List<FactPackBuilder> builders) {
        LinkedHashMap<String, String> ownersByDatasetSource = new LinkedHashMap<>();
        for (FactPackBuilder builder : builders) {
            String datasetSource = builder.getSupportedDatasetSourceCode();
            String builderName = builder.getClass().getSimpleName();
            String existing = ownersByDatasetSource.putIfAbsent(datasetSource, builderName);
            if (existing != null) {
                throw new IllegalArgumentException(
                    "Duplicate FactPackBuilder registration for datasetSource="
                        + datasetSource
                        + ": "
                        + existing
                        + " and "
                        + builderName
                );
            }
        }
    }

    private void logFactPackShape(FactPack factPack) {
        if (!log.isDebugEnabled()) {
            return;
        }

        try {
            int timeSeriesCount = 0;
            int metricsCount = 0;
            int comparisonsCount = 0;
            int classificationsCount = 0;

            if (factPack.getFacts() != null) {
                if (factPack.getFacts().getTimeSeries() != null) {
                    timeSeriesCount = factPack.getFacts().getTimeSeries().size();
                }
                if (factPack.getFacts().getMetrics() != null) {
                    metricsCount = factPack.getFacts().getMetrics().size();
                }
                if (factPack.getFacts().getComparisons() != null) {
                    comparisonsCount = factPack.getFacts().getComparisons().size();
                }
                if (factPack.getFacts().getClassifications() != null) {
                    classificationsCount = factPack.getFacts().getClassifications().size();
                }
            }

            log.debug("FactPack shape: timeSeries={}, metrics={}, comparisons={}, classifications={}",
                timeSeriesCount, metricsCount, comparisonsCount, classificationsCount);
        } catch (Exception e) {
            log.debug("Failed to log FactPack shape: {}", e.getMessage());
        }
    }
}
