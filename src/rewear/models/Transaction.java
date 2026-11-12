package rewear.models;

import java.util.Objects;

/**
 * Entity representing an immutable purchase transaction between two campus students.
 * Demonstrates Encapsulation and Transactional Integrity.
 */
public class Transaction implements Identifiable {
    private String id;
    private String listingId;
    private String itemTitle;
    private String itemPhoto;
    private String buyerId;
    private String buyerName;
    private String buyerCollege;
    private String sellerId;
    private String sellerName;
    private String sellerCollege;
    private double originalPrice;
    private double finalSalePrice;
    private String timestamp;
    private String campusMeetupLocation;
    private boolean reviewed;
    private String deliveryStatus = "PENDING";
    private String deliveryDate = "";

    public Transaction() {
        this.reviewed = false;
        this.campusMeetupLocation = "Campus Student Union / Library Desk";
    }

    public Transaction(String id, String listingId, String itemTitle, String itemPhoto,
                       String buyerId, String buyerName, String buyerCollege,
                       String sellerId, String sellerName, String sellerCollege,
                       double originalPrice, double finalSalePrice, String timestamp,
                       String campusMeetupLocation) {
        this.id = id;
        this.listingId = listingId;
        this.itemTitle = itemTitle;
        this.itemPhoto = itemPhoto;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.buyerCollege = buyerCollege;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.sellerCollege = sellerCollege;
        this.originalPrice = originalPrice;
        this.finalSalePrice = finalSalePrice;
        this.timestamp = timestamp;
        this.campusMeetupLocation = campusMeetupLocation != null ? campusMeetupLocation : "Campus Student Union";
        this.reviewed = false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getItemPhoto() {
        return itemPhoto;
    }

    public void setItemPhoto(String itemPhoto) {
        this.itemPhoto = itemPhoto;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerCollege() {
        return buyerCollege;
    }

    public void setBuyerCollege(String buyerCollege) {
        this.buyerCollege = buyerCollege;
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

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getFinalSalePrice() {
        return finalSalePrice;
    }

    public void setFinalSalePrice(double finalSalePrice) {
        this.finalSalePrice = finalSalePrice;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCampusMeetupLocation() {
        return campusMeetupLocation;
    }

    public void setCampusMeetupLocation(String campusMeetupLocation) {
        this.campusMeetupLocation = campusMeetupLocation;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }


    public String getDeliveryStatus() {
        return deliveryStatus == null || deliveryStatus.isBlank() ? "PENDING" : deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getDeliveryDate() {
        return deliveryDate == null ? "" : deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction[" + id + ": " + itemTitle + " ($" + finalSalePrice + ") from " + sellerName + " to " + buyerName + "]";
    }
}

