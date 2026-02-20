package nz.waiwatts.explanations.service;

import java.util.List;
import java.util.Locale;

public final class CitationValidationUtil {

    private CitationValidationUtil() {
    }

    public static boolean validateRequiredCitations(List<String> required, List<String> actual) {
        List<String> requiredCitations = required != null ? required : List.of();
        List<String> actualCitations = actual != null ? actual : List.of();
        return requiredCitations.stream().allMatch(req -> hasMatchingCitation(req, actualCitations));
    }

    public static boolean hasMatchingCitation(String requiredId, List<String> actualIds) {
        if (requiredId == null || requiredId.isBlank()) return true;
        if (actualIds == null || actualIds.isEmpty()) return false;

        String req = requiredId.trim().toLowerCase(Locale.ROOT);
        List<String> normalizedActualIds = actualIds.stream()
            .filter(id -> id != null && !id.isBlank())
            .map(id -> id.trim().toLowerCase(Locale.ROOT))
            .toList();

        if (normalizedActualIds.contains(req)) return true;

        if (req.endsWith(":*")) {
            String wildcardPrefix = req.substring(0, req.length() - 1);
            return normalizedActualIds.stream().anyMatch(act -> act.startsWith(wildcardPrefix));
        }

        if (req.endsWith(":__any__")) {
            String anyPrefix = req.substring(0, req.length() - ":__any__".length());
            return normalizedActualIds.stream().anyMatch(act -> act.startsWith(anyPrefix));
        }

        boolean isLawa = req.startsWith("metric:lawa:") || req.startsWith("class:lawa:");
        if (!isLawa) return false;
        int lastColon = req.lastIndexOf(':');
        if (lastColon <= 0) return false;
        String familyPrefix = req.substring(0, lastColon + 1);
        return normalizedActualIds.stream().anyMatch(act -> act.startsWith(familyPrefix));
    }
}
