package nz.waiwatts.api.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationQuarterlyRecordDto;
import nz.waiwatts.service.mbie.MbieGenerationQuarterlyReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mbie")
public class MbieGenerationQuarterlyController {

    private final MbieGenerationQuarterlyReadService readService;

    public MbieGenerationQuarterlyController(MbieGenerationQuarterlyReadService readService) {
        this.readService = readService;
    }

    @GetMapping("/generation/quarterly")
    public ResponseEntity<?> getQuarterly(
            @RequestParam(value = "fromYear", required = false) Integer fromYear,
            @RequestParam(value = "toYear", required = false) Integer toYear,
            @RequestParam(value = "quarter", required = false) Integer quarter,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "fuelType", required = false) String fuelType
    ) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            return ResponseEntity.badRequest().body(Map.of("error", "fromYear must be <= toYear"));
        }
        if (quarter != null && (quarter < 1 || quarter > 4)) {
            return ResponseEntity.badRequest().body(Map.of("error", "quarter must be between 1 and 4"));
        }
        List<MbieGenerationQuarterlyRecordDto> out = readService.find(fromYear, toYear, quarter, source, fuelType);
        return ResponseEntity.ok(out);
    }
}
