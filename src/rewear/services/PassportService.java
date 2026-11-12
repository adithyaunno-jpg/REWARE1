package rewear.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import rewear.models.PassportEvent;
import rewear.models.ReWearPassport;
import rewear.models.User;
import rewear.storage.DataStore;

/**
 * Service managing digital garment provenance passports, circular repair logs,
 * and environmental impact calculations.
 */
public class PassportService {
    private final DataStore dataStore;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PassportService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public ReWearPassport getPassport(String id) {
        return dataStore.getPassportById(id);
    }

    public ReWearPassport getPassportByListingId(String listingId) {
        for (ReWearPassport p : dataStore.getAllPassports()) {
            if (p.getListingId().equals(listingId)) {
                return p;
            }
        }
        return null;
    }

    public boolean addMaintenanceNote(User actor, String passportId, String noteText, String conditionStatus) {
        ReWearPassport p = dataStore.getPassportById(passportId);
        if (p == null || actor == null) return false;

        String now = LocalDateTime.now().format(DATE_FORMAT);
        PassportEvent event = new PassportEvent(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                "REPAIR",
                now,
                actor.getId(),
                actor.getFullName(),
                "Campus Network",
                "Maintenance & Garment Care Verified: " + (conditionStatus != null ? conditionStatus : "Condition Preserved"),
                noteText != null ? noteText.trim() : "Cleaned and inspected"
        );

        p.addEvent(event);
        dataStore.updatePassport(p);
        return true;
    }

    public static class PlatformImpact {
        public final int totalGarments;
        public final int totalReuses;
        public final double totalCo2Saved;
        public final double totalWaterSaved;

        public PlatformImpact(int totalGarments, int totalReuses, double totalCo2Saved, double totalWaterSaved) {
            this.totalGarments = totalGarments;
            this.totalReuses = totalReuses;
            this.totalCo2Saved = totalCo2Saved;
            this.totalWaterSaved = totalWaterSaved;
        }
    }

    public PlatformImpact calculateTotalImpact() {
        int garments = dataStore.getAllListings().size();
        int reuses = 0;
        double co2 = 0.0;
        double water = 0.0;

        for (ReWearPassport p : dataStore.getAllPassports()) {
            reuses += p.getReuseCount();
            co2 += p.getCo2SavedKg();
            water += p.getWaterSavedLiters();
        }

        return new PlatformImpact(garments, reuses, Math.round(co2 * 10.0) / 10.0, Math.round(water));
    }
}

