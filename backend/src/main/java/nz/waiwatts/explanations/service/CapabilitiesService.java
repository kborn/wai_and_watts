package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CapabilitiesService {

    private final CapabilityRegistry capabilityRegistry;

    public CapabilitiesService(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    public Map<String, Object> buildCapabilitiesResponse() {
        return capabilityRegistry.toCapabilitiesResponse();
    }
}
