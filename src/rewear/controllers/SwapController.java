package rewear.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import rewear.algorithms.SwapMatchEngine;
import rewear.models.*;
import rewear.security.InputSanitizer;
import rewear.security.SessionManager;
import rewear.server.CookieHelper;
import rewear.server.UrlHelper;
import rewear.services.*;
import rewear.storage.DataStore;
import rewear.views.HtmlRenderer;

/**
 * Controller managing 1-to-1 SwapMatch recommendations and multi-party SwapChain cycle resolution.
 * Demonstrates:
 * - Matching Algorithm UI
 * - Graph Cycle Visualization
 * - Consensus Agreement Flow
 */
public class SwapController implements HttpHandler {
    private final AuthService authService;
    private final MarketplaceService marketplaceService;
    private final SwapService swapService;
    private final NotificationService notificationService;
    private final DataStore dataStore;

    public SwapController(AuthService authService, MarketplaceService marketplaceService,
                          SwapService swapService, NotificationService notificationService,
                          DataStore dataStore) {
        this.authService = authService;
        this.marketplaceService = marketplaceService;
        this.swapService = swapService;
        this.notificationService = notificationService;
        this.dataStore = dataStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/swapmatch".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderSwapMatchPage(exchange);
            }
        } else if ("/swapchain".equalsIgnoreCase(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                renderSwapChainPage(exchange);
            }
        } else if ("/propose-swap".equalsIgnoreCase(path)) {
            handleProposeSwap(exchange);
        } else if ("/accept-swap".equalsIgnoreCase(path)) {
            handleAcceptSwap(exchange);
        } else if ("/reject-swap".equalsIgnoreCase(path)) {
            handleRejectSwap(exchange);
        } else if ("/swapchain-vote".equalsIgnoreCase(path)) {
            handleSwapChainVote(exchange);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void renderSwapMatchPage(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        int unreadNotifs = notificationService.getUnreadCount(student.getId());
        List<Listing> studentListings = marketplaceService.getActiveListingsBySeller(student.getId());
        List<SwapRequest> userSwapRequests = swapService.getSwapRequestsForUser(student.getId());

        Map<String, String> q = UrlHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String selectedListingId = q.get("myListing");
        if ((selectedListingId == null || selectedListingId.isEmpty()) && !studentListings.isEmpty()) {
            selectedListingId = studentListings.get(0).getId();
        }

        Listing currentListing = null;
        if (selectedListingId != null) {
            currentListing = dataStore.getListingById(selectedListingId);
        }

        StringBuilder content = new StringBuilder();

        content.append("<div class=\"section-header\" style=\"margin-top:0.5rem;\">\n")
               .append("  <div>\n")
               .append("    <h1 class=\"section-title\">SwapMatch Algorithm</h1>\n")
               .append("    <p class=\"section-subtitle\">Multi-attribute compatibility scoring based on garment category, sizing, condition, and value</p>\n")
               .append("  </div>\n")
               .append("  <a href=\"/swapchain\" class=\"btn btn-cobalt btn-sm\">Explore Multi-Person SwapChain &rarr;</a>\n")
               .append("</div>\n");

        // Swap Requests Section (Incoming & Outgoing)
        if (!userSwapRequests.isEmpty()) {
            content.append("<div style=\"margin-bottom:3rem;\">\n")
                   .append("  <h2 style=\"font-size:1.4rem; margin-bottom:1rem;\">Your Active Swap Proposals</h2>\n")
                   .append("  <div class=\"data-table-container\">\n")
                   .append("    <table class=\"data-table\">\n")
                   .append("      <thead>\n")
                   .append("        <tr>\n")
                   .append("          <th>Exchange Overview</th>\n")
                   .append("          <th>Offering Garment</th>\n")
                   .append("          <th>Target Garment</th>\n")
                   .append("          <th>Compatibility</th>\n")
                   .append("          <th>Status</th>\n")
                   .append("          <th>Action</th>\n")
                   .append("        </tr>\n")
                   .append("      </thead>\n")
                   .append("      <tbody>\n");

            for (SwapRequest sr : userSwapRequests) {
                boolean isRecipient = sr.getRecipientId().equals(student.getId());
                content.append("        <tr>\n")
                       .append("          <td>\n")
                       .append("            <strong>").append(isRecipient ? "From: " + InputSanitizer.escapeHtml(sr.getRequesterName()) : "To: " + InputSanitizer.escapeHtml(sr.getRecipientName())).append("</strong><br>\n")
                       .append("            <span style=\"font-size:0.8rem; color:var(--color-text-secondary);\">").append(InputSanitizer.escapeHtml(sr.getMessage())).append("</span>\n")
                       .append("          </td>\n")
                       .append("          <td><a href=\"/listing?id=").append(sr.getRequesterListingId()).append("\">").append(InputSanitizer.escapeHtml(sr.getRequesterItemTitle())).append("</a></td>\n")
                       .append("          <td><a href=\"/listing?id=").append(sr.getRecipientListingId()).append("\">").append(InputSanitizer.escapeHtml(sr.getRecipientItemTitle())).append("</a></td>\n")
                       .append("          <td><span class=\"badge badge-size\" style=\"font-weight:700;\">").append(sr.getCompatibilityScore()).append("% Match</span></td>\n")
                       .append("          <td><span class=\"badge ").append("ACCEPTED".equals(sr.getStatus()) ? "badge-condition" : "badge-size").append("\">").append(sr.getStatus()).append("</span></td>\n")
                       .append("          <td>\n");

                if (isRecipient && "PENDING".equalsIgnoreCase(sr.getStatus())) {
                    content.append("            <form action=\"/accept-swap\" method=\"POST\" style=\"display:inline;\">\n")
                           .append("              <input type=\"hidden\" name=\"swapId\" value=\"").append(sr.getId()).append("\">\n")
                           .append("              <button type=\"submit\" class=\"btn btn-primary btn-sm\">Accept Swap</button>\n")
                           .append("            </form>\n")
                           .append("            <form action=\"/reject-swap\" method=\"POST\" style=\"display:inline;\">\n")
                           .append("              <input type=\"hidden\" name=\"swapId\" value=\"").append(sr.getId()).append("\">\n")
                           .append("              <button type=\"submit\" class=\"btn btn-secondary btn-sm\">Decline</button>\n")
                           .append("            </form>\n");
                } else {
                    content.append("            <span style=\"font-size:0.85rem; color:var(--color-text-secondary);\">No action needed</span>\n");
                }

                content.append("          </td>\n")
                       .append("        </tr>\n");
            }

            content.append("      </tbody>\n")
                   .append("    </table>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
        }

        // Garment Selector for matching
        if (studentListings.isEmpty()) {
            content.append("<div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:3rem; text-align:center;\">\n")
                   .append("  <h3>List a Garment to Unlock SwapMatch</h3>\n")
                   .append("  <p style=\"color:var(--color-text-secondary); margin:0.5rem 0 1.5rem;\">You need at least one active clothing listing in your campus wardrobe to compute swap recommendations.</p>\n")
                   .append("  <a href=\"/sell\" class=\"btn btn-primary\">+ List an Item for Swap</a>\n")
                   .append("</div>\n");
        } else {
            content.append("<div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:1.5rem; margin-bottom:2rem;\">\n")
                   .append("  <form action=\"/swapmatch\" method=\"GET\" style=\"display:flex; align-items:center; gap:1rem; flex-wrap:wrap;\">\n")
                   .append("    <label for=\"myListing\" style=\"font-weight:700;\">Select one of your pieces to find matches for:</label>\n")
                   .append("    <select id=\"myListing\" name=\"myListing\" class=\"form-select\" style=\"max-width:350px;\">\n");

            for (Listing l : studentListings) {
                content.append("      <option value=\"").append(l.getId()).append("\"").append(l.getId().equals(selectedListingId) ? " selected" : "").append(">")
                       .append(InputSanitizer.escapeHtml(l.getItem().getTitle())).append(" (Size ").append(l.getItem().getSize()).append(" - ₹").append(String.format("%.0f", l.getPrice())).append(")")
                       .append("</option>\n");
            }

            content.append("    </select>\n")
                   .append("    <button type=\"submit\" class=\"btn btn-primary btn-sm\">Recalculate Matches</button>\n")
                   .append("  </form>\n")
                   .append("</div>\n");

            // Recommendations List
            if (currentListing != null) {
                List<SwapMatchEngine.MatchResult> recommendations = swapService.getRecommendations(currentListing, 6);

                content.append("<h2 style=\"font-size:1.4rem; margin-bottom:1.25rem;\">Recommended Swaps for '").append(InputSanitizer.escapeHtml(currentListing.getItem().getTitle())).append("'</h2>\n");

                if (recommendations.isEmpty()) {
                    content.append("<p>No other compatible garments currently available in the campus network. Check back soon!</p>\n");
                } else {
                    for (SwapMatchEngine.MatchResult match : recommendations) {
                        Listing candidate = match.getCandidateListing();
                        content.append("<div class=\"match-card\">\n")
                               // Candidate item summary
                               .append("  <div style=\"display:flex; gap:1.25rem; align-items:center;\">\n");

                        if (!candidate.getPhotoUrls().isEmpty()) {
                            content.append("    <img src=\"").append(InputSanitizer.escapeHtml(candidate.getPrimaryPhoto())).append("\" style=\"width:80px; height:95px; object-fit:cover; border-radius:var(--radius-sm);\">\n");
                        } else {
                            content.append("    <div style=\"width:80px; height:95px; background:var(--color-soft-ivory); display:flex; align-items:center; justify-content:center; font-size:1.5rem; border-radius:var(--radius-sm);\">&#128085;</div>\n");
                        }

                        content.append("    <div>\n")
                               .append("      <span class=\"badge badge-size\">").append(InputSanitizer.escapeHtml(candidate.getItem().getCategory())).append("</span>\n")
                               .append("      <h3 style=\"font-size:1.15rem; margin:0.25rem 0;\"><a href=\"/listing?id=").append(candidate.getId()).append("\">").append(InputSanitizer.escapeHtml(candidate.getItem().getTitle())).append("</a></h3>\n")
                               .append("      <div style=\"font-size:0.85rem; color:var(--color-text-secondary);\">\n")
                               .append("        Seller: <strong>").append(InputSanitizer.escapeHtml(candidate.getSellerName())).append("</strong> &bull; ").append(InputSanitizer.escapeHtml(candidate.getSellerCollege())).append("<br>\n")
                               .append("        Size ").append(InputSanitizer.escapeHtml(candidate.getItem().getSize())).append(" &bull; ").append(InputSanitizer.escapeHtml(candidate.getItem().getCondition())).append(" &bull; Estimated ₹").append(String.format("%.0f", candidate.getPrice())).append("\n")
                               .append("      </div>\n")
                               .append("    </div>\n")
                               .append("  </div>\n")

                               // Match Score Badge
                               .append("  <div class=\"match-score-badge\">\n")
                               .append("    <div class=\"match-score-pct\">").append(match.getScorePercentage()).append("%</div>\n")
                               .append("    <div class=\"match-score-label\">Match Score</div>\n")
                               .append("  </div>\n")

                               // Natural Language Compatibility Reason
                               .append("  <div style=\"font-size:0.9rem; color:var(--color-text-secondary); line-height:1.5; padding:0 1rem; border-left:1px solid var(--color-border);\">\n")
                               .append("    <strong>Algorithm Insight:</strong><br>\n")
                               .append("    ").append(InputSanitizer.escapeHtml(match.getMatchReason())).append("\n")
                               .append("  </div>\n")

                               // Propose Swap Form
                               .append("  <div>\n")
                               .append("    <form action=\"/propose-swap\" method=\"POST\">\n")
                               .append("      <input type=\"hidden\" name=\"requesterListingId\" value=\"").append(currentListing.getId()).append("\">\n")
                               .append("      <input type=\"hidden\" name=\"recipientListingId\" value=\"").append(candidate.getId()).append("\">\n")
                               .append("      <button type=\"submit\" class=\"btn btn-primary btn-sm\">Propose Swap</button>\n")
                               .append("    </form>\n")
                               .append("  </div>\n")

                               .append("</div>\n");
                    }
                }
            }
        }

        sendHtml(exchange, HtmlRenderer.renderPage("SwapMatch Recommendations", "swapmatch", student, unreadNotifs, null, null, content.toString()));
    }

    private void renderSwapChainPage(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        int unreadNotifs = notificationService.getUnreadCount(student.getId());
        List<SwapChain> chains = dataStore.getAllSwapChains();

        StringBuilder content = new StringBuilder();
        content.append("<div class=\"section-header\" style=\"margin-top:0.5rem;\">\n")
               .append("  <div>\n")
               .append("    <h1 class=\"section-title\">SwapChain: Circular Multi-Person Barter</h1>\n")
               .append("    <p class=\"section-subtitle\">Graph cycle detection connects 3 or more students in a zero-cash clothing rotation loop</p>\n")
               .append("  </div>\n")
               .append("  <a href=\"/swapmatch\" class=\"btn btn-secondary btn-sm\">&larr; 1-to-1 SwapMatch</a>\n")
               .append("</div>\n");

        content.append("<div style=\"background:var(--color-warm-ivory); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:1.5rem; margin-bottom:2.5rem;\">\n")
               .append("  <h3 style=\"margin-bottom:0.4rem;\">&#128260; How SwapChain Cycle Detection Works</h3>\n")
               .append("  <p style=\"font-size:0.95rem; color:var(--color-text-secondary); margin-bottom:0.5rem;\">\n")
               .append("    When Student A wants Student B's item, Student B wants Student C's item, and Student C wants Student A's item, ")
               .append("    no traditional 1-to-1 swap is possible. ReWear executes a <strong>Depth-First Search (DFS) Cycle Detection Algorithm</strong> ")
               .append("    over the directed preference graph to synthesize multi-person closed barter rings.\n")
               .append("  </p>\n")
               .append("  <p style=\"font-size:0.85rem; color:var(--color-sage-dark); font-weight:600; margin:0;\">\n")
               .append("    &bull; Requires consensus approval from 100% of cycle participants before ownership transfers.\n")
               .append("  </p>\n")
               .append("</div>\n");

        if (chains.isEmpty()) {
            content.append("<div style=\"background:var(--color-white); border:1px solid var(--color-border); border-radius:var(--radius-md); padding:3rem; text-align:center;\">\n")
                   .append("  <h3>No active SwapChain cycles currently formed</h3>\n")
                   .append("  <p style=\"color:var(--color-text-secondary);\">List more items and define swap preferences to trigger graph cycle detection.</p>\n")
                   .append("</div>\n");
        } else {
            for (SwapChain chain : chains) {
                boolean isParticipant = chain.isParticipant(student.getId());
                String myVote = isParticipant ? chain.getVoteStatus(student.getId()) : "N/A";

                content.append("<div class=\"swapchain-circle-card\">\n")
                       .append("  <div style=\"display:flex; justify-content:space-between; align-items:flex-start;\">\n")
                       .append("    <div>\n")
                       .append("      <span class=\"badge badge-size\">Chain ID: ").append(chain.getId()).append("</span>\n")
                       .append("      <h2 style=\"font-size:1.4rem; margin:0.35rem 0;\">").append(InputSanitizer.escapeHtml(chain.getDescription())).append("</h2>\n")
                       .append("      <div style=\"font-size:0.85rem; color:var(--color-text-secondary);\">Created on ").append(chain.getCreatedAt()).append("</div>\n")
                       .append("    </div>\n")
                       .append("    <div>\n")
                       .append("      <span class=\"badge ").append("COMPLETED".equals(chain.getStatus()) ? "badge-condition" : "badge-size").append("\" style=\"font-size:0.95rem; font-weight:700;\">")
                       .append(chain.getStatus()).append("</span>\n")
                       .append("    </div>\n")
                       .append("  </div>\n");

                // Cycle Flow Nodes
                content.append("  <div class=\"chain-steps-flow\">\n");
                List<String> partNames = chain.getParticipantNames();
                List<String> titles = chain.getItemTitles();

                for (int i = 0; i < partNames.size(); i++) {
                    String studentName = partNames.get(i);
                    String itemTitle = titles.get(i);
                    String voteStatus = chain.getApprovals().get(chain.getParticipantIds().get(i));

                    content.append("    <div class=\"chain-node\">\n")
                           .append("      <div style=\"font-size:0.75rem; text-transform:uppercase; color:var(--color-text-secondary); font-weight:700;\">Student Participant ").append(i + 1).append("</div>\n")
                           .append("      <div style=\"font-weight:700; font-size:1.05rem; color:var(--color-deep-navy); margin:0.25rem 0;\">").append(InputSanitizer.escapeHtml(studentName)).append("</div>\n")
                           .append("      <div style=\"font-size:0.85rem; color:var(--color-cobalt); margin-bottom:0.5rem;\">Contributes: <em>").append(InputSanitizer.escapeHtml(itemTitle)).append("</em></div>\n")
                           .append("      <div><span class=\"badge ").append("ACCEPTED".equals(voteStatus) ? "badge-condition" : "badge-sold").append("\">Vote: ").append(voteStatus != null ? voteStatus : "PENDING").append("</span></div>\n")
                           .append("    </div>\n");

                    if (i < partNames.size() - 1) {
                        content.append("    <div class=\"chain-arrow\">&rarr;</div>\n");
                    } else {
                        content.append("    <div class=\"chain-arrow\" style=\"color:var(--color-sage-dark); font-size:1.2rem; font-weight:700;\">&#8635; Loops to #1</div>\n");
                    }
                }
                content.append("  </div>\n");

                // Participant Action Buttons
                if (isParticipant && "PENDING".equalsIgnoreCase(chain.getStatus())) {
                    content.append("  <div style=\"padding-top:1.25rem; border-top:1px solid var(--color-border); display:flex; align-items:center; justify-content:space-between;\">\n")
                           .append("    <div>\n")
                           .append("      <strong>Your Consensus Status:</strong> ").append(myVote).append("\n")
                           .append("    </div>\n")
                           .append("    <div style=\"display:flex; gap:0.75rem;\">\n")
                           .append("      <form action=\"/swapchain-vote\" method=\"POST\" style=\"display:inline;\">\n")
                           .append("        <input type=\"hidden\" name=\"chainId\" value=\"").append(chain.getId()).append("\">\n")
                           .append("        <input type=\"hidden\" name=\"vote\" value=\"accept\">\n")
                           .append("        <button type=\"submit\" class=\"btn btn-primary btn-sm\">Approve SwapChain Agreement</button>\n")
                           .append("      </form>\n")
                           .append("      <form action=\"/swapchain-vote\" method=\"POST\" style=\"display:inline;\">\n")
                           .append("        <input type=\"hidden\" name=\"chainId\" value=\"").append(chain.getId()).append("\">\n")
                           .append("        <input type=\"hidden\" name=\"vote\" value=\"reject\">\n")
                           .append("        <button type=\"submit\" class=\"btn btn-secondary btn-sm\">Decline</button>\n")
                           .append("      </form>\n")
                           .append("    </div>\n")
                           .append("  </div>\n");
                }

                content.append("</div>\n");
            }
        }

        sendHtml(exchange, HtmlRenderer.renderPage("SwapChain Barter Rings", "swapchain", student, unreadNotifs, null, null, content.toString()));
    }

    private void handleProposeSwap(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String reqListId = form.get("requesterListingId");
        String recListId = form.get("recipientListingId");
        String msg = form.getOrDefault("message", "Proposed via ReWear SwapMatch algorithm");

        swapService.createSwapRequest(student, reqListId, recListId, msg);
        redirect(exchange, "/swapmatch");
    }

    private void handleAcceptSwap(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String swapId = form.get("swapId");
        swapService.acceptSwapRequest(currentUser, swapId);
        redirect(exchange, "/swapmatch");
    }

    private void handleRejectSwap(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (currentUser == null) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String swapId = form.get("swapId");
        swapService.rejectSwapRequest(currentUser, swapId);
        redirect(exchange, "/swapmatch");
    }

    private void handleSwapChainVote(HttpExchange exchange) throws IOException {
        String token = CookieHelper.getCookieValue(exchange, SessionManager.COOKIE_NAME);
        User currentUser = authService.getAuthenticatedUser(token);
        if (!(currentUser instanceof Student student)) {
            redirect(exchange, "/login");
            return;
        }

        Map<String, String> form = UrlHelper.parseFormBody(exchange.getRequestBody());
        String chainId = form.get("chainId");
        boolean accept = "accept".equalsIgnoreCase(form.get("vote"));

        swapService.recordChainVote(student, chainId, accept);
        redirect(exchange, "/swapchain");
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
