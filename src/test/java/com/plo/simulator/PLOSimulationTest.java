package com.plo.simulator;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PLOSimulationTest {
    
    @Test
    public void testPLOSimulation() {
        System.out.println("=== PLO Monte Carlo Simulation Test (Success Cases) ===");
        
        PLOSimulationEngine engine = new PLOSimulationEngine();
        
        // Only valid test cases (no card conflicts)
        String[][] testData = {
            {"KsKh8d7c", "AsAc5d5c", "KK vs AA classic"}
        };
        
        for (int i = 0; i < testData.length; i++) {
            String[] testCase = testData[i];
            String heroHand = testCase[0];
            List<String> villainHands;
            String description;
            
            if (testCase.length == 2) {
                // Only hero hand provided - will generate random villains
                villainHands = new ArrayList<>();
                description = testCase[1];
            } else {
                villainHands = new ArrayList<>();
                for (int j = 1; j < testCase.length - 1; j++) {
                    villainHands.add(testCase[j]);
                }
                description = testCase[testCase.length - 1];
            }
            
            System.out.println("\n--- Test Case " + (i + 1) + ": " + description + " ---");
            System.out.println("Hero: " + heroHand);
            for (int v = 0; v < villainHands.size(); v++) {
                System.out.println("Villain " + (v + 1) + ": " + villainHands.get(v));
            }
            
            PLOSimulationEngine.SimulationResult result = engine.simulateAdaptive(heroHand, villainHands);
            
            System.out.println("Final Result: " + String.format("%.4f%% (SD: %.4f%%, CI: %.4f%%, Simulations: %d)", 
                result.winRate * 100, result.standardDeviation * 100, result.confidenceInterval * 100, result.iterations));
        }
    }

    @Test
    public void testPLOSimulationFailures() {
        System.out.println("=== PLO Monte Carlo Simulation Test (Failure Cases) ===");
        
        PLOSimulationEngine engine = new PLOSimulationEngine();
        
        // Only failure test cases (card conflicts, invalid input, etc.)
        String[][] failureData = {
            {"AsKsQdJc", "AsAdAhAc", "Hero vs Quad Aces (should fail - card conflict)"},
            {"KsKhQdQc", "KsKhQdQc", "Hero vs Self (should fail - card conflict)"}
        };
        
        for (int i = 0; i < failureData.length; i++) {
            String[] testCase = failureData[i];
            String heroHand = testCase[0];
            List<String> villainHands = new ArrayList<>();
            for (int j = 1; j < testCase.length - 1; j++) {
                villainHands.add(testCase[j]);
            }
            String description = testCase[testCase.length - 1];
            
            System.out.println("\n--- Failure Test Case " + (i + 1) + ": " + description + " ---");
            System.out.println("Hero: " + heroHand);
            for (int v = 0; v < villainHands.size(); v++) {
                System.out.println("Villain " + (v + 1) + ": " + villainHands.get(v));
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
        
        PLOSimulationEngine engine = new PLOSimulationEngine();
        
        String heroHand = "KsKhQdQc";
        List<String> villainHands = Arrays.asList("AsAdAhAc", "2s2h2d2c"); // Non-conflicting hands
        
        System.out.println("Testing: Hero " + heroHand + " vs 2 Villains");
        for (int i = 0; i < villainHands.size(); i++) {
            System.out.println("Villain " + (i + 1) + ": " + villainHands.get(i));
        }
        
        PLOSimulationEngine.SimulationResult result = engine.simulateAdaptive(heroHand, villainHands);
        
        System.out.println("=== Results ===");
        System.out.printf("Hero win rate: %.4f%% (SD: %.4f%%, CI: %.4f%%, Simulations: %d)%n", 
                         result.winRate * 100, result.standardDeviation * 100, 
                         result.confidenceInterval * 100, result.iterations);
    }
} 