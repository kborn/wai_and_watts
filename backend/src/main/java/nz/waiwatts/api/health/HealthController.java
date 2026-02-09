package nz.waiwatts.api.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Global health endpoint for overall app liveness.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    /**
     * Overall application health check.
     * 
     * @return health status with timestamp
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "timestamp", Instant.now().toString(),
            "service", "wai-and-watts-api",
            "version", "1.0.0"
        ));
    }
}
