package nz.waiwatts.service.mbie;

import nz.waiwatts.api.mbie.dto.MbieGenerationRecordDto;

import java.util.List;
import org.springframework.lang.Nullable;

public interface MbieGenerationReadService {
    List<MbieGenerationRecordDto> find(@Nullable Integer fromYear,
                                       @Nullable Integer toYear,
                                       @Nullable String source);
}
