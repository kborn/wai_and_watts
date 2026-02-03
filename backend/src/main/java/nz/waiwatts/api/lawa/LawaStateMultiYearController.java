package nz.waiwatts.api.lawa;

import nz.waiwatts.api.lawa.dto.LawaStateMultiYearRecordDto;
import nz.waiwatts.service.lawa.LawaStateMultiYearReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lawa/")
public class LawaStateMultiYearController {

    private final LawaStateMultiYearReadService readService;

    public LawaStateMultiYearController(LawaStateMultiYearReadService readService){
        this.readService = readService;
    }

    @GetMapping("/state/multiyear")
    public ResponseEntity<?> getStateMultiYear(
            @RequestParam(value = "fromYear", required = false) Integer fromYear,
            @RequestParam(value = "toYear", required = false) Integer toYear,
            @RequestParam(value = "indicator", required = false) String indicator,
            @RequestParam(value = "region", required = false) String region
    ) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "fromYear must be <= toYear"));
        }
        List<LawaStateMultiYearRecordDto> out = readService.find(fromYear, toYear, indicator, region);
        return ResponseEntity.ok(out);
    }


}
