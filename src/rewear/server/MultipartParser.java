package rewear.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import rewear.security.InputSanitizer;

/**
 * Pure Core Java RFC-7578 compliant Multipart/form-data Parser.
 * Enables zero-dependency file uploads and multi-part form decoding.
 * Demonstrates:
 * - Low-level Byte Stream Processing
 * - State Machine Boundary Parsing
 * - File Security and Sanitization
 * - Zero external library dependency
 */
public class MultipartParser {

    public static class ParseResult {
        private final Map<String, String> formFields;
        private final List<String> savedFileUrls;

        public ParseResult(Map<String, String> formFields, List<String> savedFileUrls) {
            this.formFields = formFields;
            this.savedFileUrls = savedFileUrls;
        }

        public Map<String, String> getFormFields() {
            return formFields;
        }

        public List<String> getSavedFileUrls() {
            return savedFileUrls;
        }

        public String getField(String name) {
            return formFields.get(name);
        }
    }

    /**
     * Parses a multipart request stream and writes valid image files to disk.
     */
    public static ParseResult parse(InputStream inputStream, String contentType, String uploadDirectory, String publicUrlPrefix)
            throws IOException {

        Map<String, String> fields = new HashMap<>();
        List<String> fileUrls = new ArrayList<>();

        if (contentType == null || !contentType.toLowerCase().contains("multipart/form-data")) {
            return new ParseResult(fields, fileUrls);
        }

        // Extract boundary parameter
        String boundary = null;
        for (String param : contentType.split(";")) {
            param = param.trim();
            if (param.toLowerCase().startsWith("boundary=")) {
                boundary = param.substring("boundary=".length());
                if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                break;
            }
        }

        if (boundary == null || boundary.isEmpty()) {
            return new ParseResult(fields, fileUrls);
        }

        // Read complete stream into memory
        byte[] payload = readAllBytes(inputStream);
        if (payload.length == 0) {
            return new ParseResult(fields, fileUrls);
        }

        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
        List<Integer> boundaryPositions = findOccurrences(payload, boundaryBytes);

        if (boundaryPositions.size() < 2) {
            return new ParseResult(fields, fileUrls);
        }

        // Ensure upload directory exists
        Path dirPath = Paths.get(uploadDirectory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        for (int i = 0; i < boundaryPositions.size() - 1; i++) {
            int partStart = boundaryPositions.get(i) + boundaryBytes.length;
            int partEnd = boundaryPositions.get(i + 1);

            // Skip CRLF immediately after boundary
            if (partStart + 1 < payload.length && payload[partStart] == '\r' && payload[partStart + 1] == '\n') {
                partStart += 2;
            } else if (partStart < payload.length && payload[partStart] == '\n') {
                partStart += 1;
            }

            // Trim trailing CRLF before next boundary
            if (partEnd - 2 >= partStart && payload[partEnd - 2] == '\r' && payload[partEnd - 1] == '\n') {
                partEnd -= 2;
            } else if (partEnd - 1 >= partStart && payload[partEnd - 1] == '\n') {
                partEnd -= 1;
            }

            if (partStart >= partEnd) continue;

            // Locate headers and body delimiter (\r\n\r\n or \n\n)
            int headerEnd = -1;
            int bodyStart = -1;
            for (int j = partStart; j < partEnd - 3; j++) {
                if (payload[j] == '\r' && payload[j+1] == '\n' && payload[j+2] == '\r' && payload[j+3] == '\n') {
                    headerEnd = j;
                    bodyStart = j + 4;
                    break;
                }
            }
            if (headerEnd == -1) {
                for (int j = partStart; j < partEnd - 1; j++) {
                    if (payload[j] == '\n' && payload[j+1] == '\n') {
                        headerEnd = j;
                        bodyStart = j + 2;
                        break;
                    }
                }
            }

            if (headerEnd == -1 || bodyStart == -1) continue;

            String headerText = new String(payload, partStart, headerEnd - partStart, StandardCharsets.UTF_8);
            Map<String, String> headers = parseHeaders(headerText);
            String contentDisposition = headers.get("content-disposition");

            if (contentDisposition != null) {
                String fieldName = extractParam(contentDisposition, "name");
                String filename = extractParam(contentDisposition, "filename");

                if (filename != null && !filename.trim().isEmpty()) {
                    // File upload field
                    int fileDataLength = partEnd - bodyStart;
                    if (fileDataLength > 0) {
                        String sanitized = InputSanitizer.sanitizeFilename(filename);
                        String ext = getFileExtension(sanitized).toLowerCase();

                        // Validate allowed image extensions (JPEG, PNG, WebP)
                        if (Arrays.asList(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
                            String uniqueName = "img_" + UUID.randomUUID().toString().substring(0, 8) + "_" + System.currentTimeMillis() + ext;
                            File targetFile = new File(uploadDirectory, uniqueName);

                            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                                fos.write(payload, bodyStart, fileDataLength);
                            }

                            String publicPath = publicUrlPrefix + (publicUrlPrefix.endsWith("/") ? "" : "/") + uniqueName;
                            fileUrls.add(publicPath);
                        }
                    }
                } else if (fieldName != null) {
                    // Regular text form field
                    int textLength = partEnd - bodyStart;
                    String value = new String(payload, bodyStart, textLength, StandardCharsets.UTF_8);
                    fields.put(fieldName, value);
                }
            }
        }

        return new ParseResult(fields, fileUrls);
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = is.read(chunk, 0, chunk.length)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static List<Integer> findOccurrences(byte[] source, byte[] target) {
        List<Integer> list = new ArrayList<>();
        if (source.length < target.length) return list;

        for (int i = 0; i <= source.length - target.length; i++) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                list.add(i);
                i += target.length - 1; // Skip ahead
            }
        }
        return list;
    }

    private static Map<String, String> parseHeaders(String headerBlock) {
        Map<String, String> map = new HashMap<>();
        String[] lines = headerBlock.split("\\r?\\n");
        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String key = line.substring(0, colon).trim().toLowerCase();
                String val = line.substring(colon + 1).trim();
                map.put(key, val);
            }
        }
        return map;
    }

    private static String extractParam(String headerValue, String paramName) {
        String target = paramName + "=";
        int idx = headerValue.indexOf(target);
        if (idx == -1) return null;

        int start = idx + target.length();
        if (start < headerValue.length() && headerValue.charAt(start) == '"') {
            start++;
            int end = headerValue.indexOf('"', start);
            return end != -1 ? headerValue.substring(start, end) : headerValue.substring(start);
        } else {
            int end = headerValue.indexOf(';', start);
            return end != -1 ? headerValue.substring(start, end).trim() : headerValue.substring(start).trim();
        }
    }

    private static String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}

