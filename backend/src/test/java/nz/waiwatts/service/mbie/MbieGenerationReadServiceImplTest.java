package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationRecordDto;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.mbie.MbieGenerationRecord;
import nz.waiwatts.persistence.repositories.MbieGenerationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MbieGenerationReadServiceImplTest {

    private MbieGenerationRecordRepository repo;
    private MbieGenerationReadServiceImpl service;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(MbieGenerationRecordRepository.class);
        service = new MbieGenerationReadServiceImpl(repo);

        // Seed mocked repository with a small set of entities
        DatasetRelease rel = new DatasetRelease();
        rel.setId(UUID.randomUUID());

        MbieGenerationRecord r1 = new MbieGenerationRecord();
        r1.setDatasetRelease(rel);
        r1.setPeriodYear(2022);
        r1.setFuelTypeRaw("Hydro");
        r1.setFuelTypeNorm("HYDRO");
        r1.setGenerationGwh(new BigDecimal("26071.5"));

        MbieGenerationRecord r2 = new MbieGenerationRecord();
        r2.setDatasetRelease(rel);
        r2.setPeriodYear(2024);
        r2.setFuelTypeRaw("Wind");
        r2.setFuelTypeNorm("WIND");
        r2.setGenerationGwh(new BigDecimal("3918.6"));

        when(repo.findAll()).thenReturn(List.of(r1, r2));
    }

    @Test
    void find_noFilters_returnsAllMapped() {
        List<MbieGenerationRecordDto> out = service.find(null, null, null);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).getSource()).isEqualTo("HYDRO");
        assertThat(out.get(0).getSourceRaw()).isEqualTo("Hydro");
        assertThat(out.get(1).getSource()).isEqualTo("WIND");
        assertThat(out.get(1).getPeriodYear()).isEqualTo(2024);
        assertThat(out.get(1).getGenerationGwh()).isEqualByComparingTo("3918.6");
        assertThat(out.get(0).getReleaseId()).isNotNull();
    }

    @Test
    void find_withFromYear_filtersLowerBound() {
        List<MbieGenerationRecordDto> out = service.find(2023, null, null);
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getPeriodYear()).isEqualTo(2024);
    }

    @Test
    void find_withSource_caseInsensitiveMatch() {
        List<MbieGenerationRecordDto> out = service.find(null, null, "hydro");
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getSource()).isEqualTo("HYDRO");
    }
}
