package nz.waiwatts.api;

import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/capabilities")
public class CapabilitiesController {

    private final CapabilityRegistry capabilityRegistry;

    public CapabilitiesController(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCapabilities() {
        return ResponseEntity.ok(capabilityRegistry.toCapabilitiesResponse());
    }
}
