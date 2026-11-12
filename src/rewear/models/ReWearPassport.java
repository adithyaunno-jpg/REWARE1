package rewear.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing the lifelong circular provenance ledger for a garment.
 * Demonstrates Timeline Modeling, List collections, and Environmental Metrics.
 */
public class ReWearPassport implements Identifiable {
    private String id; // Matches passportId
    private String listingId;
    private String garmentTitle;
    private String originalOwnerId;
    private String originalOwnerName;
    private String originalCollege;
    private String creationDate;
    private int reuseCount;
    private double co2SavedKg;
    private double waterSavedLiters;
    private List<PassportEvent> events;

    public ReWearPassport() {
        this.events = new ArrayList<>();
        this.reuseCount = 1;
        this.co2SavedKg = 1.8;
        this.waterSavedLiters = 2700;
    }

    public ReWearPassport(String id, String listingId, String garmentTitle,
                          String originalOwnerId, String originalOwnerName, String originalCollege,
                          String creationDate) {
        this.id = id;
        this.listingId = listingId;
        this.garmentTitle = garmentTitle;
        this.originalOwnerId = originalOwnerId;
        this.originalOwnerName = originalOwnerName;
        this.originalCollege = originalCollege;
        this.creationDate = creationDate;
        this.reuseCount = 1;
        this.co2SavedKg = 1.8;
        this.waterSavedLiters = 2700;
        this.events = new ArrayList<>();

        // Add Genesis event
        PassportEvent genesis = new PassportEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                "CREATION",
                creationDate,
                originalOwnerId,
                originalOwnerName,
                originalCollege,
                "Garment registered and verified on ReWear campus catalog",
                "Original owner: " + originalOwnerName + " (" + originalCollege + ")"
        );
        this.events.add(genesis);
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

    public String getGarmentTitle() {
        return garmentTitle;
    }

    public void setGarmentTitle(String garmentTitle) {
        this.garmentTitle = garmentTitle;
    }

    public String getOriginalOwnerId() {
        return originalOwnerId;
    }

    public void setOriginalOwnerId(String originalOwnerId) {
        this.originalOwnerId = originalOwnerId;
    }

    public String getOriginalOwnerName() {
        return originalOwnerName;
    }

    public void setOriginalOwnerName(String originalOwnerName) {
        this.originalOwnerName = originalOwnerName;
    }

    public String getOriginalCollege() {
        return originalCollege;
    }

    public void setOriginalCollege(String originalCollege) {
        this.originalCollege = originalCollege;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public int getReuseCount() {
        return reuseCount;
    }

    public void setReuseCount(int reuseCount) {
        this.reuseCount = reuseCount;
    }

    public double getCo2SavedKg() {
        return co2SavedKg;
    }

    public void setCo2SavedKg(double co2SavedKg) {
        this.co2SavedKg = co2SavedKg;
    }

    public double getWaterSavedLiters() {
        return waterSavedLiters;
    }

    public void setWaterSavedLiters(double waterSavedLiters) {
        this.waterSavedLiters = waterSavedLiters;
    }

    public List<PassportEvent> getEvents() {
        return events;
    }

    public void setEvents(List<PassportEvent> events) {
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    public void addEvent(PassportEvent event) {
        if (event != null) {
            this.events.add(event);
            if ("PURCHASE".equalsIgnoreCase(event.getEventType()) || "SWAP".equalsIgnoreCase(event.getEventType())) {
                this.reuseCount++;
                this.co2SavedKg += 2.4;
                this.waterSavedLiters += 2500;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReWearPassport passport)) return false;
        return Objects.equals(id, passport.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

