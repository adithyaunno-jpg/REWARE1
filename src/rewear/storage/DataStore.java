package rewear.storage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import rewear.models.*;
import rewear.security.PasswordSecurity;

/**
 * Central DataStore managing in-memory entity caches and persistent disk storage.
 * Demonstrates:
 * - Thread-safe Collections (ConcurrentHashMap)
 * - Java File Persistence
 * - Robust Data Parsing
 * - Polymorphic Serialization
 */
public class DataStore {
    private final FilePersistenceService persistence;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // In-memory collections (DSA)
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, Listing> listings = new ConcurrentHashMap<>();
    // Cart items stored separately for each user
    private final Map<String, LinkedHashSet<String>> carts = new ConcurrentHashMap<>();
    // Saved checkout details for each user
    private final Map<String, Map<String, String>> deliveryDetails = new ConcurrentHashMap<>();
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
    private final Map<String, Review> reviews = new ConcurrentHashMap<>();
    private final Map<String, SwapRequest> swapRequests = new ConcurrentHashMap<>();
    private final Map<String, SwapChain> swapChains = new ConcurrentHashMap<>();
    private final Map<String, ReWearPassport> passports = new ConcurrentHashMap<>();
    private final Map<String, Notification> notifications = new ConcurrentHashMap<>();

    public DataStore(String dataDirectory) {
        this.persistence = new FilePersistenceService(dataDirectory);
        loadAll();

if (users.isEmpty()) {
    seedInitialData();
    saveAll();
}

addFiveDemoListingsIfMissing();

updateDemoListingPrices();

attachDemoListingPhotos();
    }

    // ==========================================
    // Persistence: Load & Save
    // ==========================================

    public synchronized void loadAll() {
        loadUsers();
        loadListings();
        loadTransactions();
        loadReviews();
        loadSwapRequests();
        loadSwapChains();
        loadPassports();
        loadNotifications();
        loadCarts();
        loadDeliveryDetails();
    }

    public synchronized void saveAll() {
        saveUsers();
        saveListings();
        saveTransactions();
        saveReviews();
        saveSwapRequests();
        saveSwapChains();
        savePassports();
        saveNotifications();
        saveCarts();
        saveDeliveryDetails();
    }

    // --- Users ---
    private void loadUsers() {
        String data = persistence.readFile("users.txt");
        List<Map<String, String>> records = RecordParser.parse(data);
        for (Map<String, String> r : records) {
            String role = r.getOrDefault("role", "STUDENT");
            User user;
            if ("ADMIN".equalsIgnoreCase(role)) {
                Admin admin = new Admin();
                admin.setDepartment(r.getOrDefault("department", "Campus Sustainability Board"));
                user = admin;
            } else {
                Student student = new Student();
                student.setCollegeName(r.getOrDefault("collegeName", ""));
                student.setBiography(r.getOrDefault("biography", ""));
                student.setProfilePhotoUrl(r.getOrDefault("profilePhotoUrl", ""));
                try {
                    student.setRatingAverage(Double.parseDouble(r.getOrDefault("ratingAverage", "5.0")));
                    student.setRatingCount(Integer.parseInt(r.getOrDefault("ratingCount", "0")));
                } catch (NumberFormatException ignored) {}
                student.setPreferredSwapCategory(r.getOrDefault("preferredSwapCategory", "All"));
                user = student;
            }
            user.setId(r.get("id"));
            user.setFullName(r.get("fullName"));
            user.setEmail(r.get("email"));
            user.setPasswordHash(r.get("passwordHash"));
            user.setSalt(r.get("salt"));
            user.setRole(role);
            user.setCreatedAt(r.getOrDefault("createdAt", LocalDateTime.now().format(DATE_FORMAT)));
            user.setActive(Boolean.parseBoolean(r.getOrDefault("active", "true")));

            if (user.getId() != null && user.getEmail() != null) {
                users.put(user.getId(), user);
                usersByEmail.put(user.getEmail().toLowerCase(), user);
            }
        }
    }

    public synchronized void saveUsers() {
        StringBuilder sb = new StringBuilder();
        for (User u : users.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("fullName", u.getFullName());
            m.put("email", u.getEmail());
            m.put("passwordHash", u.getPasswordHash());
            m.put("salt", u.getSalt());
            m.put("role", u.getRole());
            m.put("createdAt", u.getCreatedAt());
            m.put("active", String.valueOf(u.isActive()));

            if (u instanceof Student s) {
                m.put("collegeName", s.getCollegeName());
                m.put("biography", s.getBiography());
                m.put("profilePhotoUrl", s.getProfilePhotoUrl());
                m.put("ratingAverage", String.valueOf(s.getRatingAverage()));
                m.put("ratingCount", String.valueOf(s.getRatingCount()));
                m.put("preferredSwapCategory", s.getPreferredSwapCategory());
            } else if (u instanceof Admin a) {
                m.put("department", a.getDepartment());
            }
            sb.append(RecordParser.serialize(m));
        }
        persistence.writeFile("users.txt", sb.toString());
    }

    // --- Listings ---
    private void loadListings() {
        String data = persistence.readFile("listings.txt");
        List<Map<String, String>> records = RecordParser.parse(data);
        for (Map<String, String> r : records) {
            ClothingItem item = new ClothingItem(
                    r.get("item_title"),
                    r.get("item_description"),
                    r.get("item_category"),
                    r.get("item_size"),
                    r.get("item_condition"),
                    r.get("item_brand"),
                    r.get("item_color"),
                    r.get("item_passportId")
            );
            List<String> photos = RecordParser.decodeList(r.get("photoUrls"));
            double price = 0.0;
            try { price = Double.parseDouble(r.getOrDefault("price", "0.0")); } catch (Exception ignored) {}

            Listing listing = new Listing(
                    r.get("id"),
                    r.get("sellerId"),
                    r.get("sellerName"),
                    r.get("sellerCollege"),
                    item,
                    price,
                    r.get("status"),
                    photos,
                    r.get("targetSwapCategory"),
                    r.get("createdAt")
            );
            try { listing.setViewCount(Integer.parseInt(r.getOrDefault("viewCount", "0"))); } catch (Exception ignored) {}
            listing.setReported(Boolean.parseBoolean(r.getOrDefault("reported", "false")));

            if (listing.getId() != null) {
                listings.put(listing.getId(), listing);
            }
        }
    }

    public synchronized void saveListings() {
        StringBuilder sb = new StringBuilder();
        for (Listing l : listings.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("sellerId", l.getSellerId());
            m.put("sellerName", l.getSellerName());
            m.put("sellerCollege", l.getSellerCollege());
            m.put("price", String.valueOf(l.getPrice()));
            m.put("status", l.getStatus());
            m.put("targetSwapCategory", l.getTargetSwapCategory());
            m.put("createdAt", l.getCreatedAt());
            m.put("viewCount", String.valueOf(l.getViewCount()));
            m.put("reported", String.valueOf(l.isReported()));
            m.put("photoUrls", RecordParser.encodeList(l.getPhotoUrls()));

            ClothingItem item = l.getItem();
            if (item != null) {
                m.put("item_title", item.getTitle());
                m.put("item_description", item.getDescription());
                m.put("item_category", item.getCategory());
                m.put("item_size", item.getSize());
                m.put("item_condition", item.getCondition());
                m.put("item_brand", item.getBrand());
                m.put("item_color", item.getColor());
                m.put("item_passportId", item.getPassportId());
            }
            sb.append(RecordParser.serialize(m));
        }
        persistence.writeFile("listings.txt", sb.toString());
    }

    // --- Transactions ---
    private void loadTransactions() {
        String data = persistence.readFile("transactions.txt");
        List<Map<String, String>> records = RecordParser.parse(data);
        for (Map<String, String> r : records) {
            double orig = 0.0, finalPrice = 0.0;
            try {
                orig = Double.parseDouble(r.getOrDefault("originalPrice", "0.0"));
                finalPrice = Double.parseDouble(r.getOrDefault("finalSalePrice", "0.0"));
            } catch (Exception ignored) {}

            Transaction txn = new Transaction(
                    r.get("id"),
                    r.get("listingId"),
                    r.get("itemTitle"),
                    r.get("itemPhoto"),
                    r.get("buyerId"),
                    r.get("buyerName"),
                    r.get("buyerCollege"),
                    r.get("sellerId"),
                    r.get("sellerName"),
                    r.get("sellerCollege"),
                    orig,
                    finalPrice,
                    r.get("timestamp"),
                    r.get("campusMeetupLocation")
            );
            txn.setReviewed(Boolean.parseBoolean(r.getOrDefault("reviewed", "false")));
            txn.setDeliveryStatus(r.getOrDefault("deliveryStatus", "PENDING"));
            txn.setDeliveryDate(r.getOrDefault("deliveryDate", ""));
            if (txn.getId() != null) {
                transactions.put(txn.getId(), txn);
            }
        }
    }

    public synchronized void saveTransactions() {
        StringBuilder sb = new StringBuilder();
        for (Transaction t : transactions.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("listingId", t.getListingId());
            m.put("itemTitle", t.getItemTitle());
            m.put("itemPhoto", t.getItemPhoto());
            m.put("buyerId", t.getBuyerId());
            m.put("buyerName", t.getBuyerName());
            m.put("buyerCollege", t.getBuyerCollege());
            m.put("sellerId", t.getSellerId());
            m.put("sellerName", t.getSellerName());
            m.put("sellerCollege", t.getSellerCollege());
            m.put("originalPrice", String.valueOf(t.getOriginalPrice()));
            m.put("finalSalePrice", String.valueOf(t.getFinalSalePrice()));
            m.put("timestamp", t.getTimestamp());
            m.put("campusMeetupLocation", t.getCampusMeetupLocation());
            m.put("reviewed", String.valueOf(t.isReviewed()));
            m.put("deliveryStatus", t.getDeliveryStatus());
            m.put("deliveryDate", t.getDeliveryDate());
            sb.append(RecordParser.serialize(m));
        }
        persistence.writeFile("transactions.txt", sb.toString());
    }

    // --- Reviews ---
    private void loadReviews() {
        String data = persistence.readFile("reviews.txt");
        List<Map<String, String>> records = RecordParser.parse(data);
        for (Map<String, String> r : records) {
            int rating = 5;
            try { rating = Integer.parseInt(r.getOrDefault("rating", "5")); } catch (Exception ignored) {}
            Review rev = new Review(
                    r.get("id"),
                    r.get("transactionId"),
                    r.get("listingId"),
                    r.get("itemTitle"),
                    r.get("sellerId"),
                    r.get("sellerName"),
                    r.get("reviewerId"),
                    r.get("reviewerName"),
                    r.get("reviewerCollege"),
                    rating,
                    r.get("comment"),
                    r.get("timestamp")
            );
            rev.setVerifiedPurchase(Boolean.parseBoolean(r.getOrDefault("verifiedPurchase", "true")));
            if (rev.getId() != null) {
                reviews.put(rev.getId(), rev);
            }
        }
    }

    public synchronized void saveReviews() {
        StringBuilder sb = new StringBuilder();
        for (Review r : reviews.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("transactionId", r.getTransactionId());
            m.put("listingId", r.getListingId());
            m.put("itemTitle", r.getItemTitle());
            m.put("sellerId", r.getSellerId());
            m.put("sellerName", r.getSellerName());
            m.put("reviewerId", r.getReviewerId());
            m.put("reviewerName", r.getReviewerName());
            m.put("reviewerCollege", r.getReviewerCollege());
            m.put("rating", String.valueOf(r.getRating()));
            m.put("comment", r.getComment());
            m.put("timestamp", r.getTimestamp());
            m.put("verifiedPurchase", String.valueOf(r.isVerifiedPurchase()));
            sb.append(RecordParser.serialize(m));
        }
        persistence.writeFile("reviews.txt", sb.toString());
    }

    // --- Swap Requests ---
    private void loadSwapRequests() {
        String data = persistence.readFile("swap_requests.txt");
        List<Map<String, String>> records = RecordParser.parse(data);
        for (Map<String, String> r : records) {
            int score = 80;
            try { score = Integer.parseInt(r.getOrDefault("compatibilityScore", "80")); } catch (Exception ignored) {}
            SwapRequest req = new SwapRequest(
                    r.get("id"),
                    r.get("requesterId"),
                    r.get("requesterName"),
                    r.get("requesterCollege"),
                    r.get("requesterListingId"),
                    r.get("requesterItemTitle"),
                    r.get("requesterItemPhoto"),
                    r.get("recipientId"),
                    r.get("recipientName"),
                    r.get("recipientCollege"),
                    r.get("recipientListingId"),
                    r.get("recipientItemTitle"),
                    r.get("recipientItemPhoto"),
                    r.get("message"),
                    score,
                    r.get("matchReason"),
                    r.get("timestamp")
            );
            req.setStatus(r.getOrDefault("status", "PENDING"));
            if (req.getId() != null) {
                swapRequests.put(req.getId(), req);
            }
        }
    }

    public synchronized void saveSwapRequests() {
        StringBuilder sb = new StringBuilder();
        for (SwapRequest req : swapRequests.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("id", req.getId());
            m.put("requesterId", req.getRequesterId());
            m.put("requesterName", req.getRequesterName());
            m.put("requesterCollege", req.getRequesterCollege());
            m.put("requesterListingId", req.getRequesterListingId());
            m.put("requesterItemTitle", req.getRequesterItemTitle());
            m.put("requesterItemPhoto", req.getRequesterItemPhoto());
            m.put("recipientId", req.getRecipientId());
            m.put("recipientName", req.getRecipientName());
            m.put("recipientCollege", req.getRecipientCollege());
            m.put("recipientListingId", req.getRecipientListingId());
            m.put("recipientItemTitle", req.getRecipientItemTitle());
            m.put("recipientItemPhoto", req.getRecipientItemPhoto());
            m.put("status", req.getStatus());
            m.put("message", req.getMessage());
            m.put("compatibilityScore", String.valueOf(req.getCompatibilityScore()));
            m.put("matchReason", req.getMatchReason());
            m.put("timestamp", req.getTimestamp());
            sb.append(RecordParser.serialize(m));
        }
        persistence.writeFile("swap_requests.txt", sb.toString());
    }

    // --- Swap Chains ---
    private void loadSwapChains() {
        String data = persistence.readFile("swap_chains.txt");
        List<Map<String, String>> records = RecordParser.parse(data);
        for (Map<String, String> r : records) {
            List<String> partIds = RecordParser.decodeList(r.get("participantIds"));
            List<String> partNames = RecordParser.decodeList(r.get("participantNames"));
            List<String> listIds = RecordParser.decodeList(r.get("listingIds"));
            List<String> titles = RecordParser.decodeList(r.get("itemTitles"));
            List<String> photos = RecordParser.decodeList(r.get("itemPhotos"));

            SwapChain chain = new SwapChain(
                    r.get("id"),
                    partIds,
                    partNames,
                    listIds,
                    titles,
                    photos,
                    r.get("description"),
                    r.get("createdAt")
            );
            chain.setStatus(r.getOrDefault("status", "PENDING"));

            // Parse approvals
            String approvalsStr = r.get("approvals");
            if (approvalsStr != null && !approvalsStr.isEmpty()) {
                Map<String, String> appMap = new HashMap<>();
                for (String pair : approvalsStr.split(",")) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        appMap.put(kv[0].trim(), kv[1].trim());
                    }
                }
                chain.setApprovals(appMap);
            }
            if (chain.getId() != null) {
                swapChains.put(chain.getId(), chain);
            }
        }
    }

    public synchronized void saveSwapChains() {
        StringBuilder sb = new StringBuilder();
        for (SwapChain c : swapChains.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("participantIds", RecordParser.encodeList(c.getParticipantIds()));
            m.put("participantNames", RecordParser.encodeList(c.getParticipantNames()));
            m.put("listingIds", RecordParser.encodeList(c.getListingIds()));
            m.put("itemTitles", RecordParser.encodeList(c.getItemTitles()));
            m.put("itemPhotos", RecordParser.encodeList(c.getItemPhotos()));
            m.put("description", c.getDescription());
            m.put("status", c.getStatus());
            m.put("createdAt", c.getCreatedAt());

            StringBuilder appSb = new StringBuilder();
            for (Map.Entry<String, String> entry : c.getApprovals().entrySet()) {
                if (appSb.length() > 0) appSb.append(",");
                appSb.append(entry.getKey()).append(":").append(entry.getValue());
            }
            m.put("approvals", appSb.toString());
            sb.append(RecordParser.serialize(m));
        }
        persistence.writeFile("swap_chains.txt", sb.toString());
    }

    // --- Passports ---
    private void loadPassports() {
        String data = persistence.readFile("passports.txt");
        List<Map<String, String>> records = RecordParser.parse(data);
        for (Map<String, String> r : records) {
            ReWearPassport passport = new ReWearPassport();
            passport.setId(r.get("id"));
            passport.setListingId(r.get("listingId"));
            passport.setGarmentTitle(r.get("garmentTitle"));
            passport.setOriginalOwnerId(r.get("originalOwnerId"));
            passport.setOriginalOwnerName(r.get("originalOwnerName"));
            passport.setOriginalCollege(r.get("originalCollege"));
            passport.setCreationDate(r.get("creationDate"));
            try {
                passport.setReuseCount(Integer.parseInt(r.getOrDefault("reuseCount", "1")));
                passport.setCo2SavedKg(Double.parseDouble(r.getOrDefault("co2SavedKg", "1.8")));
                passport.setWaterSavedLiters(Double.parseDouble(r.getOrDefault("waterSavedLiters", "2700")));
            } catch (Exception ignored) {}

            // Parse Events
            String rawEvents = r.get("events_data");
            if (rawEvents != null && !rawEvents.isEmpty()) {
                List<PassportEvent> evts = new ArrayList<>();
                for (String evBlock : rawEvents.split("<<EVENT>>")) {
                    if (evBlock.trim().isEmpty()) continue;
                    String[] fields = evBlock.split("~~");
                    if (fields.length >= 7) {
                        PassportEvent pe = new PassportEvent(
                                fields[0], fields[1], fields[2], fields[3],
                                fields[4], fields[5], fields[6],
                                fields.length > 7 ? fields[7] : ""
                        );
                        evts.add(pe);
                    }
                }
                passport.setEvents(evts);
            }
            if (passport.getId() != null) {
                passports.put(passport.getId(), passport);
            }
        }
    }

    public synchronized void savePassports() {
        StringBuilder sb = new StringBuilder();
        for (ReWearPassport p : passports.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("listingId", p.getListingId());
            m.put("garmentTitle", p.getGarmentTitle());
            m.put("originalOwnerId", p.getOriginalOwnerId());
            m.put("originalOwnerName", p.getOriginalOwnerName());
            m.put("originalCollege", p.getOriginalCollege());
            m.put("creationDate", p.getCreationDate());
            m.put("reuseCount", String.valueOf(p.getReuseCount()));
            m.put("co2SavedKg", String.valueOf(p.getCo2SavedKg()));
            m.put("waterSavedLiters", String.valueOf(p.getWaterSavedLiters()));

            StringBuilder evSb = new StringBuilder();
            for (PassportEvent ev : p.getEvents()) {
                if (evSb.length() > 0) evSb.append("<<EVENT>>");
                evSb.append(ev.getEventId()).append("~~")
                    .append(ev.getEventType()).append("~~")
                    .append(ev.getTimestamp()).append("~~")
                    .append(ev.getActorId()).append("~~")
                    .append(ev.getActorName()).append("~~")
                    .append(ev.getActorCollege()).append("~~")
                    .append(ev.getDescription()).append("~~")
                    .append(ev.getNotes() != null ? ev.getNotes() : "");
            }
            m.put("events_data", evSb.toString());
            sb.append(RecordParser.serialize(m));
        }
        persistence.writeFile("passports.txt", sb.toString());
    }

    // --- Notifications ---
    private void loadNotifications() {
        String data = persistence.readFile("notifications.txt");
        List<Map<String, String>> records = RecordParser.parse(data);
        for (Map<String, String> r : records) {
            Notification n = new Notification(
                    r.get("id"),
                    r.get("userId"),
                    r.get("title"),
                    r.get("message"),
                    r.get("link"),
                    r.get("timestamp"),
                    r.get("type")
            );
            n.setRead(Boolean.parseBoolean(r.getOrDefault("read", "false")));
            if (n.getId() != null) {
                notifications.put(n.getId(), n);
            }
        }
    }

    public synchronized void saveNotifications() {
        StringBuilder sb = new StringBuilder();
        for (Notification n : notifications.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("userId", n.getUserId());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("link", n.getLink());
            m.put("read", String.valueOf(n.isRead()));
            m.put("timestamp", n.getTimestamp());
            m.put("type", n.getType());
            sb.append(RecordParser.serialize(m));
        }
        persistence.writeFile("notifications.txt", sb.toString());
    }

    // ==========================================
    // Seed Initial Realistic Academic Demo Data
    // ==========================================
    private void seedInitialData() {
        System.out.println("[DataStore] Seeding authentic campus demonstration data...");

        // Admin Account
        PasswordSecurity.HashResult adminHash = PasswordSecurity.hashPassword("AdminPassword123!");
        Admin admin = new Admin(
                "ADM-100",
                "Campus Sustainability Board",
                "admin@rewear.campus",
                adminHash.getHash(),
                adminHash.getSalt(),
                "Office of Campus Sustainability & Student Affairs",
                "2026-09-01 10:00"
        );
        users.put(admin.getId(), admin);
        usersByEmail.put(admin.getEmail().toLowerCase(), admin);

        // 5 Student Accounts (BTech CSE Team & Classmates)
        PasswordSecurity.HashResult p1 = PasswordSecurity.hashPassword("Pass1234!");
        Student s1 = new Student("STU-101", "Aarav Sharma", "aarav@campus.edu", p1.getHash(), p1.getSalt(),
                "Apex Institute of Technology",
                "Final-year BTech CSE student. Passionate about sustainable campus streetwear and cycling.",
                "", "2026-09-05 11:30");
        s1.setRatingAverage(4.9);
        s1.setRatingCount(7);
        s1.setPreferredSwapCategory("Jackets & Coats");

        PasswordSecurity.HashResult p2 = PasswordSecurity.hashPassword("Pass1234!");
        Student s2 = new Student("STU-102", "Priya Patel", "priya@campus.edu", p2.getHash(), p2.getSalt(),
                "Metro State University",
                "Design & Architecture sophomore. Rotating my campus wardrobe and loving vintage oversized layers.",
                "", "2026-09-08 14:15");
        s2.setRatingAverage(5.0);
        s2.setRatingCount(5);
        s2.setPreferredSwapCategory("Hoodies & Sweaters");

        PasswordSecurity.HashResult p3 = PasswordSecurity.hashPassword("Pass1234!");
        Student s3 = new Student("STU-103", "Rohan Gupta", "rohan@campus.edu", p3.getHash(), p3.getSalt(),
                "National Engineering College",
                "Mechanical engineering junior. Upcycling casual wear and trading varsity jackets.",
                "", "2026-09-12 09:45");
        s3.setRatingAverage(4.8);
        s3.setRatingCount(4);
        s3.setPreferredSwapCategory("Shirts & Tops");

        PasswordSecurity.HashResult p4 = PasswordSecurity.hashPassword("Pass1234!");
        Student s4 = new Student("STU-104", "Ananya Sen", "ananya@campus.edu", p4.getHash(), p4.getSalt(),
                "City Science & Fashion Institute",
                "Fashion styling researcher. All listings are freshly cared for and 100% authentic.",
                "", "2026-09-15 16:20");
        s4.setRatingAverage(5.0);
        s4.setRatingCount(8);
        s4.setPreferredSwapCategory("Dresses & Skirts");

        PasswordSecurity.HashResult p5 = PasswordSecurity.hashPassword("Pass1234!");
        Student s5 = new Student("STU-105", "Vikram Rao", "vikram@campus.edu", p5.getHash(), p5.getSalt(),
                "Global Technological University",
                "Tech student & campus photographer. Keeping campus style circular and waste-free.",
                "", "2026-09-20 13:00");
        s5.setRatingAverage(4.7);
        s5.setRatingCount(3);
        s5.setPreferredSwapCategory("Pants & Jeans");

        for (Student s : Arrays.asList(s1, s2, s3, s4, s5)) {
            users.put(s.getId(), s);
            usersByEmail.put(s.getEmail().toLowerCase(), s);
        }

        // 10 Curated Campus Listings across multiple categories, sizes, conditions & prices
        seedListings(s1, s2, s3, s4, s5);
    }

    private void seedListings(Student s1, Student s2, Student s3, Student s4, Student s5) {
        // Listing 1
        createAndSeedListing(
                "LST-201", s1,
                "Vintage Campus Denim Trucker Jacket",
                "Classic heavy-wash blue denim trucker jacket with brass buttons. Perfect outer layer for campus breezy evenings. Gently worn, no tears or loose seams.",
                "Jackets & Coats", "L", "Gently Used", "Levi's Vintage", "Blue",
                38.0, "Hoodies & Sweaters", "2026-10-01 10:15"
        );

        // Listing 2
        createAndSeedListing(
                "LST-202", s2,
                "Oversized Wool Blend Knit Cardigan",
                "Warm oatmeal ribbed knit sweater with tortoiseshell buttons. High quality natural wool blend, freshly dry cleaned. Minimalist Scandinavian campus vibe.",
                "Hoodies & Sweaters", "M", "Like New", "COS Eco", "Oatmeal Beige",
                42.0, "Jackets & Coats", "2026-10-05 14:30"
        );

        // Listing 3
        createAndSeedListing(
                "LST-203", s3,
                "Oxford Cotton Button-Down Shirt",
                "Crisp sky blue 100% organic cotton Oxford cloth button-down. Great for project presentations, conferences, and formal viva exams.",
                "Shirts & Tops", "M", "Like New", "Ralph Lauren", "Sky Blue",
                26.0, "Pants & Jeans", "2026-10-08 11:20"
        );

        // Listing 4
        createAndSeedListing(
                "LST-204", s4,
                "Linen Midi Wrap Dress with Belt",
                "Pure natural linen breathable wrap dress in olive sage green. Features side pockets and tie waist belt. Worn once for university induction ceremony.",
                "Dresses & Skirts", "S", "Brand New with Tags", "Reformation", "Sage Green",
                49.0, "Traditional Wear", "2026-10-10 16:45"
        );

        // Listing 5
        createAndSeedListing(
                "LST-205", s5,
                "Straight-Leg Relaxed Carpenter Denim",
                "Durable 14oz raw denim carpenter trousers with utility pockets and hammer loop. Clean hem, no stains or scuffs.",
                "Pants & Jeans", "32", "Gently Used", "Carhartt WIP", "Indigo Navy",
                35.0, "Shoes", "2026-10-14 18:00"
        );

        // Listing 6
        createAndSeedListing(
                "LST-206", s1,
                "Retro Colorblock Windbreaker Pullover",
                "Water-resistant nylon pullover windbreaker with zippered pouch pocket and hood. Lightweight and packs into its own pocket for campus commutes.",
                "Jackets & Coats", "XL", "Like New", "Patagonia Retro", "Teal / Ochre",
                45.0, "Hoodies & Sweaters", "2026-10-18 09:30"
        );

        // Listing 7
        createAndSeedListing(
                "LST-207", s2,
                "Minimalist Embroidered Campus Sweatshirt",
                "Heavyweight organic cotton fleece crewneck sweatshirt in warm cream. Brushed interior for maximum comfort during study sessions.",
                "Hoodies & Sweaters", "M", "Gently Used", "Champion Eco", "Off-White",
                28.0, "Shirts & Tops", "2026-10-22 13:10"
        );

        // Listing 8
        createAndSeedListing(
                "LST-208", s3,
                "Classic Khaki Chino Trousers",
                "Slim-straight cotton twill chinos in versatile British tan. Perfect everyday campus pants that match with sneakers and blazers alike.",
                "Pants & Jeans", "30", "Gently Used", "Dockers", "Tan Khaki",
                22.0, "Shirts & Tops", "2026-10-25 15:40"
        );

        // Listing 9 (Sold item for history demo)
        Listing soldListing = createAndSeedListing(
                "LST-209", s1,
                "Vintage Corduroy Overshirt Jacket",
                "Rich cognac brown wide-wale corduroy overshirt with twin chest pockets. Heavy cotton fabric, warm and stylish.",
                "Jackets & Coats", "L", "Like New", "Corduroy Co", "Cognac Brown",
                34.0, "Any", "2026-09-22 10:00"
        );
        soldListing.setStatus("SOLD");

        // Transaction for Sold Listing 9 (Buyer is Priya s2)
        Transaction txn1 = new Transaction(
                "TXN-901", soldListing.getId(), soldListing.getItem().getTitle(), "",
                s2.getId(), s2.getFullName(), s2.getCollegeName(),
                s1.getId(), s1.getFullName(), s1.getCollegeName(),
                340.0, 340.0, "2026-09-28 16:30", "Central Library Main Desk"
        );
        txn1.setReviewed(true);
        transactions.put(txn1.getId(), txn1);

        // Review from Priya for Aarav
        Review rev1 = new Review(
                "REV-801", txn1.getId(), soldListing.getId(), soldListing.getItem().getTitle(),
                s1.getId(), s1.getFullName(),
                s2.getId(), s2.getFullName(), s2.getCollegeName(),
                5, "Aarav handed the jacket over at the campus library right on time. The corduroy is in immaculate condition!",
                "2026-09-29 11:15"
        );
        reviews.put(rev1.getId(), rev1);

        // Transfer ReWear Passport for LST-209 to Priya
        ReWearPassport pass9 = passports.get(soldListing.getItem().getPassportId());
        if (pass9 != null) {
            pass9.addEvent(new PassportEvent(
                    "EVT-901-PURCHASE", "PURCHASE", "2026-09-28 16:30",
                    s2.getId(), s2.getFullName(), s2.getCollegeName(),
                    "Purchased by " + s2.getFullName() + " for $340.00 at Central Library",
                    "Transaction Reference: TXN-901. Condition verified as Like New."
            ));
        }

        // Listing 10 (Sold item for history demo)
        Listing soldListing2 = createAndSeedListing(
                "LST-210", s4,
                "Handloom Silk Cotton Kurta Shirt",
                "Traditional handloom breathable navy blue tunic shirt with fine thread embroidery around collar. Ideal for festival celebrations.",
                "Traditional Wear", "M", "Brand New with Tags", "FabIndia", "Deep Navy",
                30.0, "Any", "2026-09-25 11:00"
        );
        soldListing2.setStatus("SOLD");

        Transaction txn2 = new Transaction(
                "TXN-902", soldListing2.getId(), soldListing2.getItem().getTitle(), "",
                s3.getId(), s3.getFullName(), s3.getCollegeName(),
                s4.getId(), s4.getFullName(), s4.getCollegeName(),
                300.0, 300.0, "2026-10-02 14:00", "Campus Student Activity Center"
        );
        txn2.setReviewed(true);
        transactions.put(txn2.getId(), txn2);

        Review rev2 = new Review(
                "REV-802", txn2.getId(), soldListing2.getId(), soldListing2.getItem().getTitle(),
                s4.getId(), s4.getFullName(),
                s3.getId(), s3.getFullName(), s3.getCollegeName(),
                5, "Super smooth trade! Ananya provided the original tags and care bag. ReWear made sustainable fashion so easy.",
                "2026-10-03 09:30"
        );
        reviews.put(rev2.getId(), rev2);

        // Seed SwapMatch Request between s2 and s1
        SwapRequest sr1 = new SwapRequest(
                "SWP-701",
                s2.getId(), s2.getFullName(), s2.getCollegeName(),
                "LST-202", "Oversized Wool Blend Knit Cardigan", "",
                s1.getId(), s1.getFullName(), s1.getCollegeName(),
                "LST-201", "Vintage Campus Denim Trucker Jacket", "",
                "Hi Aarav! Your denim jacket matches my oversized cardigan nicely in value and seasonal style. Would love to swap!",
                92, "Both garments share premium outer-layer utility, equivalent price tiers (₹380-₹420), and like-new care standards.",
                "2026-10-26 12:00"
        );
        swapRequests.put(sr1.getId(), sr1);

        // Seed 3-Way SwapChain circle (Aarav s1 -> Priya s2 -> Rohan s3 -> Aarav s1)
        List<String> partIds = Arrays.asList(s1.getId(), s2.getId(), s3.getId());
        List<String> partNames = Arrays.asList(s1.getFullName(), s2.getFullName(), s3.getFullName());
        List<String> listIds = Arrays.asList("LST-206", "LST-202", "LST-203");
        List<String> titles = Arrays.asList(
                "Retro Colorblock Windbreaker Pullover",
                "Oversized Wool Blend Knit Cardigan",
                "Oxford Cotton Button-Down Shirt"
        );
        List<String> photos = Arrays.asList("", "", "");
        SwapChain chain1 = new SwapChain(
                "CHN-501",
                partIds, partNames, listIds, titles, photos,
                "Triangular Exchange Circle: Aarav gives Windbreaker to Priya -> Priya gives Cardigan to Rohan -> Rohan gives Oxford Shirt to Aarav",
                "2026-10-27 15:30"
        );
        // Aarav already accepted
        chain1.getApprovals().put(s1.getId(), "ACCEPTED");
        swapChains.put(chain1.getId(), chain1);

        // Seed Notifications
        Notification n1 = new Notification(
                "NOTIF-01", s1.getId(),
                "New SwapMatch Proposal Received",
                "Priya Patel proposed to swap her 'Oversized Wool Blend Knit Cardigan' with your 'Vintage Denim Trucker Jacket' (92% Match)",
                "/swapmatch", "2026-10-26 12:05", "SWAP_REQUEST"
        );
        Notification n2 = new Notification(
                "NOTIF-02", s1.getId(),
                "New Verified Review Received",
                "Priya Patel left a 5-star review for 'Vintage Corduroy Overshirt Jacket'!",
                "/profile", "2026-09-29 11:20", "REVIEW"
        );
        Notification n3 = new Notification(
                "NOTIF-03", s2.getId(),
                "Purchase Successful",
                "Your order TXN-901 has been confirmed. Meet Aarav Sharma at Central Library Desk.",
                "/profile?tab=sold", "2026-09-28 16:35", "PURCHASE"
        );
        notifications.put(n1.getId(), n1);
        notifications.put(n2.getId(), n2);
        notifications.put(n3.getId(), n3);
    }

    private Listing createAndSeedListing(String id, Student seller, String title, String desc,
                                         String category, String size, String condition, String brand,
                                         String color, double price, String targetSwapCat, String createdAt) {
        String passportId = "PASS-" + id.substring(id.indexOf('-') + 1) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        ClothingItem item = new ClothingItem(title, desc, category, size, condition, brand, color, passportId);
        Listing listing = new Listing(id, seller.getId(), seller.getFullName(), seller.getCollegeName(),
                item, price, "AVAILABLE", new ArrayList<>(), targetSwapCat, createdAt);
        listings.put(listing.getId(), listing);

        // Create initial digital passport
        ReWearPassport passport = new ReWearPassport(
                passportId, id, title, seller.getId(), seller.getFullName(), seller.getCollegeName(), createdAt
        );
        passports.put(passport.getId(), passport);
        return listing;
    }
    // ==========================================
    // Cart Persistence
    // ==========================================

    private void loadCarts() {
        String data = persistence.readFile("carts.txt");

        for (Map<String, String> record : RecordParser.parse(data)) {
        String userId = record.get("userId");
        List<String> listingIds =
                RecordParser.decodeList(record.get("listingIds"));

        if (userId != null) {
            carts.put(userId, new LinkedHashSet<>(listingIds));
        }
    }
}

public synchronized void saveCarts() {
    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, LinkedHashSet<String>> entry : carts.entrySet()) {
        Map<String, String> record = new HashMap<>();

        record.put("userId", entry.getKey());
        record.put("listingIds",
                RecordParser.encodeList(new ArrayList<>(entry.getValue())));

        sb.append(RecordParser.serialize(record));
    }

    persistence.writeFile("carts.txt", sb.toString());
}

public synchronized boolean addToCart(String userId, String listingId) {
    if (userId == null || listingId == null) {
        return false;
    }

    LinkedHashSet<String> userCart =
            carts.computeIfAbsent(userId, key -> new LinkedHashSet<>());

    boolean added = userCart.add(listingId);
    saveCarts();
    return added;
}

public synchronized void removeFromCart(String userId, String listingId) {
    if (userId == null || listingId == null) {
        return;
    }

    LinkedHashSet<String> userCart = carts.get(userId);

    if (userCart != null) {
        userCart.remove(listingId);
        saveCarts();
    }
}

public List<String> getCartListingIds(String userId) {
    LinkedHashSet<String> userCart = carts.get(userId);

    if (userCart == null) {
        return new ArrayList<>();
    }

    return new ArrayList<>(userCart);
}

public synchronized void clearCart(String userId) {
    if (userId != null) {
        carts.remove(userId);
        saveCarts();
    }
}



private synchronized void updateDemoListingPrices() {
    boolean changed = false;

    for (int i = 201; i <= 210; i++) {
        String listingId = "LST-" + i;
        Listing listing = listings.get(listingId);

        if (listing != null
                && listing.getPrice() > 0
                && listing.getPrice() < 100) {

            listing.setPrice(listing.getPrice() * 10);
            changed = true;
        }
    }

    if (changed) {
        saveListings();
        System.out.println("[DataStore] Demo listing prices updated successfully.");
    }
}



    /**
     * Attach the supplied local photographs to the 15 demo listings.
     * StaticFileHandler serves these files from the /uploads route.
     */
    private synchronized void attachDemoListingPhotos() {
        boolean changed = false;

        for (int i = 201; i <= 215; i++) {
            String listingId = "LST-" + i;
            Listing listing = listings.get(listingId);

            if (listing != null) {
                String photoUrl = "/uploads/listings/" + listingId + ".jpg";
                List<String> currentPhotos = listing.getPhotoUrls();

                if (currentPhotos == null
                        || currentPhotos.size() != 1
                        || !photoUrl.equals(currentPhotos.get(0))) {
                    listing.setPhotoUrls(Collections.singletonList(photoUrl));
                    changed = true;
                }
            }
        }

        if (changed) {
            saveListings();
            System.out.println("[DataStore] Demo listing photos connected successfully.");
        }
    }

private synchronized void addFiveDemoListingsIfMissing() {
    Student s1 = (Student) users.get("STU-101");
    Student s2 = (Student) users.get("STU-102");
    Student s3 = (Student) users.get("STU-103");
    Student s4 = (Student) users.get("STU-104");
    Student s5 = (Student) users.get("STU-105");

    if (s1 == null || s2 == null || s3 == null
            || s4 == null || s5 == null) {
        return;
    }

    boolean changed = false;

    if (!listings.containsKey("LST-211")) {
        createAndSeedListing("LST-211", s1,
                "Oversized Graphic T-Shirt",
                "Comfortable cotton graphic tee for everyday campus wear.",
                "Shirts & Tops", "L", "Like New",
                "Campus Originals", "White", 180.0,
                "Any", "2026-11-01 10:00");
        changed = true;
    }

    if (!listings.containsKey("LST-212")) {
        createAndSeedListing("LST-212", s2,
                "Black Wide-Leg Cargo Pants",
                "Relaxed-fit cargo pants with multiple utility pockets.",
                "Pants & Jeans", "M", "Gently Used",
                "Urban Style", "Black", 550.0,
                "Any", "2026-11-02 11:00");
        changed = true;
    }

    if (!listings.containsKey("LST-213")) {
        createAndSeedListing("LST-213", s3,
                "Floral Cotton Kurti",
                "Lightweight floral cotton kurti suitable for college and casual outings.",
                "Traditional Wear", "M", "Like New",
                "FabIndia", "Pink", 320.0,
                "Any", "2026-11-03 12:00");
        changed = true;
    }

    if (!listings.containsKey("LST-214")) {
        createAndSeedListing("LST-214", s4,
                "Classic Denim Jacket",
                "Blue denim jacket with front buttons and two chest pockets.",
                "Jackets & Coats", "L", "Gently Used",
                "Denim Co", "Blue", 650.0,
                "Any", "2026-11-04 13:00");
        changed = true;
    }

    if (!listings.containsKey("LST-215")) {
        createAndSeedListing("LST-215", s5,
                "Casual Campus Hoodie",
                "Soft cotton-blend hoodie perfect for everyday campus use.",
                "Hoodies & Sweaters", "XL", "Like New",
                "Campus Wear", "Grey", 420.0,
                "Any", "2026-11-05 14:00");
        changed = true;
    }

    if (changed) {
        saveListings();
        savePassports();
        System.out.println("[DataStore] Five additional demo listings added.");
    }
}


    // ==========================================
    // Public Accessors & Mutation Methods
    // ==========================================

    public User getUserById(String id) {
        return users.get(id);
    }

    public User getUserByEmail(String email) {
        if (email == null) return null;
        return usersByEmail.get(email.toLowerCase().trim());
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized void addUser(User user) {
        if (user != null && user.getId() != null) {
            users.put(user.getId(), user);
            usersByEmail.put(user.getEmail().toLowerCase().trim(), user);
            saveUsers();
        }
    }

    public synchronized void updateUser(User user) {
        if (user != null && user.getId() != null) {
            users.put(user.getId(), user);
            usersByEmail.put(user.getEmail().toLowerCase().trim(), user);
            saveUsers();
        }
    }

    public Listing getListingById(String id) {
        return listings.get(id);
    }

    public List<Listing> getAllListings() {
        return new ArrayList<>(listings.values());
    }

    public synchronized void addListing(Listing listing) {
        if (listing != null && listing.getId() != null) {
            listings.put(listing.getId(), listing);
            saveListings();
        }
    }

    public synchronized void updateListing(Listing listing) {
        if (listing != null && listing.getId() != null) {
            listings.put(listing.getId(), listing);
            saveListings();
        }
    }

    public synchronized void deleteListing(String id) {
        if (id != null) {
            listings.remove(id);
            saveListings();
        }
    }

    public Transaction getTransactionById(String id) {
        return transactions.get(id);
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions.values());
    }

    public synchronized void addTransaction(Transaction txn) {
        if (txn != null && txn.getId() != null) {
            transactions.put(txn.getId(), txn);
            saveTransactions();
        }
    }

    public synchronized void updateTransaction(Transaction txn) {
        if (txn != null && txn.getId() != null) {
            transactions.put(txn.getId(), txn);
            saveTransactions();
        }
    }

    public Review getReviewById(String id) {
        return reviews.get(id);
    }

    public List<Review> getAllReviews() {
        return new ArrayList<>(reviews.values());
    }

    public synchronized void addReview(Review review) {
        if (review != null && review.getId() != null) {
            reviews.put(review.getId(), review);
            saveReviews();
        }
    }

    public SwapRequest getSwapRequestById(String id) {
        return swapRequests.get(id);
    }

    public List<SwapRequest> getAllSwapRequests() {
        return new ArrayList<>(swapRequests.values());
    }

    public synchronized void addSwapRequest(SwapRequest req) {
        if (req != null && req.getId() != null) {
            swapRequests.put(req.getId(), req);
            saveSwapRequests();
        }
    }

    public synchronized void updateSwapRequest(SwapRequest req) {
        if (req != null && req.getId() != null) {
            swapRequests.put(req.getId(), req);
            saveSwapRequests();
        }
    }

    public SwapChain getSwapChainById(String id) {
        return swapChains.get(id);
    }

    public List<SwapChain> getAllSwapChains() {
        return new ArrayList<>(swapChains.values());
    }

    public synchronized void addSwapChain(SwapChain chain) {
        if (chain != null && chain.getId() != null) {
            swapChains.put(chain.getId(), chain);
            saveSwapChains();
        }
    }

    public synchronized void updateSwapChain(SwapChain chain) {
        if (chain != null && chain.getId() != null) {
            swapChains.put(chain.getId(), chain);
            saveSwapChains();
        }
    }

    public ReWearPassport getPassportById(String id) {
        return passports.get(id);
    }

    public List<ReWearPassport> getAllPassports() {
        return new ArrayList<>(passports.values());
    }

    public synchronized void addPassport(ReWearPassport passport) {
        if (passport != null && passport.getId() != null) {
            passports.put(passport.getId(), passport);
            savePassports();
        }
    }

    public synchronized void updatePassport(ReWearPassport passport) {
        if (passport != null && passport.getId() != null) {
            passports.put(passport.getId(), passport);
            savePassports();
        }
    }

    public List<Notification> getAllNotifications() {
        return new ArrayList<>(notifications.values());
    }

    public synchronized void addNotification(Notification notification) {
        if (notification != null && notification.getId() != null) {
            notifications.put(notification.getId(), notification);
            saveNotifications();
        }
    }

    public synchronized void updateNotification(Notification notification) {
        if (notification != null && notification.getId() != null) {
            notifications.put(notification.getId(), notification);
            saveNotifications();
        }
    }

    // ==========================================
// Saved Delivery Details
// ==========================================

private void loadDeliveryDetails() {
    String data = persistence.readFile("delivery_details.txt");

    for (Map<String, String> record : RecordParser.parse(data)) {
        String userId = record.get("userId");

        if (userId != null) {
            Map<String, String> details = new HashMap<>();

            details.put("pincode", record.getOrDefault("pincode", ""));
            details.put("college", record.getOrDefault("college", ""));
            details.put("address", record.getOrDefault("address", ""));
            details.put("mobile", record.getOrDefault("mobile", ""));

            deliveryDetails.put(userId, details);
        }
    }
}

public synchronized void saveDeliveryDetails() {
    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, Map<String, String>> entry
            : deliveryDetails.entrySet()) {

        Map<String, String> record = new HashMap<>();

        record.put("userId", entry.getKey());
        record.put("pincode",
                entry.getValue().getOrDefault("pincode", ""));
        record.put("college",
                entry.getValue().getOrDefault("college", ""));
        record.put("address",
                entry.getValue().getOrDefault("address", ""));
        record.put("mobile",
                entry.getValue().getOrDefault("mobile", ""));

        sb.append(RecordParser.serialize(record));
    }

    persistence.writeFile("delivery_details.txt", sb.toString());
}

public synchronized void saveDeliveryDetails(
        String userId,
        String pincode,
        String college,
        String address,
        String mobile) {

    if (userId == null || pincode == null || college == null
            || address == null || mobile == null) {
        return;
    }

    Map<String, String> details = new HashMap<>();

    details.put("pincode", pincode.trim());
    details.put("college", college.trim());
    details.put("address", address.trim());
    details.put("mobile", mobile.trim());

    deliveryDetails.put(userId, details);

    saveDeliveryDetails();
}

public Map<String, String> getDeliveryDetails(String userId) {
    Map<String, String> details = deliveryDetails.get(userId);

    if (details == null) {
        return new HashMap<>();
    }

    return new HashMap<>(details);
    }

}

