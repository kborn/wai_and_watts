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
    public List<MbieGenerationAnnualRecordDto> find(Integer fromYear,
                                                    Integer toYear,
                                                    String fuelType) {
        int from = fromYear != null ? fromYear : Integer.MIN_VALUE;
        int to = toYear != null ? toYear : Integer.MAX_VALUE;
        String fuelTypeNorm = fuelType != null ? fuelType.trim().toUpperCase(Locale.ROOT) : null;

        return repository.findAll().stream()
                .filter(r -> r.getPeriodYear() >= from && r.getPeriodYear() <= to)
                .filter(r -> fuelTypeNorm == null || fuelTypeNorm.equalsIgnoreCase(nullToEmpty(r.getFuelTypeNorm())))
                .map(MbieGenerationAnnualReadServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
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
