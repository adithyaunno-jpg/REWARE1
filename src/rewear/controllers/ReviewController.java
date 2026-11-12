package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import rewear.models.Student;
import rewear.models.Transaction;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.AuthService;
import rewear.services.NotificationService;
import rewear.services.ReviewService;
import rewear.storage.DataStore;
import rewear.views.HtmlRenderer;

/**
 * Controller managing verified buyer reviews, star rating submissions,
 * and duplicate review prevention.
 */
public class ReviewController implements HttpHandler {
    private final AuthService authService;
    private final ReviewService reviewService;
    private final DataStore dataStore;
    private final NotificationService notificationService;

    public ReviewController(AuthService authService, ReviewService reviewService,
                            DataStore dataStore, NotificationService notificationService) {
        this.authService = authService;
        this.reviewService = reviewService;
        this.dataStore = dataStore;
        this.notificationService = notificationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            renderReviewForm(exchange);
        } else if ("POST".equalsIgnoreCase(method)) {
            handleSubmitReview(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void renderReviewForm(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> query = UrlHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String txnId = query.get("txn");
        Transaction txn = dataStore.getTransactionById(txnId);

        if (txn == null || !txn.getBuyerId().equals(student.getId())) {
            redirect(exchange, "/profile");
            return;
        }

        if (txn.isReviewed()) {
            redirect(exchange, "/profile?id=" + txn.getSellerId() + "&tab=reviews");
            return;
        }

        int unreadNotifs = notificationService.getUnreadCount(student.getId());

        StringBuilder content = new StringBuilder();
        content.append("<div style=\"max-width:600px; margin:2rem auto;\">\n")
               .append("  <div class=\"form-card\">\n")
               .append("    <h1 class=\"form-title\">Leave a Verified Review</h1>\n")
               .append("    <p class=\"form-subtitle\">Share your feedback regarding the garment condition and campus handover experience.</p>\n")

               .append("    <div style=\"background:var(--color-warm-ivory); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:1.25rem; margin-bottom:1.5rem;\">\n")
               .append("      <div style=\"font-size:0.8rem; text-transform:uppercase; color:var(--color-text-secondary); font-weight:700;\">Transaction Reference: ").append(txn.getId()).append("</div>\n")
               .append("      <h3 style=\"margin:0.25rem 0;\">").append(InputSanitizer.escapeHtml(txn.getItemTitle())).append("</h3>\n")
               .append("      <div style=\"font-size:0.9rem; color:var(--color-text-secondary);\">\n")
               .append("        Seller: <strong>").append(InputSanitizer.escapeHtml(txn.getSellerName())).append("</strong> (").append(InputSanitizer.escapeHtml(txn.getSellerCollege())).append(")\n")
               .append("      </div>\n")
               .append("    </div>\n")

               .append("    <form action=\"/review\" method=\"POST\">\n")
               .append("      <input type=\"hidden\" name=\"txnId\" value=\"").append(txn.getId()).append("\">\n")

               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"rating\">Overall Rating (1 to 5 Stars)</label>\n")
               .append("        <select id=\"rating\" name=\"rating\" class=\"form-select\" required>\n")
               .append("          <option value=\"5\" selected>&#9733;&#9733;&#9733;&#9733;&#9733; (5 Stars - Exceptional)</option>\n")
               .append("          <option value=\"4\">&#9733;&#9733;&#9733;&#9733;&#9734; (4 Stars - Great Experience)</option>\n")
               .append("          <option value=\"3\">&#9733;&#9733;&#9733;&#9734;&#9734; (3 Stars - Satisfactory)</option>\n")
               .append("          <option value=\"2\">&#9733;&#9733;&#9734;&#9734;&#9734; (2 Stars - Disappointing)</option>\n")
               .append("          <option value=\"1\">&#9733;&#9734;&#9734;&#9734;&#9734; (1 Star - Poor)</option>\n")
               .append("        </select>\n")
               .append("      </div>\n")

               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"comment\">Written Review &amp; Comments</label>\n")
               .append("        <textarea id=\"comment\" name=\"comment\" class=\"form-textarea\" required placeholder=\"Describe garment condition accuracy, punctuality at campus meetup, and communication...\"></textarea>\n")
               .append("      </div>\n")

               .append("      <button type=\"submit\" class=\"btn btn-primary btn-block btn-lg\" style=\"margin-top:1.5rem;\">Submit Verified Review</button>\n")
               .append("    </form>\n")
               .append("  </div>\n")
               .append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("Submit Review", "marketplace", student, unreadNotifs, null, null, content.toString()));
    }

    private void handleSubmitReview(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String txnId = form.get("txnId");
        int rating = 5;
        try {
            rating = Integer.parseInt(form.getOrDefault("rating", "5"));
        } catch (NumberFormatException ignored) {}
        String comment = form.get("comment");

        ReviewService.ReviewResult res = reviewService.submitReview(student, txnId, rating, comment);
        if (res.isSuccess()) {
            redirect(exchange, "/profile?id=" + res.getReview().getSellerId() + "&tab=reviews");
        } else {
            redirect(exchange, "/profile");
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
