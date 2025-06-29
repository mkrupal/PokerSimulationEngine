package com.plo.simulator;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PLOSimulationEngine {
    
    private final PokerHandCache handCache;
    private final Set<String> fullDeck;
    
    // Statistical constants
    private static final double CONFIDENCE_LEVEL_95 = 1.96;
    private static final double DEFAULT_STOPPING_SD = 0.005; // 0.5% standard deviation threshold
    private static final double DEFAULT_STOPPING_CI = 0.01; // 1% confidence interval threshold
    private static final int MIN_ITERATIONS = 100; // Minimum iterations before allowing early stopping
    private static final int SIMULATION_BATCH_SIZE = 100;
        
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
            // Use parallel processing with number of CPU cores
            int numThreads = Runtime.getRuntime().availableProcessors();
            
            if (numThreads <= 1) {
                return simulateAdaptiveSingleThread(heroHand, villainHands);
            }
            
            return simulateAdaptiveParallel(heroHand, villainHands, numThreads);
        }
        
        private List<String> validateAndCreateDeck(String heroHand, List<String> villainHands) {
            // Validate input cards and build removeFromDeck set
            Set<String> removeFromDeck = new HashSet<>();
            validateAndCollectCards(heroHand, "Hero", removeFromDeck);
            
            if (villainHands != null && !villainHands.isEmpty()) {
                for (int i = 0; i < villainHands.size(); i++) {
                    validateAndCollectCards(villainHands.get(i), "Villain " + (i + 1), removeFromDeck);
                }
            }
            
            // Create deck without hero and villain cards
            return createDeckWithoutCards(removeFromDeck);
        }
        
        private SimulationResult simulateAdaptiveSingleThread(String heroHand, List<String> villainHands) {
            List<String> deck = validateAndCreateDeck(heroHand, villainHands);
            
            int heroWins = 0;
            int iterations = 0;
            
            while (true) {
                // Run a batch of simulations
                SimulationBatchResult batch = runSimulationBatch(heroHand, villainHands, deck);
                heroWins += batch.heroWins;
                iterations += batch.iterations;
                
                // Check stopping criteria only at meaningful checkpoints
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
        
        private static class SimulationBatchResult {
            final int heroWins;
            final int iterations;
            
            SimulationBatchResult(int heroWins, int iterations) {
                this.heroWins = heroWins;
                this.iterations = iterations;
            }
        }
        
        private SimulationBatchResult runSimulationBatch(String heroHand, List<String> villainHands, List<String> deck) {
            int heroWins = 0;
            int iterations = 0;
            
            // Run a batch of simulations
            for (int i = 0; i < SIMULATION_BATCH_SIZE; i++) {
            iterations++;
            List<String> iterationDeck = new ArrayList<>(deck);
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
                heroWins++;
            }
        }
        
        return new SimulationBatchResult(heroWins, iterations);
    }
    
    private SimulationResult simulateAdaptiveParallel(String heroHand, List<String> villainHands, int numThreads) {
        List<String> deck = validateAndCreateDeck(heroHand, villainHands);
        
        // Shared state for coordination
        final int[] totalHeroWins = {0};
        final int[] totalIterations = {0};
        final Object lock = new Object();
        final boolean[] shouldStop = {false};
        
        // Create CompletableFuture tasks
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int t = 0; t < numThreads; t++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                List<String> threadDeck = new ArrayList<>(deck);
                
                while (!shouldStop[0]) {
                    // Run a batch of simulations using the shared method
                    SimulationBatchResult batch = runSimulationBatch(heroHand, villainHands, threadDeck);
                    
                    // Update global counters
                    synchronized (lock) {
                        if (!shouldStop[0]) {
                            totalHeroWins[0] += batch.heroWins;
                            totalIterations[0] += batch.iterations;
                            
                            // Check stopping criteria only at meaningful checkpoints
                            if (shouldCheckStoppingCriteria(totalIterations[0])) {
                                double winRate = (double) totalHeroWins[0] / totalIterations[0];
                                double standardDeviation = calculateStandardDeviation(winRate, totalIterations[0]);
                                double confidenceInterval = calculateConfidenceInterval95(standardDeviation);

                                if (totalIterations[0] >= MIN_ITERATIONS && standardDeviation <= DEFAULT_STOPPING_SD && confidenceInterval <= DEFAULT_STOPPING_CI) {
                                    shouldStop[0] = true;
                                }
                            }
                        }
                    }
                }
            });
            futures.add(future);
        }
        
        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Calculate final results
        double finalWinRate = (double) totalHeroWins[0] / totalIterations[0];
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
        SimulationResult result = engine.simulateAdaptive(heroHand, villainHands);
        long endTime = System.currentTimeMillis();
        
        System.out.printf("Win Rate: %.4f%% (SD: %.4f%%, CI: %.4f%%, Iterations: %d)%n", 
                         result.winRate * 100, result.standardDeviation * 100, 
                         result.confidenceInterval * 100, result.iterations);
        System.out.printf("Execution time: %d ms%n", endTime - startTime);
    }
} 