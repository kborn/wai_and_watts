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
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        ExplanationRequest lawaRequest = new ExplanationRequest(
            "water_quality_state",
            Map.of("datasetSource", "lawa.water_quality.state")
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
            "unsupported_question",
            Map.of("datasetSource", "nonexistent.source")
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
    void testBuilderReturnsNullFactPack() {
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(null);

        // Should handle null gracefully
        assertThrows(NullPointerException.class, () -> {
            service.generateExplanation(request);
        });
    }

    @Test
    void testProviderReturnsNullExplanation() {
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any())).thenReturn(null);

        // Should handle null gracefully
        assertThrows(NullPointerException.class, () -> {
            service.generateExplanation(request);
        });
    }

    @Test
    void testCitationValidationFailsReturnsRefusal() {
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        Explanation explanation = new Explanation("Some explanation", List.of());

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any())).thenReturn(explanation);
        when(explanationProvider.validateCitations(explanation, factPack)).thenReturn(false);

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("Generated explanation missing required citations", result.getRefusalReason());
    }

    @Test
    void testBuilderThrowsException() {
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Should propagate exception
        assertThrows(RuntimeException.class, () -> {
            service.generateExplanation(request);
        });
    }

    @Test
    void testProviderThrowsException() {
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any()))
            .thenThrow(new RuntimeException("LLM provider unavailable"));

        // Should propagate exception
        assertThrows(RuntimeException.class, () -> {
            service.generateExplanation(request);
        });
    }

    @Test
    void testValidationThrowsException() {
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        Explanation explanation = new Explanation("Some explanation", List.of("citation"));

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation(any(), any())).thenReturn(explanation);
        when(explanationProvider.validateCitations(any(), any()))
            .thenThrow(new RuntimeException("Validation failed"));

        // Should propagate exception
        assertThrows(RuntimeException.class, () -> {
            service.generateExplanation(request);
        });
    }

    @Test
    void testEmptyBuilderList() {
        ExplanationService emptyService = new ExplanationServiceImpl(List.of(), explanationProvider);
        
        ExplanationRequest request = new ExplanationRequest(
            "any_question",
            Map.of("datasetSource", "any.source")
        );

        Explanation result = emptyService.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("No data source available for this request", result.getRefusalReason());
    }

    @Test
    void testNullProviderInConstructor() {
        // Should handle null provider gracefully in constructor
        assertThrows(NullPointerException.class, () -> {
            new ExplanationServiceImpl(List.of(factPackBuilder), null);
        });
    }

    @Test
    void testNullBuilderListInConstructor() {
        // Should handle null builder list gracefully in constructor
        assertThrows(NullPointerException.class, () -> {
            new ExplanationServiceImpl(null, explanationProvider);
        });
    }

    @Test
    void testSuccessfulFlowWithAllComponents() {
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        FactPack factPack = new FactPack();
        Explanation explanation = new Explanation("Successful explanation", List.of("ts:hydro:2023"));

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(factPack);
        when(explanationProvider.generateExplanation("hydro_generation_trend", factPack)).thenReturn(explanation);
        when(explanationProvider.validateCitations(explanation, factPack)).thenReturn(true);

        Explanation result = service.generateExplanation(request);

        assertFalse(result.isRefusal());
        assertEquals("Successful explanation", result.getExplanationText());
        assertEquals(List.of("ts:hydro:2023"), result.getCitations());

        // Verify all components were called correctly
        verify(factPackBuilder).canHandle(request);
        verify(factPackBuilder).buildFactPack(request);
        verify(explanationProvider).generateExplanation("hydro_generation_trend", factPack);
        verify(explanationProvider).validateCitations(explanation, factPack);
    }
}