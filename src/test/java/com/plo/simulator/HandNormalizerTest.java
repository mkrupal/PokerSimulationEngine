package com.plo.simulator;

import org.junit.jupiter.api.Test;
import java.util.*;

public class HandNormalizerTest {

    private final HandNormalizer normalizer = new HandNormalizer();

    // Helper to convert string like "AhKsKhJd" to array {"Ah","Ks","Kh","Jd"}
    private String[] toCardArray(String cards) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < cards.length(); i += 2) {
            result.add(cards.substring(i, i + 2));
        }
        return result.toArray(new String[0]);
    }

    // Helper to convert array to string like "AsKsKhJd"
    private String toCardString(String[] cards) {
        StringBuilder sb = new StringBuilder();
        for (String card : cards) sb.append(card);
        return sb.toString();
    }

    @Test
    public void testNormalizeHandSimple() {
        System.out.println("=== Testing normalizeHand ===");
        // Each entry: input -> expected
        String[][] testData = {
            {"AhKsKhJd", "AsKsKhJd"},
            {"2c3d4h5s", "5s4h3d2c"},
            {"QsJsTs9c", "QsJsTs9h"},
            {"AdKdQdJd", "AsKsQsJs"},
            {"KdKh5h3d", "KsKh5s3h"},
            {"KdKh5h3h", "KsKh5s3s"},
            {"AcAdAsAh", "AsAhAdAc"}
        };
        boolean allPassed = true;
        for (String[] test : testData) {
            String[] input = toCardArray(test[0]);
            String expected = test[1];
            HandNormalizer.NormalizationResult result = normalizer.normalizeHand(input);
            String actual = toCardString(result.normalizedCards);
            
            // Debug output
            System.out.println("Input: " + test[0]);
            System.out.println("  Sorted input: " + java.util.Arrays.toString(input));
            System.out.println("  Expected: " + expected);
            System.out.println("  Actual: " + actual);
            System.out.println("  Suit map: " + result.suitMap);
            System.out.println();
            
            if (!actual.equals(expected)) {
                System.err.println("❌ FAILED: input='" + test[0] + "' expected='" + expected + "' actual='" + actual + "'");
                allPassed = false;
            } else {
                System.out.println("✅ PASSED: input='" + test[0] + "' -> '" + actual + "'");
            }
        }
        if (allPassed) {
            System.out.println("=== All normalizeHand tests passed ===\n");
        }
    }

    @Test
    public void testNormalizeHoleCardsSimple() {
        System.out.println("=== Testing normalizeHoleCards ===");
        // Each entry: community, hole, expectedNormComm, expectedNormHole
        String[][] testData = {
            {"AhKsKhJd", "5h5dTs", "AsKsKhJd", "Th5s5d"},
            {"2c3d4h5s", "6c7d8d9h", "5s4h3d2c", "9h8d7d6c"},
            {"QhKh2h4d5s", "3hAhAdAs", "KsQs5h4d2s", "AsAhAd3s"}
        };
        boolean allPassed = true;
        for (String[] test : testData) {
            String[] community = toCardArray(test[0]);
            String[] hole = toCardArray(test[1]);
            String expectedNormComm = test[2];
            String expectedNormHole = test[3];
            
            HandNormalizer.NormalizationResult commResult = normalizer.normalizeHand(community);
            String actualNormComm = toCardString(commResult.normalizedCards);
            String[] actualNormHoleArr = normalizer.normalizeHoleCards(commResult.suitMap, hole);
            String actualNormHole = toCardString(actualNormHoleArr);
            
            // Debug output
            System.out.println("Community: " + test[0]);
            System.out.println("Hole: " + test[1]);
            System.out.println("  Expected norm comm: " + expectedNormComm);
            System.out.println("  Actual norm comm: " + actualNormComm);
            System.out.println("  Expected norm hole: " + expectedNormHole);
            System.out.println("  Actual norm hole: " + actualNormHole);
            System.out.println("  Suit map: " + commResult.suitMap);
            System.out.println();
            
            boolean commPassed = actualNormComm.equals(expectedNormComm);
            boolean holePassed = actualNormHole.equals(expectedNormHole);
            
            if (!commPassed || !holePassed) {
                if (!commPassed) {
                    System.err.println("❌ FAILED community normalization: expected='" + expectedNormComm + "' actual='" + actualNormComm + "'");
                }
                if (!holePassed) {
                    System.err.println("❌ FAILED hole normalization: expected='" + expectedNormHole + "' actual='" + actualNormHole + "'");
                }
                allPassed = false;
            } else {
                System.out.println("✅ PASSED: community='" + test[0] + "' hole='" + test[1] + "' -> comm='" + actualNormComm + "' hole='" + actualNormHole + "'");
            }
        }
        if (!allPassed) {
            throw new AssertionError("Some normalizeHoleCards tests failed - see error messages above");
        }
        System.out.println("=== All normalizeHoleCards tests passed ===\n");
    }
} 