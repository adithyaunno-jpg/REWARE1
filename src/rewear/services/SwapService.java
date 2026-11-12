package rewear.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import rewear.algorithms.CycleDetector;
import rewear.algorithms.SwapMatchEngine;
import rewear.models.*;
import rewear.storage.DataStore;

/**
 * Service managing 1-to-1 SwapMatch proposals and multi-party SwapChain cycle resolution.
 * Demonstrates:
 * - Algorithmic Matching
 * - Graph Cycle Execution
 * - Distributed Participant Consensus
 */
public class SwapService {
    private final DataStore dataStore;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public SwapService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Generates top SwapMatch recommendations for a student's active listing.
     */
    public List<SwapMatchEngine.MatchResult> getRecommendations(Listing userListing, int limit) {
        List<SwapMatchEngine.MatchResult> results = new ArrayList<>();
        if (userListing == null || !userListing.isAvailable()) {
            return results;
        }

        for (Listing candidate : dataStore.getAllListings()) {
            // Cannot swap with own listing or unavailable listings
            if (candidate.getId().equals(userListing.getId()) ||
                candidate.getSellerId().equals(userListing.getSellerId()) ||
                !candidate.isAvailable()) {
                continue;
            }

            SwapMatchEngine.MatchResult match = SwapMatchEngine.computeMatch(userListing, candidate);
            results.add(match);
        }

        // Sort descending by match percentage
        results.sort((a, b) -> Integer.compare(b.getScorePercentage(), a.getScorePercentage()));

        return results.subList(0, Math.min(limit, results.size()));
    }

    public boolean createSwapRequest(Student requester, String requesterListingId,
                                     String recipientListingId, String message) {
        Listing reqListing = dataStore.getListingById(requesterListingId);
        Listing recListing = dataStore.getListingById(recipientListingId);

        if (reqListing == null || recListing == null || !reqListing.isAvailable() || !recListing.isAvailable()) {
            return false;
        }
        if (reqListing.getSellerId().equals(recListing.getSellerId())) {
            return false;
        }

        SwapMatchEngine.MatchResult match = SwapMatchEngine.computeMatch(reqListing, recListing);
        String swapId = "SWP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = LocalDateTime.now().format(DATE_FORMAT);

        User recipient = dataStore.getUserById(recListing.getSellerId());
        String recipientCollege = (recipient instanceof Student s) ? s.getCollegeName() : "";

        SwapRequest req = new SwapRequest(
                swapId,
                requester.getId(),
                requester.getFullName(),
                requester.getCollegeName(),
                reqListing.getId(),
                reqListing.getItem().getTitle(),
                reqListing.getPrimaryPhoto(),
                recListing.getSellerId(),
                recListing.getSellerName(),
                recipientCollege,
                recListing.getId(),
                recListing.getItem().getTitle(),
                recListing.getPrimaryPhoto(),
                message,
                match.getScorePercentage(),
                match.getMatchReason(),
                now
        );

        dataStore.addSwapRequest(req);

        // Notify Recipient
        Notification notif = new Notification(
                "NOTIF-" + UUID.randomUUID().toString().substring(0, 8),
                recListing.getSellerId(),
                "New Swap Request Received",
                requester.getFullName() + " proposed a clothing swap with your '" + recListing.getItem().getTitle() + "'.",
                "/swapmatch",
                now,
                "SWAP_REQUEST"
        );
        dataStore.addNotification(notif);
        return true;
    }

    public synchronized boolean acceptSwapRequest(User currentUser, String swapRequestId) {
        SwapRequest req = dataStore.getSwapRequestById(swapRequestId);
        if (req == null || !"PENDING".equalsIgnoreCase(req.getStatus())) {
            return false;
        }
        if (!req.getRecipientId().equals(currentUser.getId())) {
            return false;
        }

        Listing reqListing = dataStore.getListingById(req.getRequesterListingId());
        Listing recListing = dataStore.getListingById(req.getRecipientListingId());

        if (reqListing == null || recListing == null) return false;

        req.setStatus("ACCEPTED");
        reqListing.setStatus("SWAPPED");
        recListing.setStatus("SWAPPED");

        dataStore.updateSwapRequest(req);
        dataStore.updateListing(reqListing);
        dataStore.updateListing(recListing);

        String now = LocalDateTime.now().format(DATE_FORMAT);

        // Update ReWear Passports for both garments
        updatePassportForSwap(reqListing, req.getRecipientId(), req.getRecipientName(), req.getRecipientCollege(), now);
        updatePassportForSwap(recListing, req.getRequesterId(), req.getRequesterName(), req.getRequesterCollege(), now);

        // Notify Requester
        Notification notif = new Notification(
                "NOTIF-" + UUID.randomUUID().toString().substring(0, 8),
                req.getRequesterId(),
                "Swap Request Accepted!",
                currentUser.getFullName() + " accepted your swap request! Plan your campus meetup to exchange garments.",
                "/swapmatch",
                now,
                "SWAP_UPDATE"
        );
        dataStore.addNotification(notif);
        return true;
    }

    public boolean rejectSwapRequest(User currentUser, String swapRequestId) {
        SwapRequest req = dataStore.getSwapRequestById(swapRequestId);
        if (req == null || !"PENDING".equalsIgnoreCase(req.getStatus())) {
            return false;
        }
        if (!req.getRecipientId().equals(currentUser.getId()) && !req.getRequesterId().equals(currentUser.getId())) {
            return false;
        }
        req.setStatus("REJECTED");
        dataStore.updateSwapRequest(req);
        return true;
    }

    private void updatePassportForSwap(Listing listing, String newOwnerId, String newOwnerName, String college, String timestamp) {
        if (listing.getItem() != null && listing.getItem().getPassportId() != null) {
            ReWearPassport passport = dataStore.getPassportById(listing.getItem().getPassportId());
            if (passport != null) {
                passport.addEvent(new PassportEvent(
                        "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                        "SWAP",
                        timestamp,
                        newOwnerId,
                        newOwnerName,
                        college,
                        "Ownership transferred via verified SwapMatch exchange to " + newOwnerName,
                        "Campus circular barter"
                ));
                dataStore.updatePassport(passport);
            }
        }
    }

    // --- SwapChain (Multi-Person Cycle) ---

    public synchronized boolean recordChainVote(Student student, String chainId, boolean accept) {
        SwapChain chain = dataStore.getSwapChainById(chainId);
        if (chain == null || !"PENDING".equalsIgnoreCase(chain.getStatus())) {
            return false;
        }

        boolean complete = chain.recordVote(student.getId(), accept);
        dataStore.updateSwapChain(chain);

        if (complete) {
            // Finalize circular swap!
            String now = LocalDateTime.now().format(DATE_FORMAT);
            List<String> partIds = chain.getParticipantIds();
            List<String> listIds = chain.getListingIds();

            for (int i = 0; i < partIds.size(); i++) {
                String giverId = partIds.get(i);
                int receiverIndex = (i + 1) % partIds.size();
                String receiverId = partIds.get(receiverIndex);
                String receiverName = chain.getParticipantNames().get(receiverIndex);

                String listingId = listIds.get(i);
                Listing listing = dataStore.getListingById(listingId);
                if (listing != null) {
                    listing.setStatus("SWAPPED");
                    dataStore.updateListing(listing);
                    updatePassportForSwap(listing, receiverId, receiverName, "Campus Partner", now);
                }

                Notification n = new Notification(
                        "NOTIF-" + UUID.randomUUID().toString().substring(0, 8),
                        giverId,
                        "SwapChain Barter Ring Completed!",
                        "All students have confirmed the exchange circle. Hand over your garment to " + receiverName + "!",
                        "/swapchain",
                        now,
                        "SWAP_UPDATE"
                );
                dataStore.addNotification(n);
            }
        }
        return true;
    }

    public List<SwapRequest> getSwapRequestsForUser(String userId) {
        List<SwapRequest> list = new ArrayList<>();
        for (SwapRequest sr : dataStore.getAllSwapRequests()) {
            if (sr.getRecipientId().equals(userId) || sr.getRequesterId().equals(userId)) {
                list.add(sr);
            }
        }
        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return list;
    }

    public List<SwapChain> getSwapChainsForUser(String userId) {
        List<SwapChain> list = new ArrayList<>();
        for (SwapChain c : dataStore.getAllSwapChains()) {
            if (c.isParticipant(userId)) {
                list.add(c);
            }
        }
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return list;
    }
}

