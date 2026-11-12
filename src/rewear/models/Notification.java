package rewear.models;

import java.util.Objects;

/**
 * Entity representing an asynchronous notification to a student or administrator.
 * Persisted in storage and delivered across server restarts.
 */
public class Notification implements Identifiable {
    private String id;
    private String userId;
    private String title;
    private String message;
    private String link;
    private boolean read;
    private String timestamp;
    private String type; // "PURCHASE", "SALE", "SWAP_REQUEST", "SWAP_UPDATE", "REVIEW", "SYSTEM"

    public Notification() {
        this.read = false;
        this.type = "SYSTEM";
    }

    public Notification(String id, String userId, String title, String message,
                        String link, String timestamp, String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.link = link != null ? link : "#";
        this.read = false;
        this.timestamp = timestamp;
        this.type = type != null ? type : "SYSTEM";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

