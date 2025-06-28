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
            
            PLOSimulationEngine.SimulationResult result = engine.simulateAdaptive(heroHand, villainHands);
            
            System.out.println("Final Result: " + String.format("%.4f%% (SD: %.4f%%, CI: %.4f%%, Simulations: %d)", 
                result.winRate * 100, result.standardDeviation * 100, result.confidenceInterval * 100, result.iterations));
        }
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
                engine.simulateAdaptive(heroHand, villainHands);
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
        
        PLOSimulationEngine.SimulationResult result = engine.simulateAdaptive(heroHand, villainHands);
        
        System.out.println("=== Results ===");
        System.out.printf("Hero win rate: %.4f%% (SD: %.4f%%, CI: %.4f%%, Simulations: %d)%n", 
                         result.winRate * 100, result.standardDeviation * 100, 
                         result.confidenceInterval * 100, result.iterations);
    }
} 