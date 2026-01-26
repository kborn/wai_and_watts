package nz.waiwatts.ingestion.mbie;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface MbieGenerationParser {
    List<MbieGenerationParsedRecord> parse(InputStream input) throws IOException;
}
