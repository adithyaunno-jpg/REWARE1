package rewear.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Domain entity representing the physical garment details.
 * Demonstrates Encapsulation and Data Modeling.
 */
public class ClothingItem implements Serializable {
    private String title;
    private String description;
    private String category;
    private String size;
    private String condition;
    private String brand;
    private String color;
    private String passportId;

    public ClothingItem() {
        this.title = "";
        this.description = "";
        this.category = "Jackets & Coats";
        this.size = "M";
        this.condition = "Gently Used";
        this.brand = "Unbranded";
        this.color = "Neutral";
        this.passportId = "";
    }

    public ClothingItem(String title, String description, String category, String size, 
                        String condition, String brand, String color, String passportId) {
        this.title = title != null ? title.trim() : "";
        this.description = description != null ? description.trim() : "";
        this.category = category != null ? category.trim() : "Jackets & Coats";
        this.size = size != null ? size.trim() : "M";
        this.condition = condition != null ? condition.trim() : "Gently Used";
        this.brand = brand != null ? brand.trim() : "Vintage/Campus";
        this.color = color != null ? color.trim() : "Multi";
        this.passportId = passportId != null ? passportId.trim() : "";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPassportId() {
        return passportId;
    }

    public void setPassportId(String passportId) {
        this.passportId = passportId;
    }

    @Override
    public String toString() {
        return title + " (" + category + ", Size " + size + ", " + condition + ")";
    }
}

