package com.plo.simulator;

import java.util.*;

public class PLOSimulationEngine {
    private final PokerHandCache handCache;
    private final String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
    private final String[] suits = {"s", "h", "d", "c"};
    
    public PLOSimulationEngine(String handCacheFile) {
        this.handCache = new PokerHandCache(handCacheFile);
    }
    
    public static void main(String[] args) {
        args = new String[] {"AsAd6h6s"};
        if (args.length < 1) {
            System.out.println("Usage: java PLOSimulationEngine <hero_hand> [villain_hand1] [villain_hand2] ...");
            System.out.println("Example: java PLOSimulationEngine Ks9h8h7s");
            System.out.println("Example: java PLOSimulationEngine Ks9h8h7s AsAdAhAc");
            return;
        }
        
        PLOSimulationEngine engine = new PLOSimulationEngine("normalized_ranked_poker_hands.txt");
        
        String heroHand = args[0];
        String[] villainHands;
        
        if (args.length == 1) {
            // No villain hands specified, generate 5 random villain hands
            villainHands = new String[0]; // Will be handled in simulate method
        } else {
            villainHands = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                villainHands[i - 1] = args[i];
            }
        }
        
        SimulationResult result = engine.simulateAdaptive(heroHand, villainHands);
        System.out.printf("Final Result: %.4f%% (SD: %.4f%%, CI: %.4f%%, Simulations: %d)%n", 
                         result.winRate * 100, result.standardDeviation * 100, 
                         result.confidenceInterval * 100, result.iterations);
    }
    
    public static class SimulationResult {
        public final double winRate;
        public final double standardDeviation;
        public final double confidenceInterval;
        public final int iterations;
        
        public SimulationResult(double winRate, double standardDeviation, double confidenceInterval, int iterations) {
            this.winRate = winRate;
            this.standardDeviation = standardDeviation;
            this.confidenceInterval = confidenceInterval;
            this.iterations = iterations;
        }
    }
    
    public SimulationResult simulateAdaptive(String heroHand, String[] villainHands) {
        System.out.println("Running adaptive PLO simulation...");
        System.out.println("Hero: " + heroHand);
        
        // Validate card conflicts
        if (!validateNoCardConflicts(heroHand, villainHands)) {
            throw new IllegalArgumentException("Card conflict detected! Players cannot share cards.");
        }
        
        // If no villain hands provided, we'll generate them dynamically for each simulation
        boolean generateRandomVillains = villainHands.length == 0;
        if (generateRandomVillains) {
            System.out.println("No villain hands specified. Will generate 1 random villain hand for each simulation.");
        }
        
        for (int i = 0; i < villainHands.length; i++) {
            System.out.println("Villain " + (i + 1) + ": " + villainHands[i]);
        }
        
        int heroWins = 0;
        int iterations = 0;
        final int MAX_ITERATIONS = 1_000_000;
        
        while (iterations < MAX_ITERATIONS) {
            iterations++;
            
            // Generate new random villain hands for each simulation if needed
            String[] currentVillainHands = villainHands;
            if (generateRandomVillains) {
                currentVillainHands = generateRandomVillainHands(heroHand, 1);
            }
            
            if (simulateOneHand(heroHand, currentVillainHands)) {
                heroWins++;
            }
            
            // Check if we should log progress (logarithmic intervals)
            boolean shouldLog = shouldLogIteration(iterations);
            
            if (shouldLog) {
                double winRate = (double) heroWins / iterations;
                double standardDeviation = calculateStandardDeviation(winRate, iterations);
                double confidenceInterval = calculateConfidenceInterval(standardDeviation, iterations);
                
                System.out.printf("Iteration %d: Win Rate: %.4f%%, SD: %.4f%%, CI: %.4f%%%n", 
                                iterations, winRate * 100, standardDeviation * 100, confidenceInterval * 100);
                
                // Check stopping criteria
                if (standardDeviation < 0.01 && confidenceInterval < 0.02) {
                    System.out.println("Stopping criteria met!");
                    break;
                }
            }
        }
        
        // Calculate final statistics
        double finalWinRate = (double) heroWins / iterations;
        double finalStandardDeviation = calculateStandardDeviation(finalWinRate, iterations);
        double finalConfidenceInterval = calculateConfidenceInterval(finalStandardDeviation, iterations);
        
        System.out.println("Hero wins: " + heroWins + "/" + iterations);
        
        return new SimulationResult(finalWinRate, finalStandardDeviation, finalConfidenceInterval, iterations);
    }
    
    private boolean shouldLogIteration(int iteration) {
        if (iteration <= 1000) {
            return iteration % 100 == 0;
        } else if (iteration <= 10000) {
            return iteration % 1000 == 0;
        } else if (iteration <= 100000) {
            return iteration % 10000 == 0;
        } else if (iteration <= 1000000) {
            return iteration % 100000 == 0;
        } else {
            return iteration % 1000000 == 0;
        }
    }
    
    private double calculateStandardDeviation(double winRate, int iterations) {
        // For binomial distribution, SD = sqrt(p * (1-p) / n)
        return Math.sqrt(winRate * (1 - winRate) / iterations);
    }
    
    private double calculateConfidenceInterval(double standardDeviation, int iterations) {
        // 95% confidence interval = 1.96 * standard error
        // Standard error = standard deviation
        return 1.96 * standardDeviation;
    }
    
    private boolean validateNoCardConflicts(String heroHand, String[] villainHands) {
        Set<String> allCards = new HashSet<>();
        
        // Add hero cards
        String[] heroCards = parseHand(heroHand);
        for (String card : heroCards) {
            if (!allCards.add(card)) {
                System.err.println("Duplicate card found in hero hand: " + card);
                return false;
            }
        }
        
        // Add villain cards
        for (String villainHand : villainHands) {
            String[] villainCards = parseHand(villainHand);
            for (String card : villainCards) {
                if (!allCards.add(card)) {
                    System.err.println("Card conflict found: " + card + " is used by multiple players");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private String[] generateRandomVillainHands(String heroHand, int numVillains) {
        Set<String> usedCards = new HashSet<>();
        usedCards.addAll(Arrays.asList(parseHand(heroHand)));
        
        List<String> availableCards = createDeck(usedCards);
        Collections.shuffle(availableCards);
        String[] villainHands = new String[numVillains];
        
        for (int i = 0; i < numVillains; i++) {
            if (availableCards.size() < 4) {
                throw new RuntimeException("Not enough cards left in deck for villain " + (i + 1));
            }
            
            String[] villainCards = new String[4];
            for (int j = 0; j < 4; j++) {
                villainCards[j] = availableCards.remove(0);
            }
            
            villainHands[i] = arrayToString(villainCards);
        }
        
        return villainHands;
    }
    
    private String arrayToString(String[] cards) {
        StringBuilder sb = new StringBuilder();
        for (String card : cards) {
            sb.append(card);
        }
        return sb.toString();
    }
    
    private boolean simulateOneHand(String heroHand, String[] villainHands) {
        long startTime = System.currentTimeMillis();
        
        // Create deck and remove all players' cards
        Set<String> usedCards = new HashSet<>();
        usedCards.addAll(Arrays.asList(parseHand(heroHand)));
        for (String villainHand : villainHands) {
            usedCards.addAll(Arrays.asList(parseHand(villainHand)));
        }
        
        long deckCreationTime = System.currentTimeMillis();
        
        List<String> deck = createDeck(usedCards);
        Collections.shuffle(deck); // Shuffle once per simulation
        
        long shuffleTime = System.currentTimeMillis();
        
        // Deal 5 community cards sequentially
        String[] communityCards = new String[5];
        for (int i = 0; i < 5; i++) {
            communityCards[i] = deck.get(i);
        }
        
        long dealTime = System.currentTimeMillis();
        
        // Evaluate all hands
        int heroBestRank = evaluatePLOHand(parseHand(heroHand), communityCards);
        int[] villainRanks = new int[villainHands.length];
        
        for (int i = 0; i < villainHands.length; i++) {
            villainRanks[i] = evaluatePLOHand(parseHand(villainHands[i]), communityCards);
        }
        
        long evaluationTime = System.currentTimeMillis();
        
        // Check if hero wins (lower rank number = better hand)
        for (int villainRank : villainRanks) {
            if (villainRank < heroBestRank) {
                return false; // Hero loses
            }
        }
        return true; // Hero wins or ties
    }
    
    private String[] parseHand(String hand) {
        if (hand.length() != 8) { // 4 cards * 2 chars each
            throw new IllegalArgumentException("Hand must be exactly 8 characters: " + hand);
        }
        
        String[] cards = new String[4];
        for (int i = 0; i < 4; i++) {
            cards[i] = hand.substring(i * 2, (i + 1) * 2);
        }
        return cards;
    }
    
    private List<String> createDeck(Set<String> usedCards) {
        List<String> deck = new ArrayList<>();
        for (String rank : ranks) {
            for (String suit : suits) {
                String card = rank + suit;
                if (!usedCards.contains(card)) {
                    deck.add(card);
                }
            }
        }
        return deck;
    }
    
    private int evaluatePLOHand(String[] holeCards, String[] communityCards) {
        if (holeCards.length != 4 || communityCards.length != 5) {
            throw new IllegalArgumentException("Invalid card counts for PLO evaluation");
        }
        
        int bestRank = Integer.MAX_VALUE;
        
        // Try all possible 2-card combinations from hole cards
        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 4; j++) {
                String[] holeCombo = {holeCards[i], holeCards[j]};
                
                // Try all possible 3-card combinations from community cards
                for (int k = 0; k < 3; k++) {
                    for (int l = k + 1; l < 4; l++) {
                        for (int m = l + 1; m < 5; m++) {
                            String[] communityCombo = {communityCards[k], communityCards[l], communityCards[m]};
                            
                            // Combine to make 5-card hand
                            String[] fiveCardHand = new String[5];
                            fiveCardHand[0] = holeCombo[0];
                            fiveCardHand[1] = holeCombo[1];
                            fiveCardHand[2] = communityCombo[0];
                            fiveCardHand[3] = communityCombo[1];
                            fiveCardHand[4] = communityCombo[2];
                            
                            // Get rank for this 5-card hand
                            int rank = handCache.getHandRank(fiveCardHand);
                            if (rank < bestRank) {
                                bestRank = rank;
                            }
                        }
                    }
                }
            }
        }
        
        return bestRank;
    }
} 