package nz.waiwatts.api;

import nz.waiwatts.explanations.service.CapabilitiesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/capabilities")
public class CapabilitiesController {

    private final CapabilitiesService capabilitiesService;

    public CapabilitiesController(CapabilitiesService capabilitiesService) {
        this.capabilitiesService = capabilitiesService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCapabilities() {
        return ResponseEntity.ok(capabilitiesService.buildCapabilitiesResponse());
    }
}

