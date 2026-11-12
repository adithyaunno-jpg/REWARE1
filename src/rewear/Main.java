package rewear;

import java.io.IOException;
import rewear.server.HttpServerManager;

/**
 * Main Application Entry Point for ReWear.
 * Demonstrates:
 * - Application bootstrapping
 * - Shutdown hooks for transactional persistence
 * - Zero external framework runtime
 */
public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static final String DATA_DIRECTORY = "data";

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("[Warning] Invalid port argument; defaulting to " + DEFAULT_PORT);
            }
        }

        HttpServerManager serverManager = new HttpServerManager(port, DATA_DIRECTORY);

        // Register Graceful Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown Hook] Commencing graceful server shutdown...");
            serverManager.stop();
        }));

        try {
            serverManager.start();
        } catch (IOException e) {
            System.err.println("[Fatal Error] Failed to start ReWear HttpServer on port " + port + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

