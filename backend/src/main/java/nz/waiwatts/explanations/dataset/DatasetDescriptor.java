package nz.waiwatts.explanations.dataset;

import java.util.List;
import java.util.Set;

public record DatasetDescriptor(
    String datasetSource,
    String displayName,
    String domain,
    String grain,
    List<String> supportedQuestionTypes,
    Set<String> supportedFilters
) {}
