package nz.waiwatts.ingestion.core;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class DatasetIngestionRequest {
    @NotBlank
    private String datasetSourceCode; // looked up via DatasetSource.code
    private String releaseLabel; // optional in Phase 4
    private LocalDate publishedDate; // nullable
    private String sourceUri; // optional metadata (URL)
    @NotBlank
    private String contentHash;

    public String getDatasetSourceCode() {
        return datasetSourceCode;
    }

    public void setDatasetSourceCode(String datasetSourceCode) {
        this.datasetSourceCode = datasetSourceCode;
    }

    public String getReleaseLabel() {
        return releaseLabel;
    }

    public void setReleaseLabel(String releaseLabel) {
        this.releaseLabel = releaseLabel;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDate publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
}
