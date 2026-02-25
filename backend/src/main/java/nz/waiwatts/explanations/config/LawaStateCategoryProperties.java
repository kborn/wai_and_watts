package nz.waiwatts.explanations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "waiwatts.lawa")
public class LawaStateCategoryProperties {

    private Map<String, List<String>> stateCategoryBands = defaultStateCategoryBands();
    private RegionalSample regionalSample = new RegionalSample();

    public Map<String, List<String>> getStateCategoryBands() {
        return stateCategoryBands;
    }

    public void setStateCategoryBands(Map<String, List<String>> stateCategoryBands) {
        if (stateCategoryBands == null || stateCategoryBands.isEmpty()) {
            this.stateCategoryBands = defaultStateCategoryBands();
            return;
        }
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        stateCategoryBands.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            normalized.put(key.trim().toUpperCase(Locale.ROOT), value);
        });
        this.stateCategoryBands = normalized.isEmpty() ? defaultStateCategoryBands() : normalized;
    }

    public RegionalSample getRegionalSample() {
        return regionalSample;
    }

    public void setRegionalSample(RegionalSample regionalSample) {
        this.regionalSample = regionalSample == null ? new RegionalSample() : regionalSample;
    }

    private static Map<String, List<String>> defaultStateCategoryBands() {
        Map<String, List<String>> defaults = new LinkedHashMap<>();
        defaults.put("EXCELLENT", List.of("A"));
        defaults.put("GOOD", List.of("B"));
        defaults.put("FAIR", List.of("C"));
        defaults.put("POOR", List.of("D", "E"));
        return defaults;
    }

    public static class RegionalSample {
        private int topK = 2;
        private int bottomK = 2;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = Math.max(1, topK);
        }

        public int getBottomK() {
            return bottomK;
        }

        public void setBottomK(int bottomK) {
            this.bottomK = Math.max(1, bottomK);
        }
    }
}
