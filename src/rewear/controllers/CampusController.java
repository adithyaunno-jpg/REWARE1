package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import rewear.models.Listing;
import rewear.models.Student;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.AuthService;
import rewear.services.NotificationService;
import rewear.storage.DataStore;
import rewear.views.HtmlRenderer;

/**
 * Controller managing Campus Circle: University Directory, Campus Leaderboard,
 * and Local Student Listings.
 */
public class CampusController implements HttpHandler {
    private final AuthService authService;
    private final NotificationService notificationService;
    private final DataStore dataStore;

    public CampusController(AuthService authService, NotificationService notificationService, DataStore dataStore) {
        this.authService = authService;
        this.notificationService = notificationService;
        this.dataStore = dataStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        int unreadNotifs = currentUser != null ? notificationService.getUnreadCount(currentUser.getId()) : 0;

        Map<String, String> query = UrlHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String collegeFilter = query.getOrDefault("college", "All").trim();

        // Calculate college circulation statistics
        Map<String, Integer> collegeListingsCount = new HashMap<>();
        Map<String, Integer> collegeStudentCount = new HashMap<>();

        for (User u : dataStore.getAllUsers()) {
            if (u instanceof Student s && !s.getCollegeName().isEmpty()) {
                collegeStudentCount.put(s.getCollegeName(), collegeStudentCount.getOrDefault(s.getCollegeName(), 0) + 1);
            }
        }

        for (Listing l : dataStore.getAllListings()) {
            if (l.getSellerCollege() != null && !l.getSellerCollege().isEmpty()) {
                collegeListingsCount.put(l.getSellerCollege(), collegeListingsCount.getOrDefault(l.getSellerCollege(), 0) + 1);
            }
        }

        // Filter students & listings
        List<Student> students = new ArrayList<>();
        for (User u : dataStore.getAllUsers()) {
            if (u instanceof Student s) {
                if ("All".equalsIgnoreCase(collegeFilter) || collegeFilter.equalsIgnoreCase(s.getCollegeName())) {
                    students.add(s);
                }
            }
        }

        List<Listing> campusListings = new ArrayList<>();
        for (Listing l : dataStore.getAllListings()) {
            if (l.isAvailable()) {
                if ("All".equalsIgnoreCase(collegeFilter) || collegeFilter.equalsIgnoreCase(l.getSellerCollege())) {
                    campusListings.add(l);
                }
            }
        }

        StringBuilder content = new StringBuilder();

        // Header Banner
        content.append("<div class=\"campus-hero-banner\">\n")
               .append("  <div>\n")
               .append("    <span class=\"badge badge-size\" style=\"margin-bottom:0.5rem;\">Campus Network Hub</span>\n")
               .append("    <h1 style=\"font-size:2.2rem; margin-bottom:0.5rem;\">Campus Circle Directory</h1>\n")
               .append("    <p style=\"color:var(--color-text-secondary); max-width:650px; margin:0;\">\n")
               .append("      Connect with peer student sellers in your university institute for zero-shipping, instant handovers.\n")
               .append("    </p>\n")
               .append("  </div>\n")
               .append("  <div style=\"display:flex; gap:0.75rem; flex-wrap:wrap;\">\n")
               .append("    <div class=\"campus-stat-pill\">&#127891; ").append(collegeStudentCount.size()).append(" Partner Campuses</div>\n")
               .append("    <div class=\"campus-stat-pill\">&#128101; ").append(dataStore.getAllUsers().size() - 1).append(" Student Members</div>\n")
               .append("  </div>\n")
               .append("</div>\n");

        // Campus Filter Bar
        content.append("<div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:1.25rem; margin-bottom:2.5rem;\">\n")
               .append("  <form action=\"/campus\" method=\"GET\" style=\"display:flex; align-items:center; gap:1rem; flex-wrap:wrap;\">\n")
               .append("    <label for=\"college\" style=\"font-weight:700;\">Filter by University Campus:</label>\n")
               .append("    <select id=\"college\" name=\"college\" class=\"form-select\" style=\"max-width:350px;\">\n")
               .append("      <option value=\"All\"").append("All".equals(collegeFilter) ? " selected" : "").append(">All Participating Campuses</option>\n");

        for (String cName : collegeStudentCount.keySet()) {
            content.append("      <option value=\"").append(InputSanitizer.escapeHtml(cName)).append("\"")
                   .append(cName.equalsIgnoreCase(collegeFilter) ? " selected" : "").append(">")
                   .append(InputSanitizer.escapeHtml(cName)).append(" (").append(collegeListingsCount.getOrDefault(cName, 0)).append(" items)")
                   .append("</option>\n");
        }

        content.append("    </select>\n")
               .append("    <button type=\"submit\" class=\"btn btn-primary btn-sm\">Filter Campus Network</button>\n")
               .append("    <a href=\"/campus\" class=\"btn btn-secondary btn-sm\">Show All</a>\n")
               .append("  </form>\n")
               .append("</div>\n");

        // Two Column: Student Directory and Campus Listings
        content.append("<div class=\"marketplace-layout\">\n");

        // Left Column: Student Directory
        content.append("  <div>\n")
               .append("    <h2 style=\"font-size:1.4rem; margin-bottom:1rem;\">Campus Student Sellers</h2>\n")
               .append("    <div style=\"display:flex; flex-direction:column; gap:1rem;\">\n");

        for (Student s : students) {
            content.append("      <div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:1.25rem; display:flex; align-items:center; gap:1rem;\">\n")
                   .append("        <div class=\"profile-avatar\" style=\"width:54px; height:54px; font-size:1.4rem;\">")
                   .append(s.getFullName().substring(0, 1)).append("</div>\n")
                   .append("        <div style=\"flex:1;\">\n")
                   .append("          <h4 style=\"margin:0;\"><a href=\"/profile?id=").append(s.getId()).append("\">").append(InputSanitizer.escapeHtml(s.getFullName())).append("</a></h4>\n")
                   .append("          <div style=\"font-size:0.8rem; color:var(--color-text-secondary);\">&#127891; ").append(InputSanitizer.escapeHtml(s.getCollegeName())).append("</div>\n")
                   .append("          <div style=\"font-size:0.8rem; color:var(--color-sage-dark); font-weight:600;\">★ ").append(String.format("%.1f", s.getRatingAverage())).append(" (").append(s.getRatingCount()).append(" verified reviews)</div>\n")
                   .append("        </div>\n")
                   .append("        <a href=\"/profile?id=").append(s.getId()).append("\" class=\"btn btn-secondary btn-sm\">Profile</a>\n")
                   .append("      </div>\n");
        }

        content.append("    </div>\n")
               .append("  </div>\n");

        // Right Column: Campus Drops
        content.append("  <div>\n")
               .append("    <h2 style=\"font-size:1.4rem; margin-bottom:1rem;\">Campus Wardrobe Drops</h2>\n");

        if (campusListings.isEmpty()) {
            content.append("    <div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:3rem; text-align:center;\">\n")
                   .append("      <h3>No active garments listed for this campus yet</h3>\n")
                   .append("      <p style=\"color:var(--color-text-secondary); margin:0.5rem 0 1.25rem;\">Be the first student to seed circular fashion at your college!</p>\n")
                   .append("      <a href=\"/sell\" class=\"btn btn-primary\">+ List a Garment</a>\n")
                   .append("    </div>\n");
        } else {
            content.append("    <div class=\"products-grid\" style=\"grid-template-columns:repeat(auto-fill, minmax(230px, 1fr));\">\n");
            for (Listing l : campusListings) {
                content.append(HtmlRenderer.renderProductCard(l));
            }
            content.append("    </div>\n");
        }

        content.append("  </div>\n")
               .append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("Campus Circle Network", "campus", currentUser, unreadNotifs, null, null, content.toString()));
    }

    private void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

