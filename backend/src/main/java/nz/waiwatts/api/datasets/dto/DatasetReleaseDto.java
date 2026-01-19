package nz.waiwatts.api.datasets.dto;

import nz.waiwatts.domain.datasets.DatasetRelease;

import java.util.UUID;

public record DatasetReleaseDto(
        UUID id,
        UUID datasetSourceId,
        String publishedDate,
        String releaseLabel,
        String retrievedAt,
        String importedAt,
        String contentHash,
        String status,
        String notes,
        String createdAt
) {
    public static DatasetReleaseDto from(DatasetRelease r) {
        return new DatasetReleaseDto(
                r.getId(),
                r.getDatasetSource() != null ? r.getDatasetSource().getId() : null,
                r.getPublishedDate() != null ? r.getPublishedDate().toString() : null,
                r.getReleaseLabel(),
                r.getRetrievedAt() != null ? r.getRetrievedAt().toString() : null,
                r.getImportedAt() != null ? r.getImportedAt().toString() : null,
                r.getContentHash(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getNotes(),
                r.getCreatedAt() != null ? r.getCreatedAt().toString() : null
        );
    }
}
