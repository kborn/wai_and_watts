package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationAnnualRecordDto;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MbieGenerationAnnualReadServiceImpl implements MbieGenerationAnnualReadService {

    private final MbieGenerationAnnualRecordRepository repository;

    public MbieGenerationAnnualReadServiceImpl(MbieGenerationAnnualRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<String> getFuelTypes(){
        return repository.findDistinctFuelTypeNormOrderByFuelTypeNorm();
    }



    @Override
    public List<MbieGenerationAnnualRecordDto> find(Integer fromYear,
                                                    Integer toYear,
                                                    String fuelType) {
        String fuelTypeNorm = fuelType != null ? fuelType.trim().toUpperCase(Locale.ROOT) : null;

        return repository.findForReadApi(fromYear, toYear, fuelTypeNorm).stream()
                .map(MbieGenerationAnnualReadServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    private static MbieGenerationAnnualRecordDto toDto(MbieGenerationAnnualRecord e) {
        UUID releaseId = e.getDatasetRelease() != null ? e.getDatasetRelease().getId() : null;
        return new MbieGenerationAnnualRecordDto(
                e.getPeriodYear(),
                e.getFuelTypeNorm(),
                e.getFuelTypeRaw(),
                e.getGenerationGwh(),
                releaseId
        );
    }
}
