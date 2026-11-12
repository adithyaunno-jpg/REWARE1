package rewear.views;

import java.util.List;
import rewear.models.*;
import rewear.security.InputSanitizer;

/**
 * Server-Side HTML Rendering Engine for the ReWear platform.
 * Produces clean, accessible, semantic HTML5 with zero client-side JavaScript.
 * Demonstrates:
 * - String Templating and Component Architecture
 * - Server-Side View Generation
 * - XSS Prevention via Input Sanitization
 */
public class HtmlRenderer {

    /**
     * Wraps body content within the full HTML5 page layout, navbar, and footer.
     */
    public static String renderPage(
            String title,
            String activeNav,
            User currentUser,
            int unreadNotifications,
            String alertType,
            String alertMessage,
            String contentHtml) {

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("  <meta charset=\"UTF-8\">\n")
            .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("  <title>").append(InputSanitizer.escapeHtml(title)).append(" | ReWear - Give Fashion a Second Life</title>\n")
            .append("  <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n")
            .append("  <link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n")
            .append("  <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:ital,wght@0,600;0,700;1,600&display=swap\" rel=\"stylesheet\">\n")
            .append("  <link rel=\"stylesheet\" href=\"/public/css/style.css\">\n")
            .append("</head>\n")
            .append("<body>\n");

        // Global Navigation Bar
        html.append("  <header class=\"navbar\">\n")
            .append("    <div class=\"container nav-container\">\n")
            .append("      <a href=\"/\" class=\"brand-logo\">\n")
            .append("        <span class=\"brand-name\">ReWear</span>\n")
            .append("        <span class=\"brand-tag\">Campus Circular</span>\n")
            .append("      </a>\n")
            .append("      <nav>\n")
            .append("        <ul class=\"nav-links\">\n")
            .append("          <li><a href=\"/marketplace\" class=\"nav-link ").append("marketplace".equals(activeNav) ? "active" : "").append("\">Marketplace</a></li>\n")
.append("          <li><a href=\"/sell\" class=\"nav-link ").append("sell".equals(activeNav) ? "active" : "").append("\">Sell an Item</a></li>\n")
.append("          <li><a href=\"/swapmatch\" class=\"nav-link ").append(("swapmatch".equals(activeNav) || "swapchain".equals(activeNav)) ? "active" : "").append("\">Swap</a></li>\n")
.append("          <li><a href=\"/campus\" class=\"nav-link ").append("campus".equals(activeNav) ? "active" : "").append("\">Campus Circle</a></li>\n");

        if (currentUser != null && currentUser.canModerate()) {
            html.append("          <li><a href=\"/admin\" class=\"nav-link ").append("admin".equals(activeNav) ? "active" : "").append("\">Admin Panel</a></li>\n");
        }

        html.append("        </ul>\n")
            .append("      </nav>\n")
            .append("      <div class=\"nav-actions\">\n");

        if (currentUser != null) {
            // Notifications icon
            html.append("        <a href=\"/notifications\" class=\"nav-icon-link\" title=\"Notifications\">\n")
            .append("          <span class=\"header-svg-icon\">")
            .append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.6\" stroke-linecap=\"round\" stroke-linejoin=\"round\">")
            .append("<path d=\"M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9\"/>")
            .append("<path d=\"M10 21h4\"/>")
            .append("</svg></span>\n");

            if (unreadNotifications > 0) {
                html.append("          <span class=\"badge-count\">")
               .append(unreadNotifications)
               .append("</span>\n");
            }

        html.append("        </a>\n");

            // Shopping Cart icon
        html.append("        <a href=\"/cart\" class=\"nav-icon-link\" title=\"Shopping Cart\">\n")
            .append("          <span class=\"header-svg-icon\">")
            .append("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.6\" stroke-linecap=\"round\" stroke-linejoin=\"round\">")
            .append("<circle cx=\"9\" cy=\"21\" r=\"1\"/>")
            .append("<circle cx=\"20\" cy=\"21\" r=\"1\"/>")
            .append("<path d=\"M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H5\"/>")
            .append("</svg></span>\n")
            .append("          <span id=\"cart-count-badge\" class=\"badge-count\" style=\"display:none;\">0</span>\n")
            .append("        </a>\n");

            // Profile icon and name
            html.append("        <a href=\"/profile\" class=\"btn btn-secondary btn-sm\">")
                .append("<span>&#128100; </span>")
              .append(InputSanitizer.escapeHtml(currentUser.getFullName().split(" ")[0]))
            
                .append("</a>\n");
                
        } else {
            html.append("        <a href=\"/login\" class=\"btn btn-secondary btn-sm\">Sign In</a>\n")
                .append("        <a href=\"/register\" class=\"btn btn-primary btn-sm\">Join ReWear</a>\n");
        }

        html.append("      </div>\n")
            .append("    </div>\n")
            .append("  </header>\n");

        // Main Container
        html.append("  <main class=\"main-content\">\n")
            .append("    <div class=\"container\">\n");

        // Flash Alert if present
        if (alertMessage != null && !alertMessage.trim().isEmpty()) {
            String alertClass = "alert-info";
            if ("success".equalsIgnoreCase(alertType)) alertClass = "alert-success";
            else if ("danger".equalsIgnoreCase(alertType) || "error".equalsIgnoreCase(alertType)) alertClass = "alert-danger";
            else if ("warning".equalsIgnoreCase(alertType)) alertClass = "alert-warning";

            html.append("      <div class=\"alert ").append(alertClass).append("\">")
                .append(InputSanitizer.escapeHtml(alertMessage))
                .append("</div>\n");
        }

        // Page specific content
        html.append(contentHtml);

        html.append("    </div>\n")
            .append("  </main>\n");

        // Footer
        html.append("  <footer class=\"footer\">\n")
            .append("    <div class=\"container\">\n")
            .append("      <div class=\"footer-grid\">\n")
            .append("        <div class=\"footer-brand\">\n")
            .append("          <h3>ReWear</h3>\n")
            .append("          <p>Give Fashion a Second Life.</p>\n")
            .append("          <p style=\"font-size:0.88rem; color:rgba(255,255,255,0.7); margin-top:0.75rem;\">\n")
            .append("            A student-driven circular fashion ecosystem empowering university campuses to buy, sell, and swap pre-loved clothing sustainably.\n")
            .append("          </p>\n")
            .append("        </div>\n")
            .append("        <div>\n")
            .append("          <h4 class=\"footer-title\">Marketplace</h4>\n")
            .append("          <ul class=\"footer-links\">\n")
            .append("            <li><a href=\"/marketplace\">All Garments</a></li>\n")
            .append("            <li><a href=\"/marketplace?category=Jackets+%26+Coats\">Jackets &amp; Coats</a></li>\n")
            .append("            <li><a href=\"/marketplace?category=Hoodies+%26+Sweaters\">Sweaters &amp; Knitwear</a></li>\n")
            .append("            <li><a href=\"/marketplace?category=Shirts+%26+Tops\">Campus Casuals</a></li>\n")
            .append("            <li><a href=\"/sell\">List a Garment</a></li>\n")
            .append("          </ul>\n")
            .append("        </div>\n")
            .append("        <div>\n")
            .append("          <h4 class=\"footer-title\">Circular Exchange</h4>\n")
            .append("          <ul class=\"footer-links\">\n")
            .append("            <li><a href=\"/swapmatch\">Swap</a></li>\n")
            .append("            <li><a href=\"/campus\">Campus Circle Hub</a></li>\n")
            .append("            <li><a href=\"/marketplace?sortBy=popular\">Popular Items</a></li>\n")
            .append("          </ul>\n")
            .append("        </div>\n")
            .append("        <div>\n")
            .append("          <h4 class=\"footer-title\">Academic Project</h4>\n")
            .append("          <p style=\"font-size:0.85rem; color:rgba(255,255,255,0.7);\">\n")
            .append("            <strong>BTech CSE Capstone Project</strong><br>\n")
            .append("            Developed by Team of 5<br>\n")
            .append("            Pure Core Java (JDK 21)<br>\n")
            .append("            Built-in HttpServer &amp; DSA\n")
            .append("          </p>\n")
            .append("        </div>\n")
            .append("      </div>\n")
    
            .append("    </div>\n")
            .append("  </footer>\n");
            
            html.append("<script>\n")
            .append("document.addEventListener('DOMContentLoaded', function() {\n")
            .append("  const badge = document.getElementById('cart-count-badge');\n")
            .append("  if (!badge) return;\n")
            .append("  function updateBadge(count) {\n")
            .append("    badge.textContent = count;\n")
            .append("    badge.style.display = count > 0 ? 'flex' : 'none';\n")
            .append("  }\n")
            .append("  fetch('/cart/count')\n")
            .append("    .then(response => response.json())\n")
            .append("    .then(data => updateBadge(data.count))\n")
           .append("    .catch(error => console.error('Cart count error:', error));\n")
    .append("  document.querySelectorAll('form[action=\"/add-to-cart\"], form[action=\"/remove-from-cart\"]').forEach(form => {\n")
    .append("    form.addEventListener('submit', async function(event) {\n")
    .append("      event.preventDefault();\n")
    .append("      const button = form.querySelector('button[type=\"submit\"]');\n")
    .append("      if (button) button.disabled = true;\n")
    .append("      try {\n")
    .append("        const response = await fetch(form.action, {\n")
    .append("          method: 'POST',\n")
    .append("          body: new URLSearchParams(new FormData(form)),\n")
    .append("          headers: {'X-Requested-With': 'XMLHttpRequest'}\n")
    .append("        });\n")
    .append("        if (!response.ok) throw new Error('Cart request failed');\n")
    .append("        const data = await response.json();\n")
    .append("        updateBadge(data.count);\n")
    .append("        if (form.action.includes('/remove-from-cart')) {\n")
    .append("          window.location.reload();\n")
    .append("        } else if (data.success) {\n")
    .append("          const oldText = button.textContent;\n")
    .append("          button.textContent = 'Added ✓';\n")
    .append("          setTimeout(() => { button.textContent = oldText; }, 1500);\n")
    .append("        } else {\n")
    .append("          alert('This item cannot be added. It may be unavailable or belong to you.');\n")
    .append("        }\n")
    .append("      } catch (error) {\n")
    .append("        alert('Unable to update your cart. Please try again.');\n")
    .append("      } finally {\n")
    .append("        if (button) button.disabled = false;\n")
    .append("      }\n")
    .append("    });\n")
    .append("  });\n")
    .append("});\n")
    .append("</script>\n");
        
            html.append("</body>\n")
            .append("</html>\n");

        return html.toString();
    }

    /**
     * Renders a premium clothing product card.
     */
    public static String renderProductCard(Listing listing) {
        StringBuilder sb = new StringBuilder();
        ClothingItem item = listing.getItem();
        String title = item != null ? item.getTitle() : "Vintage Garment";
        String category = item != null ? item.getCategory() : "Apparel";
        String size = item != null ? item.getSize() : "M";
        String condition = item != null ? item.getCondition() : "Gently Used";
        String primaryPhoto = listing.getPrimaryPhoto();

        sb.append("<div class=\"product-card\">\n")
          .append("  <div class=\"product-image-container\">\n");

        if (primaryPhoto != null && !primaryPhoto.isEmpty()) {
            sb.append("    <img src=\"").append(InputSanitizer.escapeHtml(primaryPhoto))
              .append("\" alt=\"").append(InputSanitizer.escapeHtml(title))
              .append("\" class=\"product-image\">\n");
        } else {
            // Elegant Editorial Placeholder Fallback
            sb.append("    <div class=\"image-fallback\">\n")
              .append("      <div class=\"fallback-icon\">&#128085;</div>\n")
              .append("      <div class=\"fallback-text\">").append(InputSanitizer.escapeHtml(category)).append("</div>\n")
              .append("    </div>\n");
        }

        // Overlay Badges
        sb.append("    <div class=\"product-badge-overlay\">\n");
        if (!listing.isAvailable()) {
            sb.append("      <span class=\"badge badge-sold\">").append(listing.getStatus()).append("</span>\n");
        } else {
            sb.append("      <span class=\"badge badge-condition\">").append(InputSanitizer.escapeHtml(condition)).append("</span>\n")
              .append("      <span class=\"badge badge-size\">Size ").append(InputSanitizer.escapeHtml(size)).append("</span>\n");
        }
        sb.append("    </div>\n")
          .append("  </div>\n");

        // Product Details
       sb.append("  <div class=\"product-details\">\n")
          .append("    <div class=\"product-category-meta\">").append(InputSanitizer.escapeHtml(category)).append("</div>\n")
          .append("    <h3 class=\"product-title\"><a href=\"/listing?id=").append(listing.getId()).append("\">")
          .append(InputSanitizer.escapeHtml(title)).append("</a></h3>\n")
          .append("    <div class=\"product-seller-meta\">\n")
          .append("      <span>&#127891; ").append(InputSanitizer.escapeHtml(listing.getSellerCollege())).append("</span>\n")
          .append("    </div>\n");
        sb.append("    <div class=\"product-card-footer\">\n")
         .append("      <div class=\"product-price\">₹")
          .append(String.format("%.2f", listing.getPrice()))
          .append("</div>\n");

        if (listing.isAvailable()) {
            sb.append("      <form action=\"/add-to-cart\" method=\"POST\" class=\"add-to-cart-form\">\n")
              .append("        <input type=\"hidden\" name=\"listingId\" value=\"")
              .append(InputSanitizer.escapeHtml(listing.getId()))
              .append("\">\n")
              .append("        <button type=\"submit\" class=\"btn btn-primary btn-sm\">Add to Cart</button>\n")
              .append("      </form>\n");
        }

        sb.append("      <a href=\"/listing?id=")
          .append(listing.getId())
          .append("\" class=\"btn btn-secondary btn-sm view-details-btn\">View Details</a>\n")
          .append("    </div>\n")
          .append("  </div>\n")
          .append("</div>\n");
                return sb.toString();
    }

    /**
     * Renders a category navigation button.
     */
    public static String renderCategoryCard(String name, String icon, String categoryParam) {
        return "<a href=\"/marketplace?category=" + categoryParam + "\" class=\"category-card\">\n" +
               "  <span class=\"cat-icon\">" + icon + "</span>\n" +
               "  <span class=\"cat-name\">" + InputSanitizer.escapeHtml(name) + "</span>\n" +
               "</a>\n";
    }

    /**
     * Renders star rating display string.
     */
    public static String renderStars(double rating) {
        int fullStars = (int) Math.round(rating);
        StringBuilder sb = new StringBuilder("<span class=\"stars\" title=\"Rating: " + rating + "/5\">");
        for (int i = 0; i < 5; i++) {
            if (i < fullStars) {
                sb.append("★");
            } else {
                sb.append("☆");
            }
        }
        sb.append("</span>");
        return sb.toString();
    }
}

