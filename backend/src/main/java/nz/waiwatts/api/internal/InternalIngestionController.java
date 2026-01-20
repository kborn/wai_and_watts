package nz.waiwatts.api.internal;

import nz.waiwatts.ingestion.core.DatasetIngestionRequest;
import nz.waiwatts.ingestion.core.DatasetIngestionService;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
@Profile({"dev", "test"})
public class InternalIngestionController {

    private final DatasetIngestionService ingestionService;
    private final String internalToken;

    public InternalIngestionController(DatasetIngestionService ingestionService,
                                       @Value("${waiwatts.internalToken:dev-token}") String internalToken) {
        this.ingestionService = ingestionService;
        this.internalToken = internalToken;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                    @Valid @RequestBody DatasetIngestionRequest request) {
        if (internalToken != null && !internalToken.isBlank()) {
            if (token == null || token.isBlank()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
            }
            if (!internalToken.equals(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
            }
        }
        try {
            UUID releaseId = ingestionService.ingest(request);
            // Return minimal, stable response. Callers can fetch status via read APIs.
            return ResponseEntity.ok(Map.of("releaseId", releaseId.toString()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not found"));
        } catch (Exception ex) {
            // Avoid leaking internals; details are in server logs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "internal error"));
        }
    }
}
