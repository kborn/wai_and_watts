package nz.waiwatts.ingestion.mbie;

import java.util.Set;

public final class MbieFuelNormalizer {

    private static final Set<String> KNOWN = Set.of("HYDRO", "GEOTHERMAL", "WIND", "SOLAR", "GAS", "COAL", "OTHER");

    private MbieFuelNormalizer() {
    }

    public static String normalizeFuel(String raw) {
        if (raw == null) return "OTHER";
        String s = raw.trim();
        s = s.replace('\u2011', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('-', ' ')
                .replace('/', ' ');
        s = s.replaceAll("\\s+", " ").trim().toUpperCase();
        String compact = s.replace(" ", "");
        if (compact.contains("BIOGAS")) return "OTHER";
        if (s.contains("GEO")) return "GEOTHERMAL";
        if (s.contains("SOLAR")) return "SOLAR";
        if (s.contains("WIND")) return "WIND";
        if (s.contains("HYDRO")) return "HYDRO";
        if (s.contains("GAS")) return "GAS";
        if (s.contains("COAL")) return "COAL";
        return "OTHER";
    }

    public static String mapToKnown(String token) {
        String t = token == null ? "" : token.trim().toUpperCase();
        if (KNOWN.contains(t)) return t;
        return normalizeFuel(token);
    }
}
