package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import rewear.models.Listing;
import rewear.models.Student;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.AdminService;
import rewear.services.AuthService;
import rewear.services.NotificationService;
import rewear.storage.DataStore;
import rewear.views.HtmlRenderer;

/**
 * Controller managing the Administrative Panel: Metrics, Student User Compliance,
 * Content Moderation, and Reported Listings.
 */
public class AdminController implements HttpHandler {
    private final AuthService authService;
    private final AdminService adminService;
    private final NotificationService notificationService;
    private final DataStore dataStore;

    public AdminController(AuthService authService, AdminService adminService,
                           NotificationService notificationService, DataStore dataStore) {
        this.authService = authService;
        this.adminService = adminService;
        this.notificationService = notificationService;
        this.dataStore = dataStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);

        // Server-Side Authorization Verification
        if (currentUser == null || !currentUser.canModerate()) {
            redirect(exchange, "/login");
            return;
        }

        if ("/admin".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderAdminDashboard(exchange, currentUser);
            }
        } else if ("/admin/toggle-user".equalsIgnoreCase(path)) {
            handleToggleUser(exchange, currentUser);
        } else if ("/admin/delete-listing".equalsIgnoreCase(path)) {
            handleDeleteListing(exchange, currentUser);
        } else if ("/admin/dismiss-report".equalsIgnoreCase(path)) {
            handleDismissReport(exchange, currentUser);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void renderAdminDashboard(HttpExchange exchange, User adminUser) throws IOException {
        AdminService.AdminStats stats = adminService.getStatistics();
        List<User> allUsers = dataStore.getAllUsers();
        List<Listing> reportedListings = adminService.getReportedListings();
        List<Listing> allListings = dataStore.getAllListings();
        int unreadNotifs = notificationService.getUnreadCount(adminUser.getId());

        StringBuilder content = new StringBuilder();

        content.append("<div class=\"section-header\" style=\"margin-top:0.5rem;\">\n")
               .append("  <div>\n")
               .append("    <span class=\"badge badge-sold\" style=\"margin-bottom:0.4rem;\">System Administration</span>\n")
               .append("    <h1 class=\"section-title\">ReWear Campus Control Center</h1>\n")
               .append("    <p class=\"section-subtitle\">Platform health, student account compliance, and content moderation</p>\n")
               .append("  </div>\n")
               .append("</div>\n");

        // 1. System Statistics Grid
        content.append("<div style=\"display:grid; grid-template-columns:repeat(auto-fit, minmax(180px, 1fr)); gap:1.25rem; margin-bottom:2.5rem;\">\n")
               .append("  <div class=\"form-card\" style=\"padding:1.5rem; text-align:center;\">\n")
               .append("    <div class=\"stat-value\">").append(stats.totalUsers).append("</div>\n")
               .append("    <div class=\"stat-label\">Registered Accounts</div>\n")
               .append("  </div>\n")
               .append("  <div class=\"form-card\" style=\"padding:1.5rem; text-align:center;\">\n")
               .append("    <div class=\"stat-value\">").append(stats.totalListings).append("</div>\n")
               .append("    <div class=\"stat-label\">Total Garments</div>\n")
               .append("  </div>\n")
               .append("  <div class=\"form-card\" style=\"padding:1.5rem; text-align:center;\">\n")
               .append("    <div class=\"stat-value\" style=\"color:var(--color-sage-dark);\">").append(stats.activeListings).append("</div>\n")
               .append("    <div class=\"stat-label\">Active Listings</div>\n")
               .append("  </div>\n")
               .append("  <div class=\"form-card\" style=\"padding:1.5rem; text-align:center;\">\n")
               .append("    <div class=\"stat-value\" style=\"color:var(--color-cobalt);\">").append(stats.soldListings).append("</div>\n")
               .append("    <div class=\"stat-label\">Recirculated / Sold</div>\n")
               .append("  </div>\n")
               .append("  <div class=\"form-card\" style=\"padding:1.5rem; text-align:center;\">\n")
               .append("    <div class=\"stat-value\">₹").append(String.format("%.0f", stats.totalVolume)).append("</div>\n")
               .append("  </div>\n")
               .append("  <div class=\"form-card\" style=\"padding:1.5rem; text-align:center;\">\n")
               .append("    <div class=\"stat-value\" style=\"color:var(--color-danger);\">").append(stats.reportedListings).append("</div>\n")
               .append("    <div class=\"stat-label\">Reported Content</div>\n")
               .append("  </div>\n")
               .append("</div>\n");

        // 2. Reported Content Table (if any)
        if (!reportedListings.isEmpty()) {
            content.append("<div style=\"margin-bottom:3rem;\">\n")
                   .append("  <h2 style=\"font-size:1.4rem; color:var(--color-danger); margin-bottom:1rem;\">&#9888; Reported Listings Requiring Review</h2>\n")
                   .append("  <div class=\"data-table-container\">\n")
                   .append("    <table class=\"data-table\">\n")
                   .append("      <thead>\n")
                   .append("        <tr>\n")
                   .append("          <th>Garment</th>\n")
                   .append("          <th>Seller</th>\n")
                   .append("          <th>Price</th>\n")
                   .append("          <th>Status</th>\n")
                   .append("          <th>Moderation Actions</th>\n")
                   .append("        </tr>\n")
                   .append("      </thead>\n")
                   .append("      <tbody>\n");

            for (Listing l : reportedListings) {
                content.append("        <tr>\n")
                       .append("          <td><strong><a href=\"/listing?id=").append(l.getId()).append("\">").append(InputSanitizer.escapeHtml(l.getItem().getTitle())).append("</a></strong></td>\n")
                       .append("          <td>").append(InputSanitizer.escapeHtml(l.getSellerName())).append(" (").append(InputSanitizer.escapeHtml(l.getSellerCollege())).append(")</td>\n")
                     .append("          <td>₹").append(String.format("%.2f", l.getPrice())).append("</td>\n")
                       .append("          <td>\n")
                       .append("            <form action=\"/admin/dismiss-report\" method=\"POST\" style=\"display:inline;\">\n")
                       .append("              <input type=\"hidden\" name=\"id\" value=\"").append(l.getId()).append("\">\n")
                       .append("              <button type=\"submit\" class=\"btn btn-secondary btn-sm\">Dismiss Report</button>\n")
                       .append("            </form>\n")
                       .append("            <form action=\"/admin/delete-listing\" method=\"POST\" style=\"display:inline;\" onsubmit=\"return confirm('Permanently remove listing?');\">\n")
                       .append("              <input type=\"hidden\" name=\"id\" value=\"").append(l.getId()).append("\">\n")
                       .append("              <button type=\"submit\" class=\"btn btn-danger btn-sm\">Remove Listing</button>\n")
                       .append("            </form>\n")
                       .append("          </td>\n")
                       .append("        </tr>\n");
            }

            content.append("      </tbody>\n")
                   .append("    </table>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
        }

        // 3. User Compliance & Moderation Table
        content.append("<div style=\"margin-bottom:3rem;\">\n")
               .append("  <h2 style=\"font-size:1.4rem; margin-bottom:1rem;\">Registered Student Directory &amp; Compliance</h2>\n")
               .append("  <div class=\"data-table-container\">\n")
               .append("    <table class=\"data-table\">\n")
               .append("      <thead>\n")
               .append("        <tr>\n")
               .append("          <th>Name / ID</th>\n")
               .append("          <th>Email</th>\n")
               .append("          <th>College</th>\n")
               .append("          <th>Role</th>\n")
               .append("          <th>Account Status</th>\n")
               .append("          <th>Moderation</th>\n")
               .append("        </tr>\n")
               .append("      </thead>\n")
               .append("      <tbody>\n");

        for (User u : allUsers) {
            String roleBadge = u.canModerate() ? "badge-condition" : "badge-size";
            String statusBadge = u.isActive() ? "badge-condition" : "badge-sold";
            String college = (u instanceof Student s) ? s.getCollegeName() : "Administration";

            content.append("        <tr>\n")
                   .append("          <td><strong>").append(InputSanitizer.escapeHtml(u.getFullName())).append("</strong><br><code style=\"font-size:0.75rem;\">").append(u.getId()).append("</code></td>\n")
                   .append("          <td>").append(InputSanitizer.escapeHtml(u.getEmail())).append("</td>\n")
                   .append("          <td>").append(InputSanitizer.escapeHtml(college)).append("</td>\n")
                   .append("          <td><span class=\"badge ").append(roleBadge).append("\">").append(u.getRole()).append("</span></td>\n")
                   .append("          <td><span class=\"badge ").append(statusBadge).append("\">").append(u.isActive() ? "ACTIVE" : "SUSPENDED").append("</span></td>\n")
                   .append("          <td>\n");

            if (!u.canModerate()) {
                content.append("            <form action=\"/admin/toggle-user\" method=\"POST\" style=\"display:inline;\">\n")
                       .append("              <input type=\"hidden\" name=\"userId\" value=\"").append(u.getId()).append("\">\n")
                       .append("              <button type=\"submit\" class=\"btn btn-secondary btn-sm\">")
                       .append(u.isActive() ? "Suspend" : "Reactivate").append("</button>\n")
                       .append("            </form>\n");
            } else {
                content.append("            <span style=\"font-size:0.8rem; color:var(--color-text-muted);\">Protected Admin</span>\n");
            }

            content.append("          </td>\n")
                   .append("        </tr>\n");
        }

        content.append("      </tbody>\n")
               .append("    </table>\n")
               .append("  </div>\n")
               .append("</div>\n");

        // 4. Catalog Management Table
        content.append("<div>\n")
               .append("  <h2 style=\"font-size:1.4rem; margin-bottom:1rem;\">Garment Catalog Moderation</h2>\n")
               .append("  <div class=\"data-table-container\">\n")
               .append("    <table class=\"data-table\">\n")
               .append("      <thead>\n")
               .append("        <tr>\n")
               .append("          <th>Title / ID</th>\n")
               .append("          <th>Seller</th>\n")
               .append("          <th>Price</th>\n")
               .append("          <th>Status</th>\n")
               .append("          <th>Views</th>\n")
               .append("          <th>Action</th>\n")
               .append("        </tr>\n")
               .append("      </thead>\n")
               .append("      <tbody>\n");

        for (Listing l : allListings) {
            content.append("        <tr>\n")
                   .append("          <td><strong><a href=\"/listing?id=").append(l.getId()).append("\">").append(InputSanitizer.escapeHtml(l.getItem().getTitle())).append("</a></strong><br><code style=\"font-size:0.75rem;\">").append(l.getId()).append("</code></td>\n")
                   .append("          <td>").append(InputSanitizer.escapeHtml(l.getSellerName())).append("</td>\n")
                   .append("          <td>₹").append(String.format("%.2f", l.getPrice())).append("</td>\n")
                   .append("          <td>₹").append(String.format("%.2f", l.getPrice())).append("</td>\n")
                   .append("          <td>\n")
                   .append("            <form action=\"/admin/delete-listing\" method=\"POST\" style=\"display:inline;\" onsubmit=\"return confirm('Delete this listing?');\">\n")
                   .append("              <input type=\"hidden\" name=\"id\" value=\"").append(l.getId()).append("\">\n")
                   .append("              <button type=\"submit\" class=\"btn btn-danger btn-sm\">Delete</button>\n")
                   .append("            </form>\n")
                   .append("          </td>\n")
                   .append("        </tr>\n");
        }

        content.append("      </tbody>\n")
               .append("    </table>\n")
               .append("  </div>\n")
               .append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("Admin Control Center", "admin", adminUser, unreadNotifs, null, null, content.toString()));
    }

    private void handleToggleUser(HttpExchange exchange, User adminUser) throws IOException {
        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String targetId = form.get("userId");
        adminService.toggleUserStatus(adminUser, targetId);
        redirect(exchange, "/admin");
    }

    private void handleDeleteListing(HttpExchange exchange, User adminUser) throws IOException {
        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String id = form.get("id");
        adminService.removeListing(adminUser, id);
        redirect(exchange, "/admin");
    }

    private void handleDismissReport(HttpExchange exchange, User adminUser) throws IOException {
        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String id = form.get("id");
        adminService.dismissReport(adminUser, id);
        redirect(exchange, "/admin");
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
