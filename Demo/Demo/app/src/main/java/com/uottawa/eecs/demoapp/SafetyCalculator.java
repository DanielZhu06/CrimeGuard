package com.uottawa.eecs.demoapp;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class SafetyCalculator {

    private static final String TAG = "SafetyCalculator";

    // Crime Weights (Severity)
    private static final Map<String, Integer> CRIME_WEIGHTS = new HashMap<>();

    static {
        CRIME_WEIGHTS.put("hate_crime.csv", 1);
        CRIME_WEIGHTS.put("bike_theft.csv", 2);
        CRIME_WEIGHTS.put("motor_vehicle.csv", 3);
        CRIME_WEIGHTS.put("criminal_offences.csv", 4);
        CRIME_WEIGHTS.put("shootings.csv", 5);
        CRIME_WEIGHTS.put("homicide.csv", 6);
    }

    public static class SafetyReport {
        public String neighbourhood;
        public double dangerScore;
        public String securityLevel;
        public int securityColor; // Resource ID or Color int

        public SafetyReport(String neighbourhood, double dangerScore, String securityLevel, int securityColor) {
            this.neighbourhood = neighbourhood;
            this.dangerScore = dangerScore;
            this.securityLevel = securityLevel;
            this.securityColor = securityColor;
        }
    }

    public static SafetyReport calculateSafetyScore(Context context, String neighbourhood) {
        double totalWeightedScore = 0;
        int totalCrimes = 0;

        try (FileInputStream fis = context.openFileInput("combined.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Format: SourceFile,Date,Location
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String sourceFile = parts[0];
                    String location = parts[2].trim();

                    if (location.equalsIgnoreCase(neighbourhood)) {
                        int weight = CRIME_WEIGHTS.containsKey(sourceFile) ? CRIME_WEIGHTS.get(sourceFile) : 1;
                        totalWeightedScore += weight;
                        totalCrimes++;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error calculating safety score", e);
            return new SafetyReport(neighbourhood, -1, "Error", R.color.security_accent);
        }

        // Logic to interpret score (Example logic, can be refined)
        // Adjust these thresholds based on real data ranges
        String level;
        int color;

        if (totalWeightedScore < 10) {
            level = "Very Safe";
            color = R.color.shield_grey; // Or green if available
        } else if (totalWeightedScore < 50) {
            level = "Safe";
            color = R.color.shield_grey;
        } else if (totalWeightedScore < 150) {
            level = "Moderate Risk";
            color = R.color.security_blue;
        } else {
            level = "High Risk";
            color = R.color.security_accent;
        }

        return new SafetyReport(neighbourhood, totalWeightedScore, level, color);
    }
}
