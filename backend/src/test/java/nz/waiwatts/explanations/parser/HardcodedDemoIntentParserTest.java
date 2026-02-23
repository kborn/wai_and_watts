package nz.waiwatts.explanations.parser;

import nz.waiwatts.explanations.dto.ExplanationRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HardcodedDemoIntentParserTest {

    private final HardcodedDemoIntentParser parser = new HardcodedDemoIntentParser();

    @Test
    void parsesRenewableTrendSample() {
        ExplanationRequest request = parser.parseQuestion(
            "Explain renewable generation trends between 2020 and 2023"
        );

        assertNotNull(request);
        assertEquals("renewable_generation_trend", request.getQuestionType());
        assertEquals("mbie.generation.annual", request.getDatasetSource());
        assertEquals(Map.of("startYear", 2020, "endYear", 2023), request.getFilters());
    }

    @Test
    void parsesGenerationMixSample() {
        ExplanationRequest request = parser.parseQuestion(
            "What are the main sources of electricity generation in New Zealand?"
        );

        assertNotNull(request);
        assertEquals("generation_mix_overview", request.getQuestionType());
        assertEquals("mbie.generation.annual", request.getDatasetSource());
        assertNull(request.getFilters());
    }

    @Test
    void parsesFuelComparisonSample() {
        ExplanationRequest request = parser.parseQuestion(
            "Compare hydro and geothermal generation patterns"
        );

        assertNotNull(request);
        assertEquals("fuel_type_comparison", request.getQuestionType());
        assertEquals("mbie.generation.annual", request.getDatasetSource());
        assertEquals("HYDRO", request.getFilters().get("fuelType"));
        assertEquals("GEOTHERMAL", request.getFilters().get("fuelTypeB"));
    }

    @Test
    void parsesHydroTrendSample() {
        ExplanationRequest request = parser.parseQuestion(
            "Explain hydro generation trends between 2018 and 2023"
        );

        assertNotNull(request);
        assertEquals("fuel_generation_trend", request.getQuestionType());
        assertEquals("mbie.generation.annual", request.getDatasetSource());
        assertEquals(Map.of("startYear", 2018, "endYear", 2023, "fuelType", "HYDRO"), request.getFilters());
    }

    @Test
    void refusesNonSampleQuestion() {
        ExplanationRequest request = parser.parseQuestion("Tell me something else");
        assertNull(request);
    }
}
