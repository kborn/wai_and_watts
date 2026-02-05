package nz.waiwatts.ingestion.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileIngestionUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void readFileBytes_whenFileExists_returnsContent() throws IOException {
        // Arrange
        String content = "test file content";
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, content);

        // Act
        byte[] result = FileIngestionUtil.readFileBytes(file.toString());

        // Assert
        assertEquals(content, new String(result));
    }

    @Test
    void readFileBytes_whenFileDoesNotExist_throwsException() {
        // Arrange
        String nonExistentPath = tempDir.resolve("nonexistent.txt").toString();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileIngestionUtil.readFileBytes(nonExistentPath)
        );
        assertTrue(exception.getMessage().contains("File does not exist"));
    }

    @Test
    void readFileBytes_whenFileIsNotReadable_throwsException() throws IOException {
        // Arrange
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");
        file.toFile().setReadable(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileIngestionUtil.readFileBytes(file.toString())
        );
        assertTrue(exception.getMessage().contains("File is not readable"));
        
        // Cleanup: restore readability for cleanup
        file.toFile().setReadable(true);
    }

    @Test
    void readFileBytes_whenPathIsDirectory_throwsException() {
        // Arrange
        String dirPath = tempDir.toString();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileIngestionUtil.readFileBytes(dirPath)
        );
        assertTrue(exception.getMessage().contains("Path is not a regular file"));
    }

    @Test
    void sha256Hex_returnsConsistentHash() {
        // Arrange
        String content = "test content";
        byte[] bytes = content.getBytes();

        // Act
        String hash1 = FileIngestionUtil.sha256Hex(bytes);
        String hash2 = FileIngestionUtil.sha256Hex(bytes);

        // Assert
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 hex length
        assertFalse(hash1.contains("["));
        assertFalse(hash1.contains("]"));
    }

    @Test
    void sha256Hex_differentContent_differentHashes() {
        // Arrange
        byte[] content1 = "content1".getBytes();
        byte[] content2 = "content2".getBytes();

        // Act
        String hash1 = FileIngestionUtil.sha256Hex(content1);
        String hash2 = FileIngestionUtil.sha256Hex(content2);

        // Assert
        assertNotEquals(hash1, hash2);
    }

    @Test
    void validateFilePath_whenValidPath_passes() {
        // Arrange
        String validPath = "/path/to/file.csv";

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> FileIngestionUtil.validateFilePath(validPath));
    }

    @Test
    void validateFilePath_whenNull_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileIngestionUtil.validateFilePath(null)
        );
        assertTrue(exception.getMessage().contains("File path cannot be null or empty"));
    }

    @Test
    void validateFilePath_whenEmpty_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileIngestionUtil.validateFilePath("")
        );
        assertTrue(exception.getMessage().contains("File path cannot be null or empty"));
    }

    @Test
    void validateFilePath_whenContainsParentReference_throwsException() {
        // Arrange
        String unsafePath = "/path/../file.csv";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileIngestionUtil.validateFilePath(unsafePath)
        );
        assertTrue(exception.getMessage().contains("potentially unsafe characters"));
    }

    @Test
    void validateFilePath_whenContainsHomeReference_throwsException() {
        // Arrange
        String unsafePath = "/path/~/file.csv";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileIngestionUtil.validateFilePath(unsafePath)
        );
        assertTrue(exception.getMessage().contains("potentially unsafe characters"));
    }
}