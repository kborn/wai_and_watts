package nz.waiwatts.service.lawa;

import java.util.Locale;

final class LawaReadServiceSupport {

    private LawaReadServiceSupport() {
    }

    static String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
