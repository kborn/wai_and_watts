package nz.waiwatts.api.context;

import nz.waiwatts.api.context.dto.RegionContextFactPackDto;
import nz.waiwatts.service.context.RegionContextAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RegionContextController {

    private static final Logger logger = LoggerFactory.getLogger(RegionContextController.class);
    private final RegionContextAggregationService aggregationService;

    public RegionContextController(RegionContextAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @GetMapping("/region-context")
    public ResponseEntity<RegionContextFactPackDto> getRegionContext(
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "indicator", required = false) String indicator,
            @RequestParam(value = "trendWindow", required = false) Integer trendWindow) {

        logger.info("Region context requested: region={}, indicator={}, trendWindow={}", 
                region, indicator, trendWindow);

        RegionContextFactPackDto factPack = aggregationService.getRegionContext(
                region, indicator, trendWindow);

        return ResponseEntity.ok(factPack);
    }
}
