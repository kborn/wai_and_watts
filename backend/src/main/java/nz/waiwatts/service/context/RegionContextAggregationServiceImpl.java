package nz.waiwatts.service.context;

import nz.waiwatts.api.context.dto.*;
import nz.waiwatts.domain.lawa.LawaStateMultiYearRecord;
import nz.waiwatts.domain.lawa.LawaTrendMultiYearRecord;
import nz.waiwatts.domain.mbie.MbieGenerationAnnualRecord;
import nz.waiwatts.lawa.LawaBindingNormalization;
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
    private static final int[] CANONICAL_TREND_PERIODS = {20, 15, 10, 5};
    private static final Map<Integer, Integer> CANONICAL_PERIOD_RANK = buildCanonicalPeriodRank();

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
                .filter(r -> LawaBindingNormalization.matchesNormalizedTrendIndicator(r.getIndicatorNorm(), indicator))
                .collect(Collectors.toList());

        Map<String, LawaTrendMultiYearRecord> deduplicated = deduplicateToCanonicalTrend(records, trendWindow);

        int totalUnits = deduplicated.size();
        int degrading = 0;
        int improving = 0;
        int indeterminate = 0;
        int insufficient = 0;

        for (LawaTrendMultiYearRecord r : deduplicated.values()) {
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

        int sufficientUnits = totalUnits - insufficient;
        double denominator = sufficientUnits > 0 ? sufficientUnits : 1.0;
        
        return new WaterTrendSummaryDto(
                totalUnits,
                Math.round((degrading / denominator) * 1000.0) / 10.0,
                Math.round((improving / denominator) * 1000.0) / 10.0,
                Math.round((indeterminate / denominator) * 1000.0) / 10.0,
                Math.round((insufficient / (totalUnits > 0 ? totalUnits : 1.0)) * 1000.0) / 10.0
        );
    }

    private Map<String, LawaTrendMultiYearRecord> deduplicateToCanonicalTrend(
            List<LawaTrendMultiYearRecord> records, Integer preferredPeriod) {
        
        Map<String, LawaTrendMultiYearRecord> result = new LinkedHashMap<>();

        for (LawaTrendMultiYearRecord r : records) {
            String siteId = r.getLawaSiteId();
            String ind = r.getIndicatorNorm();
            if (siteId == null || ind == null) continue;
            
            String key = siteId + "|" + ind;

            if (!result.containsKey(key)) {
                result.put(key, r);
            } else {
                LawaTrendMultiYearRecord existing = result.get(key);
                int existingPeriod = existing.getTrendPeriodYears() != null ? existing.getTrendPeriodYears() : 0;
                int newPeriod = r.getTrendPeriodYears() != null ? r.getTrendPeriodYears() : 0;

                if (preferredPeriod != null) {
                    if (existingPeriod != preferredPeriod && newPeriod == preferredPeriod) {
                        result.put(key, r);
                    }
                } else {
                    if (shouldReplaceByCanonicalFallback(existingPeriod, newPeriod)) {
                        result.put(key, r);
                    }
                }
            }
        }

        return result;
    }

    private static Map<Integer, Integer> buildCanonicalPeriodRank() {
        Map<Integer, Integer> rank = new HashMap<>();
        for (int i = 0; i < CANONICAL_TREND_PERIODS.length; i++) {
            rank.put(CANONICAL_TREND_PERIODS[i], i);
        }
        return rank;
    }

    private boolean shouldReplaceByCanonicalFallback(int existingPeriod, int newPeriod) {
        Integer existingRank = CANONICAL_PERIOD_RANK.get(existingPeriod);
        Integer newRank = CANONICAL_PERIOD_RANK.get(newPeriod);

        if (existingRank != null && newRank != null) {
            return newRank < existingRank;
        }
        if (existingRank == null && newRank != null) {
            return true;
        }
        if (existingRank != null) {
            return false;
        }
        return newPeriod > existingPeriod;
    }

    private WaterStateSummaryDto computeStateSummary(String region, String indicator) {
        List<LawaStateMultiYearRecord> records = stateRepository.findAll();

        records = records.stream()
                .filter(r -> region == null || region.isEmpty() || 
                        (r.getRegion() != null && r.getRegion().equalsIgnoreCase(region)))
                .filter(r -> LawaBindingNormalization.matchesNormalizedStateIndicator(r.getIndicatorNorm(), indicator))
                .toList();

        boolean hasIndicatorFilter = indicator != null && !indicator.isEmpty();

        Set<String> uniqueUnits;
        if (hasIndicatorFilter) {
            uniqueUnits = records.stream()
                    .map(LawaStateMultiYearRecord::getLawaSiteId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } else {
            uniqueUnits = records.stream()
                    .filter(r -> r.getLawaSiteId() != null && r.getIndicatorNorm() != null)
                    .map(r -> r.getLawaSiteId() + "|" + r.getIndicatorNorm())
                    .collect(Collectors.toSet());
        }

        int totalUnits = uniqueUnits.size();

        Map<String, Integer> bandDistribution = new LinkedHashMap<>();
        bandDistribution.put("A", 0);
        bandDistribution.put("B", 0);
        bandDistribution.put("C", 0);
        bandDistribution.put("D", 0);
        bandDistribution.put("E", 0);
        bandDistribution.put("INSUFFICIENT", 0);

        Set<String> seenUnits = new HashSet<>();
        Set<String> validBands = Set.of("A", "B", "C", "D", "E");
        
        for (LawaStateMultiYearRecord r : records) {
            String unitKey;
            if (hasIndicatorFilter) {
                unitKey = r.getLawaSiteId();
            } else {
                unitKey = r.getLawaSiteId() + "|" + r.getIndicatorNorm();
            }
            if (unitKey == null || seenUnits.contains(unitKey)) continue;
            seenUnits.add(unitKey);

            String band = r.getAttributeBand();
            boolean isInsufficient = band == null || 
                    band.isEmpty() || 
                    band.equalsIgnoreCase("NA") ||
                    !validBands.contains(band);

            if (isInsufficient) {
                bandDistribution.merge("INSUFFICIENT", 1, Integer::sum);
            } else {
                if (bandDistribution.containsKey(band)) {
                    bandDistribution.merge(band, 1, Integer::sum);
                }
            }
        }

        return new WaterStateSummaryDto(totalUnits, bandDistribution);
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
