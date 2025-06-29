package com.plo.simulator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class PokerHandGenerator {
    
    private final String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
    private final String[] suits = {"s", "h", "d", "c"};
    
    public static void main(String[] args) {
        PokerHandGenerator generator = new PokerHandGenerator();
        generator.generateAllHands("non_normalized_ranked_poker_hands.txt");
    }
    
    public void generateAllHands(String filename) {
        System.out.println("Generating all possible 5-card poker hands...");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("hand,hand_rank,hand_type");
            
            // Generate all possible 5-card combinations
            List<String[]> allHands = generateAllCombinations();
            System.out.println("Generated " + allHands.size() + " hands");
            
            // Sort all hands by strength (highest first)
            allHands.sort((a, b) -> compareHands(b, a));
            
            // Assign ranks based on equivalence
            int currentRank = 0;
            String lastEquivalent = null;
            
            for (String[] hand : allHands) {
                String handStr = sortAndFormatHand(hand);
                String handType = getHandType(hand);
                String equivalent = getEquivalentKey(hand, handType);
                
                // If this is a new equivalent hand, increment rank
                if (!equivalent.equals(lastEquivalent)) {
                    currentRank++;
                    lastEquivalent = equivalent;
                }
                
                writer.println(handStr + "," + currentRank + "," + handType);
            }
            
            System.out.println("Generated " + allHands.size() + " hands with " + currentRank + " unique ranks");
            System.out.println("Results written to " + filename);
            
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
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
            return suitOrder(b.charAt(1)) - suitOrder(a.charAt(1)); // s > h > d > c
        });
        StringBuilder sb = new StringBuilder();
        for (String card : sorted) {
            sb.append(card);
        }
        return sb.toString();
    }
    
    private String getEquivalentKey(String[] hand, String handType) {
        // For straights and flushes, use rank sequence only
        if (handType.equals("Straight") || handType.equals("Straight Flush") || handType.equals("Royal Flush")) {
            String[] sortedByRank = sortByRank(hand);
            StringBuilder key = new StringBuilder();
            for (String card : sortedByRank) {
                key.append(card.charAt(0));
            }
            return handType + ":" + key.toString();
        } else if (handType.equals("Flush")) {
            String[] sortedByRank = sortByRank(hand);
            StringBuilder key = new StringBuilder();
            for (String card : sortedByRank) {
                key.append(card.charAt(0));
            }
            return handType + ":" + key.toString();
        } else if (handType.equals("Four of a Kind")) {
            // For four of a kind: rank of the four cards + rank of kicker
            Map<Character, Integer> rankCounts = getRankCounts(hand);
            char fourRank = 0;
            char kickerRank = 0;
            for (Map.Entry<Character, Integer> entry : rankCounts.entrySet()) {
                if (entry.getValue() == 4) {
                    fourRank = entry.getKey();
                } else {
                    kickerRank = entry.getKey();
                }
            }
            return handType + ":" + fourRank + kickerRank;
        } else if (handType.equals("Full House")) {
            // For full house: rank of three + rank of pair
            Map<Character, Integer> rankCounts = getRankCounts(hand);
            char threeRank = 0;
            char pairRank = 0;
            for (Map.Entry<Character, Integer> entry : rankCounts.entrySet()) {
                if (entry.getValue() == 3) {
                    threeRank = entry.getKey();
                } else {
                    pairRank = entry.getKey();
                }
            }
            return handType + ":" + threeRank + pairRank;
        } else if (handType.equals("Three of a Kind")) {
            // For three of a kind: rank of three + ranks of kickers (sorted)
            Map<Character, Integer> rankCounts = getRankCounts(hand);
            char threeRank = 0;
            List<Character> kickers = new ArrayList<>();
            for (Map.Entry<Character, Integer> entry : rankCounts.entrySet()) {
                if (entry.getValue() == 3) {
                    threeRank = entry.getKey();
                } else {
                    kickers.add(entry.getKey());
                }
            }
            kickers.sort((a, b) -> Integer.compare(getRankValue(b), getRankValue(a))); // High to low
            return handType + ":" + threeRank + kickers.get(0) + kickers.get(1);
        } else if (handType.equals("Two Pair")) {
            // For two pair: ranks of pairs (high to low) + kicker
            Map<Character, Integer> rankCounts = getRankCounts(hand);
            List<Character> pairs = new ArrayList<>();
            char kicker = 0;
            for (Map.Entry<Character, Integer> entry : rankCounts.entrySet()) {
                if (entry.getValue() == 2) {
                    pairs.add(entry.getKey());
                } else {
                    kicker = entry.getKey();
                }
            }
            pairs.sort((a, b) -> Integer.compare(getRankValue(b), getRankValue(a))); // High to low
            return handType + ":" + pairs.get(0) + pairs.get(1) + kicker;
        } else if (handType.equals("One Pair")) {
            // For one pair: rank of pair + ranks of kickers (sorted)
            Map<Character, Integer> rankCounts = getRankCounts(hand);
            char pairRank = 0;
            List<Character> kickers = new ArrayList<>();
            for (Map.Entry<Character, Integer> entry : rankCounts.entrySet()) {
                if (entry.getValue() == 2) {
                    pairRank = entry.getKey();
                } else {
                    kickers.add(entry.getKey());
                }
            }
            kickers.sort((a, b) -> Integer.compare(getRankValue(b), getRankValue(a))); // High to low
            return handType + ":" + pairRank + kickers.get(0) + kickers.get(1) + kickers.get(2);
        } else {
            // For high card: all ranks sorted
            String[] sortedByRank = sortByRank(hand);
            StringBuilder key = new StringBuilder();
            for (String card : sortedByRank) {
                key.append(card.charAt(0));
            }
            return handType + ":" + key.toString();
        }
    }
    
    private String[] sortByRank(String[] hand) {
        String[] sorted = hand.clone();
        Arrays.sort(sorted, (a, b) -> {
            int rankA = getRankValue(a.charAt(0));
            int rankB = getRankValue(b.charAt(0));
            if (rankA != rankB) {
                return Integer.compare(rankB, rankA); // High to low
            }
            return suitOrder(b.charAt(1)) - suitOrder(a.charAt(1)); // s > h > d > c
        });
        return sorted;
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
    
    private String getHandType(String[] hand) {
        if (isRoyalFlush(hand)) return "Royal Flush";
        if (isStraightFlush(hand)) return "Straight Flush";
        if (isFourOfAKind(hand)) return "Four of a Kind";
        if (isFullHouse(hand)) return "Full House";
        if (isFlush(hand)) return "Flush";
        if (isStraight(hand)) return "Straight";
        if (isThreeOfAKind(hand)) return "Three of a Kind";
        if (isTwoPair(hand)) return "Two Pair";
        if (isOnePair(hand)) return "One Pair";
        return "High Card";
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
} 