package rewear.models;

import java.util.Objects;

/**
 * Entity representing a 1-to-1 clothing swap proposal between two campus students.
 * Demonstrates Status Machine and Object Relationships.
 */
public class SwapRequest implements Identifiable {
    private String id;
    private String requesterId;
    private String requesterName;
    private String requesterCollege;
    private String requesterListingId;
    private String requesterItemTitle;
    private String requesterItemPhoto;

    private String recipientId;
    private String recipientName;
    private String recipientCollege;
    private String recipientListingId;
    private String recipientItemTitle;
    private String recipientItemPhoto;

    private String status; // PENDING, ACCEPTED, REJECTED, CANCELLED
    private String message;
    private int compatibilityScore;
    private String matchReason;
    private String timestamp;

    public SwapRequest() {
        this.status = "PENDING";
        this.compatibilityScore = 80;
    }

    public SwapRequest(String id, String requesterId, String requesterName, String requesterCollege,
                       String requesterListingId, String requesterItemTitle, String requesterItemPhoto,
                       String recipientId, String recipientName, String recipientCollege,
                       String recipientListingId, String recipientItemTitle, String recipientItemPhoto,
                       String message, int compatibilityScore, String matchReason, String timestamp) {
        this.id = id;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requesterCollege = requesterCollege;
        this.requesterListingId = requesterListingId;
        this.requesterItemTitle = requesterItemTitle;
        this.requesterItemPhoto = requesterItemPhoto;

        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.recipientCollege = recipientCollege;
        this.recipientListingId = recipientListingId;
        this.recipientItemTitle = recipientItemTitle;
        this.recipientItemPhoto = recipientItemPhoto;

        this.status = "PENDING";
        this.message = message != null ? message.trim() : "Interested in swapping with your garment!";
        this.compatibilityScore = compatibilityScore;
        this.matchReason = matchReason != null ? matchReason : "Compatible size and condition profile";
        this.timestamp = timestamp;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterCollege() {
        return requesterCollege;
    }

    public void setRequesterCollege(String requesterCollege) {
        this.requesterCollege = requesterCollege;
    }

    public String getRequesterListingId() {
        return requesterListingId;
    }

    public void setRequesterListingId(String requesterListingId) {
        this.requesterListingId = requesterListingId;
    }

    public String getRequesterItemTitle() {
        return requesterItemTitle;
    }

    public void setRequesterItemTitle(String requesterItemTitle) {
        this.requesterItemTitle = requesterItemTitle;
    }

    public String getRequesterItemPhoto() {
        return requesterItemPhoto;
    }

    public void setRequesterItemPhoto(String requesterItemPhoto) {
        this.requesterItemPhoto = requesterItemPhoto;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientCollege() {
        return recipientCollege;
    }

    public void setRecipientCollege(String recipientCollege) {
        this.recipientCollege = recipientCollege;
    }

    public String getRecipientListingId() {
        return recipientListingId;
    }

    public void setRecipientListingId(String recipientListingId) {
        this.recipientListingId = recipientListingId;
    }

    public String getRecipientItemTitle() {
        return recipientItemTitle;
    }

    public void setRecipientItemTitle(String recipientItemTitle) {
        this.recipientItemTitle = recipientItemTitle;
    }

    public String getRecipientItemPhoto() {
        return recipientItemPhoto;
    }

    public void setRecipientItemPhoto(String recipientItemPhoto) {
        this.recipientItemPhoto = recipientItemPhoto;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCompatibilityScore() {
        return compatibilityScore;
    }

    public void setCompatibilityScore(int compatibilityScore) {
        this.compatibilityScore = compatibilityScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SwapRequest that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

