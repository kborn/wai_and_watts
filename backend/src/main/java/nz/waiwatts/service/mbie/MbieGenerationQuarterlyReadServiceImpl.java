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
    public List<MbieGenerationQuarterlyRecordDto> find(Integer fromYear,
                                                       Integer toYear,
                                                       Integer quarter,
                                                       String source,
                                                       String fuelType) {
        int from = fromYear != null ? fromYear : Integer.MIN_VALUE;
        int to = toYear != null ? toYear : Integer.MAX_VALUE;
        String sourceNorm = source != null ? source.trim().toUpperCase(Locale.ROOT) : null;
        String fuelTypeNorm = fuelType != null ? fuelType.trim().toUpperCase(Locale.ROOT) : null;

        return repository.findAll().stream()
                .filter(r -> r.getPeriodYear() >= from && r.getPeriodYear() <= to)
                .filter(r -> quarter == null || r.getPeriodQuarter() == quarter)
                .filter(r -> sourceNorm == null || sourceNorm.equalsIgnoreCase(nullToEmpty(r.getFuelTypeNorm())))
                .filter(r -> fuelTypeNorm == null || fuelTypeNorm.equalsIgnoreCase(nullToEmpty(r.getFuelTypeNorm())))
                .map(MbieGenerationQuarterlyReadServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

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
