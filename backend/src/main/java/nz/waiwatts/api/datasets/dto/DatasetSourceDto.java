package nz.waiwatts.api.datasets.dto;

import nz.waiwatts.domain.datasets.DatasetSource;

import java.util.UUID;

public record DatasetSourceDto(
        UUID id,
        String name,
        String publisher,
        String sourceUrl,
        String expectedFormat,
        String updateCadence,
        String createdAt
) {
    public static DatasetSourceDto from(DatasetSource s) {
        return new DatasetSourceDto(
                s.getId(),
                s.getName(),
                s.getPublisher() != null ? s.getPublisher().name() : null,
                s.getSourceUrl(),
                s.getExpectedFormat() != null ? s.getExpectedFormat().name() : null,
                s.getUpdateCadence(),
                s.getCreatedAt() != null ? s.getCreatedAt().toString() : null
        );
    }
}
