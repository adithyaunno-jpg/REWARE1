package rewear.services;

import rewear.models.Listing;
import rewear.storage.DataStore;

import java.util.ArrayList;
import java.util.List;

public class CartService {

    private final DataStore dataStore;

    public CartService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    // Add an item to the user's cart
    public boolean addToCart(String userId, String listingId) {

        Listing listing = dataStore.getListingById(listingId);

        if (listing == null || !listing.isAvailable()) {
            return false;
        }

        // Users cannot add their own products
        if (listing.getSellerId().equals(userId)) {
            return false;
        }

        return dataStore.addToCart(userId, listingId);
    }

    // Remove an item from the cart
    public void removeFromCart(String userId, String listingId) {
        dataStore.removeFromCart(userId, listingId);
    }

    // Get all available listings in the user's cart
    public List<Listing> getCartItems(String userId) {

        List<Listing> cartItems = new ArrayList<>();

        for (String listingId : dataStore.getCartListingIds(userId)) {

            Listing listing = dataStore.getListingById(listingId);

            if (listing != null) {
                cartItems.add(listing);
            }
        }

        return cartItems;
    }

    // Calculate the total cart price
    public double getCartTotal(String userId) {

        double total = 0;

        for (Listing listing : getCartItems(userId)) {
            if (listing.isAvailable()) {
                total += listing.getPrice();
            }
        }

        return total;
    }

    // Get the number of unique products in the cart
    public int getCartCount(String userId) {
        return dataStore.getCartListingIds(userId).size();
    }

    // Clear the user's cart
    public void clearCart(String userId) {
        dataStore.clearCart(userId);
    }
}