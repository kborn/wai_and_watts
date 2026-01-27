package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationRecordDto;
import nz.waiwatts.domain.mbie.MbieGenerationRecord;
import nz.waiwatts.persistence.repositories.MbieGenerationRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MbieGenerationReadServiceImpl implements MbieGenerationReadService {

    private final MbieGenerationRecordRepository repository;

    public MbieGenerationReadServiceImpl(MbieGenerationRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MbieGenerationRecordDto> find(Integer fromYear,
                                              Integer toYear,
                                              String source) {
        int from = fromYear != null ? fromYear : Integer.MIN_VALUE;
        int to = toYear != null ? toYear : Integer.MAX_VALUE;
        String sourceNorm = source != null ? source.trim().toUpperCase(Locale.ROOT) : null;

        return repository.findAll().stream()
                .filter(r -> r.getPeriodYear() >= from && r.getPeriodYear() <= to)
                .filter(r -> sourceNorm == null || sourceNorm.equalsIgnoreCase(nullToEmpty(r.getFuelTypeNorm())))
                .map(MbieGenerationReadServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static MbieGenerationRecordDto toDto(MbieGenerationRecord e) {
        UUID releaseId = e.getDatasetRelease() != null ? e.getDatasetRelease().getId() : null;
        return new MbieGenerationRecordDto(
                e.getPeriodYear(),
                e.getFuelTypeNorm(),
                e.getFuelTypeRaw(),
                e.getGenerationGwh(),
                releaseId
        );
    }
}
