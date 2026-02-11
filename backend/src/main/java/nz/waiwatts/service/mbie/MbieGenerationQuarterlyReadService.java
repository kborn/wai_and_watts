package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationQuarterlyRecordDto;
import org.springframework.lang.Nullable;

import java.util.List;

public interface MbieGenerationQuarterlyReadService {
    List<MbieGenerationQuarterlyRecordDto> find(@Nullable Integer fromYear,
                                                @Nullable Integer toYear,
                                                @Nullable Integer quarter,
                                                @Nullable String fuelType);
}
