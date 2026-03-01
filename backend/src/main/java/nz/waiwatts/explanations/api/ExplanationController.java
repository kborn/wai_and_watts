package nz.waiwatts.explanations.api;

import nz.waiwatts.explanations.dto.AskResult;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.service.AskService;
import nz.waiwatts.explanations.service.CapabilitiesService;
import nz.waiwatts.explanations.service.ExplanationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for generating explanations from environmental data.
 * <p>
 * Versioned public API controller under /api/v1.
 * <p>
 * Enforces structured question typing and prevents freeform chat prompts.
 * Only accepts question_type from supported classes and structured filters.
 */
@RestController
@RequestMapping("/api/v1/explanations")
public class ExplanationController {

    private static final Logger logger = LoggerFactory.getLogger(ExplanationController.class);
    private static final String LEGACY_API_SUNSET = "Wed, 31 Dec 2026 23:59:59 GMT";
    
    private final ExplanationService explanationService;
    private final AskService askService;
    private final CapabilitiesService capabilitiesService;

    public ExplanationController(
        ExplanationService explanationService,
        AskService askService,
        CapabilitiesService capabilitiesService
    ) {
        this.explanationService = explanationService;
        this.askService = askService;
        this.capabilitiesService = capabilitiesService;
    }

    /**
     * Generate an explanation for a supported question type.
     * 
     * @param request structured request with questionType, datasetSource, and filters
     * @return explanation with citations or refusal
     */
    @PostMapping
    public ResponseEntity<Explanation> generateExplanation(@RequestBody ExplanationRequest request) {
        Explanation explanation = explanationService.generateExplanation(request);
        return ResponseEntity.ok(explanation);
    }

    /**
     * Process a natural language question and generate an explanation.
     * <p>
     * Phase 12 endpoint: Parses natural language → validates → generates explanation.
     * Fact pack builders pin data to one canonical dataset_release per request.
     * Follows same refusal behavior as structured endpoint.
     * 
     * @param body request containing natural language question
     * @return explanation with citations or refusal
     */
    @PostMapping("/ask")
    public ResponseEntity<AskResult> askQuestion(@RequestBody Map<String, String> body) {
        logger.info("Received /ask request body: {}", body);
        AskService.AskResponse response = askService.ask(body != null ? body.get("question") : null);
        return ResponseEntity.status(response.httpStatus()).body(response.result());
    }

    /**
     * Get supported question types and required filter structure.
     * 
     * @return supported and unsupported question classes with filter requirements
     */
    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> getSupportedQuestionTypes() {
        return ResponseEntity.ok()
            .header("Deprecation", "true")
            .header("Sunset", LEGACY_API_SUNSET)
            .header(HttpHeaders.LINK, "</api/v1/capabilities>; rel=\"successor-version\"")
            .header(HttpHeaders.WARNING, "299 - \"Deprecated API; use /api/v1/capabilities\"")
            .body(capabilitiesService.buildCapabilitiesResponse());
    }

    /**
     * Health check for explanation service.
     * 
     * @return service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok()
            .header("Deprecation", "true")
            .header("Sunset", LEGACY_API_SUNSET)
            .header(HttpHeaders.LINK, "</api/v1/health>; rel=\"successor-version\"")
            .header(HttpHeaders.WARNING, "299 - \"Deprecated API; use /api/v1/health\"")
            .body(Map.of(
                "status", "healthy",
                "service", "explanation-api",
                "phase", "11"
            ));
    }

    /**
     * Debug endpoint to build and return full FactPack for verification.
     * 
     * @param request same request structure as /api/v1/explanations
     * @return full FactPack JSON for debugging
     */
    @PostMapping("/fact-pack")
    public ResponseEntity<Object> buildFactPack(@RequestBody ExplanationRequest request) {
        Object factPack = explanationService.buildFactPack(request);
        return ResponseEntity.ok(factPack);
    }
}
