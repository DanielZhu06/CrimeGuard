package com.uottawa.eecs.demoapp;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class CsvCombiner {

    private static final String TAG = "CsvCombiner";

    public static void combineCsvFiles(Context context, String[] fileNames, String outputFileName) {
        try (FileOutputStream fos = context.openFileOutput(outputFileName, Context.MODE_PRIVATE)) {

            // Write header
            fos.write("SOURCE,OCC_DATE,NB_NAME_EN\n".getBytes());

            for (String fileName : fileNames) {
                try (FileInputStream fis = context.openFileInput(fileName);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

                    String line;
                    boolean firstLine = true;

                    while ((line = reader.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false; // skip header of individual CSV
                            continue;
                        }

                        // Add filename as first column
                        String combinedLine = fileName + "," + line + "\n";
                        fos.write(combinedLine.getBytes());
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error reading file " + fileName, e);
                }
            }

            Log.i(TAG, "Combined CSV saved: " + context.getFilesDir() + "/" + outputFileName);

        } catch (Exception e) {
            Log.e(TAG, "Error writing combined CSV", e);
        }
    }
}
