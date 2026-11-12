
package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
import rewear.views.HtmlRenderer;

public class CartController implements HttpHandler {

    private final AuthService authService;
    private final CartService cartService;
    private final NotificationService notificationService;

    public CartController(AuthService authService,
                          CartService cartService,
                          NotificationService notificationService) {
        this.authService = authService;
        this.cartService = cartService;
        this.notificationService = notificationService;
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

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/add-to-cart".equals(path) && "POST".equalsIgnoreCase(method)) {
            handleAddToCart(exchange, student);
        } else if ("/cart/count".equals(path)
            && "GET".equalsIgnoreCase(method)) {
                sendJson(exchange,"{\"count\":" + cartService.getCartCount(student.getId()) + "}");
        } else if ("/remove-from-cart".equals(path)
                && "POST".equalsIgnoreCase(method)) {
            handleRemoveFromCart(exchange, student);
        } else if ("/cart".equals(path)
                && "GET".equalsIgnoreCase(method)) {
            renderCart(exchange, student);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleAddToCart(HttpExchange exchange,
                             Student student) throws IOException {

    var form = UrlHelper.parseFormBody(exchange.getRequestBody());
    String listingId = form.get("listingId");

    boolean added = cartService.addToCart(student.getId(), listingId);
    int count = cartService.getCartCount(student.getId());

    String response = "{\"success\":" + added
            + ",\"count\":" + count + "}";

    sendJson(exchange, response);
}

    private void handleRemoveFromCart(HttpExchange exchange,
                                  Student student) throws IOException {

    var form = UrlHelper.parseFormBody(exchange.getRequestBody());
    String listingId = form.get("listingId");

    cartService.removeFromCart(student.getId(), listingId);

    int count = cartService.getCartCount(student.getId());

    sendJson(exchange, "{\"success\":true,\"count\":" + count + "}");
}

   
private void renderCart(HttpExchange exchange,
                        Student student) throws IOException {

    List<Listing> items = cartService.getCartItems(student.getId());

    double total = cartService.getCartTotal(student.getId());

    int unreadNotifs =
            notificationService.getUnreadCount(student.getId());

    StringBuilder content = new StringBuilder();

    content.append("<div class=\"section-header\">")
           .append("<div>")
           .append("<h1 class=\"section-title\">My Shopping Cart</h1>")
           .append("<p class=\"section-subtitle\">")
           .append(items.size())
           .append(" item(s) in your cart")
           .append("</p></div></div>");

    if (items.isEmpty()) {

        content.append("<div class=\"form-card\" style=\"text-align:center;padding:3rem;\">")
               .append("<h2>Your cart is empty</h2>")
               .append("<p>Discover something special in our marketplace.</p>")
               .append("<a href=\"/marketplace\" class=\"btn btn-primary\">Explore Marketplace</a>")
               .append("</div>");

    } else {

        content.append("<div class=\"cart-layout\">");

        // LEFT SIDE: Cart products
        content.append("<div class=\"cart-items-list\">");

        for (Listing listing : items) {

            var item = listing.getItem();

            String title = item != null
                    ? item.getTitle() : "Vintage Garment";

            String category = item != null
                    ? item.getCategory() : "Apparel";

            String brand = item != null
                    ? item.getBrand() : "Unbranded";

            String size = item != null
                    ? item.getSize() : "Not specified";

            String color = item != null
                    ? item.getColor() : "Not specified";

            String condition = item != null
                    ? item.getCondition() : "Not specified";

            String description = item != null
                    ? item.getDescription() : "";

            String photo = listing.getPrimaryPhoto();

            content.append("<div class=\"cart-item-card\">");

            // Product image
            content.append("<div class=\"cart-item-image\">");

            if (photo != null && !photo.isEmpty()) {
                content.append("<img src=\"")
                       .append(InputSanitizer.escapeHtml(photo))
                       .append("\" alt=\"")
                       .append(InputSanitizer.escapeHtml(title))
                       .append("\">");
            } else {
                content.append("<div class=\"cart-image-placeholder\">")
                       .append("&#128085;")
                       .append("</div>");
            }

            content.append("</div>");

            // Product details
            content.append("<div class=\"cart-item-details\">")

                   .append("<span class=\"cart-category\">")
                   .append(InputSanitizer.escapeHtml(category))
                   .append("</span>")

                   .append("<h3>")
                   .append(InputSanitizer.escapeHtml(title))
                   .append("</h3>")

                   .append("<div class=\"cart-product-info\">")

                   .append("<span><strong>Brand:</strong> ")
                   .append(InputSanitizer.escapeHtml(brand))
                   .append("</span>")

                   .append("<span><strong>Size:</strong> ")
                   .append(InputSanitizer.escapeHtml(size))
                   .append("</span>")

                   .append("<span><strong>Colour:</strong> ")
                   .append(InputSanitizer.escapeHtml(color))
                   .append("</span>")

                   .append("<span><strong>Condition:</strong> ")
                   .append(InputSanitizer.escapeHtml(condition))
                   .append("</span>")

                   .append("</div>");

            if (description != null && !description.isBlank()) {
                content.append("<p class=\"cart-description\">")
                       .append(InputSanitizer.escapeHtml(description))
                       .append("</p>");
            }

            content.append("<p class=\"cart-seller\">")
                   .append("Seller: ")
                   .append(InputSanitizer.escapeHtml(listing.getSellerName()))
                   .append(" &nbsp; | &nbsp; ")
                   .append(InputSanitizer.escapeHtml(listing.getSellerCollege()))
                   .append("</p>");

            content.append("</div>");

            // Price and remove button
            content.append("<div class=\"cart-item-actions\">")

                   .append("<strong class=\"cart-item-price\">₹")
                   .append(String.format("%.2f", listing.getPrice()))
                   .append("</strong>");

            if (!listing.isAvailable()) {
                content.append("<span class=\"badge badge-sold\">")
                       .append("Unavailable")
                       .append("</span>");
            }

            content.append("<form action=\"/remove-from-cart\" method=\"POST\">")
                   .append("<input type=\"hidden\" name=\"listingId\" value=\"")
                   .append(InputSanitizer.escapeHtml(listing.getId()))
                   .append("\">")
                   .append("<button type=\"submit\" class=\"btn btn-secondary btn-sm\">Remove</button>")
                   .append("</form>")

                   .append("</div>");

            content.append("</div>");
        }

        content.append("</div>");

        // RIGHT SIDE: Order summary
content.append("<div class=\"cart-summary\">")

       .append("<h2>Order Summary</h2>")

       .append("<div class=\"cart-summary-row\">")
       .append("<span>Total Items</span>")
       .append("<span>").append(items.size()).append("</span>")
       .append("</div>")

       .append("<div class=\"cart-summary-row cart-total-row\">")
       .append("<strong>Total Amount</strong>")
       .append("<strong>₹")
      .append(String.format("%.2f", total))
       .append("</strong>")
       .append("</div>")

       .append("<p class=\"cart-summary-note\">")
       .append("Campus handover will be arranged after purchase confirmation.")
       .append("</p>")

       .append("<a href=\"/checkout\" class=\"btn btn-primary btn-block\">")
       .append("Proceed to Buy")
       .append("</a>")

       .append("<a href=\"/marketplace\" class=\"btn btn-secondary btn-block\">")
       .append("Continue Shopping")
       .append("</a>")

       .append("</div>");

        content.append("</div>");
    }

    
    String html = HtmlRenderer.renderPage(
            "My Cart",
            "marketplace",
            student,
            unreadNotifs,
            null,
            null,
            content.toString()
    );

    sendHtml(exchange, html);
}

private void sendJson(HttpExchange exchange, String json)
        throws IOException {

    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

    exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8");

    exchange.sendResponseHeaders(200, bytes.length);

    try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
    }
}


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
