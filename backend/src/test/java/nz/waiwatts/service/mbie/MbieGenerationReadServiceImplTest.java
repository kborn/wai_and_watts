package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationAnnualRecordDto;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MbieGenerationReadServiceImplTest {

    private MbieGenerationAnnualRecordRepository repo;
    private MbieGenerationAnnualReadServiceImpl service;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(MbieGenerationAnnualRecordRepository.class);
        service = new MbieGenerationAnnualReadServiceImpl(repo);

        // Seed mocked repository with a small set of entities
        DatasetRelease rel = new DatasetRelease();
        rel.setId(UUID.randomUUID());

        MbieGenerationAnnualRecord r1 = new MbieGenerationAnnualRecord();
        r1.setDatasetRelease(rel);
        r1.setPeriodYear(2022);
        r1.setFuelTypeRaw("Hydro");
        r1.setFuelTypeNorm("HYDRO");
        r1.setGenerationGwh(new BigDecimal("26071.5"));

        MbieGenerationAnnualRecord r2 = new MbieGenerationAnnualRecord();
        r2.setDatasetRelease(rel);
        r2.setPeriodYear(2024);
        r2.setFuelTypeRaw("Wind");
        r2.setFuelTypeNorm("WIND");
        r2.setGenerationGwh(new BigDecimal("3918.6"));

        when(repo.findForReadApi(null, null, null)).thenReturn(List.of(r1, r2));
        when(repo.findForReadApi(2023, null, null)).thenReturn(List.of(r2));
        when(repo.findForReadApi(null, null, "HYDRO")).thenReturn(List.of(r1));
    }

    @Test
    void find_noFilters_returnsAllMapped() {
        List<MbieGenerationAnnualRecordDto> out = service.find(null, null, null);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).getFuelType()).isEqualTo("HYDRO");
        assertThat(out.get(0).getFuelTypeRaw()).isEqualTo("Hydro");
        assertThat(out.get(1).getFuelType()).isEqualTo("WIND");
        assertThat(out.get(1).getPeriodYear()).isEqualTo(2024);
        assertThat(out.get(1).getGenerationGwh()).isEqualByComparingTo("3918.6");
        assertThat(out.get(0).getReleaseId()).isNotNull();
    }

    @Test
    void find_withFromYear_filtersLowerBound() {
        List<MbieGenerationAnnualRecordDto> out = service.find(2023, null, null);
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getPeriodYear()).isEqualTo(2024);
    }

    @Test
    void find_withFuelType_caseInsensitiveMatch() {
        List<MbieGenerationAnnualRecordDto> out = service.find(null, null, "hydro");
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getFuelType()).isEqualTo("HYDRO");
    }
}
