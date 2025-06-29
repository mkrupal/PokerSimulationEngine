package com.plo.simulator;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.*;

public class PLOHoleCardRanker {
    private final PLOSimulationEngine engine;
    private final HandNormalizer normalizer;
    private final String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
    private final String[] suits = {"s", "h", "d", "c"};
    
    public PLOHoleCardRanker(String handCacheFile) {
        this.engine = new PLOSimulationEngine();
        this.normalizer = new HandNormalizer();
    }
    
    public static void main(String[] args) {
        PLOHoleCardRanker ranker = new PLOHoleCardRanker("non_normalized_ranked_poker_hands.txt");
        
        if (args.length > 0 && args[0].equals("test")) {
            ranker.rankTestHands("plo_hand_rankings_test.csv");
        } else {
            ranker.rankAllHands("plo_hand_rankings.csv");
        }
    }
    
    public void rankAllHands(String csvFilename) {
        System.out.println("Generating all possible PLO hole card combinations...");
        
        List<String[]> allHands = generateAll4CardCombinations();
        System.out.println("Total hands to evaluate: " + allHands.size());
        
        // Normalize all hands first to get unique combinations
        System.out.println("Normalizing all hands...");
        Set<String> uniqueNormalizedHands = new HashSet<>();
        
        for (String[] hand : allHands) {
            HandNormalizer.NormalizationResult normalized = normalizer.normalizeHand(hand);
            String normalizedHand = arrayToString(normalized.normalizedCards);
            uniqueNormalizedHands.add(normalizedHand);
        }
        
        System.out.println(allHands.size() + " pre-flop hands normalized to " + uniqueNormalizedHands.size() + " hands");
        
        // Simulate each unique normalized hand
        Map<String, HandResult> results = new HashMap<>();
        long startTime = System.currentTimeMillis();
        int handIndex = 0;
        
        System.out.println("Starting simulation of " + uniqueNormalizedHands.size() + " unique hands...");
        
        for (String normalizedHand : uniqueNormalizedHands) {
            handIndex++;
            
            System.out.println("Processing hand " + handIndex + ": " + normalizedHand);
            
            // Simulate the hand against 1 villain
            PLOSimulationEngine.SimulationResult result = engine.simulateAdaptive(normalizedHand, new ArrayList<>());
            
            HandResult handResult = new HandResult(
                normalizedHand, 
                result.winRate, 
                result.standardDeviation, 
                result.confidenceInterval, 
                result.iterations
            );
            results.put(normalizedHand, handResult);
            
            // Calculate cumulative time
            long currentTime = System.currentTimeMillis();
            long cumulativeTime = currentTime - startTime;
            long minutes = cumulativeTime / 60000;
            long seconds = (cumulativeTime % 60000) / 1000;
            
            System.out.printf("Normalized Hand %d/%d %s %.1f%% %.4f %.4f CumulativeTime %dm%ds (iterations: %d)%n", 
                            handIndex, uniqueNormalizedHands.size(), normalizedHand,
                            result.winRate * 100, result.standardDeviation * 100, result.confidenceInterval * 100,
                            minutes, seconds, result.iterations);
        }
        
        // Sort results by win rate (highest first) and write to CSV
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilename))) {
            writer.println("rank,normalized_hand,win_rate,standard_deviation,confidence_interval,simulations");
            
            List<HandResult> sortedResults = new ArrayList<>(results.values());
            sortedResults.sort((a, b) -> Double.compare(b.winRate, a.winRate));
            
            for (int i = 0; i < sortedResults.size(); i++) {
                HandResult result = sortedResults.get(i);
                writer.printf("%d,%s,%.6f,%.6f,%.6f,%d%n",
                            i + 1, result.normalizedHand, 
                            result.winRate, result.standardDeviation, result.confidenceInterval, result.simulations);
            }
            
            System.out.println("Ranking complete! Results written to: " + csvFilename);
            System.out.println("Total unique hands evaluated: " + sortedResults.size());
            
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }
    
    public void rankTestHands(String csvFilename) {
        System.out.println("Testing PLO hole card ranking with sample hands...");
        
        // Test with a few specific hands
        String[][] testHands = {
            {"As", "Ad", "Ah", "Ac"}, // Quad aces
            {"Ks", "Kh", "Kd", "Kc"}, // Quad kings
            {"As", "Ks", "Qs", "Js"}, // AKQJ suited
            {"As", "Ad", "Ks", "Kd"}, // AAKK double paired
            {"2s", "3s", "4s", "5s"}  // Low suited connector
        };
        
        // Normalize all hands first to get unique combinations
        System.out.println("Normalizing test hands...");
        Set<String> uniqueNormalizedHands = new HashSet<>();
        
        for (String[] hand : testHands) {
            HandNormalizer.NormalizationResult normalized = normalizer.normalizeHand(hand);
            String normalizedHand = arrayToString(normalized.normalizedCards);
            uniqueNormalizedHands.add(normalizedHand);
        }
        
        System.out.println(testHands.length + " pre-flop hands normalized to " + uniqueNormalizedHands.size() + " hands");
        
        // Simulate each unique normalized hand
        Map<String, HandResult> results = new HashMap<>();
        long startTime = System.currentTimeMillis();
        int handIndex = 0;
        
        System.out.println("Starting simulation of " + uniqueNormalizedHands.size() + " unique hands...");
        
        for (String normalizedHand : uniqueNormalizedHands) {
            handIndex++;
            
            System.out.println("Processing hand " + handIndex + ": " + normalizedHand);
            
            // Simulate the hand against 1 villain
            PLOSimulationEngine.SimulationResult result = engine.simulateAdaptive(normalizedHand, new ArrayList<>());
            
            HandResult handResult = new HandResult(
                normalizedHand, 
                result.winRate, 
                result.standardDeviation, 
                result.confidenceInterval, 
                result.iterations
            );
            results.put(normalizedHand, handResult);
            
            // Calculate cumulative time
            long currentTime = System.currentTimeMillis();
            long cumulativeTime = currentTime - startTime;
            long minutes = cumulativeTime / 60000;
            long seconds = (cumulativeTime % 60000) / 1000;
            
            System.out.printf("Normalized Hand %d/%d %s %.1f%% %.4f %.4f CumulativeTime %dm%ds (iterations: %d)%n", 
                            handIndex, uniqueNormalizedHands.size(), normalizedHand,
                            result.winRate * 100, result.standardDeviation * 100, result.confidenceInterval * 100,
                            minutes, seconds, result.iterations);
        }
        
        // Sort results by win rate (highest first) and write to CSV
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilename))) {
            writer.println("rank,normalized_hand,win_rate,standard_deviation,confidence_interval,simulations");
            
            List<HandResult> sortedResults = new ArrayList<>(results.values());
            sortedResults.sort((a, b) -> Double.compare(b.winRate, a.winRate));
            
            for (int i = 0; i < sortedResults.size(); i++) {
                HandResult result = sortedResults.get(i);
                writer.printf("%d,%s,%.6f,%.6f,%.6f,%d%n",
                            i + 1, result.normalizedHand, 
                            result.winRate, result.standardDeviation, result.confidenceInterval, result.simulations);
            }
            
            System.out.println("Test ranking complete! Results written to: " + csvFilename);
            System.out.println("Total hands evaluated: " + sortedResults.size());
            
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }
    
    private List<String[]> generateAll4CardCombinations() {
        List<String[]> allHands = new ArrayList<>();
        String[] deck = new String[52];
        
        int index = 0;
        for (String rank : ranks) {
            for (String suit : suits) {
                deck[index++] = rank + suit;
            }
        }
        
        // Generate all 4-card combinations
        generateCombinations(deck, 4, 0, new String[4], 0, allHands);
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
    
    private static class HandResult {
        final String normalizedHand;
        final double winRate;
        final double standardDeviation;
        final double confidenceInterval;
        final int simulations;
        
        HandResult(String normalizedHand, double winRate, double standardDeviation, 
                  double confidenceInterval, int simulations) {
            this.normalizedHand = normalizedHand;
            this.winRate = winRate;
            this.standardDeviation = standardDeviation;
            this.confidenceInterval = confidenceInterval;
            this.simulations = simulations;
        }
    }
} 