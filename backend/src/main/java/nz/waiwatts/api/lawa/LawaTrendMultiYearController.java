package nz.waiwatts.api.lawa;

import nz.waiwatts.api.lawa.dto.LawaTrendMultiYearRecordDto;
import nz.waiwatts.api.lawa.dto.LawaRegionsResponseDto;
import nz.waiwatts.api.lawa.dto.LawaIndicatorsResponseDto;
import nz.waiwatts.service.lawa.LawaTrendMultiYearReadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Versioned public API controller under /api/v1.
 */
@RestController
@RequestMapping("/api/v1/lawa/water-quality")
public class LawaTrendMultiYearController {

    private static final Logger logger = LoggerFactory.getLogger(LawaTrendMultiYearController.class);
    private final LawaTrendMultiYearReadService readService;

    public LawaTrendMultiYearController(LawaTrendMultiYearReadService readService) {
        this.readService = readService;
    }

    @GetMapping("/trend/multiyear")
    public ResponseEntity<?> getTrendMultiYear(
            @RequestParam(value = "fromYear", required = false) Integer fromYear,
            @RequestParam(value = "toYear", required = false) Integer toYear,
            @RequestParam(value = "indicator", required = false) String indicator,
            @RequestParam(value = "region", required = false) String region
    ) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "fromYear must be <= toYear"));
        }
        List<LawaTrendMultiYearRecordDto> out = readService.find(fromYear, toYear, indicator, region);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/trend/multiyear/regions")
    public ResponseEntity<LawaRegionsResponseDto> getRegions(){
        List<String> regions = readService.getRegions();
        logger.info("Found {} unique regions in LAWA trend data set", regions.size());
        return ResponseEntity.ok(new LawaRegionsResponseDto(regions));
    }

    @GetMapping("/trend/multiyear/indicators")
    public ResponseEntity<LawaIndicatorsResponseDto> getIndicators(){
        List<String> indicators = readService.getIndicators();
        logger.info("Found {} unique indicators in LAWA trend data set", indicators.size());
        return ResponseEntity.ok(new LawaIndicatorsResponseDto(indicators));
    }

}
