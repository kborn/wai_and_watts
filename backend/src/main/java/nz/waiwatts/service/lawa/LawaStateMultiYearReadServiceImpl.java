package nz.waiwatts.service.lawa;

import nz.waiwatts.api.lawa.dto.LawaStateMultiYearRecordDto;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class LawaStateMultiYearReadServiceImpl implements LawaStateMultiYearReadService{

    private final LawaStateMultiYearRecordRepository repository;

    public LawaStateMultiYearReadServiceImpl(LawaStateMultiYearRecordRepository repository){
        this.repository = repository;
    }


    @Override
    public List<LawaStateMultiYearRecordDto> find(Integer fromYear,
                                                  Integer toYear,
                                                  String indicator,
                                                  String region){
        String indicatorNorm = LawaReadServiceSupport.normalizeFilter(indicator);
        String regionNorm = LawaReadServiceSupport.normalizeFilter(region);

        return repository.findForReadApi(fromYear, toYear, indicatorNorm, regionNorm).stream()
                .map(LawaStateMultiYearReadServiceImpl::toDto)
                .collect(Collectors.toList());

    }

    private static LawaStateMultiYearRecordDto toDto(LawaStateMultiYearRecord e) {
        UUID releaseId = e.getDatasetRelease() != null ? e.getDatasetRelease().getId() : null;
        return new LawaStateMultiYearRecordDto(
                e.getLawaSiteId(),
                e.getSiteName(),
                e.getRegion(),
                e.getCatchment(),
                e.getLatitude(),
                e.getLongitude(),
                e.getIndicatorRaw(),
                e.getIndicatorNorm(),
                e.getUnits(),
                e.getAttributeBand(),
                e.getStateNorm(),
                e.getMedian(),
                e.getP95(),
                e.getRecHealthExceed260Pct(),
                e.getRecHealthExceed540Pct(),
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
