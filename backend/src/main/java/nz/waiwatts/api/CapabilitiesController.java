package nz.waiwatts.api;

import nz.waiwatts.explanations.dto.CapabilitiesResponse;
import nz.waiwatts.explanations.service.CapabilitiesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/capabilities")
public class CapabilitiesController {

    private final CapabilitiesService capabilitiesService;

    public CapabilitiesController(CapabilitiesService capabilitiesService) {
        this.capabilitiesService = capabilitiesService;
    }

    @GetMapping
    public ResponseEntity<CapabilitiesResponse> getCapabilities() {
        return ResponseEntity.ok(capabilitiesService.buildCapabilitiesResponse());
    }
}
