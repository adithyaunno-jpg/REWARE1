package rewear.models;

/**
 * Concrete Subclass representing an Administrator in ReWear.
 * Demonstrates Inheritance, Method Overriding, and Privilege Separation.
 */
public class Admin extends User {
    private String department;

    public Admin() {
        super();
        setRole("ADMIN");
        this.department = "Campus Sustainability Board";
    }

    public Admin(String id, String fullName, String email, String passwordHash, String salt, String department, String createdAt) {
        super(id, fullName, email, passwordHash, salt, "ADMIN", createdAt);
        this.department = department;
    }

    @Override
    public String getRoleDisplay() {
        return "System Administrator (" + department + ")";
    }

    @Override
    public boolean canModerate() {
        return true;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return "Admin[id=" + getId() + ", name=" + getFullName() + ", dept=" + department + "]";
    }
}

