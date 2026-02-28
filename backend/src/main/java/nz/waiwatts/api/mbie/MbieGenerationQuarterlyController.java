package nz.waiwatts.api.mbie;

import nz.waiwatts.api.mbie.dto.MbieFuelTypesResponseDto;
import nz.waiwatts.api.mbie.dto.MbieGenerationQuarterlyRecordDto;
import nz.waiwatts.service.mbie.MbieGenerationQuarterlyReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Versioned public API controller under /api/v1.
 */
@RestController
@RequestMapping("/api/v1/mbie")
public class MbieGenerationQuarterlyController {

    private static final Logger logger = LoggerFactory.getLogger(MbieGenerationQuarterlyController.class);
    private final MbieGenerationQuarterlyReadService readService;

    public MbieGenerationQuarterlyController(MbieGenerationQuarterlyReadService readService) {
        this.readService = readService;
    }

    @GetMapping("/generation/quarterly/fuel-types")
    public ResponseEntity<MbieFuelTypesResponseDto> getFuelTypes(){
        List<String> fuelTypes = readService.getFuelTypes();
        logger.info("Found {} unique fuel types in the MBIE quarterly data set", fuelTypes.size());
        return ResponseEntity.ok(new MbieFuelTypesResponseDto(fuelTypes));
    }

    @GetMapping("/generation/quarterly")
    public ResponseEntity<?> getQuarterly(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) String fuelType
    ) {
        long startTime = System.currentTimeMillis();
        
        if (fromYear != null && toYear != null && fromYear > toYear) {
            logger.warn("Invalid parameters: fromYear ({}) must be <= toYear ({})", fromYear, toYear);
            return ResponseEntity.badRequest().body(Map.of("error", "fromYear must be <= toYear"));
        }
        if (quarter != null && (quarter < 1 || quarter > 4)) {
            logger.warn("Invalid parameters: quarter ({}) must be between 1 and 4", quarter);
            return ResponseEntity.badRequest().body(Map.of("error", "quarter must be between 1 and 4"));
        }
        
        List<MbieGenerationQuarterlyRecordDto> out = readService.find(fromYear, toYear, quarter, fuelType);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Dataset query: mbie.generation.quarterly - filters: fromYear={}, toYear={}, quarter={}, fuelType={} - rowCount: {}, duration: {}ms",
            fromYear, toYear, quarter, fuelType, out.size(), duration);
        
        return ResponseEntity.ok(out);
    }
}
