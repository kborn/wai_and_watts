package nz.waiwatts.ingestion.mbie;

import java.math.BigDecimal;

public record MbieGenerationAnnualParsedRecord(int periodYear,
                                               String fuelTypeRaw,
                                               String fuelTypeNorm,
                                               BigDecimal generationGwh) {
}
