package rewear.models;

import java.util.Objects;

/**
 * Abstract Base Class representing a User in the ReWear platform.
 * Demonstrates:
 * - Abstraction (cannot be instantiated directly)
 * - Inheritance (extended by Student and Admin)
 * - Encapsulation (private fields with controlled access)
 * - Constructor Overloading
 * - Method Overriding (equals, hashCode, toString)
 */
public abstract class User implements Identifiable {
    private String id;
    private String fullName;
    private String email;
    private String passwordHash;
    private String salt;
    private String role; // "STUDENT" or "ADMIN"
    private String createdAt;
    private boolean active;

    // Default Constructor
    public User() {
        this.active = true;
    }

    // Overloaded Constructor for core user attributes
    public User(String id, String fullName, String email, String passwordHash, String salt, String role, String createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.createdAt = createdAt;
        this.active = true;
    }

    // Abstract methods to demonstrate Polymorphism across user types
    public abstract String getRoleDisplay();
    public abstract boolean canModerate();

    // Getters and Setters with Encapsulation
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim() : "";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : "";
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id) || Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", name=" + fullName + ", email=" + email + ", role=" + role + "]";
    }
}

