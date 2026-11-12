package rewear.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Robust, line-based, key-value record serializer and parser.
 * Handles multiline text, escapes special characters, and prevents corruption.
 * Demonstrates String manipulation, File I/O parsing, and Defensive programming.
 */
public class RecordParser {
    public static final String RECORD_START = "===RECORD_START===";
    public static final String RECORD_END = "===RECORD_END===";

    /**
     * Serializes a Map of key-value pairs into a safe string block.
     */
    public static String serialize(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append(RECORD_START).append("\n");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey().trim();
            String value = entry.getValue() != null ? entry.getValue() : "";
            // Escape backslashes, newlines, and carriage returns
            value = value.replace("\\", "\\\\")
                         .replace("\n", "\\n")
                         .replace("\r", "");
            sb.append(key).append("=").append(value).append("\n");
        }
        sb.append(RECORD_END).append("\n");
        return sb.toString();
    }

    /**
     * Parses a multiline text file containing multiple blocks into a List of Key-Value Maps.
     */
    public static List<Map<String, String>> parse(String fileContent) {
        List<Map<String, String>> records = new ArrayList<>();
        if (fileContent == null || fileContent.trim().isEmpty()) {
            return records;
        }

        String[] lines = fileContent.split("\\r?\\n");
        Map<String, String> currentRecord = null;

        for (String line : lines) {
            line = line.trim();
            if (line.equals(RECORD_START)) {
                currentRecord = new HashMap<>();
            } else if (line.equals(RECORD_END)) {
                if (currentRecord != null && !currentRecord.isEmpty()) {
                    records.add(currentRecord);
                }
                currentRecord = null;
            } else if (currentRecord != null && line.contains("=")) {
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String rawValue = line.substring(equalsIndex + 1);
                    // Unescape backslash and newline sequences safely
                    String value = rawValue.replace("\\n", "\n")
                                           .replace("\\\\", "\\");
                    currentRecord.put(key, value);
                }
            }
        }
        return records;
    }

    /**
     * Helper to encode a list of strings into a single delimited string.
     */
    public static String encodeList(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(";;;");
            sb.append(items.get(i).replace(";;;", ""));
        }
        return sb.toString();
    }

    /**
     * Helper to decode a delimited string back into a list.
     */
    public static List<String> decodeList(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return list;
        String[] parts = raw.split(";;;");
        for (String p : parts) {
            if (!p.trim().isEmpty()) {
                list.add(p.trim());
            }
        }
        return list;
    }
}

