package nz.waiwatts.ingestion.mbie;

import java.math.BigDecimal;

public record MbieGenerationQuarterlyParsedRecord(int periodYear,
                                                  int periodQuarter,
                                                  String fuelTypeRaw,
                                                  String fuelTypeNorm,
                                                  BigDecimal generationGwh) {
}
