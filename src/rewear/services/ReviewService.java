package rewear.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import rewear.models.Review;
import rewear.models.Student;
import rewear.models.Transaction;
import rewear.models.User;
import rewear.storage.DataStore;

/**
 * Service managing peer reviews, rating calculation, and verified purchase verification.
 * Enforces:
 * - One review per completed transaction
 * - Only verified buyers can submit reviews
 * - Re-computes accurate mathematical seller ratings
 */
public class ReviewService {
    private final DataStore dataStore;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ReviewService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public static class ReviewResult {
        private final boolean success;
        private final String message;
        private final Review review;

        public ReviewResult(boolean success, String message, Review review) {
            this.success = success;
            this.message = message;
            this.review = review;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Review getReview() { return review; }
    }

    public synchronized ReviewResult submitReview(Student reviewer, String transactionId, int rating, String comment) {
        if (reviewer == null) {
            return new ReviewResult(false, "Authentication required to post a review.", null);
        }

        Transaction txn = dataStore.getTransactionById(transactionId);
        if (txn == null) {
            return new ReviewResult(false, "Associated transaction record not found.", null);
        }

        // Only the verified buyer can review
        if (!txn.getBuyerId().equals(reviewer.getId())) {
            return new ReviewResult(false, "Verification Failure: Only the verified buyer of this garment can submit a review.", null);
        }

        // Check if already reviewed (Prevent duplicate reviews)
        if (txn.isReviewed()) {
            return new ReviewResult(false, "A verified review has already been submitted for this transaction.", null);
        }

        // Boundary enforcement for 1 to 5 stars
        int validatedRating = Math.max(1, Math.min(5, rating));
        String reviewId = "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = LocalDateTime.now().format(DATE_FORMAT);

        Review review = new Review(
                reviewId,
                txn.getId(),
                txn.getListingId(),
                txn.getItemTitle(),
                txn.getSellerId(),
                txn.getSellerName(),
                reviewer.getId(),
                reviewer.getFullName(),
                reviewer.getCollegeName(),
                validatedRating,
                comment != null ? comment.trim() : "Smooth exchange and great communication.",
                now
        );

        dataStore.addReview(review);

        // Mark transaction as reviewed
        txn.setReviewed(true);
        dataStore.updateTransaction(txn);

        // Recalculate seller's rating average and count
        recalculateSellerRating(txn.getSellerId());

        return new ReviewResult(true, "Verified review submitted successfully!", review);
    }

    private void recalculateSellerRating(String sellerId) {
        List<Review> sellerReviews = getReviewsForSeller(sellerId);
        if (sellerReviews.isEmpty()) return;

        double sum = 0.0;
        for (Review r : sellerReviews) {
            sum += r.getRating();
        }
        double avg = sum / sellerReviews.size();

        User user = dataStore.getUserById(sellerId);
        if (user instanceof Student student) {
            student.setRatingAverage(Math.round(avg * 10.0) / 10.0);
            student.setRatingCount(sellerReviews.size());
            dataStore.updateUser(student);
        }
    }

    public List<Review> getReviewsForSeller(String sellerId) {
        List<Review> list = new ArrayList<>();
        for (Review r : dataStore.getAllReviews()) {
            if (r.getSellerId().equals(sellerId)) {
                list.add(r);
            }
        }
        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return list;
    }
}

