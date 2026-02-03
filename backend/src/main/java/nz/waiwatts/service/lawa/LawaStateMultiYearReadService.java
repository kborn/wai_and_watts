package nz.waiwatts.service.lawa;

import nz.waiwatts.api.lawa.dto.LawaStateMultiYearRecordDto;

import java.util.List;

import org.springframework.lang.Nullable;

public interface LawaStateMultiYearReadService {
    List<LawaStateMultiYearRecordDto> find(@Nullable Integer fromYear,
                                           @Nullable Integer toYear,
                                           @Nullable String indicator,
                                           @Nullable String region);
}
