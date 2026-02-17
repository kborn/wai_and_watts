package nz.waiwatts.explanations.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnsupportedIntentDetectorTest {

    private final UnsupportedIntentDetector detector = new UnsupportedIntentDetector();

    @Test
    void detectsCausalIntent() {
        assertTrue(detector.isUnsupported("Why did hydro generation fall?"));
        assertTrue(detector.isUnsupported("What caused the decline?"));
        assertTrue(detector.isUnsupported("This happened due to policy changes"));
    }

    @Test
    void detectsPredictiveIntent() {
        assertTrue(detector.isUnsupported("Predict next year generation"));
        assertTrue(detector.isUnsupported("What will happen to renewables?"));
        assertTrue(detector.isUnsupported("Any forecast for 2027?"));
    }

    @Test
    void detectsNormativeIntent() {
        assertTrue(detector.isUnsupported("What should I do?"));
        assertTrue(detector.isUnsupported("Recommend the best energy source"));
    }

    @Test
    void detectsDerivedAnalyticsIntent() {
        assertTrue(detector.isDerivedAnalyticsUnsupported("Which fuel has grown the most since 2005?"));
        assertTrue(detector.isDerivedAnalyticsUnsupported("When did wind generation grow the fastest over any 3-year period?"));
        assertTrue(detector.isDerivedAnalyticsUnsupported("When did renewables first exceed 80% of total generation?"));
    }

    @Test
    void allowsExplainOnlyQuestions() {
        assertFalse(detector.isUnsupported("Explain renewable generation trends between 2020 and 2023"));
        assertFalse(detector.isUnsupported("Compare hydro and geothermal generation patterns"));
        assertFalse(detector.isDerivedAnalyticsUnsupported("Which regions have the highest proportion of poor river water quality sites?"));
    }
}
