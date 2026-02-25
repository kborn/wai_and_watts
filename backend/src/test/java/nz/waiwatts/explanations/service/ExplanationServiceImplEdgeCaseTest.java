package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.provider.ExplanationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Edge case and comprehensive tests for ExplanationServiceImpl
 */
class ExplanationServiceImplEdgeCaseTest {

    private ExplanationServiceImpl service;
    private FactPackBuilder factPackBuilder;
    private ExplanationProvider explanationProvider;

    @BeforeEach
    void setUp() {
        factPackBuilder = mock(FactPackBuilder.class);
        explanationProvider = mock(ExplanationProvider.class);
        service = new ExplanationServiceImpl(List.of(factPackBuilder), explanationProvider);
    }

    @Test
    void testMultipleBuildersSelectsCorrectOne() {
        // Create two builders
        FactPackBuilder mbieBuilder = mock(FactPackBuilder.class);
        FactPackBuilder lawaBuilder = mock(FactPackBuilder.class);
        
        ExplanationService multiBuilderService = new ExplanationServiceImpl(
            List.of(lawaBuilder, mbieBuilder), explanationProvider
        );

        ExplanationRequest mbieRequest = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        ExplanationRequest lawaRequest = new ExplanationRequest(
            "water_quality_trend",
            Map.of("datasetSource", "lawa.water_quality.state.multi_year")
        );

        // Configure builders
        when(mbieBuilder.canHandle(mbieRequest)).thenReturn(true);
        when(lawaBuilder.canHandle(mbieRequest)).thenReturn(false);
        when(mbieBuilder.canHandle(lawaRequest)).thenReturn(false);
        when(lawaBuilder.canHandle(lawaRequest)).thenReturn(true);

        FactPack mbieFactPack = new FactPack();
        FactPack lawaFactPack = new FactPack();
        
        when(mbieBuilder.buildFactPack(mbieRequest)).thenReturn(mbieFactPack);
        when(lawaBuilder.buildFactPack(lawaRequest)).thenReturn(lawaFactPack);

        Explanation explanation = new Explanation("test", List.of("citation1"));
        when(explanationProvider.generateExplanation(any(), any())).thenReturn(explanation);
        when(explanationProvider.validateCitations(any(), any())).thenReturn(true);

        // Test MBIE request selects MBIE builder
        multiBuilderService.generateExplanation(mbieRequest);
        verify(mbieBuilder).buildFactPack(mbieRequest);
        verify(lawaBuilder, never()).buildFactPack(any());

        // Test LAWA request selects LAWA builder
        multiBuilderService.generateExplanation(lawaRequest);
        verify(lawaBuilder).buildFactPack(lawaRequest);
        verify(mbieBuilder, times(1)).buildFactPack(any()); // Only called once for MBIE
    }

    @Test
    void testNoAvailableBuilderReturnsRefusal() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(false);

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("No data source available for this request", result.getRefusalReason());
        
        // Should not call provider
        verify(explanationProvider, never()).generateExplanation(any(), any());
        verify(explanationProvider, never()).validateCitations(any(), any());
    }

    @Test
    void testMultipleMatchingBuildersFailFast() {
        FactPackBuilder builderA = mock(FactPackBuilder.class);
        FactPackBuilder builderB = mock(FactPackBuilder.class);
        ExplanationService ambiguousService = new ExplanationServiceImpl(
            List.of(builderA, builderB), explanationProvider
        );

        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(builderA.canHandle(request)).thenReturn(true);
        when(builderB.canHandle(request)).thenReturn(true);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> ambiguousService.generateExplanation(request)
        );
        assertTrue(ex.getMessage().contains("Ambiguous FactPackBuilder resolution"));
        verify(builderA, never()).buildFactPack(any());
        verify(builderB, never()).buildFactPack(any());
        verify(explanationProvider, never()).generateExplanation(any(), any());
    }

    @Test
    void testBuilderReturnsNullFactPack() {
        ExplanationRequest request = new ExplanationRequest(
            "fuel_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(null);

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
        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any())).thenReturn(null);

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

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any())).thenReturn(explanation);

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

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request))
            .thenThrow(new RuntimeException("Database connection failed"));

        Explanation result = service.generateExplanation(request);
        assertTrue(result.isRefusal());
        assertEquals("Unable to build FactPack for the requested question", result.getRefusalReason());
        verify(explanationProvider, never()).generateExplanation(any(), any());
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
        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any()))
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
        Explanation explanation = new Explanation("Some explanation", List.of("citation"));

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any())).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);
        assertFalse(result.isRefusal());
        verify(explanationProvider, never()).validateCitations(any(), any());
    }

    @Test
    void testEmptyBuilderList() {
        ExplanationService emptyService = new ExplanationServiceImpl(List.of(), explanationProvider);
        
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
        assertThrows(IllegalArgumentException.class, () -> new ExplanationServiceImpl(List.of(factPackBuilder), null));
    }

    @Test
    void testNullBuilderListInConstructor() {
        // Should handle null builder list gracefully in constructor
        assertThrows(IllegalArgumentException.class, () -> new ExplanationServiceImpl(null, explanationProvider));
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
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("m4", null, null, null, null, null));
        Explanation explanation = new Explanation("Successful explanation", List.of("ts:hydro:2023"));

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation("fuel_generation_trend", factPack)).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);

        assertFalse(result.isRefusal());
        assertEquals("Successful explanation", result.getExplanationText());
        assertEquals(List.of("ts:hydro:2023"), result.getCitations());

        // Verify all components were called correctly
        verify(factPackBuilder).canHandle(request);
        verify(factPackBuilder).buildFactPack(request);
        verify(explanationProvider).generateExplanation("fuel_generation_trend", factPack);
        verify(explanationProvider, never()).validateCitations(any(), any());
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
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("m5", null, null, null, null, null));

        Explanation explanation = new Explanation(
            "Regional water quality varies across regions.",
            List.of("metric:lawa:excellent_sites_percentage:canterbury")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any())).thenReturn(explanation);

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
        factPack.getFacts().getMetrics().add(new nz.waiwatts.explanations.dto.MetricFact("m6", null, null, null, null, null));

        Explanation explanation = new Explanation(
            "Regional water quality varies across regions.",
            List.of("metric:lawa:improving_sites_percentage:canterbury")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any())).thenReturn(explanation);

        Explanation result = service.generateExplanation(request);
        assertTrue(result.isRefusal());
        assertEquals("Generated explanation missing required citations", result.getRefusalReason());
    }
}
