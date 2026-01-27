package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationAnnualRecordDto;

import java.util.List;
import org.springframework.lang.Nullable;

public interface MbieGenerationAnnualReadService {
    List<MbieGenerationAnnualRecordDto> find(@Nullable Integer fromYear,
                                             @Nullable Integer toYear,
                                             @Nullable String source);
}
