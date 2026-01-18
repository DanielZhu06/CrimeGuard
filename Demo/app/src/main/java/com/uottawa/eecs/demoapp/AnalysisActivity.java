package com.uottawa.eecs.demoapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AnalysisActivity extends AppCompatActivity {

    public static final String EXTRA_NEIGHBOURHOOD = "extra_neighbourhood";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        String neighbourhoodStr = getIntent().getStringExtra(EXTRA_NEIGHBOURHOOD);
        if (neighbourhoodStr == null)
            neighbourhoodStr = "Unknown";
        final String neighbourhood = neighbourhoodStr;

        TextView title = findViewById(R.id.neighbourhoodTitle);
        title.setText(neighbourhood);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        Button predictButton = findViewById(R.id.predictButton);
        predictButton.setOnClickListener(v -> predictNextYear(neighbourhood));

        performAnalysis(neighbourhood);
    }

    private void predictNextYear(String neighbourhood) {
        new Thread(() -> {
            SafetyCalculator.SafetyReport prediction = SafetyCalculator.predictNextYearScore(this, neighbourhood);

            runOnUiThread(() -> {
                if (prediction != null) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Safety Prediction")
                            .setMessage("Based on historical data, the predicted danger score for next year is "
                                    + String.format("%.1f", prediction.dangerScore) + ".\n\n"
                                    + "Predicted Status: " + prediction.securityLevel)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    android.widget.Toast
                            .makeText(this, "Not enough data to predict trends.", android.widget.Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }).start();
    }

    private void performAnalysis(String neighbourhood) {
        new Thread(() -> {
            SafetyCalculator.SafetyReport report = SafetyCalculator.calculateSafetyScore(this, neighbourhood);

            runOnUiThread(() -> {
                TextView scoreText = findViewById(R.id.securityScoreText);
                TextView levelText = findViewById(R.id.securityLevelText);

                scoreText.setText(String.format("%.1f", report.dangerScore));
                scoreText.setTextColor(getResources().getColor(report.securityColor, null));

                levelText.setText(report.securityLevel);
                levelText.setTextColor(getResources().getColor(report.securityColor, null));

                populateCrimeStats(report);
            });
        }).start();
    }

    private void populateCrimeStats(SafetyCalculator.SafetyReport report) {
        android.widget.LinearLayout container = findViewById(R.id.crimeStatsContainer);
        container.removeAllViews();

        java.util.Map<String, CrimeConfig> configMap = new java.util.HashMap<>();
        configMap.put("homicide.csv", new CrimeConfig("Homicide", R.drawable.ic_homicide));
        configMap.put("bike_theft.csv", new CrimeConfig("Bike Theft", R.drawable.ic_bike));
        configMap.put("motor_vehicle.csv", new CrimeConfig("Vehicle Theft", R.drawable.ic_car));
        configMap.put("shootings.csv", new CrimeConfig("Shootings", R.drawable.ic_target));
        configMap.put("criminal_offences.csv", new CrimeConfig("Criminal Offences", R.drawable.ic_warning));
        configMap.put("hate_crime.csv", new CrimeConfig("Hate Crime", R.drawable.ic_warning));

        // Find max count to scale progress bars
        int maxCount = 0;
        for (int count : report.crimeCounts.values()) {
            if (count > maxCount)
                maxCount = count;
        }
        if (maxCount == 0)
            maxCount = 1; // Avoid divide by zero

        for (java.util.Map.Entry<String, Integer> entry : report.crimeCounts.entrySet()) {
            String file = entry.getKey();
            int count = entry.getValue();
            CrimeConfig config = configMap.getOrDefault(file,
                    new CrimeConfig("Unknown (" + file + ")", R.drawable.ic_warning));

            android.view.View view = getLayoutInflater().inflate(R.layout.item_crime_stat, container, false);

            android.widget.ImageView icon = view.findViewById(R.id.crimeIcon);
            icon.setImageResource(config.iconRes);

            TextView name = view.findViewById(R.id.crimeName);
            name.setText(config.name);

            TextView countText = view.findViewById(R.id.crimeCount);
            countText.setText(String.valueOf(count));

            android.widget.ProgressBar progress = view.findViewById(R.id.crimeProgress);
            progress.setMax(maxCount);
            progress.setProgress(count);

            container.addView(view);
        }
    }

    private static class CrimeConfig {
        String name;
        int iconRes;

        CrimeConfig(String name, int iconRes) {
            this.name = name;
            this.iconRes = iconRes;
        }
    }
}
