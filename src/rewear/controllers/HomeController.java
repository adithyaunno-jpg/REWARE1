package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import rewear.models.Listing;
import rewear.models.User;
import rewear.security.InputSanitizer;
import rewear.services.*;
import rewear.views.HtmlRenderer;

/**
 * Controller rendering the ReWear Landing Page with real stored listings,
 * environmental metrics, category navigation, and circular workflow overview.
 */
public class HomeController implements HttpHandler {
    private final AuthService authService;
    private final MarketplaceService marketplaceService;
    private final PassportService passportService;
    private final NotificationService notificationService;

    public HomeController(AuthService authService, MarketplaceService marketplaceService,
                          PassportService passportService, NotificationService notificationService) {
        this.authService = authService;
        this.marketplaceService = marketplaceService;
        this.passportService = passportService;
        this.notificationService = notificationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        String token = rewear.server.CookieHelper.parseCookies(exchange).get(rewear.security.SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        int unreadNotifs = currentUser != null ? notificationService.getUnreadCount(currentUser.getId()) : 0;

        PassportService.PlatformImpact impact = passportService.calculateTotalImpact();
        List<Listing> featured = marketplaceService.getFeaturedListings(4);
        List<Listing> recent = marketplaceService.getRecentListings(4);

        StringBuilder content = new StringBuilder();

        // 1. Hero Section
        content.append("<section class=\"hero-section\">\n")
               .append("  <span class=\"hero-badge\">Campus Circular Fashion Movement</span>\n")
               .append("  <h1 class=\"hero-title\">Give Fashion a Second Life.</h1>\n")
               .append("  <p class=\"hero-subtitle\">\n")
               .append("    A trusted, student-curated campus marketplace to buy, sell, and swap pre-loved clothing.\n")
               .append("    Reduce textile waste, save money, and elevate your wardrobe sustainably.\n")
               .append("  </p>\n")
               .append("  <div class=\"hero-cta-group\">\n")
               .append("    <a href=\"/marketplace\" class=\"btn btn-primary btn-lg\">Browse Campus Catalog</a>\n")
               .append("    <a href=\"/sell\" class=\"btn btn-cobalt btn-lg\">Sell Pre-Loved Clothes</a>\n")
               .append("    <a href=\"/swapmatch\" class=\"btn btn-secondary btn-lg\">Explore SwapMatch</a>\n")
               .append("  </div>\n")
               .append("</section>\n");

        // 2. Real Environmental Impact Counter
        content.append("<section class=\"impact-banner\">\n")
               .append("  <div class=\"container\">\n")
               .append("    <div class=\"impact-grid\">\n")
               .append("      <div>\n")
               .append("        <div class=\"impact-number\">").append(impact.totalGarments).append("+</div>\n")
               .append("        <div class=\"impact-label\">Campus Garments Listed</div>\n")
               .append("      </div>\n")
               .append("      <div>\n")
               .append("        <div class=\"impact-number\">").append(impact.totalReuses).append("</div>\n")
               .append("        <div class=\"impact-label\">Circulation Cycles</div>\n")
               .append("      </div>\n")
               .append("      <div>\n")
               .append("        <div class=\"impact-number\">").append(impact.totalCo2Saved).append(" kg</div>\n")
               .append("        <div class=\"impact-label\">CO&sub2; Emissions Prevented</div>\n")
               .append("      </div>\n")
               .append("      <div>\n")
               .append("        <div class=\"impact-number\">").append(String.format("%,.0f", impact.totalWaterSaved)).append(" L</div>\n")
               .append("        <div class=\"impact-label\">Freshwater Conserved</div>\n")
               .append("      </div>\n")
               .append("    </div>\n")
               .append("  </div>\n")
               .append("</section>\n");

        // 3. Clothing Categories
        content.append("<div style=\"margin-top:3.5rem;\">\n")
               .append("  <div class=\"section-header\">\n")
               .append("    <div>\n")
               .append("      <h2 class=\"section-title\">Browse by Category</h2>\n")
               .append("      <p class=\"section-subtitle\">Curated campus collections from fellow students</p>\n")
               .append("    </div>\n")
               .append("    <a href=\"/marketplace\" class=\"btn btn-secondary btn-sm\">View All Categories &rarr;</a>\n")
               .append("  </div>\n")
               .append("  <div class=\"categories-grid\">\n")
               .append(HtmlRenderer.renderCategoryCard("Jackets & Coats", "&#129509;", "Jackets+%26+Coats"))
               .append(HtmlRenderer.renderCategoryCard("Sweaters & Knits", "&#129509;", "Hoodies+%26+Sweaters"))
               .append(HtmlRenderer.renderCategoryCard("Shirts & Tops", "&#128085;", "Shirts+%26+Tops"))
               .append(HtmlRenderer.renderCategoryCard("Pants & Jeans", "&#128086;", "Pants+%26+Jeans"))
               .append(HtmlRenderer.renderCategoryCard("Dresses & Skirts", "&#128087;", "Dresses+%26+Skirts"))
               .append(HtmlRenderer.renderCategoryCard("Shoes & Boots", "&#128095;", "Shoes"))
               .append(HtmlRenderer.renderCategoryCard("Traditional Wear", "&#129507;", "Traditional+Wear"))
               .append(HtmlRenderer.renderCategoryCard("Accessories", "&#128092;", "Accessories"))
               .append("  </div>\n")
               .append("</div>\n");

        // 4. Featured Listings (from stored data)
        content.append("<div style=\"margin-top:2rem;\">\n")
               .append("  <div class=\"section-header\">\n")
               .append("    <div>\n")
               .append("      <h2 class=\"section-title\">Featured Campus Finds</h2>\n")
               .append("      <p class=\"section-subtitle\">Top-rated pieces trending among campus peers</p>\n")
               .append("    </div>\n")
               .append("    <a href=\"/marketplace?sortBy=popular\" class=\"btn btn-secondary btn-sm\">Browse Top Trending &rarr;</a>\n")
               .append("  </div>\n")
               .append("  <div class=\"products-grid\">\n");
        for (Listing l : featured) {
            content.append(HtmlRenderer.renderProductCard(l));
        }
        content.append("  </div>\n")
               .append("</div>\n");

        // 5. How ReWear Works
        content.append("<section class=\"how-it-works-section\">\n")
               .append("  <div class=\"container\">\n")
               .append("    <div style=\"text-align:center; max-width:650px; margin:0 auto;\">\n")
               .append("      <h2 class=\"section-title\">How ReWear Works</h2>\n")
               .append("      <p class=\"section-subtitle\">Zero shipping waste. Direct campus handovers between verified students.</p>\n")
               .append("    </div>\n")
               .append("    <div class=\"steps-grid\">\n")
               .append("      <div class=\"step-card\">\n")
               .append("        <div class=\"step-number\">1</div>\n")
               .append("        <h3 class=\"step-title\">Curate &amp; List</h3>\n")
               .append("        <p class=\"step-desc\">Upload photographs and specifications of your pre-loved clothing in under 2 minutes.</p>\n")
               .append("      </div>\n")
               .append("      <div class=\"step-card\">\n")
               .append("        <div class=\"step-number\">2</div>\n")
               .append("        <h3 class=\"step-title\">Buy or SwapMatch</h3>\n")
               .append("        <p class=\"step-desc\">Purchase affordably or let our algorithm match your piece with compatible peer garments.</p>\n")
               .append("      </div>\n")
               .append("      <div class=\"step-card\">\n")
               .append("        <div class=\"step-number\">3</div>\n")
               .append("        <h3 class=\"step-title\">Campus Handover</h3>\n")
               .append("        <p class=\"step-desc\">Meet safely at the campus library, cafeteria, or union desk for a frictionless exchange.</p>\n")
               .append("      </div>\n")
               .append("      <div class=\"step-card\">\n")
               .append("        <div class=\"step-number\">4</div>\n")
               .append("        <h3 class=\"step-title\">ReWear Passport</h3>\n")
               .append("        <p class=\"step-desc\">Every garment tracks its digital provenance ledger, ownership history, and carbon savings.</p>\n")
               .append("      </div>\n")
               .append("    </div>\n")
               .append("  </div>\n")
               .append("</section>\n");

        // 6. Recently Listed
        content.append("<div style=\"margin-top:2rem;\">\n")
               .append("  <div class=\"section-header\">\n")
               .append("    <div>\n")
               .append("      <h2 class=\"section-title\">Recently Added Garments</h2>\n")
               .append("      <p class=\"section-subtitle\">Freshly uploaded drops from your university network</p>\n")
               .append("    </div>\n")
               .append("    <a href=\"/marketplace?sortBy=newest\" class=\"btn btn-secondary btn-sm\">Explore All Drops &rarr;</a>\n")
               .append("  </div>\n")
               .append("  <div class=\"products-grid\">\n");
        for (Listing l : recent) {
            content.append(HtmlRenderer.renderProductCard(l));
        }
        content.append("  </div>\n")
               .append("</div>\n");

        // 7. Sustainability Callout
        content.append("<div style=\"background:var(--color-sage-light); border:1px solid var(--color-sage); border-radius:var(--radius-lg); padding:2.5rem; text-align:center; margin-top:3rem;\">\n")
               .append("  <h2 style=\"font-size:2rem; margin-bottom:0.75rem; color:var(--color-deep-navy);\">Fast Fashion is Out. Circularity is In.</h2>\n")
               .append("  <p style=\"max-width:700px; margin:0 auto 1.5rem; font-size:1.05rem; color:var(--color-text-primary);\">\n")
               .append("    Over 92 million tons of clothes end up in landfills each year. By rotating garments within our university campuses, ")
               .append("    we extend each piece's lifecycle, save money, and build a trusted student community.\n")
               .append("  </p>\n")
               .append("  <a href=\"/register\" class=\"btn btn-primary btn-lg\">Join Your Campus Exchange</a>\n")
               .append("</div>\n");

        String fullHtml = HtmlRenderer.renderPage("Home", "home", currentUser, unreadNotifs, null, null, content.toString());
        byte[] bytes = fullHtml.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

