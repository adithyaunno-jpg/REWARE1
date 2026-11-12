package rewear.server;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rewear.controllers.*;
import rewear.security.SessionManager;
import rewear.services.*;
import rewear.storage.DataStore;

/**
 * HTTP Server Manager initializing com.sun.net.httpserver.HttpServer,
 * routing context mappings, concurrent request thread pool executor,
 * and graceful lifecycle management.
 */
public class HttpServerManager {
    private final int port;
    private HttpServer server;
    private ExecutorService executor;

    private final DataStore dataStore;
    private final SessionManager sessionManager;
    private final AuthService authService;
    private final MarketplaceService marketplaceService;
    private final PurchaseService purchaseService;
    private final ReviewService reviewService;
    private final SwapService swapService;
    private final PassportService passportService;
    private final NotificationService notificationService;
    private final AdminService adminService;

    public HttpServerManager(int port, String dataDirectory) {
        this.port = port;
        this.dataStore = new DataStore(dataDirectory);
        this.sessionManager = new SessionManager();

        this.authService = new AuthService(dataStore, sessionManager);
        this.marketplaceService = new MarketplaceService(dataStore);
        this.purchaseService = new PurchaseService(dataStore);
        this.reviewService = new ReviewService(dataStore);
        this.swapService = new SwapService(dataStore);
        this.passportService = new PassportService(dataStore);
        this.notificationService = new NotificationService(dataStore);
        this.adminService = new AdminService(dataStore);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        executor = Executors.newFixedThreadPool(12);
        server.setExecutor(executor);

        // Static Asset Handlers
        server.createContext("/public", new StaticFileHandler("public", "/public"));
        server.createContext("/uploads", new StaticFileHandler("uploads", "/uploads"));

        // Page Controllers
        HomeController homeController = new HomeController(authService, marketplaceService, passportService, notificationService);
        server.createContext("/", homeController);

        MarketplaceController marketplaceController = new MarketplaceController(authService, marketplaceService, notificationService);
        server.createContext("/marketplace", marketplaceController);

        ListingController listingController = new ListingController(authService, marketplaceService, notificationService);
        server.createContext("/listing", listingController);
        server.createContext("/sell", listingController);
        server.createContext("/edit-listing", listingController);
        server.createContext("/delete-listing", listingController);
        server.createContext("/mark-sold", listingController);
        server.createContext("/report-listing", listingController);

        PurchaseController purchaseController = new PurchaseController(authService, marketplaceService, purchaseService, notificationService);
        server.createContext("/purchase", purchaseController);

        CartService cartService = new CartService(dataStore);

        CartController cartController = new CartController(authService, cartService, notificationService);
        server.createContext("/cart", cartController);
        server.createContext("/cart/count", cartController);
        server.createContext("/add-to-cart", cartController);
        server.createContext("/remove-from-cart", cartController);

       CheckoutController checkoutController =
        new CheckoutController(
                authService,
                cartService,
                purchaseService,
                notificationService,
                dataStore
        );

        server.createContext("/checkout", checkoutController);

        ReviewController reviewController = new ReviewController(authService, reviewService, dataStore, notificationService);
        server.createContext("/review", reviewController);

        

        ProfileController profileController = new ProfileController(authService, marketplaceService, purchaseService, reviewService, notificationService, dataStore);
        server.createContext("/profile", profileController);
        server.createContext("/edit-profile", profileController);

        SwapController swapController = new SwapController(authService, marketplaceService, swapService, notificationService, dataStore);
        server.createContext("/swapmatch", swapController);
        server.createContext("/swapchain", swapController);
        server.createContext("/propose-swap", swapController);
        server.createContext("/accept-swap", swapController);
        server.createContext("/reject-swap", swapController);
        server.createContext("/swapchain-vote", swapController);

        PassportController passportController = new PassportController(authService, passportService, notificationService);
        server.createContext("/passport", passportController);
        server.createContext("/add-passport-note", passportController);

        CampusController campusController = new CampusController(authService, notificationService, dataStore);
        server.createContext("/campus", campusController);

        NotificationController notificationController = new NotificationController(authService, notificationService);
        server.createContext("/notifications", notificationController);
        server.createContext("/notifications/read", notificationController);
        server.createContext("/notifications/read-all", notificationController);

        AuthController authController = new AuthController(authService);
        server.createContext("/login", authController);
        server.createContext("/register", authController);
        server.createContext("/logout", authController);

        AdminController adminController = new AdminController(authService, adminService, notificationService, dataStore);
        server.createContext("/admin", adminController);
        server.createContext("/admin/toggle-user", adminController);
        server.createContext("/admin/delete-listing", adminController);
        server.createContext("/admin/dismiss-report", adminController);

        server.start();

        System.out.println("==================================================");
        System.out.println("   ReWear: Sustainable Campus Fashion Marketplace");
        System.out.println("   Give Fashion a Second Life.");
        System.out.println("==================================================");
        System.out.println(" [Server Status]  HTTP Server running successfully");
        System.out.println(" [Access URL]     http://localhost:" + port);
        System.out.println(" [Core Backend]   Java built-in com.sun.net.httpserver");
        System.out.println(" [Technology]     Pure Core Java 21, HTML5, CSS3");
        System.out.println(" [Design System]  Warm Ivory, Deep Navy, Muted Sage");
        System.out.println(" [Security]       PBKDF2 Hashing, SecureRandom, Sessions");
        System.out.println(" [Barter AI]      SwapMatch & SwapChain Cycle Detection");
        System.out.println("==================================================");
        System.out.println(" Press Ctrl+C in terminal to stop server.");
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
        if (executor != null) {
            executor.shutdown();
        }
        dataStore.saveAll();
        System.out.println("[Server Status] ReWear Server terminated gracefully.");
    }

    public DataStore getDataStore() {
        return dataStore;
    }
}

