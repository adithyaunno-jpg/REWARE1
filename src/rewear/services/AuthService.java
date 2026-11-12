package rewear.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import rewear.models.Student;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.security.PasswordSecurity;
import rewear.security.SessionManager;
import rewear.storage.DataStore;

/**
 * Service orchestrating Student and Administrator authentication, session generation,
 * credential verification, and profile management.
 */
public class AuthService {
    private final DataStore dataStore;
    private final SessionManager sessionManager;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AuthService(DataStore dataStore, SessionManager sessionManager) {
        this.dataStore = dataStore;
        this.sessionManager = sessionManager;
    }

    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final User user;
        private final SessionManager.UserSession session;

        public AuthResult(boolean success, String message, User user, SessionManager.UserSession session) {
            this.success = success;
            this.message = message;
            this.user = user;
            this.session = session;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
        public SessionManager.UserSession getSession() { return session; }
    }

    /**
     * Registers a new student account with server-side validation and PBKDF2 hashing.
     */
    public AuthResult registerStudent(String fullName, String email, String password, String collegeName, String bio) {
        if (fullName == null || fullName.trim().length() < 2) {
            return new AuthResult(false, "Full name must be at least 2 characters.", null, null);
        }
        if (!InputSanitizer.isValidEmail(email)) {
            return new AuthResult(false, "Please provide a valid college or personal email address.", null, null);
        }
        if (password == null || password.length() < 6) {
            return new AuthResult(false, "Password must be at least 6 characters long.", null, null);
        }
        if (collegeName == null || collegeName.trim().isEmpty()) {
            return new AuthResult(false, "Please specify your college or university name.", null, null);
        }

        String normalizedEmail = email.toLowerCase().trim();
        if (dataStore.getUserByEmail(normalizedEmail) != null) {
            return new AuthResult(false, "An account with this email already exists.", null, null);
        }

        PasswordSecurity.HashResult hashResult = PasswordSecurity.hashPassword(password);
        String studentId = "STU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = LocalDateTime.now().format(DATE_FORMAT);

        Student student = new Student(
                studentId,
                fullName.trim(),
                normalizedEmail,
                hashResult.getHash(),
                hashResult.getSalt(),
                collegeName.trim(),
                bio != null ? bio.trim() : "Campus member enthusiastic about circular fashion.",
                "",
                now
        );

        dataStore.addUser(student);
        SessionManager.UserSession session = sessionManager.createSession(student);
        return new AuthResult(true, "Registration successful! Welcome to ReWear.", student, session);
    }

    /**
     * Authenticates user against stored PBKDF2 credentials.
     */
    public AuthResult login(String email, String password) {
        if (email == null || password == null) {
            return new AuthResult(false, "Email and password are required.", null, null);
        }
        User user = dataStore.getUserByEmail(email.toLowerCase().trim());
        if (user == null) {
            return new AuthResult(false, "Invalid email or password.", null, null);
        }
        if (!user.isActive()) {
            return new AuthResult(false, "This account has been suspended by campus administration.", null, null);
        }

        boolean valid = PasswordSecurity.verifyPassword(password, user.getSalt(), user.getPasswordHash());
        if (!valid) {
            return new AuthResult(false, "Invalid email or password.", null, null);
        }

        SessionManager.UserSession session = sessionManager.createSession(user);
        return new AuthResult(true, "Login successful.", user, session);
    }

    public void logout(String token) {
        if (token != null) {
            sessionManager.invalidate(token);
        }
    }

    public User getAuthenticatedUser(String token) {
        SessionManager.UserSession session = sessionManager.getSession(token);
        if (session != null) {
            return dataStore.getUserById(session.getUserId());
        }
        return null;
    }

    public boolean updateStudentProfile(String studentId, String fullName, String college, String bio, String swapCategory) {
        User user = dataStore.getUserById(studentId);
        if (user instanceof Student student) {
            if (fullName != null && !fullName.trim().isEmpty()) student.setFullName(fullName.trim());
            if (college != null && !college.trim().isEmpty()) student.setCollegeName(college.trim());
            if (bio != null) student.setBiography(bio.trim());
            if (swapCategory != null) student.setPreferredSwapCategory(swapCategory.trim());
            dataStore.updateUser(student);
            return true;
        }
        return false;
    }
}

