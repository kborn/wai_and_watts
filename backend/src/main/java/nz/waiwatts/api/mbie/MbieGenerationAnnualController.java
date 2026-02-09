package nz.waiwatts.api.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationAnnualRecordDto;
import nz.waiwatts.service.mbie.MbieGenerationAnnualReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mbie")
public class MbieGenerationAnnualController {

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
        if (fromYear != null && toYear != null && fromYear > toYear) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "fromYear must be <= toYear"));
        }
        List<MbieGenerationAnnualRecordDto> out = readService.find(fromYear, toYear, source, fuelType);
        return ResponseEntity.ok(out);
    }

}
