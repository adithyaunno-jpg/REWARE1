package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import rewear.models.PassportEvent;
import rewear.models.ReWearPassport;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.AuthService;
import rewear.services.NotificationService;
import rewear.services.PassportService;
import rewear.views.HtmlRenderer;

/**
 * Controller displaying the ReWear Passport digital ownership ledger and repair history.
 */
public class PassportController implements HttpHandler {
    private final AuthService authService;
    private final PassportService passportService;
    private final NotificationService notificationService;

    public PassportController(AuthService authService, PassportService passportService,
                              NotificationService notificationService) {
        this.authService = authService;
        this.passportService = passportService;
        this.notificationService = notificationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/passport".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderPassportPage(exchange);
            }
        } else if ("/add-passport-note".equalsIgnoreCase(path)) {
            handleAddPassportNote(exchange);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void renderPassportPage(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        int unreadNotifs = currentUser != null ? notificationService.getUnreadCount(currentUser.getId()) : 0;

        Map<String, String> query = UrlHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String id = query.get("id");
        ReWearPassport passport = passportService.getPassport(id);

        if (passport == null) {
            redirect(exchange, "/marketplace");
            return;
        }

        StringBuilder content = new StringBuilder();
        content.append("<div style=\"margin-bottom:1.5rem;\">\n")
               .append("  <a href=\"/listing?id=").append(passport.getListingId()).append("\" style=\"font-size:0.9rem; font-weight:600;\">&larr; Return to Garment Listing</a>\n")
               .append("</div>\n");

        content.append("<div class=\"passport-card\">\n")
               .append("  <div class=\"passport-header\">\n")
               .append("    <div>\n")
               .append("      <span class=\"passport-code\">").append(InputSanitizer.escapeHtml(passport.getId())).append("</span>\n")
               .append("      <h1 style=\"font-size:2.4rem; margin:0.5rem 0 0.25rem;\">").append(InputSanitizer.escapeHtml(passport.getGarmentTitle())).append("</h1>\n")
               .append("      <div style=\"font-size:0.95rem; color:var(--color-text-secondary);\">\n")
               .append("        Genesis Registration: <strong>").append(passport.getCreationDate()).append("</strong> by <strong>")
               .append(InputSanitizer.escapeHtml(passport.getOriginalOwnerName())).append("</strong> (").append(InputSanitizer.escapeHtml(passport.getOriginalCollege())).append(")\n")
               .append("      </div>\n")
               .append("    </div>\n")
               .append("    <div style=\"text-align:right;\">\n")
               .append("      <div style=\"font-family:var(--font-serif); font-size:2rem; font-weight:700; color:var(--color-sage-dark);\">").append(passport.getReuseCount()).append(" Cycles</div>\n")
               .append("      <div style=\"font-size:0.8rem; text-transform:uppercase; font-weight:700; color:var(--color-deep-navy);\">Circulation Count</div>\n")
               .append("    </div>\n")
               .append("  </div>\n");

        // Lifetime Sustainability Impact Box
        content.append("  <div style=\"display:grid; grid-template-columns:1fr 1fr; gap:1.5rem; background:var(--color-warm-ivory); border-radius:var(--radius-md); padding:1.25rem 1.75rem; margin-bottom:2.5rem;\">\n")
               .append("    <div>\n")
               .append("      <div style=\"font-size:0.8rem; text-transform:uppercase; color:var(--color-text-secondary); font-weight:700;\">Total CO&sub2; Prevented</div>\n")
               .append("      <div style=\"font-family:var(--font-serif); font-size:1.75rem; font-weight:700; color:var(--color-deep-navy);\">").append(String.format("%.1f", passport.getCo2SavedKg())).append(" kg</div>\n")
               .append("      <div style=\"font-size:0.82rem; color:var(--color-text-secondary);\">Equivalent to not burning ~1.5 gallons of gasoline</div>\n")
               .append("    </div>\n")
               .append("    <div>\n")
               .append("      <div style=\"font-size:0.8rem; text-transform:uppercase; color:var(--color-text-secondary); font-weight:700;\">Freshwater Conserved</div>\n")
               .append("      <div style=\"font-family:var(--font-serif); font-size:1.75rem; font-weight:700; color:var(--color-deep-navy);\">").append(String.format("%,.0f", passport.getWaterSavedLiters())).append(" L</div>\n")
               .append("      <div style=\"font-size:0.82rem; color:var(--color-text-secondary);\">Equivalent to ~35 full bathtub showers</div>\n")
               .append("    </div>\n")
               .append("  </div>\n");

        // Event Timeline
        content.append("  <h2 style=\"font-size:1.5rem; margin-bottom:1.5rem;\">Verified Garment Timeline &amp; Provenance</h2>\n")
               .append("  <div class=\"timeline\">\n");

        for (PassportEvent event : passport.getEvents()) {
            content.append("    <div class=\"timeline-event\">\n")
                   .append("      <div class=\"timeline-point\"></div>\n")
                   .append("      <div class=\"timeline-event-content\">\n")
                   .append("        <div style=\"display:flex; justify-content:space-between; align-items:flex-start;\">\n")
                   .append("          <div class=\"timeline-event-title\">").append(InputSanitizer.escapeHtml(event.getDescription())).append("</div>\n")
                   .append("          <span class=\"badge badge-size\">").append(event.getEventType()).append("</span>\n")
                   .append("        </div>\n")
                   .append("        <div class=\"timeline-event-date\">").append(InputSanitizer.escapeHtml(event.getTimestamp())).append(" &bull; ").append(InputSanitizer.escapeHtml(event.getActorName())).append(" (").append(InputSanitizer.escapeHtml(event.getActorCollege())).append(")</div>\n");

            if (event.getNotes() != null && !event.getNotes().isEmpty()) {
                content.append("        <p style=\"font-size:0.9rem; color:var(--color-text-secondary); margin:0;\">").append(InputSanitizer.escapeHtml(event.getNotes())).append("</p>\n");
            }
            content.append("      </div>\n")
                   .append("    </div>\n");
        }
        content.append("  </div>\n");

        // Add Care/Repair Note Form (if logged in)
        if (currentUser != null) {
            content.append("  <div style=\"margin-top:3rem; padding-top:2rem; border-top:1px solid var(--color-border);\">\n")
                   .append("    <h3 style=\"margin-bottom:0.5rem;\">Log Garment Maintenance or Repair</h3>\n")
                   .append("    <p style=\"font-size:0.9rem; color:var(--color-text-secondary); margin-bottom:1.25rem;\">Did you mend a button, alter the fit, or dry-clean this garment? Record it to preserve its circular passport history.</p>\n")
                   .append("    <form action=\"/add-passport-note\" method=\"POST\">\n")
                   .append("      <input type=\"hidden\" name=\"passportId\" value=\"").append(passport.getId()).append("\">\n")
                   .append("      <div class=\"form-grid-2\">\n")
                   .append("        <div class=\"form-group\">\n")
                   .append("          <label class=\"form-label\" for=\"conditionStatus\">Condition Milestone</label>\n")
                   .append("          <select id=\"conditionStatus\" name=\"conditionStatus\" class=\"form-select\">\n")
                   .append("            <option value=\"Professionally Laundered / Cleaned\">Professionally Laundered / Cleaned</option>\n")
                   .append("            <option value=\"Repaired Seam / Replaced Buttons\">Repaired Seam / Replaced Buttons</option>\n")
                   .append("            <option value=\"Condition Verified Like New\">Condition Verified Like New</option>\n")
                   .append("          </select>\n")
                   .append("        </div>\n")
                   .append("        <div class=\"form-group\">\n")
                   .append("          <label class=\"form-label\" for=\"noteText\">Care Details / Notes</label>\n")
                   .append("          <input type=\"text\" id=\"noteText\" name=\"noteText\" class=\"form-input\" required placeholder=\"e.g. Cleaned with eco-detergent, condition excellent\">\n")
                   .append("        </div>\n")
                   .append("      </div>\n")
                   .append("      <button type=\"submit\" class=\"btn btn-secondary btn-sm\">Append Verified Care Log</button>\n")
                   .append("    </form>\n")
                   .append("  </div>\n");
        }

        content.append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("ReWear Passport #" + passport.getId(), "marketplace", currentUser, unreadNotifs, null, null, content.toString()));
    }

    private void handleAddPassportNote(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String passportId = form.get("passportId");
        String noteText = form.get("noteText");
        String status = form.get("conditionStatus");

        passportService.addMaintenanceNote(currentUser, passportId, noteText, status);
        redirect(exchange, "/passport?id=" + passportId);
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
