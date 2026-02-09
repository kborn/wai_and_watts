package nz.waiwatts.api.insights;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * API controller for returning curated insights.
 */
@RestController
public class InsightsController {

    /**
     * Returns the markdown content of Insights.md.
     * 
     * @return insights content as JSON with markdown field
     */
    @GetMapping("/api/v1/insights")
    public ResponseEntity<Map<String, Object>> getInsights() {
        try {
            // Read Insights.md from project root
            Path insightsPath = Paths.get("Insights.md");
            if (Files.exists(insightsPath)) {
                String content = Files.readString(insightsPath);
                return ResponseEntity.ok(Map.of("markdown", content));
            } else {
                return ResponseEntity.ok(Map.of(
                    "markdown", "# Insights\n\nNo insights available yet. Check back later!"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "markdown", "# Insights\n\nError loading insights: " + e.getMessage()
            ));
        }
    }
}