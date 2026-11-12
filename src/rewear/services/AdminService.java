package rewear.services;

import java.util.ArrayList;
import java.util.List;
import rewear.models.Listing;
import rewear.models.User;
import rewear.storage.DataStore;

/**
 * Service managing administrative moderation, system statistics, and student user compliance.
 */
public class AdminService {
    private final DataStore dataStore;

    public AdminService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public static class AdminStats {
        public final int totalUsers;
        public final int totalListings;
        public final int activeListings;
        public final int soldListings;
        public final int totalTransactions;
        public final double totalVolume;
        public final int reportedListings;

        public AdminStats(int totalUsers, int totalListings, int activeListings, int soldListings,
                          int totalTransactions, double totalVolume, int reportedListings) {
            this.totalUsers = totalUsers;
            this.totalListings = totalListings;
            this.activeListings = activeListings;
            this.soldListings = soldListings;
            this.totalTransactions = totalTransactions;
            this.totalVolume = totalVolume;
            this.reportedListings = reportedListings;
        }
    }

    public AdminStats getStatistics() {
        int totalUsers = dataStore.getAllUsers().size();
        List<Listing> all = dataStore.getAllListings();
        int totalListings = all.size();
        int active = 0, sold = 0, reported = 0;
        for (Listing l : all) {
            if ("AVAILABLE".equalsIgnoreCase(l.getStatus())) active++;
            if ("SOLD".equalsIgnoreCase(l.getStatus()) || "SWAPPED".equalsIgnoreCase(l.getStatus())) sold++;
            if (l.isReported()) reported++;
        }

        int txns = dataStore.getAllTransactions().size();
        double volume = 0.0;
        for (var t : dataStore.getAllTransactions()) {
            volume += t.getFinalSalePrice();
        }

        return new AdminStats(totalUsers, totalListings, active, sold, txns, Math.round(volume * 100.0) / 100.0, reported);
    }

    public boolean toggleUserStatus(User adminUser, String targetUserId) {
        if (adminUser == null || !adminUser.canModerate()) return false;
        User target = dataStore.getUserById(targetUserId);
        if (target == null || target.canModerate()) return false; // Cannot suspend admin

        target.setActive(!target.isActive());
        dataStore.updateUser(target);
        return true;
    }

    public boolean removeListing(User adminUser, String listingId) {
        if (adminUser == null || !adminUser.canModerate()) return false;
        dataStore.deleteListing(listingId);
        return true;
    }

    public boolean dismissReport(User adminUser, String listingId) {
        if (adminUser == null || !adminUser.canModerate()) return false;
        Listing l = dataStore.getListingById(listingId);
        if (l != null) {
            l.setReported(false);
            dataStore.updateListing(l);
            return true;
        }
        return false;
    }

    public List<Listing> getReportedListings() {
        List<Listing> list = new ArrayList<>();
        for (Listing l : dataStore.getAllListings()) {
            if (l.isReported()) {
                list.add(l);
            }
        }
        return list;
    }
}

