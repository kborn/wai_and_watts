package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dto.Citation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("contract")
class CitationMapperTest {

    private final CitationMapper mapper = new CitationMapper();

    @Test
    void map_deduplicatesAndSortsCitationIdsDeterministically() {
        List<String> input = List.of(
            "metric:mbie:generation_gwh:2024:WIND",
            " ts:mbie:renewable_generation_gwh:2018_2024 ",
            "metric:mbie:generation_gwh:2024:WIND",
            "class:lawa:water_quality_state:EXCELLENT"
        );

        List<Citation> citations = mapper.map(input, "mbie.generation.annual");

        assertEquals(3, citations.size());
        assertEquals("class:lawa:water_quality_state:EXCELLENT", citations.get(0).getId());
        assertEquals("metric:mbie:generation_gwh:2024:WIND", citations.get(1).getId());
        assertEquals("ts:mbie:renewable_generation_gwh:2018_2024", citations.get(2).getId());
    }

    @Test
    void map_filtersNullAndBlankCitationIds() {
        List<Citation> citations = mapper.map(
            Arrays.asList(null, "", "   ", "metric:mbie:generation_gwh:2024:HYDRO"),
            "mbie.generation.annual"
        );

        assertEquals(1, citations.size());
        assertEquals("metric:mbie:generation_gwh:2024:HYDRO", citations.getFirst().getId());
    }

    @Test
    void map_parsesTimeSeriesCoverageIntoPeriod() {
        List<Citation> citations = mapper.map(
            List.of("ts:mbie:renewable_generation_gwh_quarterly:2020-Q1_to_2024-Q4"),
            "mbie.generation.quarterly"
        );

        Citation citation = citations.getFirst();
        assertEquals("TIME_SERIES", citation.getType());
        assertEquals("renewable_generation_gwh_quarterly", citation.getField());
        assertNotNull(citation.getPeriod());
        assertEquals(2020, citation.getPeriod().getStartYear());
        assertEquals(2024, citation.getPeriod().getEndYear());
    }
}
