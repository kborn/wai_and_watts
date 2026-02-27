package nz.waiwatts.persistence.repositories;

import nz.waiwatts.domain.datasets.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class DatasetLineageRepositoryTest {

    @Autowired
    private DatasetSourceRepository sourceRepo;

    @Autowired
    private DatasetReleaseRepository releaseRepo;

    @Test
    void persistSourceAndRelease_and_enforceUniqueHashPerSource() {
        DatasetSource src = new DatasetSource();
        src.setName("LAWA River Quality");
        src.setPublisher(Publisher.LAWA);
        src.setSourceUrl("https://lawa.org.nz/datasets/river-quality");
        src.setExpectedFormat(ExpectedFormat.CSV);
        src.setUpdateCadence("annual");
        src = sourceRepo.save(src);

        DatasetRelease rel = new DatasetRelease();
        rel.setDatasetSource(src);
        rel.setPublishedDate(LocalDate.of(2024, 12, 1));
        rel.setReleaseLabel("2024 annual");
        rel.setRetrievedAt(LocalDateTime.now());
        rel.setContentHash("hash-123");
        rel.setStatus(ReleaseStatus.PENDING);
        releaseRepo.save(rel);

        assertThat(releaseRepo.count()).isEqualTo(1);

        DatasetRelease dup = new DatasetRelease();
        dup.setDatasetSource(src);
        dup.setPublishedDate(LocalDate.of(2024, 12, 1));
        dup.setRetrievedAt(LocalDateTime.now());
        dup.setContentHash("hash-123"); // same hash with same source -> violates unique constraint
        dup.setStatus(ReleaseStatus.PENDING);

        assertThatThrownBy(() -> {
            releaseRepo.saveAndFlush(dup);
        }).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(sourceRepo.findBySourceUrl("https://lawa.org.nz/datasets/river-quality")).isPresent();
    }
}
