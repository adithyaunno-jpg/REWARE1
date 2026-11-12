package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import rewear.models.Notification;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.AuthService;
import rewear.services.NotificationService;
import rewear.views.HtmlRenderer;

/**
 * Controller displaying persistent user notifications with read/unread status updates.
 */
public class NotificationController implements HttpHandler {
    private final AuthService authService;
    private final NotificationService notificationService;

    public NotificationController(AuthService authService, NotificationService notificationService) {
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/notifications".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderNotifications(exchange);
            }
        } else if ("/notifications/read".equalsIgnoreCase(path)) {
            handleMarkRead(exchange);
        } else if ("/notifications/read-all".equalsIgnoreCase(path)) {
            handleMarkAllRead(exchange);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void renderNotifications(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        List<Notification> list = notificationService.getNotificationsForUser(currentUser.getId());
        int unreadNotifs = notificationService.getUnreadCount(currentUser.getId());

        StringBuilder content = new StringBuilder();
        content.append("<div style=\"max-width:800px; margin:1rem auto;\">\n")
               .append("  <div class=\"section-header\">\n")
               .append("    <div>\n")
               .append("      <h1 class=\"section-title\">Notifications</h1>\n")
               .append("      <p class=\"section-subtitle\">Updates on your orders, sales, swap requests, and reviews</p>\n")
               .append("    </div>\n");

        if (unreadNotifs > 0) {
            content.append("    <form action=\"/notifications/read-all\" method=\"POST\" style=\"display:inline;\">\n")
                   .append("      <button type=\"submit\" class=\"btn btn-secondary btn-sm\">Mark All as Read</button>\n")
                   .append("    </form>\n");
        }

        content.append("  </div>\n");

        if (list.isEmpty()) {
            content.append("  <div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:3.5rem; text-align:center;\">\n")
                   .append("    <div style=\"font-size:2.5rem; margin-bottom:1rem;\">&#128276;</div>\n")
                   .append("    <h3>All caught up!</h3>\n")
                   .append("    <p style=\"color:var(--color-text-secondary);\">No notifications at this time.</p>\n")
                   .append("  </div>\n");
        } else {
            content.append("  <div style=\"display:flex; flex-direction:column; gap:1rem;\">\n");
            for (Notification n : list) {
                String bg = n.isRead() ? "var(--color-white)" : "var(--color-warm-ivory)";
                String border = n.isRead() ? "var(--color-border)" : "var(--color-cobalt)";

                content.append("    <div style=\"background:").append(bg).append("; border:1px solid ").append(border).append("; border-radius:var(--radius-md); padding:1.25rem 1.5rem; display:flex; justify-content:space-between; align-items:center;\">\n")
                       .append("      <div>\n")
                       .append("        <div style=\"display:flex; align-items:center; gap:0.5rem; margin-bottom:0.25rem;\">\n")
                       .append("          <h4 style=\"margin:0;\">").append(InputSanitizer.escapeHtml(n.getTitle())).append("</h4>\n");

                if (!n.isRead()) {
                    content.append("          <span class=\"badge badge-sold\" style=\"font-size:0.65rem;\">NEW</span>\n");
                }

                content.append("        </div>\n")
                       .append("        <p style=\"font-size:0.95rem; color:var(--color-text-secondary); margin-bottom:0.4rem;\">")
                       .append(InputSanitizer.escapeHtml(n.getMessage()))
                       .append("</p>\n")
                       .append("        <div style=\"font-size:0.8rem; color:var(--color-text-muted);\">\n")
                       .append("          ").append(n.getTimestamp()).append("\n");

                if (n.getLink() != null && !n.getLink().equals("#")) {
                    content.append("          &bull; <a href=\"").append(n.getLink()).append("\" style=\"font-weight:600;\">Open Linked Action &rarr;</a>\n");
                }

                content.append("        </div>\n")
                       .append("      </div>\n");

                if (!n.isRead()) {
                    content.append("      <form action=\"/notifications/read\" method=\"POST\">\n")
                           .append("        <input type=\"hidden\" name=\"id\" value=\"").append(n.getId()).append("\">\n")
                           .append("        <button type=\"submit\" class=\"btn btn-secondary btn-sm\">Mark Read</button>\n")
                           .append("      </form>\n");
                }

                content.append("    </div>\n");
            }
            content.append("  </div>\n");
        }

        content.append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("Notifications", "", currentUser, unreadNotifs, null, null, content.toString()));
    }

    private void handleMarkRead(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String id = form.get("id");
        notificationService.markAsRead(id, currentUser.getId());
        redirect(exchange, "/notifications");
    }

    private void handleMarkAllRead(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        notificationService.markAllAsRead(currentUser.getId());
        redirect(exchange, "/notifications");
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
