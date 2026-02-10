package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationQuarterlyRecordDto;
import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MbieGenerationQuarterlyReadServiceImplTest {

    private MbieGenerationQuarterlyRecordRepository repo;
    private MbieGenerationQuarterlyReadServiceImpl service;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(MbieGenerationQuarterlyRecordRepository.class);
        service = new MbieGenerationQuarterlyReadServiceImpl(repo);

        DatasetRelease rel = new DatasetRelease();
        rel.setId(UUID.randomUUID());

        MbieGenerationQuarterlyRecord r1 = new MbieGenerationQuarterlyRecord();
        r1.setDatasetRelease(rel);
        r1.setPeriodYear(2023);
        r1.setPeriodQuarter(4);
        r1.setFuelTypeRaw("Hydro");
        r1.setFuelTypeNorm("HYDRO");
        r1.setGenerationGwh(new BigDecimal("7000.0"));

        MbieGenerationQuarterlyRecord r2 = new MbieGenerationQuarterlyRecord();
        r2.setDatasetRelease(rel);
        r2.setPeriodYear(2024);
        r2.setPeriodQuarter(3);
        r2.setFuelTypeRaw("Wind");
        r2.setFuelTypeNorm("WIND");
        r2.setGenerationGwh(new BigDecimal("980.1"));

        when(repo.findAll()).thenReturn(List.of(r1, r2));
    }

    @Test
    void find_noFilters_returnsAllMapped() {
        List<MbieGenerationQuarterlyRecordDto> out = service.find(null, null, null, null, null);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).getFuelTypeNorm()).isEqualTo("HYDRO");
        assertThat(out.get(1).getPeriodYear()).isEqualTo(2024);
        assertThat(out.get(1).getPeriodQuarter()).isEqualTo(3);
        assertThat(out.get(1).getGenerationGwh()).isEqualByComparingTo("980.1");
        assertThat(out.get(0).getReleaseId()).isNotNull();
    }

    @Test
    void find_withFromYear_filtersLowerBound() {
        List<MbieGenerationQuarterlyRecordDto> out = service.find(2024, null, null, null, null);
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getPeriodYear()).isEqualTo(2024);
    }

    @Test
    void find_withQuarter_filtersSpecificQuarter() {
        List<MbieGenerationQuarterlyRecordDto> out = service.find(null, null, 4, null, null);
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getPeriodQuarter()).isEqualTo(4);
    }

    @Test
    void find_withSource_caseInsensitiveMatch() {
        List<MbieGenerationQuarterlyRecordDto> out = service.find(null, null, null, "wind", null);
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getFuelTypeNorm()).isEqualTo("WIND");
    }
}
