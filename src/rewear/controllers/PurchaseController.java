package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import rewear.models.Listing;
import rewear.models.Student;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.AuthService;
import rewear.services.MarketplaceService;
import rewear.services.NotificationService;
import rewear.services.PurchaseService;
import rewear.views.HtmlRenderer;

/**
 * Controller managing simulated purchase confirmations, campus handover location selection,
 * and transaction execution.
 */
public class PurchaseController implements HttpHandler {
    private final AuthService authService;
    private final MarketplaceService marketplaceService;
    private final PurchaseService purchaseService;
    private final NotificationService notificationService;

    public PurchaseController(AuthService authService, MarketplaceService marketplaceService,
                              PurchaseService purchaseService, NotificationService notificationService) {
        this.authService = authService;
        this.marketplaceService = marketplaceService;
        this.purchaseService = purchaseService;
        this.notificationService = notificationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            renderPurchasePage(exchange);
        } else if ("POST".equalsIgnoreCase(method)) {
            handlePurchase(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void renderPurchasePage(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> query = UrlHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String id = query.get("id");
        Listing listing = marketplaceService.getListing(id);

        if (listing == null || !listing.isAvailable()) {
            redirect(exchange, "/marketplace");
            return;
        }

        if (listing.getSellerId().equals(student.getId())) {
            redirect(exchange, "/listing?id=" + id);
            return;
        }

        int unreadNotifs = notificationService.getUnreadCount(student.getId());

        StringBuilder content = new StringBuilder();
        content.append("<div style=\"max-width:680px; margin:2rem auto;\">\n")
               .append("  <div class=\"form-card\">\n")
               .append("    <h1 class=\"form-title\">Confirm Campus Purchase</h1>\n")
               .append("    <p class=\"form-subtitle\">Simulated checkout: Zero packaging waste, direct campus handover.</p>\n")

               .append("    <div style=\"background:var(--color-warm-ivory); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:1.5rem; margin-bottom:1.75rem;\">\n")
               .append("      <h3 style=\"margin-bottom:0.35rem;\">").append(InputSanitizer.escapeHtml(listing.getItem().getTitle())).append("</h3>\n")
               .append("      <div style=\"font-size:0.9rem; color:var(--color-text-secondary); margin-bottom:0.75rem;\">\n")
               .append("        ").append(InputSanitizer.escapeHtml(listing.getItem().getCategory())).append(" &bull; Size ")
               .append(InputSanitizer.escapeHtml(listing.getItem().getSize())).append(" &bull; ")
               .append(InputSanitizer.escapeHtml(listing.getItem().getCondition())).append("\n")
               .append("      </div>\n")
               .append("      <div style=\"display:flex; justify-content:space-between; align-items:center; border-top:1px solid var(--color-border); padding-top:0.75rem;\">\n")
               .append("        <span style=\"font-weight:600;\">Total Agreed Price:</span>\n") 
             .append("          <span style=\"font-family:var(--font-serif); font-size:1.75rem; font-weight:700; color:var(--color-deep-navy);\">₹")
               .append(String.format("%.2f", listing.getPrice())).append("</span>\n")
               .append("      </div>\n")
               .append("    </div>\n")

               .append("    <form action=\"/purchase\" method=\"POST\">\n")
               .append("      <input type=\"hidden\" name=\"listingId\" value=\"").append(listing.getId()).append("\">\n")

               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"meetupLocation\">Campus Meetup &amp; Handover Location</label>\n")
               .append("        <select id=\"meetupLocation\" name=\"meetupLocation\" class=\"form-select\" required>\n")
               .append("          <option value=\"Campus Central Library Main Desk\">Campus Central Library Main Circulation Desk</option>\n")
               .append("          <option value=\"Student Activity Center Union Lobby\">Student Activity Center Union Lobby</option>\n")
               .append("          <option value=\"University Cafeteria North Entrance\">University Cafeteria North Entrance</option>\n")
               .append("          <option value=\"Engineering Block Ground Floor\">Engineering Block Ground Floor Lounge</option>\n")
               .append("        </select>\n")
               .append("        <p class=\"form-hint\">Choose a safe, well-lit campus public area for garment inspection and handover.</p>\n")
               .append("      </div>\n")

               .append("      <div style=\"background:var(--color-sage-light); border:1px solid var(--color-sage); border-radius:var(--radius-sm); padding:1rem; margin:1.5rem 0; font-size:0.85rem; color:var(--color-deep-navy);\">\n")
               .append("        &#127807; <strong>Circularity Notice:</strong> Once this purchase is completed, ownership of ReWear Passport <strong>#")
               .append(listing.getItem().getPassportId()).append("</strong> will transfer to you, and your verified purchase badge will unlock to review ")
               .append(InputSanitizer.escapeHtml(listing.getSellerName())).append(".\n")
               .append("      </div>\n")

               .append("      <div style=\"display:flex; gap:1rem;\">\n")
               .append("        <button type=\"submit\" class=\"btn btn-primary btn-block btn-lg\">Confirm Purchase &amp; Handover</button>\n")
               .append("        <a href=\"/listing?id=").append(listing.getId()).append("\" class=\"btn btn-secondary\">Cancel</a>\n")
               .append("      </div>\n")
               .append("    </form>\n")
               .append("  </div>\n")
               .append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("Purchase Confirmation", "marketplace", student, unreadNotifs, null, null, content.toString()));
    }

    private void handlePurchase(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String listingId = form.get("listingId");
        String location = form.get("meetupLocation");

        PurchaseService.PurchaseResult result = purchaseService.executePurchase(student, listingId, location);
        if (result.isSuccess()) {
            redirect(exchange, "/profile?tab=sold");
        } else {
            redirect(exchange, "/listing?id=" + listingId);
        }
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
