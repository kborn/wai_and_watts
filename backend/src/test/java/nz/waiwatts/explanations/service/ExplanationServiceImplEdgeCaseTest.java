package nz.waiwatts.explanations.service;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.generator.ExplanationGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Edge case and comprehensive tests for ExplanationServiceImpl
 */
@Tag("contract")
class ExplanationServiceImplEdgeCaseTest {

    private ExplanationServiceImpl service;
    private FactPackBuilder factPackBuilder;
    private ExplanationGenerator explanationGenerator;
    private RequestValidationService validationService;
    private SimpleMeterRegistry meterRegistry;
    private ExplanationRequestNormalizer requestNormalizer;

    @BeforeEach
    void setUp() {
        factPackBuilder = mock(FactPackBuilder.class);
        explanationGenerator = mock(ExplanationGenerator.class);
        validationService = mock(RequestValidationService.class);
        when(validationService.validateRequest(any())).thenReturn(RequestValidationService.ValidationResult.success());
        requestNormalizer = new ExplanationRequestNormalizer(
            new nz.waiwatts.explanations.capabilities.CapabilityRegistry(new nz.waiwatts.explanations.dataset.DatasetCatalog())
        );
        service = new ExplanationServiceImpl(List.of(factPackBuilder), explanationGenerator, validationService, requestNormalizer);
        meterRegistry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(meterRegistry);
    }

    @AfterEach
    void tearDown() {
        Metrics.globalRegistry.remove(meterRegistry);
        meterRegistry.close();
    }

    @Test
    void testMultipleBuildersSelectsCorrectOne() {
        // Create two builders
        FactPackBuilder mbieBuilder = mock(FactPackBuilder.class);
        FactPackBuilder lawaBuilder = mock(FactPackBuilder.class);
        when(mbieBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(lawaBuilder.getSupportedDatasetSourceCode()).thenReturn("lawa.water_quality.state.multi_year");
        
        ExplanationService multiBuilderService = new ExplanationServiceImpl(
            List.of(lawaBuilder, mbieBuilder), explanationGenerator, validationService, requestNormalizer
        );

        ExplanationRequest mbieRequest = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        ExplanationRequest lawaRequest = new ExplanationRequest(
            "water_quality_trend",
            Map.of("datasetSource", "lawa.water_quality.state.multi_year")
        );
        FactPack mbieFactPack = new FactPack();
        FactPack lawaFactPack = new FactPack();
        
        when(mbieBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(mbieFactPack);
        when(lawaBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(lawaFactPack);

        Explanation explanation = new Explanation("test", List.of("citation1"));
        when(explanationGenerator.generateExplanation(any(), any())).thenReturn(explanation);
        when(explanationGenerator.validateCitations(any(), any())).thenReturn(true);

        // Test MBIE request selects MBIE builder
        multiBuilderService.generateExplanation(mbieRequest);
        verify(mbieBuilder).buildFactPack(any(ExplanationRequest.class));
        verify(lawaBuilder, never()).buildFactPack(any());

        // Test LAWA request selects LAWA builder
        multiBuilderService.generateExplanation(lawaRequest);
        verify(lawaBuilder).buildFactPack(any(ExplanationRequest.class));
        verify(mbieBuilder, times(1)).buildFactPack(any()); // Only called once for MBIE
    }

    @Test
    void testNoAvailableBuilderReturnsRefusal() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("other.dataset");

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("No data source available for this request", result.getRefusalReason());
        
        // Should not call provider
        verify(explanationGenerator, never()).generateExplanation(any(), any());
        verify(explanationGenerator, never()).validateCitations(any(), any());
    }

    @Test
    void testMultipleMatchingBuildersFailFast() {
        FactPackBuilder builderA = mock(FactPackBuilder.class);
        FactPackBuilder builderB = mock(FactPackBuilder.class);

        when(builderA.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(builderB.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new ExplanationServiceImpl(List.of(builderA, builderB), explanationGenerator, validationService, requestNormalizer)
        );
        assertTrue(ex.getMessage().contains("Duplicate FactPackBuilder registration"));
    }

    @Test
    void testBuilderReturnsNullFactPack() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(null);

        // Should handle null gracefully by refusing, not crashing
        Explanation result = service.generateExplanation(request);
        assertTrue(result.isRefusal());
        assertEquals("Unable to build FactPack for the requested question", result.getRefusalReason());
    }

    @Test
    void testProviderReturnsNullExplanation() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        // Satisfy pre-provider gates
        factPack.getGuardrails().setAllowedClaims(List.of("claim:trend"));
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("m0", null, null, null, null, null));
        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(factPack);
        when(explanationGenerator.generateExplanation(any(), any())).thenReturn(null);

        // Should handle null gracefully by refusing, not crashing
        Explanation result = service.generateExplanation(request);
        assertTrue(result.isRefusal());
        assertEquals("Explanation provider failed to generate an explanation", result.getRefusalReason());
    }

    @Test
    void testCitationValidationFailsReturnsRefusal() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        // Ensure pre-provider gates pass: allowedClaims not empty and at least one fact present
        factPack.getGuardrails().setAllowedClaims(List.of("claim:trend"));
        factPack.getGuardrails().setRequiredCitations(List.of("metric:mbie:generation_gwh:2023:HYDRO"));
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("m1", null, null, null, null, null));
        Explanation explanation = new Explanation("Some explanation", List.of());

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(factPack);
        when(explanationGenerator.generateExplanation(any(), any())).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("Generated explanation missing required citations", result.getRefusalReason());
    }

    @Test
    void testBuilderThrowsException() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        Explanation result = service.generateExplanation(request);
        assertTrue(result.isRefusal());
        assertEquals("Unable to build FactPack for the requested question", result.getRefusalReason());
        verify(explanationGenerator, never()).generateExplanation(any(), any());
    }

    @Test
    void testProviderThrowsException() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        // Satisfy pre-provider gates
        factPack.getGuardrails().setAllowedClaims(List.of("claim:trend"));
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("m2", null, null, null, null, null));
        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(factPack);
        when(explanationGenerator.generateExplanation(any(), any()))
            .thenThrow(new RuntimeException("LLM provider unavailable"));

        Explanation result = service.generateExplanation(request);
        assertTrue(result.isRefusal());
        assertEquals("Explanation provider failed to generate an explanation", result.getRefusalReason());
    }

    @Test
    void testValidationIsServiceOwnedAndProviderValidationIsNotCalled() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        // Satisfy pre-provider gates
        factPack.getGuardrails().setAllowedClaims(List.of("claim:trend"));
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("m3", null, null, null, null, null));
        Explanation explanation = new Explanation("Some explanation", List.of("m3"));

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(factPack);
        when(explanationGenerator.generateExplanation(any(), any())).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);
        assertFalse(result.isRefusal());
        verify(explanationGenerator, never()).validateCitations(any(), any());
    }

    @Test
    void testEmptyBuilderList() {
        ExplanationService emptyService = new ExplanationServiceImpl(List.of(), explanationGenerator, validationService, requestNormalizer);
        
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        Explanation result = emptyService.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("No data source available for this request", result.getRefusalReason());
    }

    @Test
    void testNullProviderInConstructor() {
        // Should handle null provider gracefully in constructor
        assertThrows(IllegalArgumentException.class, () -> new ExplanationServiceImpl(List.of(factPackBuilder), null, validationService, requestNormalizer));
    }

    @Test
    void testNullBuilderListInConstructor() {
        // Should handle null builder list gracefully in constructor
        assertThrows(IllegalArgumentException.class, () -> new ExplanationServiceImpl(null, explanationGenerator, validationService, requestNormalizer));
    }

    @Test
    void testNullValidationServiceInConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new ExplanationServiceImpl(List.of(factPackBuilder), explanationGenerator, null, requestNormalizer));
    }

    @Test
    void testSuccessfulFlowWithAllComponents() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        // Satisfy pre-provider gates
        factPack.getGuardrails().setAllowedClaims(List.of("claim:trend"));
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("metric:mbie:generation_gwh:2023:HYDRO", null, null, null, null, null));
        Explanation explanation = new Explanation("Successful explanation", List.of("metric:mbie:generation_gwh:2023:HYDRO"));

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(factPack);
        when(explanationGenerator.generateExplanation("fuel_generation_trend", factPack)).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);

        assertFalse(result.isRefusal());
        assertEquals("Successful explanation", result.getExplanationText());
        assertEquals(List.of("metric:mbie:generation_gwh:2023:HYDRO"), result.getCitations());

        // Verify all components were called correctly
        verify(factPackBuilder, atLeastOnce()).getSupportedDatasetSourceCode();
        verify(factPackBuilder).buildFactPack(any(ExplanationRequest.class));
        verify(explanationGenerator).generateExplanation("fuel_generation_trend", factPack);
        verify(explanationGenerator, never()).validateCitations(any(), any());
        assertEquals(1.0, meterRegistry.get("waiwatts.explanation.stage.count").tag("stage", "provider").counter().count());
        assertEquals(1.0, meterRegistry.get("waiwatts.explanation.stage.count").tag("stage", "citation_validation").counter().count());
        assertEquals(1L, meterRegistry.get("waiwatts.explanation.stage.duration").tag("stage", "provider").timer().count());
        assertEquals(1L, meterRegistry.get("waiwatts.explanation.stage.duration").tag("stage", "citation_validation").timer().count());
    }

    @Test
    void testCitationValidationSupportsWildcardFamilyPrefix() {
        ExplanationRequest request = new ExplanationRequest(
            "regional_water_quality",
            Map.of("datasetSource", "lawa.water_quality.state.multi_year")
        );

        FactPack factPack = new FactPack();
        factPack.getGuardrails().setAllowedClaims(List.of("distribution"));
        factPack.getGuardrails().setRequiredCitations(List.of("metric:lawa:excellent_sites_percentage:*"));
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("metric:lawa:excellent_sites_percentage:canterbury", null, null, null, null, null));

        Explanation explanation = new Explanation(
            "Regional water quality varies across regions.",
            List.of("metric:lawa:excellent_sites_percentage:canterbury")
        );

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("lawa.water_quality.state.multi_year");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(factPack);
        when(explanationGenerator.generateExplanation(any(), any())).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);
        assertFalse(result.isRefusal());
    }

    @Test
    void testCitationValidationRejectsWrongWildcardFamily() {
        ExplanationRequest request = new ExplanationRequest(
            "regional_water_quality",
            Map.of("datasetSource", "lawa.water_quality.state.multi_year")
        );

        FactPack factPack = new FactPack();
        factPack.getGuardrails().setAllowedClaims(List.of("distribution"));
        factPack.getGuardrails().setRequiredCitations(List.of("metric:lawa:excellent_sites_percentage:*"));
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("metric:lawa:excellent_sites_percentage:canterbury", null, null, null, null, null));

        Explanation explanation = new Explanation(
            "Regional water quality varies across regions.",
            List.of("metric:lawa:improving_sites_percentage:canterbury")
        );

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("lawa.water_quality.state.multi_year");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(factPack);
        when(explanationGenerator.generateExplanation(any(), any())).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);
        assertTrue(result.isRefusal());
        assertEquals("Generated explanation missing required citations", result.getRefusalReason());
    }

    @Test
    void testCitationValidationRejectsEmptyCitationsEvenWhenNoRequiredList() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        factPack.getGuardrails().setAllowedClaims(List.of("trend"));
        factPack.getGuardrails().setRequiredCitations(List.of());
        factPack.getFacts().getMetrics().add(
            new nz.waiwatts.explanations.dto.MetricFact("metric:mbie:generation_gwh:2023:HYDRO", null, null, null, null, null)
        );
        Explanation explanation = new Explanation("Some explanation", List.of());

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(factPack);
        when(explanationGenerator.generateExplanation(any(), any())).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);
        assertTrue(result.isRefusal());
        assertEquals("Generated explanation missing required citations", result.getRefusalReason());
    }

    @Test
    void testCitationValidationRejectsCitationNotPresentInFactPack() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        factPack.getGuardrails().setAllowedClaims(List.of("trend"));
        factPack.getGuardrails().setRequiredCitations(List.of("metric:mbie:generation_gwh:2023:HYDRO"));
        factPack.getFacts().getMetrics().add(
            new nz.waiwatts.explanations.dto.MetricFact("metric:mbie:generation_gwh:2023:HYDRO", null, null, null, null, null)
        );
        Explanation explanation = new Explanation(
            "Some explanation",
            List.of("metric:mbie:generation_gwh:2024:HYDRO")
        );

        when(factPackBuilder.getSupportedDatasetSourceCode()).thenReturn("mbie.generation.annual");
        when(factPackBuilder.buildFactPack(any(ExplanationRequest.class))).thenReturn(factPack);
        when(explanationGenerator.generateExplanation(any(), any())).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);
        assertTrue(result.isRefusal());
        assertEquals("Generated explanation missing required citations", result.getRefusalReason());
    }
}
