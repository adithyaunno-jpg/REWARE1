package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import rewear.models.Listing;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.AuthService;
import rewear.services.MarketplaceService;
import rewear.services.NotificationService;
import rewear.views.HtmlRenderer;

/**
 * Controller managing Marketplace browsing, multi-criteria filtering,
 * keyword searching, and sorting via pure HTML GET parameters.
 */
public class MarketplaceController implements HttpHandler {
    private final AuthService authService;
    private final MarketplaceService marketplaceService;
    private final NotificationService notificationService;

    public MarketplaceController(AuthService authService, MarketplaceService marketplaceService,
                                 NotificationService notificationService) {
        this.authService = authService;
        this.marketplaceService = marketplaceService;
        this.notificationService = notificationService;
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

        Map<String, String> queryParams = UrlHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String q = queryParams.getOrDefault("q", "").trim();
        String category = queryParams.getOrDefault("category", "All").trim();
        String size = queryParams.getOrDefault("size", "All").trim();
        String condition = queryParams.getOrDefault("condition", "All").trim();
        String sortBy = queryParams.getOrDefault("sortBy", "newest").trim();

        double minPrice = 0.0;
        double maxPrice = 0.0;
        try {
            if (queryParams.containsKey("minPrice") && !queryParams.get("minPrice").isEmpty()) {
                minPrice = Double.parseDouble(queryParams.get("minPrice"));
            }
            if (queryParams.containsKey("maxPrice") && !queryParams.get("maxPrice").isEmpty()) {
                maxPrice = Double.parseDouble(queryParams.get("maxPrice"));
            }
        } catch (NumberFormatException ignored) {}

        List<Listing> listings = marketplaceService.searchAndFilter(q, category, size, condition, minPrice, maxPrice, sortBy);

        StringBuilder content = new StringBuilder();

        // Header Section
        content.append("<div class=\"section-header\" style=\"margin-top:0.5rem;\">\n")
               .append("  <div>\n")
               .append("    <h1 class=\"section-title\">Campus Marketplace</h1>\n")
               .append("    <p class=\"section-subtitle\">Showing ").append(listings.size()).append(" verified pre-loved pieces available for campus exchange</p>\n")
               .append("  </div>\n")
               .append("  <a href=\"/sell\" class=\"btn btn-primary btn-sm\">+ List a Garment</a>\n")
               .append("</div>\n");

        // Two-column layout: Filters on left, Products on right
        content.append("<div class=\"marketplace-layout\">\n");

        // Sidebar Filters (Pure HTML GET form)
        content.append("  <aside class=\"filters-sidebar\">\n")
               .append("    <form action=\"/marketplace\" method=\"GET\">\n")
               .append("      <div class=\"filter-title\">\n")
               .append("        <span>Filters &amp; Search</span>\n")
               .append("        <a href=\"/marketplace\" style=\"font-size:0.8rem; font-weight:500;\">Reset</a>\n")
               .append("      </div>\n")

               // Keyword Search
               .append("      <div class=\"filter-group\">\n")
               .append("        <label for=\"q\">Keyword Search</label>\n")
               .append("        <input type=\"text\" id=\"q\" name=\"q\" value=\"").append(InputSanitizer.escapeHtml(q)).append("\" class=\"filter-control\" placeholder=\"e.g. Denim, Cardigan, Blue...\">\n")
               .append("      </div>\n")

               // Category
               .append("      <div class=\"filter-group\">\n")
               .append("        <label for=\"category\">Category</label>\n")
               .append("        <select id=\"category\" name=\"category\" class=\"filter-control\">\n")
               .append(renderOption("All", "All Categories", category))
               .append(renderOption("Jackets & Coats", "Jackets & Coats", category))
               .append(renderOption("Hoodies & Sweaters", "Hoodies & Sweaters", category))
               .append(renderOption("Shirts & Tops", "Shirts & Tops", category))
               .append(renderOption("Pants & Jeans", "Pants & Jeans", category))
               .append(renderOption("Dresses & Skirts", "Dresses & Skirts", category))
               .append(renderOption("Shoes", "Shoes", category))
               .append(renderOption("Traditional Wear", "Traditional Wear", category))
               .append(renderOption("Accessories", "Accessories", category))
               .append("        </select>\n")
               .append("      </div>\n")

               // Size
               .append("      <div class=\"filter-group\">\n")
               .append("        <label for=\"size\">Size</label>\n")
               .append("        <select id=\"size\" name=\"size\" class=\"filter-control\">\n")
               .append(renderOption("All", "All Sizes", size))
               .append(renderOption("XS", "XS (Extra Small)", size))
               .append(renderOption("S", "S (Small)", size))
               .append(renderOption("M", "M (Medium)", size))
               .append(renderOption("L", "L (Large)", size))
               .append(renderOption("XL", "XL (Extra Large)", size))
               .append(renderOption("XXL", "XXL", size))
               .append(renderOption("30", "Waist 30", size))
               .append(renderOption("32", "Waist 32", size))
               .append("        </select>\n")
               .append("      </div>\n")

               // Condition
               .append("      <div class=\"filter-group\">\n")
               .append("        <label for=\"condition\">Condition</label>\n")
               .append("        <select id=\"condition\" name=\"condition\" class=\"filter-control\">\n")
               .append(renderOption("All", "All Conditions", condition))
               .append(renderOption("Brand New with Tags", "Brand New with Tags", condition))
               .append(renderOption("Like New", "Like New", condition))
               .append(renderOption("Gently Used", "Gently Used", condition))
               .append(renderOption("Well Worn", "Well Worn", condition))
               .append("        </select>\n")
               .append("      </div>\n")

               // Price Range
               .append("      <div class=\"filter-group\">\n")
               .append("        <label>Price Range (₹)</label>\n")
               .append("        <div style=\"display:flex; gap:0.5rem; align-items:center;\">\n")
               .append("          <input type=\"number\" step=\"1\" name=\"minPrice\" value=\"").append(minPrice > 0 ? (int)minPrice : "").append("\" placeholder=\"Min\" class=\"filter-control\" style=\"width:50%;\">\n")
               .append("          <span>-</span>\n")
               .append("          <input type=\"number\" step=\"1\" name=\"maxPrice\" value=\"").append(maxPrice > 0 ? (int)maxPrice : "").append("\" placeholder=\"Max\" class=\"filter-control\" style=\"width:50%;\">\n")
               .append("        </div>\n")
               .append("      </div>\n")

               // Sort By
               .append("      <div class=\"filter-group\">\n")
               .append("        <label for=\"sortBy\">Sort By</label>\n")
               .append("        <select id=\"sortBy\" name=\"sortBy\" class=\"filter-control\">\n")
               .append(renderOption("newest", "Newest Listed", sortBy))
               .append(renderOption("price_asc", "Price: Low to High", sortBy))
               .append(renderOption("price_desc", "Price: High to Low", sortBy))
               .append(renderOption("popular", "Most Popular", sortBy))
               .append("        </select>\n")
               .append("      </div>\n")

               .append("      <button type=\"submit\" class=\"btn btn-primary btn-block\">Apply Filters</button>\n")
               .append("    </form>\n")
               .append("  </aside>\n");

        // Product Catalog Results
        content.append("  <section>\n");
        if (listings.isEmpty()) {
            content.append("    <div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:4rem 2rem; text-align:center;\">\n")
                   .append("      <div style=\"font-size:3rem; margin-bottom:1rem;\">&#128269;</div>\n")
                   .append("      <h3 style=\"font-size:1.5rem; margin-bottom:0.5rem;\">No matching garments found</h3>\n")
                   .append("      <p style=\"color:var(--color-text-secondary); margin-bottom:1.5rem;\">Try adjusting your filters, expanding your price range, or searching with broader keywords.</p>\n")
                   .append("      <a href=\"/marketplace\" class=\"btn btn-secondary\">Clear All Filters</a>\n")
                   .append("    </div>\n");
        } else {
            content.append("    <div class=\"products-grid\">\n");
            for (Listing l : listings) {
                content.append(HtmlRenderer.renderProductCard(l));
            }
            content.append("    </div>\n");
        }
        content.append("  </section>\n")
               .append("</div>\n");

        String fullHtml = HtmlRenderer.renderPage("Marketplace", "marketplace", currentUser, unreadNotifs, null, null, content.toString());
        byte[] bytes = fullHtml.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String renderOption(String value, String label, String currentVal) {
        boolean selected = value.equalsIgnoreCase(currentVal);
        return "<option value=\"" + InputSanitizer.escapeHtml(value) + "\"" + (selected ? " selected" : "") + ">" +
                InputSanitizer.escapeHtml(label) + "</option>\n";
    }
}

