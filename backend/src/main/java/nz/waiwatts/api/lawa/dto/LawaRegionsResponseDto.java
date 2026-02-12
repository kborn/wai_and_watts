package nz.waiwatts.api.lawa.dto;

import java.util.List;

/**
 * DTO for LAWA regions response
 */
public class LawaRegionsResponseDto {
    
    private List<String> regions;
    
    public LawaRegionsResponseDto() {
    }
    
    public LawaRegionsResponseDto(List<String> regions) {
        this.regions = regions;
    }
    
    public List<String> getRegions() {
        return regions;
    }
    
    public void setRegions(List<String> regions) {
        this.regions = regions;
    }
}