package rewear.models;

import java.util.Objects;

/**
 * Entity representing an authentic peer review from a verified campus buyer.
 * Demonstrates Data Validation, Business Constraints, and Encapsulation.
 */
public class Review implements Identifiable {
    private String id;
    private String transactionId;
    private String listingId;
    private String itemTitle;
    private String sellerId;
    private String sellerName;
    private String reviewerId;
    private String reviewerName;
    private String reviewerCollege;
    private int rating; // 1 to 5
    private String comment;
    private String timestamp;
    private boolean verifiedPurchase;

    public Review() {
        this.rating = 5;
        this.verifiedPurchase = true;
    }

    public Review(String id, String transactionId, String listingId, String itemTitle,
                  String sellerId, String sellerName, String reviewerId, String reviewerName,
                  String reviewerCollege, int rating, String comment, String timestamp) {
        this.id = id;
        this.transactionId = transactionId;
        this.listingId = listingId;
        this.itemTitle = itemTitle;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewerCollege = reviewerCollege;
        this.setRating(rating); // validated setter
        this.comment = comment != null ? comment.trim() : "";
        this.timestamp = timestamp;
        this.verifiedPurchase = true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerCollege() {
        return reviewerCollege;
    }

    public void setReviewerCollege(String reviewerCollege) {
        this.reviewerCollege = reviewerCollege;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        // Strict academic boundary enforcement: 1 <= rating <= 5
        this.rating = Math.max(1, Math.min(5, rating));
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment != null ? comment.trim() : "";
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isVerifiedPurchase() {
        return verifiedPurchase;
    }

    public void setVerifiedPurchase(boolean verifiedPurchase) {
        this.verifiedPurchase = verifiedPurchase;
    }

    public String getStarsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                sb.append("★");
            } else {
                sb.append("☆");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review review)) return false;
        return Objects.equals(id, review.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

