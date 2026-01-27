package nz.waiwatts.ingestion.mbie;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface MbieGenerationAnnualParser {
    List<MbieGenerationAnnualParsedRecord> parse(InputStream input) throws IOException;
}
