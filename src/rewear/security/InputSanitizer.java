package rewear.security;

import java.util.regex.Pattern;

/**
 * Utility for sanitizing user inputs, preventing Cross-Site Scripting (XSS),
 * and mitigating Directory Traversal attacks.
 */
public class InputSanitizer {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Escapes critical HTML characters to prevent XSS injection.
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(Math.max(16, text.length()));
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#x27;");
                    break;
                default:
                    out.append(c);
                    break;
            }
        }
        return out.toString();
    }

    /**
     * Sanitizes uploaded filenames to prevent directory traversal and malicious characters.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "file.dat";
        }
        // Extract base name only (strip any path components)
        String base = filename.replace("\\", "/");
        int lastSlash = base.lastIndexOf('/');
        if (lastSlash >= 0) {
            base = base.substring(lastSlash + 1);
        }
        // Remove directory traversal sequences and invalid characters
        base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.isEmpty() || base.startsWith(".")) {
            base = "upload_" + base;
        }
        return base;
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}

