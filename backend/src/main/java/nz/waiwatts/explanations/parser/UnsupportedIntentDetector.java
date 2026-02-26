package nz.waiwatts.explanations.parser;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deterministic detector for explain-only guardrails.
 * <p>
 * Refuses causal, predictive, or normative questions before LLM parsing.
 */
@Component
public class UnsupportedIntentDetector {

    private static final List<Pattern> CAUSAL_PATTERNS = List.of(
        Pattern.compile("\\bwhy\\b"),
        Pattern.compile("\\bcause\\b"),
        Pattern.compile("\\bcauses\\b"),
        Pattern.compile("\\bcaused\\b"),
        Pattern.compile("\\bbecause\\b"),
        Pattern.compile("\\bdue to\\b"),
        Pattern.compile("\\bdriver\\b"),
        Pattern.compile("\\bdrivers\\b"),
        Pattern.compile("\\bled to\\b")
    );

    private static final List<Pattern> PREDICTIVE_PATTERNS = List.of(
        Pattern.compile("\\bpredict\\b"),
        Pattern.compile("\\bforecast\\b"),
        Pattern.compile("\\bwill\\b"),
        Pattern.compile("\\bexpect\\b"),
        Pattern.compile("\\bnext year\\b")
    );

    private static final List<Pattern> NORMATIVE_PATTERNS = List.of(
        Pattern.compile("\\bshould\\b"),
        Pattern.compile("\\brecommend\\b"),
        Pattern.compile("\\bbest\\b"),
        Pattern.compile("\\bwhat should i do\\b")
    );

    private static final List<Pattern> DERIVED_ANALYTICS_PATTERNS = List.of(
        Pattern.compile("\\bfastest\\b"),
        Pattern.compile("\\bgrown the most\\b"),
        Pattern.compile("\\bgrew the most\\b"),
        Pattern.compile("\\bbiggest increase\\b"),
        Pattern.compile("\\blargest increase\\b"),
        Pattern.compile("\\blargest drop\\b"),
        Pattern.compile("\\bmost increase\\b"),
        Pattern.compile("\\bwhich fuel\\b.*\\b(most|highest|lowest|largest)\\b"),
        Pattern.compile("\\bexceed\\w*\\b.*%"),
        Pattern.compile("\\b(first|when)\\b.*\\b(percent|%)\\b.*\\b(total|generation)\\b"),
        Pattern.compile("\\bshare\\b.*\\b(total|generation)\\b")
    );

    public boolean isUnsupported(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        return matchesAny(CAUSAL_PATTERNS, normalized)
            || matchesAny(PREDICTIVE_PATTERNS, normalized)
            || matchesAny(NORMATIVE_PATTERNS, normalized);
    }

    public boolean isDerivedAnalyticsUnsupported(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        return matchesAny(DERIVED_ANALYTICS_PATTERNS, normalized);
    }

    private boolean matchesAny(List<Pattern> patterns, String text) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
