package nz.waiwatts.ingestion.lawa;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface LawaTrendMultiYearParser {
    List<LawaTrendMultiYearParsedRecord> parse(InputStream input) throws IOException;
}
