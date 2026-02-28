package nz.waiwatts.ingestion.core;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for file operations in ingestion.
 * Provides common methods for reading files from classpath or local filesystem.
 */
public class FileIngestionUtil {

    private static final List<Path> TRUSTED_ROOTS = buildTrustedRoots();

    private FileIngestionUtil() {
        // Utility class - no instantiation
    }

    /**
     * Read all bytes from a local filesystem path.
     *
     * @param filePath the file path to read
     * @return file contents as byte array
     * @throws IllegalArgumentException if file does not exist or is not readable
     * @throws RuntimeException if file cannot be read
     */
    public static byte[] readFileBytes(String filePath) {
        Path path = resolveReadableRegularFile(filePath);
        return readFileBytes(path);
    }

    /**
     * Read all bytes from a previously validated file path.
     *
     * @param filePath validated readable file path
     * @return file contents as byte array
     * @throws RuntimeException if file cannot be read
     */
    public static byte[] readFileBytes(Path filePath) {
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath.toAbsolutePath(), e);
        }
    }

    public static byte[] readClasspathBytes(String classpath) {
        try {
            ClassPathResource resource = new ClassPathResource(classpath);
            try (var inputStream = resource.getInputStream()) {
                return inputStream.readAllBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load fixture from classpath: " + classpath, e);
        }
    }

    /**
     * Compute SHA-256 hash of file bytes.
     *
     * @param bytes file contents
     * @return SHA-256 hash as hex string
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Resolve and validate a user-provided readable file path.
     *
     * @param filePath the file path to validate
     * @return normalized absolute path that points to a readable regular file
     */
    // JvmTaintAnalysis false positive: path traversal is mitigated by raw-token screening,
    // absolute normalization, trusted-root containment, and regular-file/symlink checks below.
    @SuppressWarnings("JvmTaintAnalysis")
    public static Path resolveReadableRegularFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        validateRawPathInput(filePath, "File path");

        if (filePath.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("File path contains illegal null byte");
        }

        final Path normalized;
        try {
            normalized = Paths.get(filePath).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("File path is invalid: " + filePath, e);
        }
        requireTrustedRoot(normalized, "File path");

        if (!Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("File does not exist: " + normalized);
        }

        if (Files.isSymbolicLink(normalized)) {
            throw new IllegalArgumentException("Symbolic links are not allowed for ingestion input: " + normalized);
        }

        if (!Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Path is not a regular file: " + normalized);
        }

        if (!Files.isReadable(normalized)) {
            throw new IllegalArgumentException("File is not readable: " + normalized);
        }

        try {
            if (Files.size(normalized) == 0) {
                throw new IllegalArgumentException("File is empty: " + normalized);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read file size: " + normalized, e);
        }

        return normalized;
    }

    /**
     * Validate that a file path is safe to read.
     *
     * @param filePath the file path to validate
     * @throws IllegalArgumentException if path appears unsafe
     */
    public static void validateFilePath(String filePath) {
        resolveReadableRegularFile(filePath);
    }

    /**
     * Resolve and validate a writable CSV output path.
     *
     * @param outputPath path where transformed CSV should be written
     * @return normalized absolute output path
     */
    // JvmTaintAnalysis false positive: output path is validated with the same traversal
    // controls (token screening + normalized trusted-root containment + file-type checks).
    @SuppressWarnings("JvmTaintAnalysis")
    public static Path resolveWritableCsvOutputPath(String outputPath) {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be null or empty");
        }
        validateRawPathInput(outputPath, "Output path");

        if (outputPath.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Output path contains illegal null byte");
        }

        final Path normalized;
        try {
            normalized = Paths.get(outputPath).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Output path is invalid: " + outputPath, e);
        }
        requireTrustedRoot(normalized, "Output path");

        String fileName = normalized.getFileName() == null ? "" : normalized.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Output path must end with .csv: " + normalized);
        }

        if (Files.exists(normalized, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Output path must be a regular file: " + normalized);
        }
        if (Files.exists(normalized) && !Files.isWritable(normalized)) {
            throw new IllegalArgumentException("Output file is not writable: " + normalized);
        }

        Path parent = normalized.getParent();
        if (parent != null && Files.exists(parent, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(parent)) {
            throw new IllegalArgumentException("Output directory cannot be a symbolic link: " + parent);
        }

        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to create output directory: " + parent, e);
            }
        }
        if (parent != null && !Files.isWritable(parent)) {
            throw new IllegalArgumentException("Output directory is not writable: " + parent);
        }

        return normalized;
    }

    public static String fileUri(Path path) {
        return path.toUri().toString();
    }

    private static List<Path> buildTrustedRoots() {
        List<Path> roots = new ArrayList<>();
        roots.add(Paths.get("").toAbsolutePath().normalize());
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir != null && !tmpDir.isBlank()) {
            roots.add(Paths.get(tmpDir).toAbsolutePath().normalize());
        }
        return roots;
    }

    private static void requireTrustedRoot(Path path, String label) {
        for (Path root : TRUSTED_ROOTS) {
            if (path.startsWith(root)) {
                return;
            }
        }
        throw new IllegalArgumentException(label + " must be under trusted roots: " + path);
    }

    private static void validateRawPathInput(String rawPath, String label) {
        String lowered = rawPath.toLowerCase(Locale.ROOT);
        if (rawPath.contains("..")
            || lowered.contains("%2e")
            || lowered.contains("%2f")
            || lowered.contains("%5c")
            || rawPath.contains("~")) {
            throw new IllegalArgumentException(label + " contains traversal-sensitive tokens");
        }
    }
}
