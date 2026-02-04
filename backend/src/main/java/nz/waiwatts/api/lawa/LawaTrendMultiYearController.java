package nz.waiwatts.api.lawa;

import nz.waiwatts.api.lawa.dto.LawaTrendMultiYearRecordDto;
import nz.waiwatts.service.lawa.LawaTrendMultiYearReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lawa/water-quality")
public class LawaTrendMultiYearController {

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
}
