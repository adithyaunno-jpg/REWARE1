package rewear.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import rewear.models.Listing;

/**
 * Multi-Attribute Compatibility Engine for clothing barter (SwapMatch).
 * Mathematical compatibility score based on:
 * - Category Preference (35%)
 * - Size Compatibility (25%)
 * - Condition Parity (20%)
 * - Valuation Proximity (20%)
 * Demonstrates Weighted Scoring, Algorithm Design, and Natural Language Synthesis.
 */
public class SwapMatchEngine {

    public static class MatchResult {
        private final Listing candidateListing;
        private final int scorePercentage;
        private final String matchReason;

        public MatchResult(Listing candidateListing, int scorePercentage, String matchReason) {
            this.candidateListing = candidateListing;
            this.scorePercentage = scorePercentage;
            this.matchReason = matchReason;
        }

        public Listing getCandidateListing() {
            return candidateListing;
        }

        public int getScorePercentage() {
            return scorePercentage;
        }

        public String getMatchReason() {
            return matchReason;
        }
    }

    /**
     * Calculates compatibility score (0-100) between userListing and targetListing.
     */
    public static MatchResult computeMatch(Listing userListing, Listing targetListing) {
        if (userListing == null || targetListing == null) {
            return new MatchResult(null, 0, "Invalid garments");
        }

        double categoryScore = 0.0;
        double sizeScore = 0.0;
        double conditionScore = 0.0;
        double valueScore = 0.0;
        List<String> insights = new ArrayList<>();

        // 1. Category alignment (35%)
        String userCat = userListing.getItem().getCategory();
        String targetCat = targetListing.getItem().getCategory();
        String userPref = userListing.getTargetSwapCategory();
        String targetPref = targetListing.getTargetSwapCategory();

        boolean userHappy = "Any".equalsIgnoreCase(userPref) || userPref.equalsIgnoreCase(targetCat);
        boolean targetHappy = "Any".equalsIgnoreCase(targetPref) || targetPref.equalsIgnoreCase(userCat);

        if (userHappy && targetHappy) {
            categoryScore = 1.0;
            insights.add("Both categories (" + userCat + " & " + targetCat + ") fulfill mutual swap goals");
        } else if (userHappy || targetHappy) {
            categoryScore = 0.7;
            insights.add("Complementary category pairing (" + userCat + " / " + targetCat + ")");
        } else {
            categoryScore = 0.4;
        }

        // 2. Size compatibility (25%)
        String s1 = userListing.getItem().getSize();
        String s2 = targetListing.getItem().getSize();
        if (s1.equalsIgnoreCase(s2)) {
            sizeScore = 1.0;
            insights.add("Identical sizing: Size " + s1);
        } else if (isAdjacentSize(s1, s2)) {
            sizeScore = 0.75;
            insights.add("Adjacent size overlap (" + s1 + " / " + s2 + ")");
        } else {
            sizeScore = 0.35;
        }

        // 3. Condition parity (20%)
        String c1 = userListing.getItem().getCondition();
        String c2 = targetListing.getItem().getCondition();
        if (c1.equalsIgnoreCase(c2)) {
            conditionScore = 1.0;
            insights.add("Equal wear standard: '" + c1 + "'");
        } else {
            conditionScore = 0.70;
            insights.add("Well-maintained condition: " + c1 + " vs " + c2);
        }

        // 4. Valuation proximity (20%)
        double p1 = userListing.getPrice();
        double p2 = targetListing.getPrice();
        double maxP = Math.max(p1, p2);
        if (maxP <= 0.01) {
            valueScore = 1.0;
        } else {
            double diffRatio = Math.abs(p1 - p2) / maxP;
            valueScore = Math.max(0.0, 1.0 - diffRatio);
        }
        if (Math.abs(p1 - p2) <= 10.0) {
           insights.add("Balanced estimated value (~₹" + String.format("%.0f", p1) + " vs ~₹" + String.format("%.0f", p2) + ")");
        }

        // Weighted Total Score
        double totalWeighted = (0.35 * categoryScore) + (0.25 * sizeScore) + (0.20 * conditionScore) + (0.20 * valueScore);
        int finalPct = (int) Math.round(totalWeighted * 100);
        finalPct = Math.max(35, Math.min(99, finalPct)); // Naturalistic clamping

        String explanation = String.join(". ", insights) + ".";
        return new MatchResult(targetListing, finalPct, explanation);
    }

    private static boolean isAdjacentSize(String a, String b) {
        List<String> order = Arrays.asList("XS", "S", "M", "L", "XL", "XXL");
        int idxA = order.indexOf(a.toUpperCase());
        int idxB = order.indexOf(b.toUpperCase());
        if (idxA >= 0 && idxB >= 0) {
            return Math.abs(idxA - idxB) <= 1;
        }
        return false;
    }
}

