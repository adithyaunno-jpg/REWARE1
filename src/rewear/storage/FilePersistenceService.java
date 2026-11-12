package rewear.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * File I/O service providing safe UTF-8 file reading and writing.
 * Automatically creates directories, ensures non-destructive persistence,
 * and handles IOExceptions gracefully.
 */
public class FilePersistenceService {
    private final String dataDirectory;

    public FilePersistenceService(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        initDirectory();
    }

    private void initDirectory() {
        try {
            Path path = Paths.get(dataDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("[FilePersistenceService] Warning: Could not create data directory: " + e.getMessage());
        }
    }

    public synchronized String readFile(String filename) {
        try {
            Path path = Paths.get(dataDirectory, filename);
            if (!Files.exists(path)) {
                return "";
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[FilePersistenceService] Error reading " + filename + ": " + e.getMessage());
            return "";
        }
    }

    public synchronized void writeFile(String filename, String content) {
        try {
            Path path = Paths.get(dataDirectory, filename);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(
                    path,
                    content != null ? content : "",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            System.err.println("[FilePersistenceService] Error writing " + filename + ": " + e.getMessage());
        }
    }

    public synchronized void appendToFile(String filename, String content) {
        try {
            Path path = Paths.get(dataDirectory, filename);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(
                    path,
                    content != null ? content : "",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("[FilePersistenceService] Error appending to " + filename + ": " + e.getMessage());
        }
    }

    public boolean exists(String filename) {
        return Files.exists(Paths.get(dataDirectory, filename));
    }
}

