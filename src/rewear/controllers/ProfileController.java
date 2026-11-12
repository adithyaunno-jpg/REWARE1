package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import rewear.models.*;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.*;
import rewear.storage.DataStore;
import rewear.views.HtmlRenderer;

/**
 * Controller managing Student Profile with three integrated sub-sections:
 * 1. Currently on Sale
 * 2. Sold Items & Sales History
 * 3. Verified Buyer Reviews and Calculated Ratings
 * Pure HTML query parameter tab switching (?tab=active | sold | reviews).
 */
public class ProfileController implements HttpHandler {
    private final AuthService authService;
    private final MarketplaceService marketplaceService;
    private final PurchaseService purchaseService;
    private final ReviewService reviewService;
    private final NotificationService notificationService;
    private final DataStore dataStore;

    public ProfileController(AuthService authService, MarketplaceService marketplaceService,
                             PurchaseService purchaseService, ReviewService reviewService,
                             NotificationService notificationService, DataStore dataStore) {
        this.authService = authService;
        this.marketplaceService = marketplaceService;
        this.purchaseService = purchaseService;
        this.reviewService = reviewService;
        this.notificationService = notificationService;
        this.dataStore = dataStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/profile".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderProfilePage(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleEditProfile(exchange);
            }
        } else if ("/edit-profile".equalsIgnoreCase(path)) {
            handleEditProfile(exchange);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void renderProfilePage(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User sessionUser = authService.getAuthenticatedUser(token);

        Map<String, String> query = UrlHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String targetUserId = query.get("id");

        User targetUser;
        if (targetUserId != null && !targetUserId.trim().isEmpty()) {
            targetUser = dataStore.getUserById(targetUserId);
        } else {
            targetUser = sessionUser;
        }

        if (targetUser == null) {
            redirect(exchange, "/login");
            return;
        }

        boolean isOwnProfile = sessionUser != null && sessionUser.getId().equals(targetUser.getId());
        String activeTab = query.getOrDefault("tab", "active").toLowerCase();
        int unreadNotifs = sessionUser != null ? notificationService.getUnreadCount(sessionUser.getId()) : 0;

        Student student = (targetUser instanceof Student s) ? s : null;
        String college = student != null ? student.getCollegeName() : "Campus Administration";
        String bio = student != null ? student.getBiography() : "Administrative Staff";
        double ratingAvg = student != null ? student.getRatingAverage() : 5.0;

        // Fetch user data
        List<Listing> allUserListings = marketplaceService.getAllListingsBySeller(targetUser.getId());
        List<Listing> activeListings = new ArrayList<>();
        for (Listing l : allUserListings) {
            if (l.isAvailable()) activeListings.add(l);
        }

        List<Transaction> soldTransactions = purchaseService.getTransactionsForSeller(targetUser.getId());
        List<Transaction> buyerOrders = purchaseService.getTransactionsForBuyer(targetUser.getId());
        List<Review> userReviews = reviewService.getReviewsForSeller(targetUser.getId());

        StringBuilder content = new StringBuilder();

        // 1. Profile Header Card
        content.append("<div class=\"profile-header-card\">\n")
               .append("  <div class=\"profile-avatar\">\n");

        if (student != null && !student.getProfilePhotoUrl().isEmpty()) {
            content.append("    <img src=\"").append(InputSanitizer.escapeHtml(student.getProfilePhotoUrl())).append("\" alt=\"Avatar\">\n");
        } else {
            String initial = !targetUser.getFullName().isEmpty() ? targetUser.getFullName().substring(0, 1).toUpperCase() : "U";
            content.append("    <span>").append(initial).append("</span>\n");
        }

        content.append("  </div>\n")
               .append("  <div class=\"profile-info\">\n")
               .append("    <h1 class=\"profile-name\">").append(InputSanitizer.escapeHtml(targetUser.getFullName())).append("</h1>\n")
               .append("    <div class=\"profile-college\">&#127891; ").append(InputSanitizer.escapeHtml(college)).append("</div>\n")
               .append("    <p class=\"profile-bio\">").append(InputSanitizer.escapeHtml(bio)).append("</p>\n");

        if (isOwnProfile) {
            content.append("    <div style=\"margin-top:0.85rem;\">\n")
                   .append("      <button type=\"button\" onclick=\"document.getElementById(\'edit-profile-section\').style.display=\'block\';document.getElementById(\'edit-profile-section\').scrollIntoView({behavior:\'smooth\'});\" class=\"btn btn-secondary btn-sm\">&#9998; Edit Wardrobe Profile</button>\n")
                   .append("    </div>\n");
        }

        content.append("  </div>\n");

        // Profile Statistics Box
        content.append("  <div class=\"profile-stats\">\n")
               .append("    <div class=\"stat-box\">\n")
               .append("      <div class=\"stat-value\">").append(activeListings.size()).append("</div>\n")
               .append("      <div class=\"stat-label\">On Sale</div>\n")
               .append("    </div>\n")
               .append("    <div class=\"stat-box\">\n")
               .append("      <div class=\"stat-value\">").append(soldTransactions.size()).append("</div>\n")
               .append("      <div class=\"stat-label\">Sold Items</div>\n")
               .append("    </div>\n")
               .append("    <div class=\"stat-box\">\n")
               .append("      <div class=\"stat-value\">").append(userReviews.size()).append("</div>\n")
               .append("      <div class=\"stat-label\">Reviews</div>\n")
               .append("    </div>\n")
               .append("    <div class=\"stat-box\">\n")
               .append("      <div class=\"stat-value\">").append(String.format("%.1f", ratingAvg)).append("★</div>\n")
               .append("      <div class=\"stat-label\">Seller Rating</div>\n")
               .append("    </div>\n")
               .append("  </div>\n")
               .append("</div>\n");

        // 2. Three Tab Navigation Links (Pure HTML)
        String baseProfileUrl = "/profile" + (targetUserId != null ? "?id=" + targetUserId + "&tab=" : "?tab=");
        content.append("<nav class=\"profile-tabs-nav\">\n")
               .append("  <a href=\"").append(baseProfileUrl).append("active\" class=\"profile-tab-btn ").append("active".equals(activeTab) ? "active" : "").append("\">\n")
               .append("    Currently on Sale (").append(activeListings.size()).append(")\n")
               .append("  </a>\n")
               .append("  <a href=\"").append(baseProfileUrl).append("sold\" class=\"profile-tab-btn ").append("sold".equals(activeTab) ? "active" : "").append("\">\n")
               .append("    Sold Items (").append(soldTransactions.size()).append(")\n")
               .append("  </a>\n")
               .append("  <a href=\"").append(baseProfileUrl).append("reviews\" class=\"profile-tab-btn ").append("reviews".equals(activeTab) ? "active" : "").append("\">\n")
               .append("    Reviews &amp; Ratings (").append(userReviews.size()).append(")\n")
               .append("  </a>\n");
       
if (isOwnProfile && student != null) {
    content.append("  <a href=\"")
            .append(baseProfileUrl)
            .append("orders\" class=\"profile-tab-btn ")
            .append("orders".equals(activeTab) ? "active" : "")
            .append("\">\n")
            .append("    Orders (")
            .append(buyerOrders.stream()
                    .filter(t -> "PENDING".equalsIgnoreCase(t.getDeliveryStatus()))
                    .count())
            .append(")\n")
            .append("  </a>\n");
}

        content.append("</nav>\n");

        // 3. Tab Content Sections
        if ("active".equals(activeTab)) {
            // TAB 1: CURRENTLY ON SALE
            content.append("<section>\n");
            if (activeListings.isEmpty()) {
                content.append("  <div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:3rem; text-align:center;\">\n")
                       .append("    <h3>No garments currently on sale</h3>\n")
                       .append("    <p style=\"color:var(--color-text-secondary); margin:0.5rem 0 1.5rem;\">Ready to refresh your campus wardrobe?</p>\n");
                if (isOwnProfile) {
                    content.append("    <a href=\"/sell\" class=\"btn btn-primary\">+ List an Item Now</a>\n");
                }
                content.append("  </div>\n");
            } else {
                content.append("  <div class=\"products-grid\">\n");
                for (Listing l : activeListings) {
                    content.append(HtmlRenderer.renderProductCard(l));
                }
                content.append("  </div>\n");
            }
            content.append("</section>\n");

        } else if ("sold".equals(activeTab)) {
            // TAB 2: SOLD ITEMS & SALES HISTORY
            content.append("<section>\n");
            if (soldTransactions.isEmpty()) {
                content.append("  <div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:3rem; text-align:center;\">\n")
                       .append("    <h3>No sales history recorded yet</h3>\n")
                       .append("    <p style=\"color:var(--color-text-secondary);\">Completed sales and circular transactions will appear here.</p>\n")
                       .append("  </div>\n");
            } else {
                content.append("  <div class=\"data-table-container\">\n")
                       .append("    <table class=\"data-table\">\n")
                       .append("      <thead>\n")
                       .append("        <tr>\n")
                       .append("          <th>Garment</th>\n")
                       .append("          <th>Listed Price</th>\n")
                       .append("          <th>Final Sale Price</th>\n")
                       .append("          <th>Sale Date</th>\n")
                       .append("          <th>Txn Reference</th>\n")
                       .append("          <th>Verified Buyer</th>\n")
                       .append("        </tr>\n")
                       .append("      </thead>\n")
                       .append("      <tbody>\n");

                for (Transaction t : soldTransactions) {
                    content.append("        <tr>\n")
                           .append("          <td><strong><a href=\"/listing?id=").append(t.getListingId()).append("\">").append(InputSanitizer.escapeHtml(t.getItemTitle())).append("</a></strong></td>\n")
                           .append("          <td>₹").append(String.format("%.2f", t.getOriginalPrice())).append("</td>\n")
                           .append("          <td style=\"font-weight:700; color:var(--color-deep-navy);\">₹").append(String.format("%.2f", t.getFinalSalePrice())).append("</td>\n")
                           .append("          <td>").append(InputSanitizer.escapeHtml(t.getTimestamp())).append("</td>\n")
                           .append("          <td><code>").append(InputSanitizer.escapeHtml(t.getId())).append("</code></td>\n")                           .append("          <td>").append(InputSanitizer.escapeHtml(t.getBuyerName())).append(" (").append(InputSanitizer.escapeHtml(t.getBuyerCollege())).append(")</td>\\n")
                           .append("          <td>").append(InputSanitizer.escapeHtml(t.getDeliveryStatus())).append("</td>\\n");
                    if ("PENDING".equalsIgnoreCase(t.getDeliveryStatus())) {
                        content.append("<td><form action=\"/profile\" method=\"POST\"><input type=\"hidden\" name=\"action\" value=\"mark-delivered\"><input type=\"hidden\" name=\"transactionId\" value=\"")
                                .append(InputSanitizer.escapeHtml(t.getId()))
                                .append("\"><button class=\"btn btn-primary btn-sm\" type=\"submit\">Mark as Delivered</button></form></td>");
                    } else {
                        content.append("<td>").append(InputSanitizer.escapeHtml(t.getDeliveryDate())).append("</td>");
                    }
                    content.append("        </tr>\\n");
                }

                content.append("      </tbody>\n")
                       .append("    </table>\n")
                       .append("  </div>\n");
            }
            content.append("</section>\n");

        } else if ("orders".equals(activeTab) && isOwnProfile && student != null) {
            content.append("<section><h2>My Orders</h2>");
            if (buyerOrders.isEmpty()) {
                content.append("<div class=\"form-card\"><h3>No orders yet</h3><p>Your purchases will appear here.</p></div>");
            } else {
                content.append("<div class=\"data-table-container\"><table class=\"data-table\"><thead><tr><th>Item</th><th>Seller</th><th>Amount</th><th>Order Date</th><th>Status</th><th>Delivery Date</th><th>Action</th></tr></thead><tbody>");
                
for (Transaction t : buyerOrders) {
    String status = t.getDeliveryStatus();

    if (!"PENDING".equalsIgnoreCase(status)) {
        continue;
    }

    String delivery;
    if ("DELIVERED".equalsIgnoreCase(status)) {
        delivery = t.getDeliveryDate();
    } else if ("CANCELLED".equalsIgnoreCase(status)) {
        delivery = "Cancelled";
    } else {
        delivery = "Not delivered yet";
    }

    content.append("<tr><td>")
            .append(InputSanitizer.escapeHtml(t.getItemTitle()))
            .append("</td><td>")
            .append(InputSanitizer.escapeHtml(t.getSellerName()))
            .append("</td><td>₹")
            .append(String.format("%.2f", t.getFinalSalePrice()))
            .append("</td><td>")
            .append(InputSanitizer.escapeHtml(t.getTimestamp()))
            .append("</td><td>")
            .append(InputSanitizer.escapeHtml(status))
            .append("</td><td>")
            .append(InputSanitizer.escapeHtml(delivery))
            .append("</td><td>");

    if ("PENDING".equalsIgnoreCase(status)) {
        content.append("<form action=\"/profile\" method=\"POST\">")
                .append("<input type=\"hidden\" name=\"action\" value=\"cancel-order\">")
                .append("<input type=\"hidden\" name=\"transactionId\" value=\"")
                .append(InputSanitizer.escapeHtml(t.getId()))
                .append("\">")
                .append("<button type=\"submit\" class=\"btn btn-secondary btn-sm\">Cancel Order</button>")
                .append("</form>");
    } else {
        content.append("—");
    }

    content.append("</td></tr>");
}

                content.append("</tbody></table></div>");
               
content.append("<h2 style=\"margin-top:2rem;\">Order History</h2>")
        .append("<div class=\"data-table-container\"><table class=\"data-table\">")
        .append("<thead><tr><th>Item</th><th>Seller</th><th>Amount</th><th>Delivery Date</th></tr></thead><tbody>");

boolean hasHistory = false;

for (Transaction t : buyerOrders) {
    String orderStatus = t.getDeliveryStatus();

    if ("DELIVERED".equalsIgnoreCase(orderStatus)
            || "CANCELLED".equalsIgnoreCase(orderStatus)) {

        hasHistory = true;

        String historyDate = "CANCELLED".equalsIgnoreCase(orderStatus)
                ? "Cancelled"
                : t.getDeliveryDate();

        content.append("<tr><td>")
                .append(InputSanitizer.escapeHtml(t.getItemTitle()))
                .append("</td><td>")
                .append(InputSanitizer.escapeHtml(t.getSellerName()))
                .append("</td><td>₹")
                .append(String.format("%.2f", t.getFinalSalePrice()))
                .append("</td><td>")
                .append(InputSanitizer.escapeHtml(historyDate))
                .append("</td></tr>");
    }
}

if (!hasHistory) {
    content.append("<tr><td colspan=\"4\">No delivered or cancelled orders yet.</td></tr>");
}

content.append("</tbody></table></div>");

            }
            content.append("</section>");

        } else if ("reviews".equals(activeTab)) {
            // TAB 3: REVIEWS AND RATINGS
            content.append("<section>\n")
                   .append("  <div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:1.75rem; margin-bottom:2rem; display:flex; align-items:center; justify-content:space-between;\">\n")
                   .append("    <div>\n")
                   .append("      <h3 style=\"margin-bottom:0.25rem;\">Authentic Campus Seller Reputation</h3>\n")
                   .append("      <p style=\"font-size:0.9rem; color:var(--color-text-secondary); margin:0;\">Calculated dynamically from ").append(userReviews.size()).append(" verified campus exchanges</p>\n")
                   .append("    </div>\n")
                   .append("    <div style=\"text-align:right;\">\n")
                   .append("      <div style=\"font-family:var(--font-serif); font-size:2.2rem; font-weight:700; color:var(--color-deep-navy);\">").append(String.format("%.1f", ratingAvg)).append(" / 5.0</div>\n")
                   .append("      <div>").append(HtmlRenderer.renderStars(ratingAvg)).append("</div>\n")
                   .append("    </div>\n")
                   .append("  </div>\n");

            if (userReviews.isEmpty()) {
                content.append("  <div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:3rem; text-align:center;\">\n")
                       .append("    <h3>No reviews received yet</h3>\n")
                       .append("    <p style=\"color:var(--color-text-secondary);\">Reviews are submitted exclusively by verified buyers after completing a campus handover.</p>\n")
                       .append("  </div>\n");
            } else {
                content.append("  <div class=\"reviews-grid\">\n");
                for (Review r : userReviews) {
                    content.append("    <div class=\"review-card\">\n")
                           .append("      <div class=\"review-header\">\n")
                           .append("        <div>\n")
                           .append("          <div class=\"reviewer-info\">\n")
                           .append("            <span>").append(InputSanitizer.escapeHtml(r.getReviewerName())).append("</span>\n")
                           .append("            <span class=\"verified-badge\">&#10003; Verified Campus Buyer</span>\n")
                           .append("          </div>\n")
                           .append("          <div style=\"font-size:0.8rem; color:var(--color-text-secondary); margin-top:0.2rem;\">\n")
                           .append("            Garment: <em>").append(InputSanitizer.escapeHtml(r.getItemTitle())).append("</em> &bull; ").append(InputSanitizer.escapeHtml(r.getReviewerCollege())).append("\n")
                           .append("          </div>\n")
                           .append("        </div>\n")
                           .append("        <div style=\"text-align:right;\">\n")
                           .append("          <div>").append(r.getStarsDisplay()).append("</div>\n")
                           .append("          <div class=\"review-date\">").append(InputSanitizer.escapeHtml(r.getTimestamp())).append("</div>\n")
                           .append("        </div>\n")
                           .append("      </div>\n")
                           .append("      <p style=\"font-size:0.95rem; color:var(--color-text-primary); margin-top:0.5rem;\">")
                           .append(InputSanitizer.escapeHtml(r.getComment()))
                           .append("</p>\n")
                           .append("    </div>\n");
                }
                content.append("  </div>\n");
            }
            content.append("</section>\n");
        }

        // 4. Edit Profile Section (if viewing own profile)
        if (isOwnProfile && student != null) {
            content.append("<div id=\"edit-profile-section\" style=\"display:none;margin-top:3.5rem;\">\n")
                   .append("  <div class=\"form-card\">\n")
                   .append("    <h2 class=\"form-title\">Update Student Profile</h2>\n")
                   .append("    <p class=\"form-subtitle\">Keep your campus details and clothing swap preferences current.</p>\n")
                   .append("    <form action=\"/edit-profile\" method=\"POST\">\n")
                   .append("      <div class=\"form-grid-2\">\n")
                   .append("        <div class=\"form-group\">\n")
                   .append("          <label class=\"form-label\" for=\"fullName\">Full Name</label>\n")
                   .append("          <input type=\"text\" id=\"fullName\" name=\"fullName\" class=\"form-input\" value=\"").append(InputSanitizer.escapeHtml(student.getFullName())).append("\" required>\n")
                   .append("        </div>\n")
                   .append("        <div class=\"form-group\">\n")
                   .append("          <label class=\"form-label\" for=\"collegeName\">College / University</label>\n")
                   .append("          <input type=\"text\" id=\"collegeName\" name=\"collegeName\" class=\"form-input\" value=\"").append(InputSanitizer.escapeHtml(student.getCollegeName())).append("\" required>\n")
                   .append("        </div>\n")
                   .append("      </div>\n")
                   .append("      <div class=\"form-group\">\n")
                   .append("        <label class=\"form-label\" for=\"bio\">Student Biography</label>\n")
                   .append("        <textarea id=\"bio\" name=\"bio\" class=\"form-textarea\">").append(InputSanitizer.escapeHtml(student.getBiography())).append("</textarea>\n")
                   .append("      </div>\n")
                   .append("      <div class=\"form-group\">\n")
                   .append("        <label class=\"form-label\" for=\"swapCategory\">Preferred Swap Category</label>\n")
                   .append("        <select id=\"swapCategory\" name=\"swapCategory\" class=\"form-select\">\n")
                   .append("          <option value=\"All\"").append("All".equals(student.getPreferredSwapCategory()) ? " selected" : "").append(">All Categories</option>\n")
                   .append("          <option value=\"Jackets & Coats\"").append("Jackets & Coats".equals(student.getPreferredSwapCategory()) ? " selected" : "").append(">Jackets &amp; Coats</option>\n")
                   .append("          <option value=\"Hoodies & Sweaters\"").append("Hoodies & Sweaters".equals(student.getPreferredSwapCategory()) ? " selected" : "").append(">Hoodies &amp; Sweaters</option>\n")
                   .append("          <option value=\"Shirts & Tops\"").append("Shirts & Tops".equals(student.getPreferredSwapCategory()) ? " selected" : "").append(">Shirts &amp; Tops</option>\n")
                   .append("          <option value=\"Pants & Jeans\"").append("Pants & Jeans".equals(student.getPreferredSwapCategory()) ? " selected" : "").append(">Pants &amp; Jeans</option>\n")
                   .append("        </select>\n")
                   .append("      </div>\n")
                   .append("      <button type=\"submit\" class=\"btn btn-primary\">Save Profile</button>\n")
                   .append("    </form>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
        }

        // Sign Out option inside the owner's profile
        if (isOwnProfile) {
            content.append("<div class=\"profile-signout-section\">\n")
                 .append("  <h3>Account</h3>\n")
                 .append("  <p>Finished using ReWear?</p>\n")
                 .append("  <form action=\"/logout\" method=\"POST\">\n")
                 .append("    <button type=\"submit\" class=\"btn btn-secondary\">Sign Out</button>\n")
                 .append("  </form>\n")
                 .append("</div>\n");
        }
        
        sendHtml(exchange, HtmlRenderer.renderPage(targetUser.getFullName() + "'s Wardrobe", "profile", sessionUser, unreadNotifs, null, null, content.toString()));

    }   

    private void markDelivered(HttpExchange exchange, User currentUser, String transactionId) throws IOException {
        if (!(currentUser instanceof Student)) {
            redirect(exchange, "/login");
            return;
        }
        Transaction transaction = dataStore.getTransactionById(transactionId);
        if (transaction == null || !currentUser.getId().equals(transaction.getSellerId())) {
            redirect(exchange, "/profile?tab=sold");
            return;
        }
        if ("PENDING".equalsIgnoreCase(transaction.getDeliveryStatus())) {
            transaction.setDeliveryStatus("DELIVERED");
            transaction.setDeliveryDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            dataStore.updateTransaction(transaction);
        }
        redirect(exchange, "/profile?tab=sold");
    }

    private void handleEditProfile(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        if ("mark-delivered".equals(form.get("action"))) {
            markDelivered(exchange, currentUser, form.get("transactionId"));
            return;
        }

        if ("cancel-order".equals(form.get("action"))) {
    if (currentUser instanceof Student) {
        purchaseService.cancelPurchase(
                form.get("transactionId"),
                currentUser.getId()
        );
    }
    redirect(exchange, "/profile?tab=orders");
    return;
}   
        String fullName = form.get("fullName");
        String college = form.get("collegeName");
        String bio = form.get("bio");
        String swapCat = form.get("swapCategory");

        authService.updateStudentProfile(currentUser.getId(), fullName, college, bio, swapCat);
        redirect(exchange, "/profile");
    }

    private void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }
}
