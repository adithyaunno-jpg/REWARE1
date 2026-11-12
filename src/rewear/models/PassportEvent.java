package rewear.models;

import java.io.Serializable;

/**
 * Single immutable event node along a garment's ReWear Passport timeline.
 */
public class PassportEvent implements Serializable {
    private String eventId;
    private String eventType; // "CREATION", "PURCHASE", "SWAP", "REPAIR", "CONDITION_UPDATE"
    private String timestamp;
    private String actorId;
    private String actorName;
    private String actorCollege;
    private String description;
    private String notes;

    public PassportEvent() {
    }

    public PassportEvent(String eventId, String eventType, String timestamp, String actorId,
                         String actorName, String actorCollege, String description, String notes) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorCollege = actorCollege;
        this.description = description;
        this.notes = notes != null ? notes : "";
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorCollege() {
        return actorCollege;
    }

    public void setActorCollege(String actorCollege) {
        this.actorCollege = actorCollege;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

