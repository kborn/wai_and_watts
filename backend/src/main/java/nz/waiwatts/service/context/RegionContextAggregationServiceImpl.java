package nz.waiwatts.service.context;

import nz.waiwatts.api.context.dto.*;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.persistence.repositories.LawaStateMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.LawaTrendMultiYearRecordRepository;
import nz.waiwatts.persistence.repositories.MbieGenerationAnnualRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RegionContextAggregationServiceImpl implements RegionContextAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(RegionContextAggregationServiceImpl.class);

    private final LawaTrendMultiYearRecordRepository trendRepository;
    private final LawaStateMultiYearRecordRepository stateRepository;
    private final MbieGenerationAnnualRecordRepository mbieRepository;

    public RegionContextAggregationServiceImpl(
            LawaTrendMultiYearRecordRepository trendRepository,
            LawaStateMultiYearRecordRepository stateRepository,
            MbieGenerationAnnualRecordRepository mbieRepository) {
        this.trendRepository = trendRepository;
        this.stateRepository = stateRepository;
        this.mbieRepository = mbieRepository;
    }

    @Override
    public RegionContextFactPackDto getRegionContext(String regionId, String indicator, Integer trendWindow) {
        logger.info("Computing region context for region={}, indicator={}, trendWindow={}", regionId, indicator, trendWindow);

        WaterTrendSummaryDto trendSummary = computeTrendSummary(regionId, indicator, trendWindow);
        WaterStateSummaryDto stateSummary = computeStateSummary(regionId, indicator);
        EnergySummaryDto energySummary = computeEnergySummary();

        WaterContextDto waterContext = new WaterContextDto(trendSummary, stateSummary);

        return new RegionContextFactPackDto(
                regionId != null ? regionId : "ALL",
                Instant.now(),
                waterContext,
                energySummary
        );
    }

    private WaterTrendSummaryDto computeTrendSummary(String region, String indicator, Integer trendWindow) {
        List<LawaTrendMultiYearRecord> records = trendRepository.findAll();

        records = records.stream()
                .filter(r -> region == null || region.isEmpty() || 
                        (r.getRegion() != null && r.getRegion().equalsIgnoreCase(region)))
                .filter(r -> indicator == null || indicator.isEmpty() || 
                        (r.getIndicatorNorm() != null && r.getIndicatorNorm().equalsIgnoreCase(indicator)))
                .filter(r -> trendWindow == null || 
                        r.getTrendPeriodYears() == trendWindow)
                .collect(Collectors.toList());

        int totalSites = records.size();
        int degrading = 0;
        int improving = 0;
        int indeterminate = 0;
        int insufficient = 0;

        for (LawaTrendMultiYearRecord r : records) {
            Integer score = r.getTrendScore();
            if (score == null || score == -99) {
                insufficient++;
            } else if (score < 0) {
                degrading++;
            } else if (score > 0) {
                improving++;
            } else {
                indeterminate++;
            }
        }

        double total = totalSites > 0 ? totalSites : 1.0;
        return new WaterTrendSummaryDto(
                totalSites,
                Math.round((degrading / total) * 1000.0) / 10.0,
                Math.round((improving / total) * 1000.0) / 10.0,
                Math.round((indeterminate / total) * 1000.0) / 10.0,
                Math.round((insufficient / total) * 1000.0) / 10.0
        );
    }

    private WaterStateSummaryDto computeStateSummary(String region, String indicator) {
        List<LawaStateMultiYearRecord> records = stateRepository.findAll();

        records = records.stream()
                .filter(r -> region == null || region.isEmpty() || 
                        (r.getRegion() != null && r.getRegion().equalsIgnoreCase(region)))
                .filter(r -> indicator == null || indicator.isEmpty() || 
                        (r.getIndicatorNorm() != null && r.getIndicatorNorm().equalsIgnoreCase(indicator)))
                .collect(Collectors.toList());

        int totalSites = records.size();
        Map<String, Integer> bandDistribution = new LinkedHashMap<>();
        bandDistribution.put("A", 0);
        bandDistribution.put("B", 0);
        bandDistribution.put("C", 0);
        bandDistribution.put("D", 0);
        bandDistribution.put("E", 0);
        bandDistribution.put("INSUFFICIENT", 0);

        for (LawaStateMultiYearRecord r : records) {
            if (r.getMedian() == null || r.getMedian().intValue() == -99) {
                bandDistribution.merge("INSUFFICIENT", 1, Integer::sum);
            } else {
                String band = r.getAttributeBand();
                if (band != null && bandDistribution.containsKey(band)) {
                    bandDistribution.merge(band, 1, Integer::sum);
                }
            }
        }

        return new WaterStateSummaryDto(totalSites, bandDistribution);
    }

    private EnergySummaryDto computeEnergySummary() {
        List<MbieGenerationAnnualRecord> records = mbieRepository.findAll();

        if (records.isEmpty()) {
            return new EnergySummaryDto(0, 0.0, 0.0, 0.0);
        }

        int latestYear = records.stream()
                .mapToInt(MbieGenerationAnnualRecord::getPeriodYear)
                .max()
                .orElse(0);

        int fiveYearsAgo = latestYear - 5;

        double latestRenewable = calculateRenewableShare(records, latestYear);
        double fiveYearAgoRenewable = calculateRenewableShare(records, fiveYearsAgo);
        double fossilLatest = calculateFossilShare(records, latestYear);

        double delta = latestRenewable - fiveYearAgoRenewable;

        return new EnergySummaryDto(
                latestYear,
                Math.round(latestRenewable * 10.0) / 10.0,
                Math.round(delta * 10.0) / 10.0,
                Math.round(fossilLatest * 10.0) / 10.0
        );
    }

    private double calculateRenewableShare(List<MbieGenerationAnnualRecord> records, int year) {
        double total = 0;
        double renewable = 0;
        for (MbieGenerationAnnualRecord r : records) {
            if (r.getPeriodYear() == year) {
                double gwh = r.getGenerationGwh().doubleValue();
                total += gwh;
                String fuel = r.getFuelTypeNorm();
                if ("HYDRO".equals(fuel) || "WIND".equals(fuel) || 
                    "GEOTHERMAL".equals(fuel) || "SOLAR".equals(fuel)) {
                    renewable += gwh;
                }
            }
        }
        return total > 0 ? (renewable / total) * 100 : 0;
    }

    private double calculateFossilShare(List<MbieGenerationAnnualRecord> records, int year) {
        double total = 0;
        double fossil = 0;
        for (MbieGenerationAnnualRecord r : records) {
            if (r.getPeriodYear() == year) {
                double gwh = r.getGenerationGwh().doubleValue();
                total += gwh;
                String fuel = r.getFuelTypeNorm();
                if ("GAS".equals(fuel) || "COAL".equals(fuel)) {
                    fossil += gwh;
                }
            }
        }
        return total > 0 ? (fossil / total) * 100 : 0;
    }
}
