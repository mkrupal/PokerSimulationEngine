package com.plo.simulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Arrays;

public class PokerHandCache {
    private final Map<String, Integer> handRankings = new ConcurrentHashMap<>();
    private final String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
    private final String[] suits = {"s", "h", "d", "c"};

    public PokerHandCache() {
        this("non_normalized_ranked_poker_hands.txt");
    }

    public PokerHandCache(String filename) {
        loadHandRankings(filename);
    }

    private void loadHandRankings(String filename) {
        int lineCount = 0;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                lineCount++;
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String handKey = parts[0];
                    int rank = Integer.parseInt(parts[1]);
                    handRankings.put(handKey, rank);
                }
            }
            System.out.println("Loaded " + handRankings.size() + " unique hand rankings from cache (" + lineCount + " lines read)");
        } catch (Exception e) {
            System.err.println("Error loading hand rankings: " + e.getMessage());
        }
    }

    public int getHandRank(String[] cards) {
        if (cards.length != 5) {
            throw new IllegalArgumentException("Must have exactly 5 cards for hand evaluation");
        }
        String handKey = sortAndFormatHand(cards);
        Integer rank = handRankings.get(handKey);
        if (rank == null) {
            throw new RuntimeException("Hand not found in cache: " + handKey);
        }
        return rank;
    }

    private String sortAndFormatHand(String[] hand) {
        // Sort by rank (high to low), then by suit (s > h > d > c)
        String[] sorted = hand.clone();
        Arrays.sort(sorted, (a, b) -> {
            int rankA = getRankValue(a.charAt(0));
            int rankB = getRankValue(b.charAt(0));
            if (rankA != rankB) {
                return Integer.compare(rankB, rankA); // High to low
            }
            // Suit order: s > h > d > c
            return suitOrder(b.charAt(1)) - suitOrder(a.charAt(1));
        });
        StringBuilder sb = new StringBuilder();
        for (String card : sorted) {
            sb.append(card);
        }
        return sb.toString();
    }

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

    private int suitOrder(char suit) {
        switch (suit) {
            case 's': return 3; // spades highest
            case 'h': return 2; // hearts
            case 'd': return 1; // diamonds
            case 'c': return 0; // clubs lowest
            default: return -1;
        }
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
} 