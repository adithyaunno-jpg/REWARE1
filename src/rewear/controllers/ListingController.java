package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import rewear.models.*;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.MultipartParser;
import rewear.server.UrlHelper;
import rewear.services.AuthService;
import rewear.services.MarketplaceService;
import rewear.services.NotificationService;
import rewear.views.HtmlRenderer;

/**
 * Controller managing garment detail views, multipart image listing creation,
 * listing updates, deletion, reporting, and status changes.
 */
public class ListingController implements HttpHandler {
    private final AuthService authService;
    private final MarketplaceService marketplaceService;
    private final NotificationService notificationService;

    public ListingController(AuthService authService, MarketplaceService marketplaceService,
                             NotificationService notificationService) {
        this.authService = authService;
        this.marketplaceService = marketplaceService;
        this.notificationService = notificationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/listing".equalsIgnoreCase(path)) {
            renderListingDetailPage(exchange);
        } else if ("/sell".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderSellPage(exchange, null, null);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleCreateListing(exchange);
            }
        } else if ("/edit-listing".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderEditPage(exchange, null, null);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleUpdateListing(exchange);
            }
        } else if ("/delete-listing".equalsIgnoreCase(path)) {
            handleDeleteListing(exchange);
        } else if ("/mark-sold".equalsIgnoreCase(path)) {
            handleMarkSold(exchange);
        } else if ("/report-listing".equalsIgnoreCase(path)) {
            handleReportListing(exchange);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void renderListingDetailPage(HttpExchange exchange) throws IOException {
        Map<String, String> query = UrlHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String id = query.get("id");
        Listing listing = marketplaceService.getListing(id);

        if (listing == null) {
            redirect(exchange, "/marketplace");
            return;
        }

        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        int unreadNotifs = currentUser != null ? notificationService.getUnreadCount(currentUser.getId()) : 0;

        ClothingItem item = listing.getItem();
        boolean isOwner = currentUser != null && currentUser.getId().equals(listing.getSellerId());
        boolean canModerate = currentUser != null && currentUser.canModerate();

        StringBuilder content = new StringBuilder();
        content.append("<div style=\"margin-bottom:1.5rem;\">\n")
               .append("  <a href=\"/marketplace\" style=\"font-size:0.9rem; font-weight:600;\">&larr; Back to Marketplace</a>\n")
               .append("</div>\n");

        content.append("<div class=\"product-detail-layout\">\n");

        // Left Column: Photographs & Fallback
        content.append("  <div class=\"product-gallery\">\n")
               .append("    <div class=\"product-main-view\">\n");

        if (!listing.getPhotoUrls().isEmpty()) {
            content.append("      <img src=\"").append(InputSanitizer.escapeHtml(listing.getPrimaryPhoto())).append("\" alt=\"").append(InputSanitizer.escapeHtml(item.getTitle())).append("\">\n");
        } else {
            content.append("      <div class=\"image-fallback\" style=\"height:100%;\">\n")
                   .append("        <div class=\"fallback-icon\" style=\"font-size:4rem;\">&#128085;</div>\n")
                   .append("        <div class=\"fallback-text\" style=\"font-size:1.1rem;\">").append(InputSanitizer.escapeHtml(item.getCategory())).append("</div>\n")
                   .append("      </div>\n");
        }
        content.append("    </div>\n");

        // Thumbnails if multiple photos
        if (listing.getPhotoUrls().size() > 1) {
            content.append("    <div class=\"product-thumbnails\">\n");
            for (String photoUrl : listing.getPhotoUrls()) {
                content.append("      <div class=\"thumb-item\"><img src=\"").append(InputSanitizer.escapeHtml(photoUrl)).append("\" alt=\"Photo\"></div>\n");
            }
            content.append("    </div>\n");
        }
        content.append("  </div>\n");

        // Right Column: Details & Actions
        content.append("  <div>\n")
               .append("    <div style=\"display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:0.75rem;\">\n")
               .append("      <span class=\"badge badge-size\">").append(InputSanitizer.escapeHtml(item.getCategory())).append("</span>\n");

        if (!listing.isAvailable()) {
            content.append("      <span class=\"badge badge-sold\" style=\"font-size:0.9rem;\">").append(listing.getStatus()).append("</span>\n");
        } else {
            content.append("      <span class=\"badge badge-condition\" style=\"font-size:0.9rem;\">").append(InputSanitizer.escapeHtml(item.getCondition())).append("</span>\n");
        }
        content.append("    </div>\n");

        content.append("    <h1 style=\"font-size:2.25rem; margin-bottom:0.75rem;\">").append(InputSanitizer.escapeHtml(item.getTitle())).append("</h1>\n")
               .append("    <div style=\"font-family:var(--font-serif); font-size:2.2rem; font-weight:700; color:var(--color-deep-navy); margin-bottom:1.5rem;\">\n")
               .append(" ₹").append(String.format("%.2f", listing.getPrice()))
               .append("    </div>\n");

        // Specifications Grid
        content.append("    <div class=\"spec-list\">\n")
               .append("      <div class=\"spec-item\"><div class=\"spec-label\">Size</div><div class=\"spec-value\">").append(InputSanitizer.escapeHtml(item.getSize())).append("</div></div>\n")
               .append("      <div class=\"spec-item\"><div class=\"spec-label\">Condition</div><div class=\"spec-value\">").append(InputSanitizer.escapeHtml(item.getCondition())).append("</div></div>\n")
               .append("      <div class=\"spec-item\"><div class=\"spec-label\">Brand / Label</div><div class=\"spec-value\">").append(InputSanitizer.escapeHtml(item.getBrand())).append("</div></div>\n")
               .append("      <div class=\"spec-item\"><div class=\"spec-label\">Color</div><div class=\"spec-value\">").append(InputSanitizer.escapeHtml(item.getColor())).append("</div></div>\n")
               .append("      <div class=\"spec-item\"><div class=\"spec-label\">Target Swap Preference</div><div class=\"spec-value\">").append(InputSanitizer.escapeHtml(listing.getTargetSwapCategory())).append("</div></div>\n")
               .append("      <div class=\"spec-item\"><div class=\"spec-label\">Digital Passport</div><div class=\"spec-value\"><a href=\"/passport?id=").append(item.getPassportId()).append("\" style=\"color:var(--color-sage-dark); font-weight:700;\">").append(item.getPassportId()).append(" &rarr;</a></div></div>\n")
               .append("    </div>\n");

        // Description
        content.append("    <div style=\"margin:1.75rem 0;\">\n")
               .append("      <h4 style=\"margin-bottom:0.5rem;\">Description &amp; Care Details</h4>\n")
               .append("      <p style=\"font-size:1rem; line-height:1.7; color:var(--color-text-secondary);\">")
               .append(InputSanitizer.escapeHtml(item.getDescription()).replace("\n", "<br>"))
               .append("</p>\n")
               .append("    </div>\n");

        // Seller Info Box
        content.append("    <div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:1.25rem; margin-bottom:1.75rem; display:flex; align-items:center; justify-content:space-between;\">\n")
               .append("      <div>\n")
               .append("        <div style=\"font-size:0.75rem; text-transform:uppercase; color:var(--color-text-secondary); font-weight:700;\">Student Seller</div>\n")
               .append("        <div style=\"font-weight:700; font-size:1.1rem; color:var(--color-deep-navy);\">\n")
               .append("          <a href=\"/profile?id=").append(listing.getSellerId()).append("\">").append(InputSanitizer.escapeHtml(listing.getSellerName())).append("</a>\n")
               .append("        </div>\n")
               .append("        <div style=\"font-size:0.85rem; color:var(--color-text-secondary);\">&#127891; ").append(InputSanitizer.escapeHtml(listing.getSellerCollege())).append("</div>\n")
               .append("      </div>\n")
               .append("      <div>\n")
               .append("        <a href=\"/profile?id=").append(listing.getSellerId()).append("\" class=\"btn btn-secondary btn-sm\">View Seller Profile</a>\n")
               .append("      </div>\n")
               .append("    </div>\n");

        // Action Buttons
        content.append("    <div style=\"display:flex; gap:1rem; flex-wrap:wrap; margin-bottom:1.5rem;\">\n");

        if (listing.isAvailable()) {
            if (isOwner) {
                // Owner management actions
                content.append("      <a href=\"/edit-listing?id=").append(listing.getId()).append("\" class=\"btn btn-primary\">Edit Listing</a>\n")
                       .append("      <form action=\"/mark-sold\" method=\"POST\" style=\"display:inline;\">\n")
                       .append("        <input type=\"hidden\" name=\"id\" value=\"").append(listing.getId()).append("\">\n")
                       .append("        <button type=\"submit\" class=\"btn btn-secondary\">Mark as Sold</button>\n")
                       .append("      </form>\n")
                       .append("      <form action=\"/delete-listing\" method=\"POST\" style=\"display:inline;\" onsubmit=\"return confirm('Are you sure you want to delete this listing?');\">\n")
                       .append("        <input type=\"hidden\" name=\"id\" value=\"").append(listing.getId()).append("\">\n")
                       .append("        <button type=\"submit\" class=\"btn btn-danger\">Delete</button>\n")
                       .append("      </form>\n");
            } else {
                // Buyer / Swapper actions
                content.append("      <a href=\"/purchase?id=").append(listing.getId()).append("\" class=\"btn btn-primary btn-lg\" style=\"flex:1;\">Buy Now (₹").append(String.format("%.2f", listing.getPrice())).append(")</a>\n")
                       .append("      <a href=\"/swapmatch?with=").append(listing.getId()).append("\" class=\"btn btn-cobalt btn-lg\" style=\"flex:1;\">Propose Swap</a>\n");
            }
        } else {
            content.append("      <div class=\"alert alert-warning\" style=\"width:100%;\">\n")
                   .append("        This garment has been marked as <strong>").append(listing.getStatus()).append("</strong> and is no longer available for new orders.\n")
                   .append("      </div>\n");
        }

        content.append("    </div>\n");

        // Digital Passport & Report Flags
        content.append("    <div style=\"display:flex; justify-content:space-between; align-items:center; padding-top:1rem; border-top:1px solid var(--color-border); font-size:0.85rem;\">\n")
               .append("      <a href=\"/passport?id=").append(item.getPassportId()).append("\" style=\"color:var(--color-sage-dark); font-weight:600;\">&#128220; View Lifetime ReWear Passport</a>\n");

        if (!isOwner && currentUser != null) {
            content.append("      <form action=\"/report-listing\" method=\"POST\" style=\"display:inline;\">\n")
                   .append("        <input type=\"hidden\" name=\"id\" value=\"").append(listing.getId()).append("\">\n")
                   .append("        <button type=\"submit\" style=\"background:none; border:none; color:var(--color-danger); cursor:pointer; font-size:0.82rem; text-decoration:underline;\">Report Inappropriate Listing</button>\n")
                   .append("      </form>\n");
        }
        content.append("    </div>\n");

        content.append("  </div>\n")
               .append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage(item.getTitle(), "marketplace", currentUser, unreadNotifs, null, null, content.toString()));
    }

    private void renderSellPage(HttpExchange exchange, String alertType, String alertMsg) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        int unreadNotifs = notificationService.getUnreadCount(currentUser.getId());

        StringBuilder content = new StringBuilder();
        content.append("<div style=\"max-width:760px; margin:1.5rem auto;\">\n")
               .append("  <div class=\"form-card\">\n")
               .append("    <h1 class=\"form-title\">List a Garment for Campus Exchange</h1>\n")
               .append("    <p class=\"form-subtitle\">Sell or swap your pre-loved fashion. A permanent ReWear Passport will be generated for your item.</p>\n")
               .append("    <form action=\"/sell\" method=\"POST\" enctype=\"multipart/form-data\">\n")

               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"title\">Garment Title</label>\n")
               .append("        <input type=\"text\" id=\"title\" name=\"title\" class=\"form-input\" required placeholder=\"e.g. Vintage Levi's Denim Trucker Jacket\">\n")
               .append("      </div>\n")

               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"description\">Detailed Description &amp; Care Details</label>\n")
               .append("        <textarea id=\"description\" name=\"description\" class=\"form-textarea\" required placeholder=\"Mention the fit, fabric composition, wear history, and campus meetup availability...\"></textarea>\n")
               .append("      </div>\n")

               .append("      <div class=\"form-grid-2\">\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"category\">Category</label>\n")
               .append("          <select id=\"category\" name=\"category\" class=\"form-select\" required>\n")
               .append("            <option value=\"Jackets & Coats\">Jackets &amp; Coats</option>\n")
               .append("            <option value=\"Hoodies & Sweaters\">Hoodies &amp; Sweaters</option>\n")
               .append("            <option value=\"Shirts & Tops\">Shirts &amp; Tops</option>\n")
               .append("            <option value=\"Pants & Jeans\">Pants &amp; Jeans</option>\n")
               .append("            <option value=\"Dresses & Skirts\">Dresses &amp; Skirts</option>\n")
               .append("            <option value=\"Shoes\">Shoes</option>\n")
               .append("            <option value=\"Traditional Wear\">Traditional Wear</option>\n")
               .append("            <option value=\"Accessories\">Accessories</option>\n")
               .append("          </select>\n")
               .append("        </div>\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"size\">Size</label>\n")
               .append("          <select id=\"size\" name=\"size\" class=\"form-select\" required>\n")
               .append("            <option value=\"XS\">XS</option>\n")
               .append("            <option value=\"S\">S</option>\n")
               .append("            <option value=\"M\" selected>M</option>\n")
               .append("            <option value=\"L\">L</option>\n")
               .append("            <option value=\"XL\">XL</option>\n")
               .append("            <option value=\"XXL\">XXL</option>\n")
               .append("            <option value=\"30\">30 Waist</option>\n")
               .append("            <option value=\"32\">32 Waist</option>\n")
               .append("            <option value=\"Free Size\">Free Size</option>\n")
               .append("          </select>\n")
               .append("        </div>\n")
               .append("      </div>\n")

               .append("      <div class=\"form-grid-2\">\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"condition\">Condition</label>\n")
               .append("          <select id=\"condition\" name=\"condition\" class=\"form-select\" required>\n")
               .append("            <option value=\"Brand New with Tags\">Brand New with Tags</option>\n")
               .append("            <option value=\"Like New\">Like New</option>\n")
               .append("            <option value=\"Gently Used\" selected>Gently Used</option>\n")
               .append("            <option value=\"Well Worn\">Well Worn</option>\n")
               .append("          </select>\n")
               .append("        </div>\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"price\">Price (₹)</label>\n")
               .append("          <input type=\"number\" step=\"0.50\" id=\"price\" name=\"price\" class=\"form-input\" required min=\"0\" placeholder=\"25.00\">\n")
               .append("        </div>\n")
               .append("      </div>\n")

               .append("      <div class=\"form-grid-2\">\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"brand\">Brand / Label</label>\n")
               .append("          <input type=\"text\" id=\"brand\" name=\"brand\" class=\"form-input\" placeholder=\"e.g. Zara, Levi's, Thrifted\">\n")
               .append("        </div>\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"color\">Primary Color</label>\n")
               .append("          <input type=\"text\" id=\"color\" name=\"color\" class=\"form-input\" placeholder=\"e.g. Navy Blue, Oatmeal\">\n")
               .append("        </div>\n")
               .append("      </div>\n")

               // Target Swap Preference
               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"targetSwapCategory\">Desired Swap Category (for SwapMatch algorithm)</label>\n")
               .append("        <select id=\"targetSwapCategory\" name=\"targetSwapCategory\" class=\"form-select\">\n")
               .append("          <option value=\"Any\">Any Category (Open to all offers)</option>\n")
               .append("          <option value=\"Jackets & Coats\">Jackets &amp; Coats</option>\n")
               .append("          <option value=\"Hoodies & Sweaters\">Hoodies &amp; Sweaters</option>\n")
               .append("          <option value=\"Shirts & Tops\">Shirts &amp; Tops</option>\n")
               .append("          <option value=\"Pants & Jeans\">Pants &amp; Jeans</option>\n")
               .append("          <option value=\"Dresses & Skirts\">Dresses &amp; Skirts</option>\n")
               .append("        </select>\n")
               .append("      </div>\n")

               // Multipart Image Upload Field
               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\">Garment Photographs (JPEG, PNG, WebP)</label>\n")
               .append("        <div class=\"file-upload-box\">\n")
               .append("          <div class=\"file-upload-icon\">&#128247;</div>\n")
               .append("          <p style=\"font-weight:600; margin-bottom:0.35rem;\">Choose high-quality photos of your clothing</p>\n")
               .append("          <p class=\"form-hint\">Upload multiple photos showing front, back, and label. Max 10MB per file.</p>\n")
               .append("          <input type=\"file\" name=\"photos\" multiple accept=\"image/jpeg,image/png,image/webp\" style=\"margin-top:0.75rem;\">\n")
               .append("        </div>\n")
               .append("      </div>\n")

               .append("      <button type=\"submit\" class=\"btn btn-primary btn-block btn-lg\" style=\"margin-top:2rem;\">Publish Campus Listing</button>\n")
               .append("    </form>\n")
               .append("  </div>\n")
               .append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("Sell an Item", "sell", currentUser, unreadNotifs, alertType, alertMsg, content.toString()));
    }

    private void handleCreateListing(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        MultipartParser.ParseResult parseResult = MultipartParser.parse(
                exchange.getRequestBody(),
                contentType,
                "uploads/listings",
                "/uploads/listings"
        );

        Map<String, String> fields = parseResult.getFormFields();
        String title = fields.get("title");
        String desc = fields.get("description");
        String category = fields.get("category");
        String size = fields.get("size");
        String condition = fields.get("condition");
        String brand = fields.get("brand");
        String color = fields.get("color");
        String targetSwap = fields.get("targetSwapCategory");

        double price = 0.0;
        try {
            price = Double.parseDouble(fields.getOrDefault("price", "0.0"));
        } catch (NumberFormatException ignored) {}

        List<String> photos = parseResult.getSavedFileUrls();

        MarketplaceService.ListingResult result = marketplaceService.createListing(
                student, title, desc, category, size, condition, brand, color, price, photos, targetSwap
        );

        if (result.isSuccess()) {
            redirect(exchange, "/listing?id=" + result.getListing().getId());
        } else {
            renderSellPage(exchange, "danger", result.getMessage());
        }
    }

    private void renderEditPage(HttpExchange exchange, String alertType, String alertMsg) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> q = UrlHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String id = q.get("id");
        Listing listing = marketplaceService.getListing(id);

        if (listing == null || (!listing.getSellerId().equals(currentUser.getId()) && !currentUser.canModerate())) {
            redirect(exchange, "/marketplace");
            return;
        }

        ClothingItem item = listing.getItem();
        int unreadNotifs = notificationService.getUnreadCount(currentUser.getId());

        StringBuilder content = new StringBuilder();
        content.append("<div style=\"max-width:760px; margin:1.5rem auto;\">\n")
               .append("  <div class=\"form-card\">\n")
               .append("    <h1 class=\"form-title\">Edit Garment Listing</h1>\n")
               .append("    <p class=\"form-subtitle\">Update listing details for ").append(InputSanitizer.escapeHtml(item.getTitle())).append("</p>\n")
               .append("    <form action=\"/edit-listing\" method=\"POST\">\n")
               .append("      <input type=\"hidden\" name=\"id\" value=\"").append(listing.getId()).append("\">\n")

               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"title\">Garment Title</label>\n")
               .append("        <input type=\"text\" id=\"title\" name=\"title\" class=\"form-input\" value=\"").append(InputSanitizer.escapeHtml(item.getTitle())).append("\" required>\n")
               .append("      </div>\n")

               .append("      <div class=\"form-group\">\n")
               .append("        <label class=\"form-label\" for=\"description\">Description</label>\n")
               .append("        <textarea id=\"description\" name=\"description\" class=\"form-textarea\" required>").append(InputSanitizer.escapeHtml(item.getDescription())).append("</textarea>\n")
               .append("      </div>\n")

               .append("      <div class=\"form-grid-2\">\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"category\">Category</label>\n")
               .append("          <select id=\"category\" name=\"category\" class=\"form-select\">\n")
               .append("            <option value=\"Jackets & Coats\"").append("Jackets & Coats".equals(item.getCategory()) ? " selected" : "").append(">Jackets &amp; Coats</option>\n")
               .append("            <option value=\"Hoodies & Sweaters\"").append("Hoodies & Sweaters".equals(item.getCategory()) ? " selected" : "").append(">Hoodies &amp; Sweaters</option>\n")
               .append("            <option value=\"Shirts & Tops\"").append("Shirts & Tops".equals(item.getCategory()) ? " selected" : "").append(">Shirts &amp; Tops</option>\n")
               .append("            <option value=\"Pants & Jeans\"").append("Pants & Jeans".equals(item.getCategory()) ? " selected" : "").append(">Pants &amp; Jeans</option>\n")
               .append("            <option value=\"Dresses & Skirts\"").append("Dresses & Skirts".equals(item.getCategory()) ? " selected" : "").append(">Dresses &amp; Skirts</option>\n")
               .append("          </select>\n")
               .append("        </div>\n")
               .append("        <div class=\"form-group\">\n")
               .append("          <label class=\"form-label\" for=\"price\">Price (₹)</label>\n")
               .append("          <input type=\"number\" step=\"0.50\" id=\"price\" name=\"price\" class=\"form-input\" value=\"").append(listing.getPrice()).append("\" required min=\"0\">\n")
               .append("        </div>\n")
               .append("      </div>\n")

               .append("      <button type=\"submit\" class=\"btn btn-primary btn-block\">Save Changes</button>\n")
               .append("    </form>\n")
               .append("  </div>\n")
               .append("</div>\n");

        sendHtml(exchange, HtmlRenderer.renderPage("Edit Listing", "marketplace", currentUser, unreadNotifs, alertType, alertMsg, content.toString()));
    }

    private void handleUpdateListing(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String id = form.get("id");
        String title = form.get("title");
        String desc = form.get("description");
        String category = form.get("category");
        double price = 0.0;
        try {
            price = Double.parseDouble(form.getOrDefault("price", "0.0"));
        } catch (NumberFormatException ignored) {}

        MarketplaceService.ListingResult res = marketplaceService.updateListing(
                currentUser, id, title, desc, category, null, null, null, null, price, null, null
        );

        if (res.isSuccess()) {
            redirect(exchange, "/listing?id=" + id);
        } else {
            redirect(exchange, "/profile");
        }
    }

    private void handleDeleteListing(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String id = form.get("id");
        marketplaceService.deleteListing(currentUser, id);
        redirect(exchange, "/profile");
    }

    private void handleMarkSold(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String id = form.get("id");
        marketplaceService.markAsSold(currentUser, id);
        redirect(exchange, "/profile?tab=sold");
    }

    private void handleReportListing(HttpExchange exchange) throws IOException {
        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String id = form.get("id");
        marketplaceService.reportListing(id);
        redirect(exchange, "/listing?id=" + id);
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
