package rewear.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import rewear.algorithms.SearchAndSort;
import rewear.models.ClothingItem;
import rewear.models.Listing;
import rewear.models.ReWearPassport;
import rewear.models.Student;
import rewear.models.User;
import rewear.storage.DataStore;

/**
 * Service managing marketplace catalog, listing operations, search/filter algorithms,
 * and seller permissions.
 */
public class MarketplaceService {
    private final DataStore dataStore;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MarketplaceService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public static class ListingResult {
        private final boolean success;
        private final String message;
        private final Listing listing;

        public ListingResult(boolean success, String message, Listing listing) {
            this.success = success;
            this.message = message;
            this.listing = listing;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Listing getListing() { return listing; }
    }

    public ListingResult createListing(
            Student seller,
            String title,
            String description,
            String category,
            String size,
            String condition,
            String brand,
            String color,
            double price,
            List<String> photoUrls,
            String targetSwapCategory) {

        if (seller == null) {
            return new ListingResult(false, "Authentication required to create a listing.", null);
        }
        if (title == null || title.trim().length() < 3) {
            return new ListingResult(false, "Title must be at least 3 characters long.", null);
        }
        if (description == null || description.trim().length() < 5) {
            return new ListingResult(false, "Please provide a descriptive explanation of your garment.", null);
        }
        if (price < 0) {
            return new ListingResult(false, "Price cannot be negative.", null);
        }

        String listingId = "LST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String passportId = "PASS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = LocalDateTime.now().format(DATE_FORMAT);

        ClothingItem item = new ClothingItem(
                title.trim(),
                description.trim(),
                category != null ? category.trim() : "Jackets & Coats",
                size != null ? size.trim() : "M",
                condition != null ? condition.trim() : "Gently Used",
                brand != null && !brand.trim().isEmpty() ? brand.trim() : "Vintage/Campus",
                color != null && !color.trim().isEmpty() ? color.trim() : "Neutral",
                passportId
        );

        Listing listing = new Listing(
                listingId,
                seller.getId(),
                seller.getFullName(),
                seller.getCollegeName(),
                item,
                price,
                "AVAILABLE",
                photoUrls != null ? photoUrls : new ArrayList<>(),
                targetSwapCategory != null ? targetSwapCategory : "Any",
                now
        );

        dataStore.addListing(listing);

        // Create ReWear Passport Genesis
        ReWearPassport passport = new ReWearPassport(
                passportId,
                listingId,
                title.trim(),
                seller.getId(),
                seller.getFullName(),
                seller.getCollegeName(),
                now
        );
        dataStore.addPassport(passport);

        return new ListingResult(true, "Listing published successfully with ReWear Passport #" + passportId, listing);
    }

    public ListingResult updateListing(
            User currentUser,
            String listingId,
            String title,
            String description,
            String category,
            String size,
            String condition,
            String brand,
            String color,
            double price,
            List<String> photoUrls,
            String targetSwapCategory) {

        Listing listing = dataStore.getListingById(listingId);
        if (listing == null) {
            return new ListingResult(false, "Listing not found.", null);
        }

        // Ownership & Privilege check
        if (!listing.getSellerId().equals(currentUser.getId()) && !currentUser.canModerate()) {
            return new ListingResult(false, "Unauthorized: You may only modify your own listings.", null);
        }

        if (title != null && !title.trim().isEmpty()) listing.getItem().setTitle(title.trim());
        if (description != null && !description.trim().isEmpty()) listing.getItem().setDescription(description.trim());
        if (category != null) listing.getItem().setCategory(category.trim());
        if (size != null) listing.getItem().setSize(size.trim());
        if (condition != null) listing.getItem().setCondition(condition.trim());
        if (brand != null) listing.getItem().setBrand(brand.trim());
        if (color != null) listing.getItem().setColor(color.trim());
        if (price >= 0) listing.setPrice(price);
        if (photoUrls != null && !photoUrls.isEmpty()) listing.setPhotoUrls(photoUrls);
        if (targetSwapCategory != null) listing.setTargetSwapCategory(targetSwapCategory);

        dataStore.updateListing(listing);
        return new ListingResult(true, "Listing updated successfully.", listing);
    }

    public boolean deleteListing(User currentUser, String listingId) {
        Listing listing = dataStore.getListingById(listingId);
        if (listing == null) return false;
        if (!listing.getSellerId().equals(currentUser.getId()) && !currentUser.canModerate()) {
            return false;
        }
        dataStore.deleteListing(listingId);
        return true;
    }

    public boolean markAsSold(User currentUser, String listingId) {
        Listing listing = dataStore.getListingById(listingId);
        if (listing == null) return false;
        if (!listing.getSellerId().equals(currentUser.getId()) && !currentUser.canModerate()) {
            return false;
        }
        listing.setStatus("SOLD");
        dataStore.updateListing(listing);
        return true;
    }

    public List<Listing> searchAndFilter(String query, String category, String size,
                                         String condition, double minPrice, double maxPrice, String sortBy) {
        return SearchAndSort.filterAndSort(
                dataStore.getAllListings(),
                query, category, size, condition, minPrice, maxPrice, sortBy
        );
    }

    public List<Listing> getFeaturedListings(int limit) {
        List<Listing> available = SearchAndSort.filterAndSort(
                dataStore.getAllListings(), null, null, null, null, 0, 0, "popular"
        );
        return available.subList(0, Math.min(limit, available.size()));
    }

    public List<Listing> getRecentListings(int limit) {
        List<Listing> available = SearchAndSort.filterAndSort(
                dataStore.getAllListings(), null, null, null, null, 0, 0, "newest"
        );
        return available.subList(0, Math.min(limit, available.size()));
    }

    public List<Listing> getActiveListingsBySeller(String sellerId) {
        List<Listing> res = new ArrayList<>();
        for (Listing l : dataStore.getAllListings()) {
            if (l.getSellerId().equals(sellerId) && l.isAvailable()) {
                res.add(l);
            }
        }
        res.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return res;
    }

    public List<Listing> getAllListingsBySeller(String sellerId) {
        List<Listing> res = new ArrayList<>();
        for (Listing l : dataStore.getAllListings()) {
            if (l.getSellerId().equals(sellerId)) {
                res.add(l);
            }
        }
        res.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return res;
    }

    public Listing getListing(String id) {
        Listing l = dataStore.getListingById(id);
        if (l != null) {
            l.incrementViews();
            dataStore.updateListing(l);
        }
        return l;
    }

    public void reportListing(String listingId) {
        Listing l = dataStore.getListingById(listingId);
        if (l != null) {
            l.setReported(true);
            dataStore.updateListing(l);
        }
    }
}

