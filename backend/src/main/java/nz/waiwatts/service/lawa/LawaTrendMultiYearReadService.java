package nz.waiwatts.service.lawa;

import nz.waiwatts.api.lawa.dto.LawaTrendMultiYearRecordDto;
import org.springframework.lang.Nullable;

import java.util.List;

public interface LawaTrendMultiYearReadService {
    List<LawaTrendMultiYearRecordDto> find(@Nullable Integer fromYear,
                                           @Nullable Integer toYear,
                                           @Nullable String indicator,
                                           @Nullable String region);
}
