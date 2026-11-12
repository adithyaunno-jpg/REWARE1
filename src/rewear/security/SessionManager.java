package rewear.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import rewear.models.Student;
import rewear.models.User;

/**
 * Thread-safe HTTP session state manager.
 * Demonstrates Concurrency, Map data structures, and Token-based Session Management.
 */
public class SessionManager {
    public static final String COOKIE_NAME = "rewear_session";
    private static final long SESSION_LIFETIME_MS = 24 * 60 * 60 * 1000L; // 24 hours

    public static class UserSession {
        private final String token;
        private final String userId;
        private final String email;
        private final String role;
        private final String fullName;
        private final String collegeName;
        private final long expiryTime;

        public UserSession(String token, String userId, String email, String role, 
                           String fullName, String collegeName, long expiryTime) {
            this.token = token;
            this.userId = userId;
            this.email = email;
            this.role = role;
            this.fullName = fullName;
            this.collegeName = collegeName;
            this.expiryTime = expiryTime;
        }

        public String getToken() {
            return token;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }

        public String getFullName() {
            return fullName;
        }

        public String getCollegeName() {
            return collegeName;
        }

        public long getExpiryTime() {
            return expiryTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

        public boolean isAdmin() {
            return "ADMIN".equalsIgnoreCase(role);
        }
    }

    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    public UserSession createSession(User user) {
        String token = UUID.randomUUID().toString();
        long expiry = System.currentTimeMillis() + SESSION_LIFETIME_MS;
        String college = "";
        if (user instanceof Student student) {
            college = student.getCollegeName();
        }

        UserSession session = new UserSession(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName(),
                college,
                expiry
        );

        activeSessions.put(token, session);
        cleanExpiredSessions();
        return session;
    }

    public UserSession getSession(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        UserSession session = activeSessions.get(token);
        if (session != null) {
            if (session.isExpired()) {
                activeSessions.remove(token);
                return null;
            }
            return session;
        }
        return null;
    }

    public void invalidate(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }

    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry -> entry.getValue().getExpiryTime() < now);
    }
}
