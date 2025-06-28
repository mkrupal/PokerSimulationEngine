package com.plo.simulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PokerHandCache {
    private final Map<String, Integer> handRankings = new HashMap<>();
    private final HandNormalizer normalizer = new HandNormalizer();
    
    // Profiler for cache lookup
    private static class LookupProfiler {
        long totalNormalize = 0;
        long totalString = 0;
        long totalLookup = 0;
        int count = 0;
        void record(long norm, long str, long lookup) {
            totalNormalize += norm;
            totalString += str;
            totalLookup += lookup;
            count++;
        }
        void printStats() {
            if (count == 0) return;
            System.out.println("\n=== PokerHandCache Lookup Profiling ===");
            System.out.printf("Normalization: %.4fms (%.1f%%)\n", (double)totalNormalize/count/1e6, 100.0*totalNormalize/(totalNormalize+totalString+totalLookup));
            System.out.printf("String Construction: %.4fms (%.1f%%)\n", (double)totalString/count/1e6, 100.0*totalString/(totalNormalize+totalString+totalLookup));
            System.out.printf("Map Lookup: %.4fms (%.1f%%)\n", (double)totalLookup/count/1e6, 100.0*totalLookup/(totalNormalize+totalString+totalLookup));
            System.out.printf("Total per lookup: %.4fms\n", (double)(totalNormalize+totalString+totalLookup)/count/1e6);
        }
    }
    private static final LookupProfiler profiler = new LookupProfiler();
    
    public PokerHandCache(String filename) {
        loadHandRankings(filename);
    }
    
    private void loadHandRankings(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String normalizedHand = parts[0];
                    int rank = Integer.parseInt(parts[1]);
                    handRankings.put(normalizedHand, rank);
                }
            }
            System.out.println("Loaded " + handRankings.size() + " hand rankings from cache");
        } catch (Exception e) {
            System.err.println("Error loading hand rankings: " + e.getMessage());
        }
    }
    
    public int getHandRank(String[] cards) {
        if (cards.length != 5) {
            throw new IllegalArgumentException("Must have exactly 5 cards for hand evaluation");
        }
        long t0 = System.nanoTime();
        // Normalize the hand
        HandNormalizer.NormalizationResult result = normalizer.normalizeHand(cards);
        long t1 = System.nanoTime();
        String normalizedHand = arrayToString(result.normalizedCards);
        long t2 = System.nanoTime();
        Integer rank = handRankings.get(normalizedHand);
        long t3 = System.nanoTime();
        profiler.record(t1-t0, t2-t1, t3-t2);
        if (rank == null) {
            throw new RuntimeException("Hand not found in cache: " + normalizedHand);
        }
        return rank;
    }
    
    private String arrayToString(String[] cards) {
        StringBuilder sb = new StringBuilder();
        for (String card : cards) {
            sb.append(card);
        }
        return sb.toString();
    }
    
    public int getHandRank(String handString) {
        if (handString.length() != 10) { // 5 cards * 2 chars each
            throw new IllegalArgumentException("Hand string must be exactly 10 characters");
        }
        
        String[] cards = new String[5];
        for (int i = 0; i < 5; i++) {
            cards[i] = handString.substring(i * 2, (i + 1) * 2);
        }
        return getHandRank(cards);
    }

    public static void printProfilerStats() {
        profiler.printStats();
    }
} 