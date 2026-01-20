package nz.waiwatts.domain.datasets;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dataset_source")
public class DatasetSource {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "code", unique = true)
    private String code; // stable identifier for lookup (distinct from URL)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Publisher publisher; // LAWA | MBIE

    @Column(name = "source_url", nullable = false, unique = true)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_format", nullable = false)
    private ExpectedFormat expectedFormat; // CSV | XLSX | ZIP

    @Column(name = "update_cadence")
    private String updateCadence;

    @OneToMany(mappedBy = "datasetSource", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DatasetRelease> releases = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public ExpectedFormat getExpectedFormat() {
        return expectedFormat;
    }

    public void setExpectedFormat(ExpectedFormat expectedFormat) {
        this.expectedFormat = expectedFormat;
    }

    public String getUpdateCadence() {
        return updateCadence;
    }

    public void setUpdateCadence(String updateCadence) {
        this.updateCadence = updateCadence;
    }

    public List<DatasetRelease> getReleases() {
        return releases;
    }

    public void addRelease(DatasetRelease release) {
        releases.add(release);
        release.setDatasetSource(this);
    }

    public void removeRelease(DatasetRelease release) {
        releases.remove(release);
        release.setDatasetSource(null);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
