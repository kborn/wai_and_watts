package nz.waiwatts.api.mbie.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class MbieGenerationAnnualRecordDto {
    private int periodYear;
    private String source; // normalized fuel type
    private String sourceRaw;
    private BigDecimal generationGwh;
    private UUID releaseId;

    public MbieGenerationAnnualRecordDto() {}

    public MbieGenerationAnnualRecordDto(int periodYear, String source, String sourceRaw, BigDecimal generationGwh, UUID releaseId) {
        this.periodYear = periodYear;
        this.source = source;
        this.sourceRaw = sourceRaw;
        this.generationGwh = generationGwh;
        this.releaseId = releaseId;
    }

    public int getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(int periodYear) {
        this.periodYear = periodYear;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceRaw() {
        return sourceRaw;
    }

    public void setSourceRaw(String sourceRaw) {
        this.sourceRaw = sourceRaw;
    }

    public BigDecimal getGenerationGwh() {
        return generationGwh;
    }

    public void setGenerationGwh(BigDecimal generationGwh) {
        this.generationGwh = generationGwh;
    }

    public UUID getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(UUID releaseId) {
        this.releaseId = releaseId;
    }
}
