package nz.waiwatts.service.context;

import nz.waiwatts.api.context.dto.RegionContextFactPackDto;
import org.jspecify.annotations.Nullable;

public interface RegionContextAggregationService {
    RegionContextFactPackDto getRegionContext(
            @Nullable String regionId,
            @Nullable String indicator,
            @Nullable Integer trendWindow);
}
