package com.plo.simulator;

import java.util.*;

public class HandNormalizer {
    
    /**
     * Result class to hold both normalized cards and the suit mapping
     */
    public static class NormalizationResult {
        public final String[] normalizedCards;
        public final Map<Character, Set<Character>> suitMap;
        
        public NormalizationResult(String[] normalizedCards, Map<Character, Set<Character>> suitMap) {
            this.normalizedCards = normalizedCards;
            this.suitMap = suitMap;
        }
        
        @Override
        public String toString() {
            return "NormalizationResult{" +
                    "normalizedCards=" + Arrays.toString(normalizedCards) +
                    ", suitMap=" + suitMap +
                    '}';
        }
    }
    
    // Performance profiler for normalization
    private static class NormalizationProfiler {
        long totalSort = 0;
        long totalSuitMapping = 0;
        long totalNormalization = 0;
        long totalFinalSort = 0;
        int count = 0;
        
        void record(long sort, long suitMap, long norm, long finalSort) {
            totalSort += sort;
            totalSuitMapping += suitMap;
            totalNormalization += norm;
            totalFinalSort += finalSort;
            count++;
        }
        
        void printStats() {
            if (count == 0) return;
            long total = totalSort + totalSuitMapping + totalNormalization + totalFinalSort;
            System.out.println("\n=== HandNormalizer Profiling ===");
            System.out.printf("Initial Sort: %.4fms (%.1f%%)\n", (double)totalSort/count/1e6, 100.0*totalSort/total);
            System.out.printf("Suit Mapping: %.4fms (%.1f%%)\n", (double)totalSuitMapping/count/1e6, 100.0*totalSuitMapping/total);
            System.out.printf("Normalization: %.4fms (%.1f%%)\n", (double)totalNormalization/count/1e6, 100.0*totalNormalization/total);
            System.out.printf("Final Sort: %.4fms (%.1f%%)\n", (double)totalFinalSort/count/1e6, 100.0*totalFinalSort/total);
            System.out.printf("Total per normalization: %.4fms\n", (double)total/count/1e6);
        }
    }
    
    private static final NormalizationProfiler profiler = new NormalizationProfiler();
    
    public static void printProfilerStats() {
        profiler.printStats();
    }
    
    /**
     * Generic method to normalize any number of cards and return suit mapping
     */
    public NormalizationResult normalizeHand(String[] cards) {
        long t0 = System.nanoTime();
        
        if (cards == null || cards.length == 0) {
            return new NormalizationResult(cards, new HashMap<>());
        }
        
        // Step 1: Sort cards by rank (high to low), then by suit
        String[] sortedCards = cards.clone();
        Arrays.sort(sortedCards, (a, b) -> {
            int rankA = getRankValue(a.charAt(0));
            int rankB = getRankValue(b.charAt(0));
            if (rankA != rankB) {
                return Integer.compare(rankB, rankA); // High to low
            }
            return Character.compare(a.charAt(1), b.charAt(1)); // Then by suit
        });
        
        long t1 = System.nanoTime();
        
        // Step 2: Create suit mapping
        Map<Character, Character> suitToCanonical = new HashMap<>();
        char[] canonicalSuits = {'s', 'h', 'd', 'c'};
        int canonicalIndex = 0;
        
        for (String card : sortedCards) {
            char suit = card.charAt(1);
            if (!suitToCanonical.containsKey(suit)) {
                suitToCanonical.put(suit, canonicalSuits[canonicalIndex++]);
            }
        }
        
        long t2 = System.nanoTime();
        
        // Step 3: For each rank group, assign canonical suits in canonical order to the suits in that group (in sorted order)
        Map<Character, List<Integer>> rankToIndices = new HashMap<>();
        for (int i = 0; i < sortedCards.length; i++) {
            char rank = sortedCards[i].charAt(0);
            rankToIndices.computeIfAbsent(rank, k -> new ArrayList<>()).add(i);
        }
        
        Map<Character, Set<Character>> suitMap = new HashMap<>();
        Map<String, Character> cardToCanonical = new HashMap<>();
        for (Map.Entry<Character, List<Integer>> entry : rankToIndices.entrySet()) {
            List<Integer> indices = entry.getValue();
            if (indices.size() == 1) {
                char suit = sortedCards[indices.get(0)].charAt(1);
                char canonical = suitToCanonical.get(suit);
                suitMap.put(suit, Set.of(canonical));
                cardToCanonical.put(sortedCards[indices.get(0)], canonical);
            } else {
                // Multiple cards of same rank: assign canonical suits in canonical order to suits in this group (sorted order)
                List<Character> groupSuits = new ArrayList<>();
                for (int idx : indices) groupSuits.add(sortedCards[idx].charAt(1));
                // Sort groupSuits by canonical suit order for deterministic mapping
                groupSuits.sort(Comparator.comparingInt(s -> {
                    for (int i = 0; i < canonicalSuits.length; i++) if (suitToCanonical.get(s) == canonicalSuits[i]) return i;
                    return 99;
                }));
                for (int i = 0; i < groupSuits.size(); i++) {
                    char suit = groupSuits.get(i);
                    char canonical = canonicalSuits[i];
                    cardToCanonical.put(sortedCards[indices.get(i)], canonical);
                }
                Set<Character> canonicals = new HashSet<>();
                for (int i = 0; i < groupSuits.size(); i++) {
                    canonicals.add(canonicalSuits[i]);
                }
                for (char suit : groupSuits) {
                    suitMap.put(suit, new HashSet<>(canonicals));
                }
            }
        }
        
        long t3 = System.nanoTime();
        
        // Step 4: Normalize all cards using the mapping (for same-rank groups, use local mapping)
        String[] normalized = new String[cards.length];
        for (int i = 0; i < cards.length; i++) {
            // Find the card in sortedCards
            char rank = cards[i].charAt(0);
            char suit = cards[i].charAt(1);
            // Find the first matching card in sortedCards that hasn't been used
            Character canonicalSuit = null;
            for (int j = 0; j < sortedCards.length; j++) {
                if (sortedCards[j].charAt(0) == rank && sortedCards[j].charAt(1) == suit && !cardToCanonical.containsKey("used"+j)) {
                    canonicalSuit = cardToCanonical.get(sortedCards[j]);
                    cardToCanonical.put("used"+j, canonicalSuit); // Mark as used
                    break;
                }
            }
            if (canonicalSuit == null) canonicalSuit = suitToCanonical.get(suit); // fallback
            normalized[i] = rank + "" + canonicalSuit;
        }
        
        long t4 = System.nanoTime();
        
        // Step 5: Sort the normalized cards by rank (high to low), then by canonical suit order s < h < d < c
        Arrays.sort(normalized, (a, b) -> {
            int rankA = getRankValue(a.charAt(0));
            int rankB = getRankValue(b.charAt(0));
            if (rankA != rankB) {
                return Integer.compare(rankB, rankA);
            }
            int suitA = -1, suitB = -1;
            for (int i = 0; i < canonicalSuits.length; i++) {
                if (a.charAt(1) == canonicalSuits[i]) suitA = i;
                if (b.charAt(1) == canonicalSuits[i]) suitB = i;
            }
            return Integer.compare(suitA, suitB);
        });
        
        long t5 = System.nanoTime();
        
        profiler.record(t1-t0, t2-t1, t4-t3, t5-t4);
        
        return new NormalizationResult(normalized, suitMap);
    }
    
    /**
     * Normalize hole cards using existing suit mapping and resolve ambiguities
     */
    public String[] normalizeHoleCards(Map<Character, Set<Character>> suitMap, String[] holeCards) {
        if (holeCards == null || holeCards.length == 0) {
            return holeCards;
        }
        
        // Create mutable copy of suit map for constraint resolution
        Map<Character, Set<Character>> workingSuitMap = new HashMap<>();
        for (Map.Entry<Character, Set<Character>> entry : suitMap.entrySet()) {
            workingSuitMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        // Find next available canonical suits
        Set<Character> usedCanonicalSuits = new HashSet<>();
        for (Set<Character> canonicalSet : workingSuitMap.values()) {
            usedCanonicalSuits.addAll(canonicalSet);
        }
        
        char[] canonicalSuits = {'s', 'h', 'd', 'c'};
        Queue<Character> availableCanonicalSuits = new LinkedList<>();
        for (char suit : canonicalSuits) {
            if (!usedCanonicalSuits.contains(suit)) {
                availableCanonicalSuits.add(suit);
            }
        }
        
        // Process hole cards in input order
        String[] normalizedHole = new String[holeCards.length];
        
        for (int i = 0; i < holeCards.length; i++) {
            String card = holeCards[i];
            char rank = card.charAt(0);
            char suit = card.charAt(1);
            
            if (workingSuitMap.containsKey(suit)) {
                Set<Character> possibleCanonicalSuits = workingSuitMap.get(suit);
                
                if (possibleCanonicalSuits.size() == 1) {
                    // Already resolved
                    char canonicalSuit = possibleCanonicalSuits.iterator().next();
                    normalizedHole[i] = rank + "" + canonicalSuit;
                } else {
                    // Resolve ambiguity - take highest (first) available
                    char bestCanonical = '?';
                    for (char canonical : canonicalSuits) {
                        if (possibleCanonicalSuits.contains(canonical)) {
                            bestCanonical = canonical;
                            break;
                        }
                    }
                    
                    // Lock in this mapping
                    workingSuitMap.put(suit, Set.of(bestCanonical));
                    resolveAmbiguity(workingSuitMap, suit, bestCanonical);
                    
                    normalizedHole[i] = rank + "" + bestCanonical;
                }
            } else {
                // New suit - assign next available
                if (!availableCanonicalSuits.isEmpty()) {
                    char newCanonical = availableCanonicalSuits.poll();
                    workingSuitMap.put(suit, Set.of(newCanonical));
                    normalizedHole[i] = rank + "" + newCanonical;
                } else {
                    normalizedHole[i] = card;
                }
            }
        }
        
        // Sort the final normalized hole cards by rank (high to low), then by suit
        Arrays.sort(normalizedHole, (a, b) -> {
            int rankA = getRankValue(a.charAt(0));
            int rankB = getRankValue(b.charAt(0));
            if (rankA != rankB) {
                return Integer.compare(rankB, rankA); // High to low
            }
            // Then by canonical suit order: s < h < d < c
            int suitA = -1, suitB = -1;
            for (int i = 0; i < canonicalSuits.length; i++) {
                if (a.charAt(1) == canonicalSuits[i]) suitA = i;
                if (b.charAt(1) == canonicalSuits[i]) suitB = i;
            }
            return Integer.compare(suitA, suitB);
        });
        
        return normalizedHole;
    }
    
    /**
     * Resolve ambiguity when one mapping is locked in
     */
    private void resolveAmbiguity(Map<Character, Set<Character>> suitMap, char lockedOriginal, char lockedCanonical) {
        for (Map.Entry<Character, Set<Character>> entry : suitMap.entrySet()) {
            if (entry.getKey() != lockedOriginal && entry.getValue().contains(lockedCanonical) && entry.getValue().size() > 1) {
                Set<Character> remaining = new HashSet<>(entry.getValue());
                remaining.remove(lockedCanonical);
                entry.setValue(remaining);
            }
        }
    }
    
    /**
     * Get numeric value for card rank for sorting
     */
    private int getRankValue(char rank) {
        switch (rank) {
            case 'A': return 14;
            case 'K': return 13;
            case 'Q': return 12;
            case 'J': return 11;
            case 'T': return 10;
            default: return Character.getNumericValue(rank);
        }
    }
    
    /**
     * Simple normalize method that just takes cards and returns normalized array
     * For backward compatibility
     */
    public String[] normalizeCards(String[] cards) {
        NormalizationResult result = normalizeHand(cards);
        return result.normalizedCards;
    }
}