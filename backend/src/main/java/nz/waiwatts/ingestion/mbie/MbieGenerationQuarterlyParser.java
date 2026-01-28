package nz.waiwatts.ingestion.mbie;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface MbieGenerationQuarterlyParser {
    List<MbieGenerationQuarterlyParsedRecord> parse(InputStream input) throws IOException;
}
