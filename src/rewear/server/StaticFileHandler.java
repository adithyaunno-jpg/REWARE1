package rewear.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * HTTP Handler for serving static CSS assets and uploaded garment photographs.
 * Enforces Path Traversal prevention and MIME type mappings.
 */
public class StaticFileHandler implements HttpHandler {
    private final String baseFileSystemPath;
    private final String uriRoutePrefix;

    public StaticFileHandler(String baseFileSystemPath, String uriRoutePrefix) {
        this.baseFileSystemPath = baseFileSystemPath;
        this.uriRoutePrefix = uriRoutePrefix;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (!requestPath.startsWith(uriRoutePrefix)) {
            sendError(exchange, 404, "Not Found");
            return;
        }

        String relativePath = requestPath.substring(uriRoutePrefix.length());
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        Path basePath = Paths.get(baseFileSystemPath).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(relativePath).normalize();

        // Path Traversal Security Verification
        if (!targetPath.startsWith(basePath)) {
            sendError(exchange, 403, "Access Denied");
            return;
        }

        File file = targetPath.toFile();
        if (!file.exists() || !file.isFile()) {
            sendError(exchange, 404, "Asset Not Found: " + relativePath);
            return;
        }

        String mimeType = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
        exchange.sendResponseHeaders(200, file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = exchange.getResponseBody()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        }
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        return "application/octet-stream";
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] bytes = message.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

