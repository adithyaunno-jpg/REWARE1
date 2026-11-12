package rewear.models;

/**
 * Concrete Subclass representing a registered College Student in ReWear.
 * Demonstrates Inheritance, Method Overriding, and Constructor Overloading.
 */
public class Student extends User {
    private String collegeName;
    private String biography;
    private String profilePhotoUrl;
    private double ratingAverage;
    private int ratingCount;
    private String preferredSwapCategory;

    // Default Constructor
    public Student() {
        super();
        setRole("STUDENT");
        this.ratingAverage = 5.0;
        this.ratingCount = 0;
        this.collegeName = "";
        this.biography = "";
        this.profilePhotoUrl = "";
        this.preferredSwapCategory = "All";
    }

    // Overloaded Constructor
    public Student(String id, String fullName, String email, String passwordHash, String salt,
                   String collegeName, String biography, String profilePhotoUrl, String createdAt) {
        super(id, fullName, email, passwordHash, salt, "STUDENT", createdAt);
        this.collegeName = collegeName;
        this.biography = biography;
        this.profilePhotoUrl = profilePhotoUrl;
        this.ratingAverage = 5.0;
        this.ratingCount = 0;
        this.preferredSwapCategory = "All";
    }

    // Method Overriding (Runtime Polymorphism)
    @Override
    public String getRoleDisplay() {
        return "Student Member (" + (collegeName.isEmpty() ? "Campus Network" : collegeName) + ")";
    }

    @Override
    public boolean canModerate() {
        return false;
    }

    // Getters and Setters
    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName != null ? collegeName.trim() : "";
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography != null ? biography.trim() : "";
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl != null ? profilePhotoUrl.trim() : "";
    }

    public double getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(double ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public String getPreferredSwapCategory() {
        return preferredSwapCategory;
    }

    public void setPreferredSwapCategory(String preferredSwapCategory) {
        this.preferredSwapCategory = preferredSwapCategory;
    }

    @Override
    public String toString() {
        return "Student[id=" + getId() + ", name=" + getFullName() + ", college=" + collegeName + ", rating=" + ratingAverage + "]";
    }
}

