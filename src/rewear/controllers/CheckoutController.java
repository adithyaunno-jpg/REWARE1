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
import rewear.services.AuthService;
import rewear.services.CartService;
import rewear.services.NotificationService;
import rewear.services.PurchaseService;
import rewear.storage.DataStore;
import rewear.views.HtmlRenderer;

public class CheckoutController implements HttpHandler {

    private final AuthService authService;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final PurchaseService purchaseService;
    private final DataStore dataStore;

    public CheckoutController(
            AuthService authService,
            CartService cartService,
            PurchaseService purchaseService,
            NotificationService notificationService,
            DataStore dataStore) {
        this.authService = authService;
        this.cartService = cartService;
        this.purchaseService = purchaseService;
        this.notificationService = notificationService;
        this.dataStore = dataStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(
                exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);

        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            renderAddressPage(exchange, student, null);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        Map<String, String> form =
                UrlHelper.parseFormBody(exchange.getRequestBody());

        String action = form.getOrDefault("action", "address");

        if ("demo-payment".equals(action)) {
            completeDemoPayment(exchange, student, form);
        } else {
            handleAddressSubmission(exchange, student, form);
        }
    }

    // ---------------- DELIVERY DETAILS ----------------

    private void renderAddressPage(
            HttpExchange exchange, Student student, String error)
            throws IOException {

        List<Listing> items = cartService.getCartItems(student.getId());
        if (items.isEmpty()) {
            redirect(exchange, "/cart");
            return;
        }

        Map<String, String> saved =
                dataStore.getDeliveryDetails(student.getId());

        String pincode = saved.getOrDefault("pincode", "");
        String college = saved.getOrDefault("college", "");
        String address = saved.getOrDefault("address", "");
        String mobile = saved.getOrDefault("mobile", "");

        int unread = notificationService.getUnreadCount(student.getId());
        StringBuilder content = new StringBuilder();

        content.append("<div class=\"form-card\" style=\"max-width:700px;margin:2rem auto;\">")
                .append("<h1 class=\"form-title\">Checkout</h1>")
                .append("<p class=\"form-subtitle\">Enter your delivery details.</p>");

        if (error != null) {
            content.append("<p style=\"padding:12px;background:#fee2e2;color:#991b1b;border-radius:8px;\">")
                    .append(InputSanitizer.escapeHtml(error)).append("</p>");
        }

        content.append("<form action=\"/checkout\" method=\"POST\">")
                .append("<div class=\"form-group\"><label class=\"form-label\" for=\"pincode\">Pincode</label>")
                .append("<input class=\"form-input\" id=\"pincode\" name=\"pincode\" maxlength=\"6\" pattern=\"[0-9]{6}\" value=\"")
                .append(InputSanitizer.escapeHtml(pincode)).append("\" required></div>")
                .append("<div class=\"form-group\"><label class=\"form-label\" for=\"college\">College / University</label>")
                .append("<input class=\"form-input\" id=\"college\" name=\"college\" value=\"")
                .append(InputSanitizer.escapeHtml(college)).append("\" required></div>")
                .append("<div class=\"form-group\"><label class=\"form-label\" for=\"deliveryAddress\">Delivery Address</label>")
                .append("<textarea class=\"form-input\" id=\"deliveryAddress\" name=\"deliveryAddress\" rows=\"3\" maxlength=\"300\" required>")
                .append(InputSanitizer.escapeHtml(address)).append("</textarea></div>")
                .append("<div class=\"form-group\"><label class=\"form-label\" for=\"mobile\">Mobile Number</label>")
                .append("<input class=\"form-input\" type=\"tel\" id=\"mobile\" name=\"mobile\" maxlength=\"14\" value=\"")
                .append(InputSanitizer.escapeHtml(mobile)).append("\" required></div>")
                .append("<button type=\"submit\" class=\"btn btn-primary btn-block btn-lg\">Continue to Order Review</button>")
                .append("</form>")
                .append("<a href=\"/cart\" class=\"btn btn-secondary btn-block\" style=\"margin-top:1rem;\">Back to Cart</a>")
                .append("</div>");

        sendHtml(exchange, HtmlRenderer.renderPage(
                "Checkout", "marketplace", student,
                unread, null, null, content.toString()));
    }

    private void handleAddressSubmission(
            HttpExchange exchange, Student student, Map<String, String> form)
            throws IOException {

        String pincode = form.getOrDefault("pincode", "").trim();
        String college = form.getOrDefault("college", "").trim();
        String address = form.getOrDefault("deliveryAddress", "").trim();
        String mobile = form.getOrDefault("mobile", "").trim();

        if (!pincode.matches("[0-9]{6}")) {
            renderAddressPage(exchange, student,
                    "Please enter a valid 6-digit pincode.");
            return;
        }

        if (college.isBlank()) {
            renderAddressPage(exchange, student,
                    "Please enter your college or university.");
            return;
        }

        if (address.length() < 5 || address.length() > 300) {
            renderAddressPage(exchange, student,
                    "Address must contain between 5 and 300 characters.");
            return;
        }

        if (!mobile.matches("(\\+91[- ]?)?[6-9][0-9]{9}")) {
            renderAddressPage(exchange, student,
                    "Please enter a valid Indian mobile number.");
            return;
        }

        dataStore.saveDeliveryDetails(
                student.getId(), pincode, college, address, mobile);

        renderOrderReview(exchange, student, address, mobile, null);
    }

    // ---------------- ORDER REVIEW ----------------

    private void renderOrderReview(
            HttpExchange exchange, Student student,
            String address, String mobile, String error)
            throws IOException {

        List<Listing> items = cartService.getCartItems(student.getId());
        if (items.isEmpty()) {
            redirect(exchange, "/cart");
            return;
        }
        double total = cartService.getCartTotal(student.getId());
        int unread = notificationService.getUnreadCount(student.getId());

        StringBuilder content = new StringBuilder();

        content.append("<div class=\"form-card\" style=\"max-width:750px;margin:2rem auto;\">")
                .append("<h1 class=\"form-title\">Review Your Order</h1>")
                .append("<h3>Delivery Details</h3><p>")
                .append(InputSanitizer.escapeHtml(address))
                .append("</p><p><strong>Mobile:</strong> ")
                .append(InputSanitizer.escapeHtml(mobile))
                .append("</p><hr style=\"margin:1.5rem 0;\"><h3>Order Items</h3>");

        for (Listing listing : items) {
            String title = listing.getItem() != null
                    ? listing.getItem().getTitle() : "Vintage Garment";

            content.append("<div class=\"cart-summary-row\"><span>")
                    .append(InputSanitizer.escapeHtml(title))
                    .append("</span><strong>₹")
                    .append(String.format("%.2f", listing.getPrice()))
                    .append("</strong></div>");
        }

        content.append("<hr style=\"margin:1rem 0;\"><div class=\"cart-summary-row\">")
                .append("<strong>Total Amount</strong><strong>₹")
                .append(String.format("%.2f", total))
                .append("</strong></div>");

        if (error != null) {
            content.append("<p style=\"padding:12px;background:#fee2e2;color:#991b1b;\">")
                    .append(InputSanitizer.escapeHtml(error)).append("</p>");
        }

        content.append("<h3 style=\"margin-top:25px;\">Choose Payment App</h3>")
                .append("<p class=\"form-hint\">Demo payment only — no real money will be transferred.</p>")
                .append("<form action=\"/checkout\" method=\"POST\">")
                .append("<input type=\"hidden\" name=\"action\" value=\"demo-payment\">")
                .append("<div style=\"display:grid;grid-template-columns:1fr 1fr;gap:12px;margin:15px 0;\">");

        appendPaymentOption(content, "Google Pay", "gpay", true);
        appendPaymentOption(content, "PhonePe", "phonepe", false);
        appendPaymentOption(content, "Paytm", "paytm", false);
        appendPaymentOption(content, "super.money", "supermoney", false);
        appendPaymentOption(content, "BHIM UPI", "bhim", false);
        appendPaymentOption(content, "Other UPI", "other", false);

        content.append("</div>")
                .append("<button type=\"submit\" class=\"btn btn-primary btn-block btn-lg\">Pay Now - ₹")
                .append(String.format("%.2f", total))
                .append("</button></form>")
                .append("<a href=\"/checkout\" class=\"btn btn-secondary btn-block\" style=\"margin-top:10px;\">Edit Delivery Details</a>")
                .append("<a href=\"/cart\" class=\"btn btn-secondary btn-block\" style=\"margin-top:10px;\">Back to Cart</a>")
                .append("</div>");

        sendHtml(exchange, HtmlRenderer.renderPage(
                "Order Review", "marketplace", student,
                unread, null, null, content.toString()));
    }

    private void appendPaymentOption(
            StringBuilder content, String label, String value, boolean selected) {
        content.append("<label style=\"display:flex;align-items:center;gap:10px;padding:14px;border:1px solid var(--color-border);border-radius:10px;cursor:pointer;\">")
                .append("<input type=\"radio\" name=\"paymentApp\" value=\"")
                .append(value).append("\" ")
                .append(selected ? "checked" : "")
                .append(" required><strong>")
                .append(label)
                .append("</strong></label>");
    }

    // ---------------- DEMO PAYMENT ----------------

    private void completeDemoPayment(
            HttpExchange exchange, Student student, Map<String, String> form)
            throws IOException {

        String paymentApp = form.getOrDefault("paymentApp", "");

        List<String> allowedApps = List.of(
                "gpay", "phonepe", "paytm", "supermoney", "bhim", "other");

        if (!allowedApps.contains(paymentApp)) {
            renderPaymentError(exchange, student,
                    "Please select a payment app.");
            return;
        }

        List<Listing> items = cartService.getCartItems(student.getId());
        if (items.isEmpty()) {
            redirect(exchange, "/cart");
            return;
        }

        List<String> listingIds = items.stream()
                .map(Listing::getId)
                .toList();

        PurchaseService.PurchaseResult result =
                purchaseService.executeCartPurchase(
                        student, listingIds, "Campus Student Union / Main Desk");

        if (!result.isSuccess()) {
            renderPaymentError(exchange, student,
                    "The demo payment was not completed because an item is no longer available. Please review your cart.");
            return;
        }

        double total = cartService.getCartTotal(student.getId()) * 20.0;
        cartService.clearCart(student.getId());

        renderDemoPaymentSuccess(exchange, student, paymentApp, total);
    }

    private void renderDemoPaymentSuccess(
            HttpExchange exchange, Student student,
            String paymentApp, double total) throws IOException {

        int unread = notificationService.getUnreadCount(student.getId());
        String appName = paymentAppName(paymentApp);

        String content = "<div class=\"form-card\" style=\"max-width:650px;margin:2rem auto;text-align:center;\">"
                + "<h1 class=\"form-title\">Demo Payment Successful!</h1>"
                + "<p>Your ReWear demo order has been recorded.</p>"
                + "<h2 style=\"margin:25px 0;\">₹" + String.format("%.2f", total) + "</h2>"
                + "<p><strong>Selected app:</strong> " + InputSanitizer.escapeHtml(appName) + "</p>"
                + "<p style=\"padding:12px;background:#fff7ed;border-radius:8px;\">This was a simulated payment. No real money was transferred.</p>"
                + "<a href=\"/marketplace\" class=\"btn btn-primary btn-block\">Continue Shopping</a>"
                + "</div>";

        sendHtml(exchange, HtmlRenderer.renderPage(
                "Demo Payment Successful", "marketplace", student,
                unread, null, null, content));
    }

    private String paymentAppName(String value) {
        return switch (value) {
            case "gpay" -> "Google Pay";
            case "phonepe" -> "PhonePe";
            case "paytm" -> "Paytm";
            case "supermoney" -> "super.money";
            case "bhim" -> "BHIM UPI";
            default -> "Other UPI";
        };
    }

    private void renderPaymentError(
            HttpExchange exchange, Student student, String error)
            throws IOException {

        int unread = notificationService.getUnreadCount(student.getId());
        String content = "<div class=\"form-card\" style=\"max-width:650px;margin:2rem auto;\">"
                + "<h1 class=\"form-title\">Payment Not Completed</h1><p>"
                + InputSanitizer.escapeHtml(error) + "</p>"
                + "<a href=\"/checkout\" class=\"btn btn-primary btn-block\">Return to Checkout</a>"
                + "<a href=\"/cart\" class=\"btn btn-secondary btn-block\" style=\"margin-top:10px;\">Back to Cart</a></div>";

        sendHtml(exchange, HtmlRenderer.renderPage(
                "Payment Error", "marketplace", student,
                unread, null, null, content));
    }

    // ---------------- RESPONSE HELPERS ----------------

    private void sendHtml(HttpExchange exchange, String html)
            throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void redirect(HttpExchange exchange, String location)
            throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }
}
