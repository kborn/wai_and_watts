package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.provider.ExplanationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Edge case and comprehensive tests for ExplanationServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class ExplanationServiceImplEdgeCaseTest {

    @Mock
    private FactPackBuilder factPackBuilder;
    private ExplanationProvider explanationProvider;

    @BeforeEach
    void setUp() {
        factPackBuilder = mock(FactPackBuilder.class);
        explanationProvider = mock(ExplanationProvider.class);
    }

    @Test
    void testMultipleBuildersSelectsCorrectOne() {
        // Create two builders
        FactPackBuilder mbieBuilder = mock(FactPackBuilder.class);
        FactPackBuilder lawaBuilder = mock(FactPackBuilder.class);
        ExplanationService service = new ExplanationServiceImpl(List.of(lawaBuilder, mbieBuilder), explanationProvider);

        ExplanationRequest mbieRequest = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        ExplanationRequest lawaRequest = new ExplanationRequest(
            "water_quality_state",
            Map.of("datasetSource", "lawa.water_quality.state")
        );

        when(mbieBuilder.canHandle(mbieRequest)).thenReturn(true);
        when(lawaBuilder.canHandle(mbieRequest)).thenReturn(false);
        when(mbieBuilder.buildFactPack(mbieRequest))
            .thenReturn(new FactPack());
        when(explanationProvider.generateExplanation(any(String.class), any(FactPack.class)))
            .thenReturn(new Explanation("Test explanation", List.of("citation1")));
        when(explanationProvider.validateCitations(any(Explanation.class), any(FactPack.class)))
            .thenReturn(true);

        Explanation result = service.generateExplanation(mbieRequest);

        assertNotNull(result);
        verify(mbieBuilder).canHandle(mbieRequest);
        verify(mbieBuilder).buildFactPack(mbieRequest);
        verify(explanationProvider).generateExplanation(any(String.class), any(FactPack.class));
        verify(explanationProvider).validateCitations(any(Explanation.class), any(FactPack.class));
    }

    @Test
    void testNoAvailableBuilderReturnsRefusal() {
        ExplanationService service = new ExplanationServiceImpl(List.of(), explanationProvider);
        
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("No data source available for this request", result.getRefusalReason());
        
        verify(explanationProvider, never()).generateExplanation(any(String.class), any(FactPack.class));
        verify(explanationProvider, never()).validateCitations(any(Explanation.class), any(FactPack.class));
    }

    @Test
    void testNullFactPackFromBuilderReturnsRefusal() {
        ExplanationService service = new ExplanationServiceImpl(List.of(factPackBuilder), explanationProvider);
        
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request)).thenReturn(null);

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("No data source available for this request", result.getRefusalReason());
        
        verify(factPackBuilder).canHandle(request);
        verify(factPackBuilder).buildFactPack(request);
        verify(explanationProvider, never()).generateExplanation(any(String.class), any(FactPack.class));
        verify(explanationProvider, never()).validateCitations(any(Explanation.class), any(FactPack.class));
    }

    @Test
    void testNullExplanationFromProviderReturnsRefusal() {
        ExplanationService service = new ExplanationServiceImpl(List.of(factPackBuilder), explanationProvider);
        
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request))
            .thenReturn(new FactPack());

        when(explanationProvider.generateExplanation(any(String.class), any(FactPack.class)))
            .thenReturn(Explanation.refusal("Provider returned null"));

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("Provider returned null", result.getRefusalReason());
        
        verify(factPackBuilder).canHandle(request);
        verify(factPackBuilder).buildFactPack(request);
        verify(explanationProvider).generateExplanation(any(String.class), any(FactPack.class));
        verify(explanationProvider, never()).validateCitations(any(Explanation.class), any(FactPack.class));
    }

    @Test
    void testCitationValidationFailsReturnsRefusal() {
        ExplanationService service = new ExplanationServiceImpl(List.of(factPackBuilder), explanationProvider);
        
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request))
            .thenReturn(new FactPack());

        when(explanationProvider.generateExplanation(any(String.class), any(FactPack.class)))
            .thenReturn(new Explanation("Some explanation", List.of("citation1")));

        when(explanationProvider.validateCitations(any(Explanation.class), any(FactPack.class)))
            .thenReturn(false);

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("Generated explanation missing required citations", result.getRefusalReason());
        
        verify(factPackBuilder).canHandle(request);
        verify(factPackBuilder).buildFactPack(request);
        verify(explanationProvider).generateExplanation(any(String.class), any(FactPack.class));
        verify(explanationProvider).validateCitations(any(Explanation.class), any(FactPack.class));
    }

    @Test
    void testCitationValidationPassesReturnsExplanation() {
        ExplanationService service = new ExplanationServiceImpl(List.of(factPackBuilder), explanationProvider);
        
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request))
            .thenReturn(new FactPack());

        when(explanationProvider.generateExplanation(any(String.class), any(FactPack.class)))
            .thenReturn(new Explanation("Some explanation", List.of("citation1")));

        when(explanationProvider.validateCitations(any(Explanation.class), any(FactPack.class)))
            .thenReturn(true);

        Explanation result = service.generateExplanation(request);

        assertFalse(result.isRefusal());
        assertEquals("Some explanation", result.getExplanationText());
        assertEquals(List.of("citation1"), result.getCitations());
        
        verify(factPackBuilder).canHandle(request);
        verify(factPackBuilder).buildFactPack(request);
        verify(explanationProvider).generateExplanation(any(String.class), any(FactPack.class));
        verify(explanationProvider).validateCitations(any(Explanation.class), any(FactPack.class));
    }

    @Test
    void testExceptionPropagation() {
        ExplanationService service = new ExplanationServiceImpl(List.of(factPackBuilder), explanationProvider);
        
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        when(factPackBuilder.canHandle(request)).thenReturn(true);
        when(factPackBuilder.buildFactPack(request))
            .thenThrow(new RuntimeException("Database error"));

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("No data source available for this request", result.getRefusalReason());
        
        verify(factPackBuilder).canHandle(request);
        verify(factPackBuilder).buildFactPack(request);
        verify(explanationProvider, never()).generateExplanation(any(String.class), any(FactPack.class));
        verify(explanationProvider, never()).validateCitations(any(Explanation.class), any(FactPack.class));
    }

    @Test
    void testEmptyBuilderListReturnsRefusal() {
        ExplanationService service = new ExplanationServiceImpl(List.of(), explanationProvider);
        
        ExplanationRequest request = new ExplanationRequest(
            "hydro_generation_trend",
            Map.of("datasetSource", "mbie.generation.annual")
        );

        Explanation result = service.generateExplanation(request);

        assertTrue(result.isRefusal());
        assertEquals("No data source available for this request", result.getRefusalReason());
        
        verify(explanationProvider, never()).generateExplanation(any(String.class), any(FactPack.class));
        verify(explanationProvider, never()).validateCitations(any(Explanation.class), any(FactPack.class));
    }
}