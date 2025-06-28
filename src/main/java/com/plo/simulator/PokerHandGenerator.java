package com.plo.simulator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class PokerHandGenerator {
    
    private final HandNormalizer normalizer = new HandNormalizer();
    private final String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
    private final String[] suits = {"s", "h", "d", "c"};
    
    public static void main(String[] args) {
        PokerHandGenerator generator = new PokerHandGenerator();
        generator.generateAllHands("all_poker_hands.txt");
    }
    
    public void generateAllHands(String filename) {
        System.out.println("Generating all possible 5-card poker hands...");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("normalized_hand,hand_rank,hand_type");
            
            // Generate all possible 5-card combinations
            List<String[]> allHands = generateAllCombinations();
            System.out.println("Generated " + allHands.size() + " hands");
            
            // Track seen normalized hands to avoid duplicates
            Set<String> seenNormalizedHands = new HashSet<>();
            
            // Process each hand
            int rank = 1;
            
            // Royal Flush (A-K-Q-J-T of same suit)
            rank = processHandType(writer, allHands, rank, "Royal Flush", this::isRoyalFlush, seenNormalizedHands);
            
            // Straight Flush (5 consecutive cards of same suit)
            rank = processHandType(writer, allHands, rank, "Straight Flush", this::isStraightFlush, seenNormalizedHands);
            
            // Four of a Kind
            rank = processHandType(writer, allHands, rank, "Four of a Kind", this::isFourOfAKind, seenNormalizedHands);
            
            // Full House
            rank = processHandType(writer, allHands, rank, "Full House", this::isFullHouse, seenNormalizedHands);
            
            // Flush
            rank = processHandType(writer, allHands, rank, "Flush", this::isFlush, seenNormalizedHands);
            
            // Straight
            rank = processHandType(writer, allHands, rank, "Straight", this::isStraight, seenNormalizedHands);
            
            // Three of a Kind
            rank = processHandType(writer, allHands, rank, "Three of a Kind", this::isThreeOfAKind, seenNormalizedHands);
            
            // Two Pair
            rank = processHandType(writer, allHands, rank, "Two Pair", this::isTwoPair, seenNormalizedHands);
            
            // One Pair
            rank = processHandType(writer, allHands, rank, "One Pair", this::isOnePair, seenNormalizedHands);
            
            // High Card
            rank = processHandType(writer, allHands, rank, "High Card", this::isHighCard, seenNormalizedHands);
            
            System.out.println("Generated " + (rank - 1) + " unique normalized hands");
            System.out.println("Results written to " + filename);
            
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
    
    private int processHandType(PrintWriter writer, List<String[]> allHands, int startRank, 
                              String handType, HandEvaluator evaluator, Set<String> seenNormalizedHands) {
        int currentRank = startRank;
        List<String[]> handsOfType = new ArrayList<>();
        
        for (String[] hand : allHands) {
            if (evaluator.evaluate(hand)) {
                handsOfType.add(hand);
            }
        }
        
        // Sort hands of this type by strength (highest first)
        handsOfType.sort((a, b) -> compareHands(b, a)); // Reverse order for highest first
        
        // Normalize and write each hand
        for (String[] hand : handsOfType) {
            HandNormalizer.NormalizationResult result = normalizer.normalizeHand(hand);
            String normalizedHand = arrayToString(result.normalizedCards);
            
            // Only write if we haven't seen this normalized hand before
            if (!seenNormalizedHands.contains(normalizedHand)) {
                writer.println(normalizedHand + "," + currentRank + "," + handType);
                seenNormalizedHands.add(normalizedHand);
                currentRank++;
            }
        }
        
        System.out.println(handType + ": " + handsOfType.size() + " hands, " + 
                          (currentRank - startRank) + " unique normalized");
        return currentRank;
    }
    
    private List<String[]> generateAllCombinations() {
        List<String[]> allHands = new ArrayList<>();
        String[] deck = new String[52];
        
        int index = 0;
        for (String rank : ranks) {
            for (String suit : suits) {
                deck[index++] = rank + suit;
            }
        }
        
        // Generate all 5-card combinations
        generateCombinations(deck, 5, 0, new String[5], 0, allHands);
        return allHands;
    }
    
    private void generateCombinations(String[] deck, int r, int start, String[] current, 
                                    int currentIndex, List<String[]> result) {
        if (currentIndex == r) {
            result.add(current.clone());
            return;
        }
        
        for (int i = start; i < deck.length; i++) {
            current[currentIndex] = deck[i];
            generateCombinations(deck, r, i + 1, current, currentIndex + 1, result);
        }
    }
    
    private String arrayToString(String[] cards) {
        StringBuilder sb = new StringBuilder();
        for (String card : cards) {
            sb.append(card);
        }
        return sb.toString();
    }
    
    private int compareHands(String[] hand1, String[] hand2) {
        // Compare by hand type first, then by card values
        int type1 = getHandTypeValue(hand1);
        int type2 = getHandTypeValue(hand2);
        
        if (type1 != type2) {
            return Integer.compare(type1, type2);
        }
        
        // Same type, compare by card values
        return compareCardValues(hand1, hand2);
    }
    
    private int getHandTypeValue(String[] hand) {
        if (isRoyalFlush(hand)) return 9;
        if (isStraightFlush(hand)) return 8;
        if (isFourOfAKind(hand)) return 7;
        if (isFullHouse(hand)) return 6;
        if (isFlush(hand)) return 5;
        if (isStraight(hand)) return 4;
        if (isThreeOfAKind(hand)) return 3;
        if (isTwoPair(hand)) return 2;
        if (isOnePair(hand)) return 1;
        return 0; // High Card
    }
    
    private int compareCardValues(String[] hand1, String[] hand2) {
        // Sort both hands by rank (high to low)
        String[] sorted1 = sortByRank(hand1);
        String[] sorted2 = sortByRank(hand2);
        
        for (int i = 0; i < 5; i++) {
            int rank1 = getRankValue(sorted1[i].charAt(0));
            int rank2 = getRankValue(sorted2[i].charAt(0));
            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2);
            }
        }
        return 0; // Equal
    }
    
    private String[] sortByRank(String[] hand) {
        String[] sorted = hand.clone();
        Arrays.sort(sorted, (a, b) -> {
            int rankA = getRankValue(a.charAt(0));
            int rankB = getRankValue(b.charAt(0));
            return Integer.compare(rankB, rankA); // High to low
        });
        return sorted;
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
    
    // Hand evaluation methods
    private boolean isRoyalFlush(String[] hand) {
        return isStraightFlush(hand) && hasAce(hand) && hasKing(hand);
    }
    
    private boolean isStraightFlush(String[] hand) {
        return isFlush(hand) && isStraight(hand);
    }
    
    private boolean isFourOfAKind(String[] hand) {
        Map<Character, Integer> rankCounts = getRankCounts(hand);
        return rankCounts.values().stream().anyMatch(count -> count == 4);
    }
    
    private boolean isFullHouse(String[] hand) {
        Map<Character, Integer> rankCounts = getRankCounts(hand);
        List<Integer> counts = new ArrayList<>(rankCounts.values());
        return counts.size() == 2 && counts.contains(3) && counts.contains(2);
    }
    
    private boolean isFlush(String[] hand) {
        char firstSuit = hand[0].charAt(1);
        return Arrays.stream(hand).allMatch(card -> card.charAt(1) == firstSuit);
    }
    
    private boolean isStraight(String[] hand) {
        List<Integer> rankValues = new ArrayList<>();
        for (String card : hand) {
            rankValues.add(getRankValue(card.charAt(0)));
        }
        Collections.sort(rankValues);
        
        // Check for regular straight
        for (int i = 1; i < rankValues.size(); i++) {
            if (rankValues.get(i) != rankValues.get(i-1) + 1) {
                // Check for A-2-3-4-5 straight (wheel)
                if (i == rankValues.size() - 1 && rankValues.get(0) == 2 && rankValues.get(4) == 14) {
                    return true;
                }
                return false;
            }
        }
        return true;
    }
    
    private boolean isThreeOfAKind(String[] hand) {
        Map<Character, Integer> rankCounts = getRankCounts(hand);
        return rankCounts.values().stream().anyMatch(count -> count == 3);
    }
    
    private boolean isTwoPair(String[] hand) {
        Map<Character, Integer> rankCounts = getRankCounts(hand);
        long pairCount = rankCounts.values().stream().filter(count -> count == 2).count();
        return pairCount == 2;
    }
    
    private boolean isOnePair(String[] hand) {
        Map<Character, Integer> rankCounts = getRankCounts(hand);
        return rankCounts.values().stream().anyMatch(count -> count == 2);
    }
    
    private boolean isHighCard(String[] hand) {
        return !isOnePair(hand) && !isTwoPair(hand) && !isThreeOfAKind(hand) && 
               !isStraight(hand) && !isFlush(hand) && !isFullHouse(hand) && 
               !isFourOfAKind(hand) && !isStraightFlush(hand) && !isRoyalFlush(hand);
    }
    
    private Map<Character, Integer> getRankCounts(String[] hand) {
        Map<Character, Integer> counts = new HashMap<>();
        for (String card : hand) {
            char rank = card.charAt(0);
            counts.put(rank, counts.getOrDefault(rank, 0) + 1);
        }
        return counts;
    }
    
    private boolean hasAce(String[] hand) {
        return Arrays.stream(hand).anyMatch(card -> card.charAt(0) == 'A');
    }
    
    private boolean hasKing(String[] hand) {
        return Arrays.stream(hand).anyMatch(card -> card.charAt(0) == 'K');
    }
    
    @FunctionalInterface
    private interface HandEvaluator {
        boolean evaluate(String[] hand);
    }
} 