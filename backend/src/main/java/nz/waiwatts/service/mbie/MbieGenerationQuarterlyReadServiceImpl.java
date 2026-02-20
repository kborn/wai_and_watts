package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationQuarterlyRecordDto;
import nz.waiwatts.domain.mbie.MbieGenerationQuarterlyRecord;
import nz.waiwatts.persistence.repositories.MbieGenerationQuarterlyRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MbieGenerationQuarterlyReadServiceImpl implements MbieGenerationQuarterlyReadService {

    private final MbieGenerationQuarterlyRecordRepository repository;

    public MbieGenerationQuarterlyReadServiceImpl(MbieGenerationQuarterlyRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<String> getFuelTypes(){
        return repository.findDistinctFuelTypeNormOrderByFuelTypeNorm();
    }

    @Override
    public List<MbieGenerationQuarterlyRecordDto> find(Integer fromYear,
                                                       Integer toYear,
                                                       Integer quarter,
                                                       String fuelType) {
        String fuelTypeNorm = fuelType != null ? fuelType.trim().toLowerCase(Locale.ROOT) : null;

        return repository.findForReadApi(fromYear, toYear, quarter, fuelTypeNorm).stream()
                .map(MbieGenerationQuarterlyReadServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    private static MbieGenerationQuarterlyRecordDto toDto(MbieGenerationQuarterlyRecord e) {
        UUID releaseId = e.getDatasetRelease() != null ? e.getDatasetRelease().getId() : null;
        return new MbieGenerationQuarterlyRecordDto(
                e.getPeriodYear(),
                e.getPeriodQuarter(),
                e.getFuelTypeNorm(),
                e.getFuelTypeRaw(),
                e.getGenerationGwh(),
                releaseId
        );
    }
}
