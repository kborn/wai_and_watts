package nz.waiwatts.api.lawa;

import nz.waiwatts.api.lawa.dto.LawaStateMultiYearRecordDto;
import nz.waiwatts.api.lawa.dto.LawaRegionsResponseDto;
import nz.waiwatts.api.lawa.dto.LawaIndicatorsResponseDto;
import nz.waiwatts.service.lawa.LawaStateMultiYearReadService;
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
public class LawaStateMultiYearController {

    private static final Logger logger = LoggerFactory.getLogger(LawaStateMultiYearController.class);
    private final LawaStateMultiYearReadService readService;

    public LawaStateMultiYearController(LawaStateMultiYearReadService readService){
        this.readService = readService;
    }

    @GetMapping("/state/multiyear")
    public ResponseEntity<?> getStateMultiYear(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) String indicator,
            @RequestParam(required = false) String region
    ) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "fromYear must be <= toYear"));
        }
        List<LawaStateMultiYearRecordDto> out = readService.find(fromYear, toYear, indicator, region);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/state/multiyear/regions")
    public ResponseEntity<LawaRegionsResponseDto> getRegions(){
        List<String> regions = readService.getRegions();
        logger.info("Found {} unique regions in LAWA state data set", regions.size());
        return ResponseEntity.ok(new LawaRegionsResponseDto(regions));
    }

    @GetMapping("/state/multiyear/indicators")
    public ResponseEntity<LawaIndicatorsResponseDto> getIndicators(){
        List<String> indicators = readService.getIndicators();
        logger.info("Found {} unique indicators in LAWA state data set", indicators.size());
        return ResponseEntity.ok(new LawaIndicatorsResponseDto(indicators));
    }


}
