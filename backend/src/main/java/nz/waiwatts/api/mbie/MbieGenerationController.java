package nz.waiwatts.api.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationRecordDto;
import nz.waiwatts.service.mbie.MbieGenerationReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mbie")
public class MbieGenerationController {

    private final MbieGenerationReadService readService;

    public MbieGenerationController(MbieGenerationReadService readService) {
        this.readService = readService;
    }

    @GetMapping("/generation")
    public ResponseEntity<?> getGeneration(
            @RequestParam(value = "fromYear", required = false) Integer fromYear,
            @RequestParam(value = "toYear", required = false) Integer toYear,
            @RequestParam(value = "source", required = false) String source
    ) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "fromYear must be <= toYear"));
        }
        List<MbieGenerationRecordDto> out = readService.find(fromYear, toYear, source);
        return ResponseEntity.ok(out);
    }
}
