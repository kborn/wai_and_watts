package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.provider.ExplanationProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Explanation Service that orchestrates the explanation generation process.
 */
@Service
public class ExplanationServiceImpl implements ExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ExplanationServiceImpl.class);

    private final List<FactPackBuilder> factPackBuilders;
    private final ExplanationProvider explanationProvider;

    public ExplanationServiceImpl(List<FactPackBuilder> factPackBuilders, ExplanationProvider explanationProvider) {
        if (factPackBuilders == null) {
            throw new IllegalArgumentException("FactPack builders list cannot be null");
        }
        if (explanationProvider == null) {
            throw new IllegalArgumentException("ExplanationProvider cannot be null");
        }
        this.factPackBuilders = factPackBuilders;
        this.explanationProvider = explanationProvider;
    }

    @Override
    public Explanation generateExplanation(ExplanationRequest request) {
        // Validate request structure first
        String validationError = validateRequest(request);
        if (validationError != null) {
            return Explanation.refusal(validationError);
        }

        // Select appropriate Fact Pack Builder
        FactPackBuilder builder = selectFactPackBuilder(request);

        if (builder == null) {
            // Helpful context for diagnosis without exposing internals
            try {
                String ds = request != null ? request.getDatasetSource() : null;
                if (ds == null && request != null && request.getFilters() != null) {
                    ds = (String) request.getFilters().get("datasetSource");
                }
                log.info("FactPackBuilder selection: none found for questionType={} datasetSource={}",
                        request != null ? request.getQuestionType() : null, ds);
            } catch (Exception ignore) {
                // avoid impacting user flow
            }
            return Explanation.refusal("No data source available for this request");
        }

        // Generate Fact Pack
        try {
            String ds = request.getDatasetSource();
            if (ds == null) {
                ds = request.getFilters() != null ? (String) request.getFilters().get("datasetSource") : null;
            }
            log.info("FactPackBuilder selected: {} for questionType={} datasetSource={}",
                    builder.getClass().getSimpleName(), request.getQuestionType(), ds);
        } catch (Exception ignore) {
            // do not fail due to logging
        }
        FactPack factPack = builder.buildFactPack(request);

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
        if (noFacts) {
            return Explanation.refusal("No facts available to answer the question");
        }

        // Generate explanation using provider (question derived from questionType)
        Explanation explanation = explanationProvider.generateExplanation(request.getQuestionType(), factPack);

        // Handle null explanation from provider
        if (explanation == null) {
            return Explanation.refusal("Explanation provider failed to generate an explanation");
        }

        // Validate citations — service enforces; provider validation still called for redundancy in Phase 11
        if (!explanation.isRefusal()) {
            boolean serviceCitationsOk = validateCitations(explanation, factPack);
            if (!serviceCitationsOk) {
                // Internal debug payload to assist development without leaking to clients
                logCitationFailureDebug(request, explanation, factPack);
                return Explanation.refusal("Generated explanation missing required citations");
            }

            // Backward compatibility: also consult provider's validator (may be removed in a future phase)
            if (!explanationProvider.validateCitations(explanation, factPack)) {
                logCitationFailureDebug(request, explanation, factPack);
                return Explanation.refusal("Generated explanation missing required citations");
            }
        }

        return explanation;
    }

    @Override
    public Object buildFactPack(ExplanationRequest request) {
        // Validate request structure first
        String validationError = validateRequest(request);
        if (validationError != null) {
            return Map.of(
                "error", validationError,
                "request", request
            );
        }

        // Select appropriate Fact Pack Builder
        FactPackBuilder builder = selectFactPackBuilder(request);

        if (builder == null) {
            return Map.of(
                "error", "No FactPackBuilder found for this request",
                "request", request
            );
        }

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
            // Vacuously true if no required citations
            return required.stream().allMatch(actual::contains);
        } catch (Exception e) {
            // Defensive: on unexpected structure, fail validation
            return false;
        }
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

    /**
     * Validate request structure and constraints
     */
    private String validateRequest(ExplanationRequest request) {
        if (request == null) {
            return "Invalid request: request cannot be null";
        }

        String questionType = request.getQuestionType();
        String datasetSource = request.getDatasetSource();
        Map<String, Object> filters = request.getFilters();

        // Validate question type
        if (questionType == null || questionType.trim().isEmpty()) {
            return "Invalid request: questionType is required";
        }

        // Validate dataset source (now a top-level field in Phase 12)
        if (datasetSource == null || datasetSource.trim().isEmpty()) {
            // Backward compatibility: check filters for datasetSource
            if (filters != null) {
                Object dsObj = filters.get("datasetSource");
                if (dsObj instanceof String) {
                    datasetSource = (String) dsObj;
                }
            }
            
            if (datasetSource == null || datasetSource.trim().isEmpty()) {
                return "Invalid request: datasetSource is required";
            }
            
            // Update the request object to maintain consistency
            request.setDatasetSource(datasetSource);
        }

        // Validate time range filters (if present)
        return validateTimeRangeFilters(filters); // No validation errors
    }


    /**
     * Validate time range filters (startYear, endYear)
     */
    private String validateTimeRangeFilters(Map<String, Object> filters) {
        Object startYear = filters.get("startYear");
        Object endYear = filters.get("endYear");

        if (startYear != null && endYear != null) {
            try {
                int start = Integer.parseInt(startYear.toString());
                int end = Integer.parseInt(endYear.toString());

                if (start > end) {
                    log.info("Auto-swapping time range: startYear ({}) > endYear ({}), swapping", start, end);
                    int temp = start;
                    start = end;
                    end = temp;
                }

                // Validate reasonable bounds (e.g., no future data, reasonable historical range)
                int currentYear = java.time.Year.now().getValue();
                if (end > currentYear) {
                    return String.format("Invalid time range: endYear (%d) cannot be in the future", end);
                }

                // TODO(Phase 12/metadata): Replace hardcoded lower bound with dataset-derived metadata
                if (end < 1990 || start < 1990) {
                    return "Invalid time range: years before 1990 are not supported";
                }

            } catch (NumberFormatException e) {
                return "Invalid time range: startYear and endYear must be valid integers";
            }
        }

        return null; // No time range validation errors
    }

    private FactPackBuilder selectFactPackBuilder(ExplanationRequest request) {
        return factPackBuilders.stream()
            .filter(builder -> builder.canHandle(request))
            .findFirst()
            .orElse(null);
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