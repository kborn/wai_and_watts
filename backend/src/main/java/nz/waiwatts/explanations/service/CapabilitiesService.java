package nz.waiwatts.explanations.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.waiwatts.explanations.capabilities.CapabilityRegistry;
import nz.waiwatts.explanations.dto.CapabilitiesResponse;
import org.springframework.stereotype.Service;

@Service
public class CapabilitiesService {

    private final CapabilityRegistry capabilityRegistry;
    private final ObjectMapper objectMapper;

    public CapabilitiesService(CapabilityRegistry capabilityRegistry, ObjectMapper objectMapper) {
        this.capabilityRegistry = capabilityRegistry;
        this.objectMapper = objectMapper;
    }

    public CapabilitiesResponse buildCapabilitiesResponse() {
        return objectMapper.convertValue(capabilityRegistry.toCapabilitiesResponse(), CapabilitiesResponse.class);
    }
}
