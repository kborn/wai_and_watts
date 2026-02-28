package nz.waiwatts.ingestion.core;

import java.util.UUID;

public record ReleaseRegistrationResult(UUID releaseId, boolean created) {
}
