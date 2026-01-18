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
        public int securityColor;
        public Map<String, Integer> crimeCounts;

        public SafetyReport(String neighbourhood, double dangerScore, String securityLevel, int securityColor,
                Map<String, Integer> crimeCounts) {
            this.neighbourhood = neighbourhood;
            this.dangerScore = dangerScore;
            this.securityLevel = securityLevel;
            this.securityColor = securityColor;
            this.crimeCounts = crimeCounts;
        }
    }

    public static SafetyReport calculateSafetyScore(Context context, String neighbourhood) {
        double totalWeightedScore = 0;
        int totalCrimes = 0;
        Map<String, Integer> counts = new HashMap<>();

        // Initialize counts
        for (String key : CRIME_WEIGHTS.keySet()) {
            counts.put(key, 0);
        }

        try (FileInputStream fis = context.openFileInput("combined.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String sourceFile = parts[0];
                    String location = parts[2].trim();

                    if (location.equalsIgnoreCase(neighbourhood)) {
                        int weight = CRIME_WEIGHTS.containsKey(sourceFile) ? CRIME_WEIGHTS.get(sourceFile) : 1;
                        totalWeightedScore += weight;
                        totalCrimes++;

                        if (counts.containsKey(sourceFile)) {
                            counts.put(sourceFile, counts.get(sourceFile) + 1);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error calculating safety score", e);
            return new SafetyReport(neighbourhood, -1, "Error", R.color.security_accent, new HashMap<>());
        }

        String level;
        int color;

        if (totalWeightedScore < 10) {
            level = "Very Safe";
            color = R.color.safety_very_safe;
        } else if (totalWeightedScore < 40) {
            level = "Safe";
            color = R.color.safety_safe;
        } else if (totalWeightedScore < 100) {
            level = "Moderate Risk";
            color = R.color.safety_moderate;
        } else {
            level = "High Risk";
            color = R.color.safety_dangerous;
        }

        return new SafetyReport(neighbourhood, totalWeightedScore, level, color, counts);
    }

    public static SafetyReport predictNextYearScore(Context context, String neighbourhood) {
        Map<Integer, Double> yearlyScores = new HashMap<>();
        int maxYear = 0;

        try (FileInputStream fis = context.openFileInput("combined.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String sourceFile = parts[0];
                    String dateStr = parts[1].trim(); // expected format YYYY/MM/DD
                    String location = parts[2].trim();

                    if (location.equalsIgnoreCase(neighbourhood)) {
                        int year = 0;
                        try {
                            String yearStr = dateStr.split("/")[0];
                            year = Integer.parseInt(yearStr);
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                            continue;
                        }

                        if (year > maxYear)
                            maxYear = year;

                        int weight = CRIME_WEIGHTS.containsKey(sourceFile) ? CRIME_WEIGHTS.get(sourceFile) : 1;
                        yearlyScores.put(year, yearlyScores.getOrDefault(year, 0.0) + weight);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error predicting score", e);
            return null;
        }

        if (yearlyScores.size() < 2) {
            return null; // Not enough data points
        }

        // Linear Regression
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;
        int n = yearlyScores.size();

        for (Map.Entry<Integer, Double> entry : yearlyScores.entrySet()) {
            int x = entry.getKey();
            double y = entry.getValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        int nextYear = maxYear + 1;
        double predictedScore = slope * nextYear + intercept;

        if (predictedScore < 0)
            predictedScore = 0;

        String level;
        int color;

        if (predictedScore < 10) {
            level = "Very Safe";
            color = R.color.safety_very_safe;
        } else if (predictedScore < 40) {
            level = "Safe";
            color = R.color.safety_safe;
        } else if (predictedScore < 100) {
            level = "Moderate Risk";
            color = R.color.safety_moderate;
        } else {
            level = "High Risk";
            color = R.color.safety_dangerous;
        }

        return new SafetyReport(neighbourhood, predictedScore, level, color, new HashMap<>());
    }
}
