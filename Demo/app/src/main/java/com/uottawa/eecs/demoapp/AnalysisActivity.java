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

        String neighbourhood = getIntent().getStringExtra(EXTRA_NEIGHBOURHOOD);
        if (neighbourhood == null)
            neighbourhood = "Unknown";

        TextView title = findViewById(R.id.neighbourhoodTitle);
        title.setText(neighbourhood);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        performAnalysis(neighbourhood);
    }

    private void performAnalysis(String neighbourhood) {
        // Run in background
        new Thread(() -> {
            SafetyCalculator.SafetyReport report = SafetyCalculator.calculateSafetyScore(this, neighbourhood);

            runOnUiThread(() -> {
                TextView scoreText = findViewById(R.id.securityScoreText);
                TextView levelText = findViewById(R.id.securityLevelText);

                scoreText.setText(String.format("%.1f", report.dangerScore));
                scoreText.setTextColor(getResources().getColor(report.securityColor, null));

                levelText.setText(report.securityLevel);
                levelText.setTextColor(getResources().getColor(report.securityColor, null));
            });
        }).start();
    }
}
