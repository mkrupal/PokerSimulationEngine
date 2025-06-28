package com.plo.simulator;

import org.junit.jupiter.api.Test;

public class PLOSimulationTest {
    
    @Test
    public void testPLOSimulation() {
        System.out.println("=== PLO Monte Carlo Simulation Test (Success Cases) ===");
        
        PLOSimulationEngine engine = new PLOSimulationEngine("normalized_ranked_poker_hands.txt");
        
        // Only valid test cases (no card conflicts)
        String[][] testData = {

            {"KsKh8d7c", "AsAc5d5c", "KK vs AA classic"}
        };
        
        long totalStartTime = System.currentTimeMillis();
        int totalIterations = 0;
        
        for (int i = 0; i < testData.length; i++) {
            String[] testCase = testData[i];
            String heroHand = testCase[0];
            String[] villainHands;
            String description;
            
            if (testCase.length == 2) {
                // Only hero hand provided - will generate random villains
                villainHands = new String[0];
                description = testCase[1];
            } else {
                villainHands = new String[testCase.length - 2]; // Last element is description
                for (int j = 1; j < testCase.length - 1; j++) {
                    villainHands[j - 1] = testCase[j];
                }
                description = testCase[testCase.length - 1];
            }
            
            System.out.println("\n--- Test Case " + (i + 1) + ": " + description + " ---");
            System.out.println("Hero: " + heroHand);
            for (int v = 0; v < villainHands.length; v++) {
                System.out.println("Villain " + (v + 1) + ": " + villainHands[v]);
            }
            
            long startTime = System.currentTimeMillis();
            double winRate = engine.simulate(heroHand, villainHands, 10000);
            long endTime = System.currentTimeMillis();
            long evalTime = endTime - startTime;
            
            System.out.println("Win Rate: " + String.format("%.2f%%", winRate * 100));
            System.out.println("Evaluation Time: " + evalTime + "ms");
            
            totalIterations += 10000;
        }
        
        long totalEndTime = System.currentTimeMillis();
        long totalEvalTime = totalEndTime - totalStartTime;
        
        System.out.println("\n=== Summary ===");
        System.out.println("Total Test Cases: " + testData.length);
        System.out.println("Total Iterations: " + totalIterations);
        System.out.println("Total Evaluation Time: " + totalEvalTime + "ms");
        System.out.println("Average Time per Test: " + (totalEvalTime / testData.length) + "ms");
        System.out.println("Average Time per 10k Iterations: " + (totalEvalTime * 10000 / totalIterations) + "ms");
    }

    @Test
    public void testPLOSimulationFailures() {
        System.out.println("=== PLO Monte Carlo Simulation Test (Failure Cases) ===");
        
        PLOSimulationEngine engine = new PLOSimulationEngine("normalized_ranked_poker_hands.txt");
        
        // Only failure test cases (card conflicts, invalid input, etc.)
        String[][] failureData = {
            {"AsKsQdJc", "AsAdAhAc", "Hero vs Quad Aces (should fail - card conflict)"},
            {"KsKhQdQc", "KsKhQdQc", "Hero vs Self (should fail - card conflict)"}
        };
        
        for (int i = 0; i < failureData.length; i++) {
            String[] testCase = failureData[i];
            String heroHand = testCase[0];
            String[] villainHands = new String[testCase.length - 2];
            for (int j = 1; j < testCase.length - 1; j++) {
                villainHands[j - 1] = testCase[j];
            }
            String description = testCase[testCase.length - 1];
            
            System.out.println("\n--- Failure Test Case " + (i + 1) + ": " + description + " ---");
            System.out.println("Hero: " + heroHand);
            for (int v = 0; v < villainHands.length; v++) {
                System.out.println("Villain " + (v + 1) + ": " + villainHands[v]);
            }
            try {
                engine.simulate(heroHand, villainHands, 10000);
                System.out.println("❌ FAILED: Expected card conflict error but simulation ran successfully");
            } catch (IllegalArgumentException e) {
                System.out.println("✅ PASSED: Card conflict correctly detected: " + e.getMessage());
            }
        }
    }
    
    @Test
    public void testMultipleVillains() {
        System.out.println("=== Multiple Villains Test ===");
        
        PLOSimulationEngine engine = new PLOSimulationEngine("normalized_ranked_poker_hands.txt");
        
        String heroHand = "KsKhQdQc";
        String[] villainHands = {"AsAdAhAc", "2s2h2d2c"}; // Non-conflicting hands
        
        System.out.println("Testing: Hero " + heroHand + " vs 2 Villains");
        for (int i = 0; i < villainHands.length; i++) {
            System.out.println("Villain " + (i + 1) + ": " + villainHands[i]);
        }
        
        double winRate = engine.simulate(heroHand, villainHands, 5000);
        
        System.out.println("=== Results ===");
        System.out.printf("Hero win rate: %.2f%%%n", winRate * 100);
    }
} 