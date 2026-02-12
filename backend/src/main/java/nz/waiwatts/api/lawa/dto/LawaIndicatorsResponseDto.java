package nz.waiwatts.api.lawa.dto;

import java.util.List;

/**
 * DTO for LAWA indicators response
 */
public class LawaIndicatorsResponseDto {
    
    private List<String> indicators;
    
    public LawaIndicatorsResponseDto() {
    }
    
    public LawaIndicatorsResponseDto(List<String> indicators) {
        this.indicators = indicators;
    }
    
    public List<String> getIndicators() {
        return indicators;
    }
    
    public void setIndicators(List<String> indicators) {
        this.indicators = indicators;
    }
}