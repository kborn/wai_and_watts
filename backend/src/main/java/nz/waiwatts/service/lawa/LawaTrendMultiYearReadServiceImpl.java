package nz.waiwatts.service.lawa;

import nz.waiwatts.api.lawa.dto.LawaTrendMultiYearRecordDto;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LawaTrendMultiYearReadServiceImpl implements LawaTrendMultiYearReadService {

    private final LawaTrendMultiYearRecordRepository repository;

    public LawaTrendMultiYearReadServiceImpl(LawaTrendMultiYearRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<LawaTrendMultiYearRecordDto> find(Integer fromYear,
                                                  Integer toYear,
                                                  String indicator,
                                                  String region) {
        String indicatorNorm = LawaReadServiceSupport.normalizeFilter(indicator);
        String regionNorm = LawaReadServiceSupport.normalizeFilter(region);

        return repository.findForReadApi(fromYear, toYear, indicatorNorm, regionNorm).stream()
                .map(LawaTrendMultiYearReadServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    private static LawaTrendMultiYearRecordDto toDto(LawaTrendMultiYearRecord e) {
        UUID releaseId = e.getDatasetRelease() != null ? e.getDatasetRelease().getId() : null;
        return new LawaTrendMultiYearRecordDto(
                e.getLawaSiteId(),
                e.getSiteName(),
                e.getRegion(),
                e.getCatchment(),
                e.getLatitude(),
                e.getLongitude(),
                e.getIndicatorRaw(),
                e.getIndicatorNorm(),
                e.getTrendRaw(),
                e.getTrendNorm(),
                e.getTrendScore(),
                e.getTrendPeriodYears(),
                e.getTrendDataFrequency(),
                e.getPeriodType(),
                e.getPeriodStartYear(),
                e.getPeriodEndYear(),
                releaseId
        );
    }

    @Override
    public List<String> getRegions() {
        return repository.findDistinctRegionOrderByRegion();
    }

    @Override
    public List<String> getIndicators() {
        return repository.findDistinctIndicatorNormOrderByIndicatorNorm();
    }

}
