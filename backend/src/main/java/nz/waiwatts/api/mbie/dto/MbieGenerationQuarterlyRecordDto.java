package nz.waiwatts.api.mbie.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class MbieGenerationQuarterlyRecordDto {
    private int periodYear;
    private int periodQuarter;
    private String source; // normalized fuel type
    private String sourceRaw;
    private BigDecimal generationGwh;
    private UUID releaseId;

    public MbieGenerationQuarterlyRecordDto() {}

    public MbieGenerationQuarterlyRecordDto(int periodYear, int periodQuarter, String source, String sourceRaw, BigDecimal generationGwh, UUID releaseId) {
        this.periodYear = periodYear;
        this.periodQuarter = periodQuarter;
        this.source = source;
        this.sourceRaw = sourceRaw;
        this.generationGwh = generationGwh;
        this.releaseId = releaseId;
    }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public int getPeriodQuarter() { return periodQuarter; }
    public void setPeriodQuarter(int periodQuarter) { this.periodQuarter = periodQuarter; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceRaw() { return sourceRaw; }
    public void setSourceRaw(String sourceRaw) { this.sourceRaw = sourceRaw; }

    public BigDecimal getGenerationGwh() { return generationGwh; }
    public void setGenerationGwh(BigDecimal generationGwh) { this.generationGwh = generationGwh; }

    public UUID getReleaseId() { return releaseId; }
    public void setReleaseId(UUID releaseId) { this.releaseId = releaseId; }
}
