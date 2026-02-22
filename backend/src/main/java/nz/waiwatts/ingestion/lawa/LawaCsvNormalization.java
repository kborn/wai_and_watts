package nz.waiwatts.ingestion.lawa;

import java.util.Map;

final class LawaCsvNormalization {

    private LawaCsvNormalization() {
    }

    static String normalizeIndicator(String indicatorRaw,
                                     String indicatorNormFromFile,
                                     Map<String, String> indicatorMap) {
        if (isNotBlank(indicatorNormFromFile)) {
            return indicatorNormFromFile.trim();
        }
        String raw = indicatorRaw == null ? "" : collapseWhitespace(indicatorRaw.trim());
        return indicatorMap.getOrDefault(raw, "OTHER");
    }

    static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static String collapseWhitespace(String value) {
        return value.replaceAll("\\s+", " ");
    }
}
