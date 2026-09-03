package db;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TreeSpeciesBulkUploader {
    private static final String TAG = "TreeSpeciesBulkUploader";
    private static final String PREF_NAME = "TreeSpeciesImporterPrefs";
    private static final String KEY_IMPORT_COMPLETED = "species_import_completed";

    /**
     * Check if this is the first run of the app
     */
    private static boolean isFirstRun(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return !prefs.getBoolean(KEY_IMPORT_COMPLETED, false);
    }

    /**
     * Mark the first run as complete
     */
    private static void markImportComplete(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IMPORT_COMPLETED, true);
        editor.apply();
        Log.d(TAG, "Marked tree species import as completed");
    }

    /**
     * Check if the tree species table is empty
     */
    private static boolean hasTreeSpeciesData(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM TreeSpecies", null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int count = cursor.getInt(0);
                        Log.d(TAG, "TreeSpecies table contains " + count + " records");
                        return count > 0;
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking TreeSpecies table: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Bulk uploads tree species from CSV file to the database.
     * Improved to handle database restoration scenarios.
     */
    public static void bulkUploadTreeSpecies(Context context, SQLiteDatabase db) {
        // Check if table is empty
        boolean isEmpty = isTableEmpty(db);
        boolean firstRun = isFirstRun(context);
        
        if (!isEmpty && !firstRun) {
            Log.d(TAG, "Tree species table is not empty and not first run. Skipping import.");
            return;
        }
        
        if (isEmpty) {
            Log.d(TAG, "Tree species table is empty, will perform import regardless of first run status.");
        } else if (firstRun) {
            Log.d(TAG, "First run detected, will import tree species.");
        }
        
        BufferedReader reader = null;
        try {
            Log.d(TAG, "Starting bulk upload process");
            Log.d(TAG, "Checking assets directory access");
            
            // Open the CSV file from assets
            InputStream inputStream = context.getAssets().open("tree_species.csv");
            Log.d(TAG, "Successfully opened assets directory");
            reader = new BufferedReader(new InputStreamReader(inputStream));
            Log.d(TAG, "Created BufferedReader for CSV file");

            String line;
            int lineCount = 0;
            int successCount = 0;
            
            // Read each line of the CSV file
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(";");

                Log.d(TAG, "Reading line " + (++lineCount) + ": " + line.substring(0, Math.min(line.length(), 50)) + "...");

                // Skip the header row if present
                if (columns[0].equalsIgnoreCase("latinName")) continue;

                String latinName = columns[0].trim();
                String englishName = columns[1].trim();
                String characteristics = columns[2].trim();
                characteristics = characteristics.replaceAll("\\.\\s(?=[^.]*\\.)", ".\n").replaceAll("\n ", "\n");
                String otherNames = columns[3].trim();
                
                // Insert each row into the TreeSpecies table
                ContentValues values = new ContentValues();
                values.put("latinName", latinName);
                values.put("englishName", englishName);
                values.put("characteristics", characteristics);
                values.put("otherNames", otherNames);

                long result = db.insert("TreeSpecies", null, values);
                if (result != -1) {
                    successCount++;
                    Log.d(TAG, "Inserted species: " + latinName);
                } else {
                    Log.e(TAG, "Failed to insert species: " + latinName);
                }
            }

            Log.i(TAG, "Successfully processed " + lineCount + " lines from CSV file");
            Log.i(TAG, "Successfully imported " + successCount + " species");

            if (successCount > 0) {
                markImportComplete(context);
                Log.d(TAG, "Tree species import completed successfully");
            } else {
                Log.e(TAG, "No species were imported successfully");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during bulk upload process: " + e.getMessage());
            if (e instanceof java.io.FileNotFoundException) {
                Log.e(TAG, "Critical error: File 'tree_species.csv' not found in assets folder");
                Log.e(TAG, "Attempted to access: " + context.getAssets().toString());
            }
            e.printStackTrace();
            Log.e(TAG, "Full stack trace: ", e);
        } finally {
            Log.d(TAG, "Cleaning up resources");
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing reader", e);
            }
        }
    }

    /**
     * Check if TreeSpecies table is empty
     */
    private static boolean isTableEmpty(SQLiteDatabase db) {
        try {
            android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM TreeSpecies", null);
            if (cursor != null) {
                cursor.moveToFirst();
                int count = cursor.getInt(0);
                cursor.close();
                Log.d(TAG, "TreeSpecies table has " + count + " rows");
                return count == 0;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if TreeSpecies table is empty: " + e.getMessage());
            return true; // If there's an error, assume it's empty to trigger import
        }
    }
}
