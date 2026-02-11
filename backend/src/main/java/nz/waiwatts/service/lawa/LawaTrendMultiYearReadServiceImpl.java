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
        int from = fromYear != null ? fromYear : Integer.MIN_VALUE;
        int to = toYear != null ? toYear : Integer.MAX_VALUE;

        String indicatorNorm = indicator != null ? collapseWhitespace(indicator.trim()) : null;
        String regionNorm = region != null ? collapseWhitespace(region.trim()) : null;

        return repository.findAll().stream()
                .filter(r -> r.getPeriodEndYear() >= from && r.getPeriodStartYear() <= to)
                .filter(r -> indicatorNorm == null || indicatorNorm.equalsIgnoreCase(nullToEmpty(r.getIndicatorNorm())))
                .filter(r -> regionNorm == null || regionNorm.equalsIgnoreCase(nullToEmpty(r.getRegion())))
                .map(LawaTrendMultiYearReadServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String collapseWhitespace(String s) { return s.replaceAll("\\s+", " "); }

    private static LawaTrendMultiYearRecordDto toDto(LawaTrendMultiYearRecord e) {
        UUID releaseId = e.getDatasetRelease() != null ? e.getDatasetRelease().getId() : null;
        return new LawaTrendMultiYearRecordDto(
                e.getLawaSiteId(),
                e.getSiteName(),
                e.getRegion(),
                e.getLatitude(),
                e.getLongitude(),
                e.getIndicatorRaw(),
                e.getIndicatorNorm(),
                e.getUnits(),
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
