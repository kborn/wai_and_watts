package nz.waiwatts.api.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationAnnualRecordDto;
import nz.waiwatts.api.mbie.dto.MbieFuelTypesResponseDto;
import nz.waiwatts.service.mbie.MbieGenerationAnnualReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Versioned public API controller under /api/v1.
 */
@RestController
@RequestMapping("/api/v1/mbie")
public class MbieGenerationAnnualController {

    private static final Logger logger = LoggerFactory.getLogger(MbieGenerationAnnualController.class);
    private final MbieGenerationAnnualReadService readService;

    public MbieGenerationAnnualController(MbieGenerationAnnualReadService readService) {
        this.readService = readService;
    }

    @GetMapping("/generation/annual/fuel-types")
    public ResponseEntity<MbieFuelTypesResponseDto> getFuelTypes(){
        List<String> fuelTypes = readService.getFuelTypes();
        logger.info("Found {} unique fuel types in the MBIE annual data set", fuelTypes.size());
        return ResponseEntity.ok(new MbieFuelTypesResponseDto(fuelTypes));
    }

    // Canonical variant-specific endpoint
    @GetMapping("/generation/annual")
    public ResponseEntity<?> getGenerationAnnual(
            @RequestParam(value = "fromYear", required = false) Integer fromYear,
            @RequestParam(value = "toYear", required = false) Integer toYear,
            @RequestParam(value = "fuelType", required = false) String fuelType
    ) {
        long startTime = System.currentTimeMillis();
        
        if (fromYear != null && toYear != null && fromYear > toYear) {
            logger.warn("Invalid parameters: fromYear ({}) must be <= toYear ({})", fromYear, toYear);
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "fromYear must be <= toYear"));
        }
        
        List<MbieGenerationAnnualRecordDto> out = readService.find(fromYear, toYear, fuelType);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Dataset query: mbie.generation.annual - filters: fromYear={}, toYear={}, fuelType={} - rowCount: {}, duration: {}ms",
            fromYear, toYear, fuelType, out.size(), duration);
        
        return ResponseEntity.ok(out);
    }

}
