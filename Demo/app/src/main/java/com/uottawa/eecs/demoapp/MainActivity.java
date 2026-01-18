package com.uottawa.eecs.demoapp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Yellowcake";
    private static final String API_KEY = "yc_live_KVPBaZo5WhrfM15_knduFTyAg8yx-O8Mm0EK_4CDhgQ=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        if (useTestData()) {
            new Thread(this::loadTestData).start();
        } else {
            new Thread(this::fetchAllCsv).start();
        }
    }

    private void initUI() {
    }

    private boolean useTestData() {
        return true; // Set to true to use test data
    }

    private void loadTestData() {
        try (java.io.InputStream is = getAssets().open("test_data.csv");
                java.io.FileOutputStream fos = openFileOutput("combined.csv", MODE_PRIVATE)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            Log.i(TAG, "Test data loaded to combined.csv");

            // Load locations for ListView
            runOnUiThread(this::loadLocations);

        } catch (IOException e) {
            Log.e(TAG, "Error loading test data", e);
        }
    }

    private void fetchAllCsv() {
        // Example URLs and file names
        fetchAndSaveCsv(
                "https://services7.arcgis.com/2vhcNzw0NfUwAD3d/arcgis/rest/services/Homicide_Open_Data/FeatureServer/0/query?where=1%3D1&outFields=OCC_DATE,NB_NAME_EN&outSR=4326&f=json",
                "Get the date the crime was committed and location",
                "homicide.csv");

        fetchAndSaveCsv(
                "https://services7.arcgis.com/2vhcNzw0NfUwAD3d/arcgis/rest/services/Bike_Theft_Open_Data/FeatureServer/0/query?where=1%3D1&outFields=OCC_DATE,NB_NAME_EN&outSR=4326&f=json",
                "Get the date the crime was committed and location",
                "bike_theft.csv");

        fetchAndSaveCsv(
                "https://services7.arcgis.com/2vhcNzw0NfUwAD3d/arcgis/rest/services/Criminal_Offences_Open_Data/FeatureServer/0/query?where=1%3D1&outFields=OCC_DATE,NB_NAME_EN&outSR=4326&f=json",
                "Get the date the crime was committed and location",
                "criminal_offences.csv");

        fetchAndSaveCsv(
                "https://services7.arcgis.com/2vhcNzw0NfUwAD3d/arcgis/rest/services/Hate_Crime_Open_Data/FeatureServer/0/query?where=1%3D1&outFields=OCC_DATE,NB_NAME_EN&outSR=4326&f=json",
                "Get the date the crime was committed and location",
                "hate_crime.csv");

        fetchAndSaveCsv(
                "https://services7.arcgis.com/2vhcNzw0NfUwAD3d/arcgis/rest/services/Shootings_Open_Data/FeatureServer/0/query?where=1%3D1&outFields=OCC_DATE,NB_NAME_EN&outSR=4326&f=json",
                "Get the date the crime was committed and location",
                "shootings.csv");

        fetchAndSaveCsv(
                "https://services7.arcgis.com/2vhcNzw0NfUwAD3d/arcgis/rest/services/Auto_Theft_Open_Data/FeatureServer/0/query?where=1%3D1&outFields=NB_NAME_EN,OCC_DATE&outSR=4326&f=json",
                "Get the date the crime was committed and location",
                "motor_vehicle.csv");

        String[] allCsvs = {
                "homicide.csv",
                "bike_theft.csv",
                "criminal_offences.csv",
                "hate_crime.csv",
                "shootings.csv",
                "motor_vehicle.csv"
        };

        CsvCombiner.combineCsvFiles(this, allCsvs, "combined.csv");

        // Load locations for ListView
        runOnUiThread(this::loadLocations);
    }

    private void loadLocations() {
        java.util.List<String> locations = new java.util.ArrayList<>();
        try (java.io.FileInputStream fis = openFileInput("combined.csv");
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(fis))) {

            String line;
            boolean isFirstLine = true;
            // Use a Set to avoid duplicates
            java.util.Set<String> uniqueLocations = new java.util.HashSet<>();

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    uniqueLocations.add(parts[2].trim());
                }
            }
            locations.addAll(uniqueLocations);
            java.util.Collections.sort(locations); // Sort alphabetically

        } catch (java.io.IOException e) {
            Log.e(TAG, "Error reading combined.csv for list", e);
        }

        if (!locations.isEmpty()) {
            android.widget.ListView listView = findViewById(R.id.neighbourhoodListView);
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                    this,
                    R.layout.item_neighbourhood,
                    locations);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedNeighbourhood = locations.get(position);
                android.content.Intent intent = new android.content.Intent(MainActivity.this, AnalysisActivity.class);
                intent.putExtra(AnalysisActivity.EXTRA_NEIGHBOURHOOD, selectedNeighbourhood);
                startActivity(intent);
            });
        }
    }

    private void fetchAndSaveCsv(String url, String prompt, String fileName) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        String jsonBody = "{\n" +
                "  \"url\": \"" + url + "\",\n" +
                "  \"prompt\": \"" + prompt + "\"\n" +
                "}";

        Request request = new Request.Builder()
                .url("https://api.yellowcake.dev/v1/extract-stream")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-API-Key", API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                Log.e(TAG, "Request failed: " + response.code());
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                Log.d(TAG, line);

                // Only process final result line
                if (line.startsWith("data:") && line.contains("\"success\":true")) {
                    String jsonPart = line.substring(line.indexOf("{")).trim();

                    try {
                        JSONObject obj = new JSONObject(jsonPart);
                        JSONArray data = obj.getJSONArray("data");

                        try (FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE)) {
                            fos.write("Date,Location\n".getBytes());

                            for (int i = 0; i < data.length(); i++) {
                                JSONObject entry = data.getJSONObject(i);
                                String date = entry.optString("crime_date").replace(",", "");
                                String location = entry.optString("location").replace(",", "");
                                fos.write((date + "," + location + "\n").getBytes());
                            }
                        }

                        Log.i(TAG, "CSV file saved: " + getFilesDir() + "/" + fileName);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing JSON or writing CSV", e);
                    }
                    break; // Stop after final result
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error during network request", e);
        }
    }
}
