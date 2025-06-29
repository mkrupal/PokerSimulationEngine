package com.plo.simulator;

import java.util.*;

public class PLOSimulationEngine {
    
    private final PokerHandCache handCache;
    private final Set<String> fullDeck;
    
    // Statistical constants
    private static final double CONFIDENCE_LEVEL_95 = 1.96;
    private static final double DEFAULT_STOPPING_SD = 0.005; // 0.5% standard deviation threshold
    private static final double DEFAULT_STOPPING_CI = 0.01; // 1% confidence interval threshold
    private static final int MIN_ITERATIONS = 100; // Minimum iterations before allowing early stopping
    
    public PLOSimulationEngine() {
        this.handCache = new PokerHandCache();
        this.fullDeck = initializeFullDeck();
    }
    
    private Set<String> initializeFullDeck() {
        Set<String> deck = new HashSet<>();
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
        String[] suits = {"c", "d", "h", "s"};
        
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(rank + suit);
            }
        }
        return deck;
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
    
    public SimulationResult simulateAdaptive(String heroHand, List<String> villainHands) {
        return simulateAdaptive(heroHand, villainHands, 1);
    }
    
    public SimulationResult simulateAdaptive(String heroHand, List<String> villainHands, int numThreads) {
        if (numThreads <= 1) {
            return simulateAdaptiveSingleThread(heroHand, villainHands);
        }
        
        return simulateAdaptiveParallel(heroHand, villainHands, numThreads);
    }
    
    private SimulationResult simulateAdaptiveSingleThread(String heroHand, List<String> villainHands) {
        // Validate input cards and build removeFromDeck set
        Set<String> removeFromDeck = new HashSet<>();
        validateAndCollectCards(heroHand, "Hero", removeFromDeck);
        
        if (villainHands != null && !villainHands.isEmpty()) {
            for (int i = 0; i < villainHands.size(); i++) {
                validateAndCollectCards(villainHands.get(i), "Villain " + (i + 1), removeFromDeck);
            }
        }
        
        // Create deck without hero and villain cards
        List<String> deck = createDeckWithoutCards(removeFromDeck);
        
        int heroWins = 0;
        int iterations = 0;
        
        while (true) {
            iterations++;
            List<String> iterationDeck = new ArrayList<>(deck); // fresh deck for this iteration
            Collections.shuffle(iterationDeck);
            int currentDeckIndex = 0;

            // Deal villain hands if none specified
            List<String> currentVillainHands = new ArrayList<>();
            if (villainHands == null || villainHands.isEmpty()) {
                // Deal first 4 cards for villain (like a real dealer)
                String villainHand = dealSequentialHand(iterationDeck, currentDeckIndex, 4);
                currentVillainHands.add(villainHand);
                currentDeckIndex += 4;
            } else {
                // Use specified villain hands
                currentVillainHands.addAll(villainHands);
            }

            // Deal community cards (next 5 cards after villain cards)
            String communityCards = dealSequentialHand(iterationDeck, currentDeckIndex, 5);

            // Evaluate hands - hero must beat ALL villains
            int heroRank = evaluatePLOHand(heroHand, communityCards);
            boolean heroWinsThis = true;
            
            for (String villainHand : currentVillainHands) {
                int villainRank = evaluatePLOHand(villainHand, communityCards);
                if (villainRank <= heroRank) { // villain wins or ties
                    heroWinsThis = false;
                    break;
                }
            }
            
            if (heroWinsThis) {
                heroWins++;
            }

            // Check stopping criteria at checkpoints
            if (shouldCheckStoppingCriteria(iterations)) {
                double winRate = (double) heroWins / iterations;
                double standardDeviation = calculateStandardDeviation(winRate, iterations);
                double confidenceInterval = calculateConfidenceInterval95(standardDeviation);

                if (iterations >= MIN_ITERATIONS && standardDeviation <= DEFAULT_STOPPING_SD && confidenceInterval <= DEFAULT_STOPPING_CI) {
                    break;
                }
            }
        }
        
        double finalWinRate = (double) heroWins / iterations;
        double finalStandardDeviation = calculateStandardDeviation(finalWinRate, iterations);
        double finalConfidenceInterval = calculateConfidenceInterval95(finalStandardDeviation);
        
        return new SimulationResult(finalWinRate, finalStandardDeviation, finalConfidenceInterval, iterations);
    }
    
    private SimulationResult simulateAdaptiveParallel(String heroHand, List<String> villainHands, int numThreads) {
        // Validate input cards and build removeFromDeck set
        Set<String> removeFromDeck = new HashSet<>();
        validateAndCollectCards(heroHand, "Hero", removeFromDeck);
        
        if (villainHands != null && !villainHands.isEmpty()) {
            for (int i = 0; i < villainHands.size(); i++) {
                validateAndCollectCards(villainHands.get(i), "Villain " + (i + 1), removeFromDeck);
            }
        }
        
        // Create deck without hero and villain cards
        List<String> deck = createDeckWithoutCards(removeFromDeck);
        
        // Shared counters for parallel processing
        final int[] heroWins = {0};
        final int[] totalIterations = {0};
        final Object lock = new Object();
        
        // Create and start worker threads
        Thread[] threads = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                List<String> threadDeck = new ArrayList<>(deck);
                int threadHeroWins = 0;
                int threadIterations = 0;
                
                while (true) {
                    threadIterations++;
                    List<String> iterationDeck = new ArrayList<>(threadDeck);
                    Collections.shuffle(iterationDeck);
                    int currentDeckIndex = 0;

                    // Deal villain hands if none specified
                    List<String> currentVillainHands = new ArrayList<>();
                    if (villainHands == null || villainHands.isEmpty()) {
                        String villainHand = dealSequentialHand(iterationDeck, currentDeckIndex, 4);
                        currentVillainHands.add(villainHand);
                        currentDeckIndex += 4;
                    } else {
                        currentVillainHands.addAll(villainHands);
                    }

                    String communityCards = dealSequentialHand(iterationDeck, currentDeckIndex, 5);

                    int heroRank = evaluatePLOHand(heroHand, communityCards);
                    boolean heroWinsThis = true;
                    
                    for (String villainHand : currentVillainHands) {
                        int villainRank = evaluatePLOHand(villainHand, communityCards);
                        if (villainRank <= heroRank) {
                            heroWinsThis = false;
                            break;
                        }
                    }
                    
                    if (heroWinsThis) {
                        threadHeroWins++;
                    }

                    // Check stopping criteria at checkpoints
                    if (shouldCheckStoppingCriteria(threadIterations)) {
                        synchronized (lock) {
                            int totalWins = heroWins[0] + threadHeroWins;
                            int totalIters = totalIterations[0] + threadIterations;
                            
                            double winRate = (double) totalWins / totalIters;
                            double standardDeviation = calculateStandardDeviation(winRate, totalIters);
                            double confidenceInterval = calculateConfidenceInterval95(standardDeviation);

                            if (totalIters >= MIN_ITERATIONS && standardDeviation <= DEFAULT_STOPPING_SD && confidenceInterval <= DEFAULT_STOPPING_CI) {
                                // Update global counters and break
                                heroWins[0] = totalWins;
                                totalIterations[0] = totalIters;
                                return;
                            }
                        }
                    }
                }
            });
            threads[t].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Simulation interrupted", e);
            }
        }
        
        double finalWinRate = (double) heroWins[0] / totalIterations[0];
        double finalStandardDeviation = calculateStandardDeviation(finalWinRate, totalIterations[0]);
        double finalConfidenceInterval = calculateConfidenceInterval95(finalStandardDeviation);
        
        return new SimulationResult(finalWinRate, finalStandardDeviation, finalConfidenceInterval, totalIterations[0]);
    }
    
    private void validateAndCollectCards(String hand, String playerName, Set<String> removeFromDeck) {
        if (hand == null || hand.length() != 8) {
            throw new IllegalArgumentException(playerName + " hand must be exactly 8 characters (4 cards)");
        }
        
        for (int i = 0; i < 4; i++) {
            String card = hand.substring(i * 2, (i + 1) * 2);
            
            // Check if card is valid (exists in full deck)
            if (!fullDeck.contains(card)) {
                throw new IllegalArgumentException(playerName + " hand contains invalid card: " + card);
            }
            
            // Check if card is already in removeFromDeck (duplicate)
            if (!removeFromDeck.add(card)) {
                throw new IllegalArgumentException("Card " + card + " is used by multiple players");
            }
        }
    }
    
    private List<String> createDeckWithoutCards(Set<String> removeFromDeck) {
        List<String> deck = new ArrayList<>();
        
        for (String card : fullDeck) {
            if (!removeFromDeck.contains(card)) {
                deck.add(card);
            }
        }
        
        return deck;
    }
    
    private String dealSequentialHand(List<String> deck, int startIndex, int numCards) {
        StringBuilder hand = new StringBuilder();
        for (int i = startIndex; i < startIndex + numCards; i++) {
            hand.append(deck.get(i));
        }
        return hand.toString();
    }
    
    private int evaluatePLOHand(String holeCards, String communityCards) {
        // Parse hole cards (4 cards)
        String[] hole = new String[4];
        for (int i = 0; i < 4; i++) {
            hole[i] = holeCards.substring(i * 2, (i + 1) * 2);
        }
        
        // Parse community cards (5 cards)
        String[] community = new String[5];
        for (int i = 0; i < 5; i++) {
            community[i] = communityCards.substring(i * 2, (i + 1) * 2);
        }
        
        int bestRank = Integer.MAX_VALUE;
        
        // Try all possible 2-card combinations from hole cards
        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 4; j++) {
                String[] holeCombo = {hole[i], hole[j]};
                
                // Try all possible 3-card combinations from community cards
                for (int k = 0; k < 3; k++) {
                    for (int l = k + 1; l < 4; l++) {
                        for (int m = l + 1; m < 5; m++) {
                            String[] communityCombo = {community[k], community[l], community[m]};
                            
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
    
    private boolean shouldCheckStoppingCriteria(int iteration) {
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
        return Math.sqrt(winRate * (1 - winRate) / iterations);
    }
    
    private double calculateConfidenceInterval95(double standardDeviation) {
        return CONFIDENCE_LEVEL_95 * standardDeviation;
    }
    
    public static void main(String[] args) {
        PLOSimulationEngine engine = new PLOSimulationEngine();
        
        String heroHand = "2s9s7h2d";
        List<String> villainHands = new ArrayList<>();
        // villainHands.add("AsAdAhAc"); // Uncomment to add specific villain hands
        
        // Use parallel processing with number of CPU cores
        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("Running simulation with " + numThreads + " threads...");
        
        long startTime = System.currentTimeMillis();
        SimulationResult result = engine.simulateAdaptive(heroHand, villainHands, numThreads);
        long endTime = System.currentTimeMillis();
        
        System.out.printf("Win Rate: %.4f%% (SD: %.4f%%, CI: %.4f%%, Iterations: %d)%n", 
                         result.winRate * 100, result.standardDeviation * 100, 
                         result.confidenceInterval * 100, result.iterations);
        System.out.printf("Execution time: %d ms%n", endTime - startTime);
    }
} 