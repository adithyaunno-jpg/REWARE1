package rewear.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a marketplace clothing listing on ReWear.
 * Demonstrates:
 * - Interface Implementation (Identifiable, Searchable)
 * - Encapsulation
 * - Method Overloading
 * - List Collections
 */
public class Listing implements Identifiable, Searchable {
    private String id;
    private String sellerId;
    private String sellerName;
    private String sellerCollege;
    private ClothingItem item;
    private double price;
    private String status; // "AVAILABLE", "SOLD", "SWAPPED"
    private List<String> photoUrls;
    private String targetSwapCategory;
    private String createdAt;
    private int viewCount;
    private boolean reported;

    public Listing() {
        this.photoUrls = new ArrayList<>();
        this.status = "AVAILABLE";
        this.item = new ClothingItem();
        this.targetSwapCategory = "Any";
        this.viewCount = 0;
        this.reported = false;
    }

    public Listing(String id, String sellerId, String sellerName, String sellerCollege,
                   ClothingItem item, double price, String status, List<String> photoUrls,
                   String targetSwapCategory, String createdAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.sellerCollege = sellerCollege;
        this.item = item != null ? item : new ClothingItem();
        this.price = Math.max(0.0, price);
        this.status = status != null ? status : "AVAILABLE";
        this.photoUrls = photoUrls != null ? new ArrayList<>(photoUrls) : new ArrayList<>();
        this.targetSwapCategory = targetSwapCategory != null ? targetSwapCategory : "Any";
        this.createdAt = createdAt;
        this.viewCount = 0;
        this.reported = false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerCollege() {
        return sellerCollege;
    }

    public void setSellerCollege(String sellerCollege) {
        this.sellerCollege = sellerCollege;
    }

    public ClothingItem getItem() {
        return item;
    }

    public void setItem(ClothingItem item) {
        this.item = item;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = Math.max(0.0, price);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls != null ? new ArrayList<>(photoUrls) : new ArrayList<>();
    }

    public void addPhotoUrl(String photoUrl) {
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            this.photoUrls.add(photoUrl.trim());
        }
    }

    public String getPrimaryPhoto() {
        if (photoUrls != null && !photoUrls.isEmpty()) {
            return photoUrls.get(0);
        }
        return "";
    }

    public String getTargetSwapCategory() {
        return targetSwapCategory;
    }

    public void setTargetSwapCategory(String targetSwapCategory) {
        this.targetSwapCategory = targetSwapCategory;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public void incrementViews() {
        this.viewCount++;
    }

    public boolean isReported() {
        return reported;
    }

    public void setReported(boolean reported) {
        this.reported = reported;
    }

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(this.status);
    }

    // Keyword matching implementation for Searchable interface
    @Override
    public boolean matchesKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String q = keyword.toLowerCase().trim();
        return (item != null && (
                item.getTitle().toLowerCase().contains(q) ||
                item.getDescription().toLowerCase().contains(q) ||
                item.getCategory().toLowerCase().contains(q) ||
                item.getBrand().toLowerCase().contains(q) ||
                item.getColor().toLowerCase().contains(q)
        )) || (sellerName != null && sellerName.toLowerCase().contains(q))
           || (sellerCollege != null && sellerCollege.toLowerCase().contains(q));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Listing listing)) return false;
        return Objects.equals(id, listing.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Listing[id=" + id + ", title=" + (item != null ? item.getTitle() : "N/A") + ", price=" + price + ", status=" + status + "]";
    }
}

