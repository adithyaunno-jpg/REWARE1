package rewear.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import rewear.models.Listing;

/**
 * High-performance Searching and Sorting algorithms for the ReWear Marketplace.
 * Demonstrates:
 * - Method Overloading (multiple filter/search signatures)
 * - Custom Comparators (Price asc/desc, Date newest, Views)
 * - Binary Search implementation for viva examination
 * - Predicate filtering
 */
public class SearchAndSort {

    /**
     * Filters and sorts listings based on multi-criteria parameters.
     */
    public static List<Listing> filterAndSort(
            List<Listing> input,
            String query,
            String category,
            String size,
            String condition,
            double minPrice,
            double maxPrice,
            String sortBy) {

        List<Listing> filtered = new ArrayList<>();

        for (Listing item : input) {
            // Only available items for general browsing (unless querying specific view)
            if (!item.isAvailable()) {
                continue;
            }

            // Keyword match
            if (query != null && !query.trim().isEmpty()) {
                if (!item.matchesKeyword(query)) {
                    continue;
                }
            }

            // Category filter
            if (category != null && !category.equalsIgnoreCase("All") && !category.trim().isEmpty()) {
                if (item.getItem() == null || !category.equalsIgnoreCase(item.getItem().getCategory())) {
                    continue;
                }
            }

            // Size filter
            if (size != null && !size.equalsIgnoreCase("All") && !size.trim().isEmpty()) {
                if (item.getItem() == null || !size.equalsIgnoreCase(item.getItem().getSize())) {
                    continue;
                }
            }

            // Condition filter
            if (condition != null && !condition.equalsIgnoreCase("All") && !condition.trim().isEmpty()) {
                if (item.getItem() == null || !condition.equalsIgnoreCase(item.getItem().getCondition())) {
                    continue;
                }
            }

            // Price boundaries
            if (minPrice > 0 && item.getPrice() < minPrice) {
                continue;
            }
            if (maxPrice > 0 && item.getPrice() > maxPrice) {
                continue;
            }

            filtered.add(item);
        }

        // Apply Sorting using custom Comparators
        if (sortBy == null || sortBy.trim().isEmpty() || "newest".equalsIgnoreCase(sortBy)) {
            // Newest first (descending by createdAt or ID)
            filtered.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        } else if ("price_asc".equalsIgnoreCase(sortBy)) {
            filtered.sort(Comparator.comparingDouble(Listing::getPrice));
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            filtered.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
        } else if ("popular".equalsIgnoreCase(sortBy)) {
            filtered.sort((a, b) -> Integer.compare(b.getViewCount(), a.getViewCount()));
        }

        return filtered;
    }

    /**
     * Binary Search to find a listing matching an exact price target.
     * Demonstrates classic Divide-and-Conquer $O(\log n)$ search on sorted listings.
     * Required for academic viva demonstration.
     */
    public static int binarySearchByPrice(List<Listing> sortedByPrice, double targetPrice, double tolerance) {
        int low = 0;
        int high = sortedByPrice.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            double midPrice = sortedByPrice.get(mid).getPrice();

            if (Math.abs(midPrice - targetPrice) <= tolerance) {
                return mid; // match found
            } else if (midPrice < targetPrice) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1; // Not found
    }
}

