package rewear.server;

import com.sun.net.httpserver.HttpExchange;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for parsing and setting HTTP cookies via native com.sun.net.httpserver headers.
 */
public class CookieHelper {

    public static Map<String, String> parseCookies(HttpExchange exchange) {
        Map<String, String> cookies = new HashMap<>();
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null && !cookieHeader.trim().isEmpty()) {
            String[] pairs = cookieHeader.split(";");
            for (String pair : pairs) {
                String[] kv = pair.trim().split("=", 2);
                if (kv.length == 2) {
                    cookies.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        return cookies;
    }

    public static String getCookieValue(HttpExchange exchange, String name) {
        return parseCookies(exchange).get(name);
    }

    public static void setCookie(HttpExchange exchange, String name, String value, int maxAgeSeconds) {
        String cookieValue = String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax", name, value, maxAgeSeconds);
        exchange.getResponseHeaders().add("Set-Cookie", cookieValue);
    }

    public static void deleteCookie(HttpExchange exchange, String name) {
        String cookieValue = String.format("%s=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax", name);
        exchange.getResponseHeaders().add("Set-Cookie", cookieValue);
    }
}

