package nz.waiwatts.explanations.builder;

import java.util.Map;
import java.util.Set;

public record LawaStateFactPackSettings(
    Map<String, Set<String>> stateCategoryBands,
    int regionalTopK,
    int regionalBottomK
) {
}
