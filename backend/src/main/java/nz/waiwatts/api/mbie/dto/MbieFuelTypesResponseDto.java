package nz.waiwatts.api.mbie.dto;

import java.util.List;

/**
 * DTO for MBIE fuel types response
 */
public class MbieFuelTypesResponseDto {
    
    private List<String> fuelTypes;
    
    public MbieFuelTypesResponseDto() {
    }
    
    public MbieFuelTypesResponseDto(List<String> fuelTypes) {
        this.fuelTypes = fuelTypes;
    }
    
    public List<String> getFuelTypes() {
        return fuelTypes;
    }
    
    public void setFuelTypes(List<String> fuelTypes) {
        this.fuelTypes = fuelTypes;
    }
}