package nz.waiwatts.api.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationAnnualRecordDto;
import nz.waiwatts.service.mbie.MbieGenerationAnnualReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mbie")
public class MbieGenerationAnnualController {

    private static final Logger logger = LoggerFactory.getLogger(MbieGenerationAnnualController.class);
    private final MbieGenerationAnnualReadService readService;

    public MbieGenerationAnnualController(MbieGenerationAnnualReadService readService) {
        this.readService = readService;
    }

    // Canonical variant-specific endpoint
    @GetMapping("/generation/annual")
    public ResponseEntity<?> getGenerationAnnual(
            @RequestParam(value = "fromYear", required = false) Integer fromYear,
            @RequestParam(value = "toYear", required = false) Integer toYear,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "fuelType", required = false) String fuelType
    ) {
        long startTime = System.currentTimeMillis();
        
        if (fromYear != null && toYear != null && fromYear > toYear) {
            logger.warn("Invalid parameters: fromYear ({}) must be <= toYear ({})", fromYear, toYear);
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "fromYear must be <= toYear"));
        }
        
        List<MbieGenerationAnnualRecordDto> out = readService.find(fromYear, toYear, source, fuelType);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Dataset query: mbie.generation.annual - filters: fromYear={}, toYear={}, source={}, fuelType={} - rowCount: {}, duration: {}ms",
            fromYear, toYear, source, fuelType, out.size(), duration);
        
        return ResponseEntity.ok(out);
    }

}
