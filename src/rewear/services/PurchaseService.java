package rewear.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import rewear.models.*;
import rewear.storage.DataStore;

/**
 * Service managing simulated purchases, transactional state transitions,
 * passport lifecycle updates, and peer review eligibility.
 */
public class PurchaseService {
    private final DataStore dataStore;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PurchaseService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public static class PurchaseResult {
        private final boolean success;
        private final String message;
        private final Transaction transaction;

        public PurchaseResult(boolean success, String message, Transaction transaction) {
            this.success = success;
            this.message = message;
            this.transaction = transaction;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Transaction getTransaction() { return transaction; }
    }

    public synchronized PurchaseResult executePurchase(Student buyer, String listingId, String campusMeetupLocation) {
        if (buyer == null) {
            return new PurchaseResult(false, "Authentication required to purchase.", null);
        }

        Listing listing = dataStore.getListingById(listingId);
        if (listing == null) {
            return new PurchaseResult(false, "Selected listing does not exist.", null);
        }

        if (!listing.isAvailable()) {
            return new PurchaseResult(false, "This garment is no longer available (already sold or swapped).", null);
        }

        if (listing.getSellerId().equals(buyer.getId())) {
            return new PurchaseResult(false, "Academic Policy: You cannot purchase your own clothing listing.", null);
        }

        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = LocalDateTime.now().format(DATE_FORMAT);
        String location = (campusMeetupLocation != null && !campusMeetupLocation.trim().isEmpty())
                ? campusMeetupLocation.trim()
                : "Campus Student Union / Main Desk";

        Transaction txn = new Transaction(
                txnId,
                listing.getId(),
                listing.getItem().getTitle(),
                listing.getPrimaryPhoto(),
                buyer.getId(),
                buyer.getFullName(),
                buyer.getCollegeName(),
                listing.getSellerId(),
                listing.getSellerName(),
                listing.getSellerCollege(),
                listing.getPrice(),
                listing.getPrice(),
                now,
                location
        );

        // Update listing status
        listing.setStatus("SOLD");
        dataStore.updateListing(listing);

        // Save transaction
        dataStore.addTransaction(txn);

        // Update ReWear Passport
        if (listing.getItem() != null && listing.getItem().getPassportId() != null) {
            ReWearPassport passport = dataStore.getPassportById(listing.getItem().getPassportId());
            if (passport != null) {
                PassportEvent purchaseEvent = new PassportEvent(
                        "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                        "PURCHASE",
                        now,
                        buyer.getId(),
                        buyer.getFullName(),
                        buyer.getCollegeName(),
                       "Garment acquired by " + buyer.getFullName() + " for ₹" + String.format("%.2f", listing.getPrice()),
                        "Meetup Location: " + location + ". Transaction Ref: " + txnId
                );
                passport.addEvent(purchaseEvent);
                dataStore.updatePassport(passport);
            }
        }

        // Notify Seller
        Notification sellerNotif = new Notification(
                "NOTIF-" + UUID.randomUUID().toString().substring(0, 8),
                listing.getSellerId(),
                "Your Item Has Been Purchased!",
                "Great news! " + buyer.getFullName() + " (" + buyer.getCollegeName() + ") purchased your '" +
                        listing.getItem().getTitle() + "'. Handover location: " + location + ".",
                "/profile?tab=sold",
                now,
                "SALE"
        );
        dataStore.addNotification(sellerNotif);

        // Notify Buyer
        Notification buyerNotif = new Notification(
                "NOTIF-" + UUID.randomUUID().toString().substring(0, 8),
                buyer.getId(),
                "Purchase Confirmed!",
                "You purchased '" + listing.getItem().getTitle() + "' from " + listing.getSellerName() +
                        ". Meetup location: " + location + ". Ref: " + txnId + ".",
                "/profile?tab=sold",
                now,
                "PURCHASE"
        );
        dataStore.addNotification(buyerNotif);

        return new PurchaseResult(true, "Purchase successful! Meetup confirmed.", txn);
    }


public synchronized PurchaseResult executeCartPurchase(
        Student buyer,
        List<String> listingIds,
        String campusMeetupLocation) {

    if (buyer == null) {
        return new PurchaseResult(
                false, "Please log in to place an order.", null);
    }

    if (listingIds == null || listingIds.isEmpty()) {
        return new PurchaseResult(
                false, "Your cart is empty.", null);
    }

    // Validate every item before purchasing any item.
    for (String listingId : listingIds) {
        Listing listing = dataStore.getListingById(listingId);

        if (listing == null || !listing.isAvailable()) {
            return new PurchaseResult(
                    false,
                    "An item in your cart is no longer available. Please review your cart.",
                    null);
        }

        if (buyer.getId().equals(listing.getSellerId())) {
            return new PurchaseResult(
                    false,
                    "You cannot purchase your own listing.",
                    null);
        }
    }

    // All items passed validation. Complete each purchase.
    Transaction lastTransaction = null;

    for (String listingId : listingIds) {
        PurchaseResult result = executePurchase(
                buyer, listingId, campusMeetupLocation);

        if (!result.isSuccess()) {
            return result;
        }

        lastTransaction = result.getTransaction();
    }

    return new PurchaseResult(
            true,
            "All cart items were purchased successfully!",
            lastTransaction);
}



public synchronized boolean cancelPurchase(String transactionId, String buyerId) {
    Transaction transaction = dataStore.getTransactionById(transactionId);

    if (transaction == null) {
        return false;
    }

    // Only the buyer who placed the order can cancel it.
    if (!buyerId.equals(transaction.getBuyerId())) {
        return false;
    }

    // Only pending orders can be cancelled.
    if (!"PENDING".equalsIgnoreCase(transaction.getDeliveryStatus())) {
        return false;
    }

    Listing listing = dataStore.getListingById(transaction.getListingId());

    if (listing == null || !"SOLD".equalsIgnoreCase(listing.getStatus())) {
        return false;
    }

    transaction.setDeliveryStatus("CANCELLED");
    dataStore.updateTransaction(transaction);

    // Make the item available again.
    listing.setStatus("AVAILABLE");
    dataStore.updateListing(listing);

    return true;
}


    public List<Transaction> getTransactionsForSeller(String sellerId) {
        List<Transaction> list = new ArrayList<>();
        for (Transaction t : dataStore.getAllTransactions()) {
            if (t.getSellerId().equals(sellerId)) {
                list.add(t);
            }
        }
        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return list;
    }

    public List<Transaction> getTransactionsForBuyer(String buyerId) {
        List<Transaction> list = new ArrayList<>();
        for (Transaction t : dataStore.getAllTransactions()) {
            if (t.getBuyerId().equals(buyerId)) {
                list.add(t);
            }
        }
        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return list;
    }
}

