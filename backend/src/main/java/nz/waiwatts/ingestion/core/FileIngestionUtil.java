package nz.waiwatts.ingestion.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for file operations in ingestion.
 * Provides common methods for reading files from classpath or local filesystem.
 */
public class FileIngestionUtil {

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
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("File is not readable: " + filePath);
        }
        
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path is not a regular file: " + filePath);
        }
        
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
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
     * Validate that a file path is safe to read.
     * Basic validation to prevent obvious path traversal issues.
     *
     * @param filePath the file path to validate
     * @throws IllegalArgumentException if path appears unsafe
     */
    public static void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        // Basic path traversal checks
        if (filePath.contains("..") || filePath.contains("~")) {
            throw new IllegalArgumentException("File path contains potentially unsafe characters: " + filePath);
        }
    }
}