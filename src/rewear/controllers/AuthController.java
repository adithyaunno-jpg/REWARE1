package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.AuthService;
import rewear.views.HtmlRenderer;

/**
 * Controller managing Student Registration, Authentication, Sessions, and Logout.
 * Pure HTML form handlers with zero client-side scripts.
 */
public class AuthController implements HttpHandler {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/login".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderLoginPage(exchange, null, null);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            }
        } else if ("/register".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderRegisterPage(exchange, null, null);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleRegister(exchange);
            }
        } else if ("/logout".equalsIgnoreCase(path)) {
            handleLogout(exchange);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void renderLoginPage(HttpExchange exchange, String alertType, String alertMsg) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser != null) {
            redirect(exchange, "/profile");
            return;
        }

        StringBuilder content = new StringBuilder();
        content.append("<div style=\"max-width:480px; margin:2rem auto;\">\n")
               .append("  <div class=\"form-card\">\n")
               .append("    <h1 class=\"form-title\">Sign In to ReWear</h1>\n")
               .append("    <p class=\"form-subtitle\">Access your campus wardrobe, swaps, and listings.</p>\n")
               .append("    <form action=\"/login\" method=\"POST\">\n")
               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"email\">College or Registered Email</label>\n")
               .append("        <input type=\"email\" id=\"email\" name=\"email\" class=\"form-input\" required placeholder=\"student@campus.edu\">\n")
               .append("      </div>\n")
               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"password\">Password</label>\n")
               .append("        <input type=\"password\" id=\"password\" name=\"password\" class=\"form-input\" required placeholder=\"&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;\">\n")
               .append("      </div>\n")
               .append("      <button type=\"submit\" class=\"btn btn-primary btn-block\" style=\"margin-top:1.5rem;\">Sign In</button>\n")
               .append("    </form>\n")
               .append("    <div style=\"margin-top:1.75rem; text-align:center; font-size:0.9rem; color:var(--color-text-secondary);\">\n")
               .append("      Don't have an account yet? <a href=\"/register\" style=\"font-weight:600;\">Register as a Student</a>\n")
               .append("    </div>\n")
               .append("    <div style=\"margin-top:1.5rem; padding:1rem; background:var(--color-warm-ivory); border-radius:var(--radius-sm); font-size:0.82rem; color:var(--color-text-secondary);\">\n")
               .append("      <strong>Academic Demo Credentials:</strong><br>\n")
               .append("      Student: <code>aarav@campus.edu</code> (Pass: <code>Pass1234!</code>)<br>\n")
               .append("      Student: <code>priya@campus.edu</code> (Pass: <code>Pass1234!</code>)<br>\n")
               .append("      Admin: <code>admin@rewear.campus</code> (Pass: <code>AdminPassword123!</code>)\n")
               .append("    </div>\n")
               .append("  </div>\n")
               .append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("Sign In", "login", null, 0, alertType, alertMsg, content.toString()));
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String email = form.get("email");
        String password = form.get("password");

        AuthService.AuthResult result = authService.login(email, password);
        if (result.isSuccess()) {
            CookieHelper.setCookie(exchange, SessionManager.COOKIE_NAME, result.getSession().getToken(), 86400);
            redirect(exchange, "/profile");
        } else {
            renderLoginPage(exchange, "danger", result.getMessage());
        }
    }

    private void renderRegisterPage(HttpExchange exchange, String alertType, String alertMsg) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser != null) {
            redirect(exchange, "/profile");
            return;
        }

        StringBuilder content = new StringBuilder();
        content.append("<div style=\"max-width:580px; margin:2rem auto;\">\n")
               .append("  <div class=\"form-card\">\n")
               .append("    <h1 class=\"form-title\">Create Student Account</h1>\n")
               .append("    <p class=\"form-subtitle\">Join your university circular fashion exchange.</p>\n")
               .append("    <form action=\"/register\" method=\"POST\">\n")
               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"fullName\">Full Name</label>\n")
               .append("        <input type=\"text\" id=\"fullName\" name=\"fullName\" class=\"form-input\" required placeholder=\"e.g. Aarav Sharma\">\n")
               .append("      </div>\n")
               .append("      <div class=\"form-grid-2\">\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"email\">College Email</label>\n")
               .append("          <input type=\"email\" id=\"email\" name=\"email\" class=\"form-input\" required placeholder=\"student@campus.edu\">\n")
               .append("        </div>\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"password\">Password (Min 6 chars)</label>\n")
               .append("          <input type=\"password\" id=\"password\" name=\"password\" class=\"form-input\" required minlength=\"6\" placeholder=\"&bull;&bull;&bull;&bull;&bull;&bull;\">\n")
               .append("        </div>\n")
               .append("      </div>\n")
               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"collegeName\">University / College Institute</label>\n")
               .append("        <input type=\"text\" id=\"collegeName\" name=\"collegeName\" class=\"form-input\" required placeholder=\"e.g. Apex Institute of Technology\">\n")
               .append("      </div>\n")
               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"bio\">Student Wardrobe Bio</label>\n")
               .append("        <textarea id=\"bio\" name=\"bio\" class=\"form-textarea\" placeholder=\"Tell fellow campus students about your style, favorite clothing brands, or swap preferences...\"></textarea>\n")
               .append("      </div>\n")
               .append("      <button type=\"submit\" class=\"btn btn-primary btn-block\" style=\"margin-top:1.25rem;\">Complete Registration</button>\n")
               .append("    </form>\n")
               .append("    <div style=\"margin-top:1.5rem; text-align:center; font-size:0.9rem; color:var(--color-text-secondary);\">\n")
               .append("      Already registered? <a href=\"/login\" style=\"font-weight:600;\">Sign In Here</a>\n")
               .append("    </div>\n")
               .append("  </div>\n")
               .append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("Register", "register", null, 0, alertType, alertMsg, content.toString()));
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String fullName = form.get("fullName");
        String email = form.get("email");
        String password = form.get("password");
        String collegeName = form.get("collegeName");
        String bio = form.get("bio");

        AuthService.AuthResult result = authService.registerStudent(fullName, email, password, collegeName, bio);
        if (result.isSuccess()) {
            CookieHelper.setCookie(exchange, SessionManager.COOKIE_NAME, result.getSession().getToken(), 86400);
            redirect(exchange, "/profile");
        } else {
            renderRegisterPage(exchange, "danger", result.getMessage());
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        if (token != null) {
            authService.logout(token);
            CookieHelper.deleteCookie(exchange, SessionManager.COOKIE_NAME);
        }
        redirect(exchange, "/login");
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
