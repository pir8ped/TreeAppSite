package util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.john.TreeApp.beans.Location;
import com.john.TreeApp.beans.Tree;
import com.john.TreeApp.beans.Collection;
import db.DatabaseCreator;

import db.TreeService;
import db.CollectionDAO;
import db.TreeDAO;
import db.LocationDAO;
import db.NoteDAO;

import java.io.*;
import java.util.*;
import java.sql.Date;

public class TreeImporter {
    private static final String TAG = "TreeImporter";
    private static final String CSV_FILE = "476trees.txt";
    private static final String PREF_NAME = "TreeImporterPrefs";
    private static final String KEY_IMPORT_COMPLETED = "import_completed";
    private static final String VALLEY_COLLECTION = "Valley";
    private final TreeService treeService;
    private final SQLiteDatabase database;
    private final CollectionDAO collectionDAO;
    private final boolean isTestMode;

    /**
     * Constructor with injected DAOs
     */
    public TreeImporter(Context context, SQLiteDatabase database,
            CollectionDAO collectionDAO, TreeDAO treeDAO,
            LocationDAO locationDAO, NoteDAO noteDAO,
            boolean isTestMode) {
        this.database = database;
        this.collectionDAO = collectionDAO;

        // Create TreeService with all injected DAOs (including Scion DAOs)
        this.treeService = new TreeService(treeDAO, locationDAO, noteDAO, collectionDAO,
                DatabaseCreator.getInstance().getScionDAO(),
                DatabaseCreator.getInstance().getTreeScionDAO());

        this.isTestMode = isTestMode;
        Log.d(TAG, "TreeImporter constructed with injected DAOs. isTestMode: " + isTestMode);

        if (isTestMode) {
            forceReimport(context);
            Log.d(TAG, "Force reimport called. Import status reset.");
        }
    }

    /**
     * Legacy constructor for backward compatibility
     */
    public TreeImporter(Context context, SQLiteDatabase database, boolean isTestMode) {
        // Call the main constructor with DAOs from DatabaseCreator
        this(context, database,
                DatabaseCreator.getInstance().getCollectionDAO(),
                DatabaseCreator.getInstance().getTreeDAO(),
                DatabaseCreator.getInstance().getLocationDAO(),
                DatabaseCreator.getInstance().getNoteDAO(),
                isTestMode);
    }

    public TreeImporter(Context context, SQLiteDatabase database) {
        this(context, database, false);
    }

    private void forceReimport(Context context) {
        Log.d(TAG, "Test mode: Forcing reimport by clearing import status");
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IMPORT_COMPLETED, false);
        editor.apply();

        // Verify the preference was set correctly
        boolean importCompletedAfterReset = prefs.getBoolean(KEY_IMPORT_COMPLETED, true);
        Log.d(TAG, "After reset, import_completed is: " + importCompletedAfterReset);
    }

    private boolean shouldSkipSpecies(String latinName) {
        return latinName.toLowerCase().startsWith("juglans") ||
                latinName.toLowerCase().startsWith("vitis") ||
                latinName.toLowerCase().startsWith("ficus") ||
                latinName.toLowerCase().startsWith("salix");
    }

    private boolean isFirstRun(Context context) {
        try {
            // Simply check if Tree table has any entries
            String query = "SELECT COUNT(*) FROM Tree";
            android.database.Cursor cursor = database.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                cursor.close();

                Log.d(TAG, "Found " + count + " trees in database");
                return count == 0; // Import only if there are no trees
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for existing trees: " + e.getMessage());
        }

        // If we can't check the database, assume we need to import
        return true;
    }

    private void markImportComplete(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IMPORT_COMPLETED, true);
        editor.apply();
        Log.d(TAG, "Marked tree import as completed");
    }

    private int ensureValleyCollectionExists() {
        Log.d(TAG, "Checking for Valley collection");
        List<Collection> collections = collectionDAO.getAllCollections();
        Log.d(TAG, "Found " + collections.size() + " collections in database");

        // First check if Valley collection already exists
        for (Collection collection : collections) {
            Log.d(TAG, "Checking collection: " + collection.getName());
            if (VALLEY_COLLECTION.equals(collection.getName())) {
                Log.d(TAG, "Found existing Valley collection with ID: " + collection.getId());
                return collection.getId();
            }
        }

        return collectionDAO.addCollection(new Collection("Valley", true));

    }

    private boolean treeSpeciesExists(String latinName) {
        String query = "SELECT 1 FROM TreeSpecies WHERE latinName COLLATE NOCASE = ?";
        Log.d(TAG, "Checking if tree species exists: " + latinName);
        try (android.database.Cursor cursor = database.rawQuery(query, new String[] { latinName })) {
            boolean exists = cursor.moveToFirst();
            Log.d(TAG, "Species " + latinName + " exists: " + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if species exists: " + e.getMessage());
            return false;
        }
    }

    private Date parseDatePlanted(String timestamp) {
        try {
            // Convert milliseconds timestamp to Date
            long millis = Long.parseLong(timestamp);
            return new Date(millis);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid date format: " + timestamp);
            return null;
        }
    }

    public void importTrees(Context context) throws IOException {
        Log.d(TAG, "Starting tree import process");

        // Check if this is the first run
        if (!isFirstRun(context)) {
            Log.d(TAG, "Trees have already been imported. Skipping import.");
            // No longer needed for testing - return to prevent reimporting
            return;
        }

        int collectionId = ensureValleyCollectionExists();
        Log.d(TAG, "Valley collection ID for importing trees: " + collectionId);

        List<String> skippedRows = new ArrayList<>();
        int totalRows = 0;
        int successfulImports = 0;
        int skippedJuglans = 0;

        Log.d(TAG, "About to open CSV file: " + CSV_FILE);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(CSV_FILE)));
            Log.d(TAG, "CSV file opened successfully");

            String line;
            reader.readLine(); // Skip header
            Log.d(TAG, "Skipped header row");

            while ((line = reader.readLine()) != null) {
                totalRows++;
                if (totalRows % 50 == 0) {
                    Log.d(TAG, "Processed " + totalRows + " rows so far");
                }

                String[] data = line.split(";");
                if (data.length < 9) {
                    Log.w(TAG, "Skipping invalid row (insufficient columns): " + line);
                    continue;
                }

                String latinName = data[1].trim();
                Log.d(TAG, "Processing tree: " + latinName);

                // Skip Juglans trees
                if (shouldSkipSpecies(latinName)) {
                    Log.d(TAG, "Skipping Juglans tree: " + latinName);
                    skippedJuglans++;
                    continue;
                }

                // Check if the species exists in TreeSpecies table
                if (!treeSpeciesExists(latinName)) {
                    Log.w(TAG, "Species not found in TreeSpecies table: " + latinName);
                    skippedRows.add(line);
                    continue;
                }

                double latitude = Double.parseDouble(data[2].trim());
                double longitude = Double.parseDouble(data[3].trim());
                String datePlantedStr = data[4].trim();
                String notes = data[5].trim();
                String origin = data[6].trim();
                String rootstock = data[7].trim();
                String variety = data[8].trim();

                // Create Location object
                Location location = new Location();
                location.setLatitude(latitude);
                location.setLongitude(longitude);

                // Create Tree object
                Tree tree = new Tree.Builder(latinName)
                        .origin(origin)
                        .rootstock(rootstock)
                        .variety(variety)
                        .datePlanted(parseDatePlanted(datePlantedStr))
                        .collectionId(collectionId)
                        .build();

                // Use TreeService to add the tree and its note
                int treeId = treeService.addTree(tree, location, notes);
                if (treeId == -1) {
                    Log.e(TAG, "Failed to import tree: " + latinName);
                    skippedRows.add(line);
                } else {
                    successfulImports++;
                    Log.d(TAG, "Successfully imported tree: " + latinName + " with ID: " + treeId);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV file: " + e.getMessage());
            throw e;
        }

        // Output summary
        Log.d(TAG, "Import Summary:");
        Log.d(TAG, "Total rows processed: " + totalRows);
        Log.d(TAG, "Successfully imported: " + successfulImports);
        Log.d(TAG, "Skipped rows (invalid/not found): " + skippedRows.size());
        Log.d(TAG, "Skipped Juglans trees: " + skippedJuglans);

        // Output skipped rows
        if (!skippedRows.isEmpty()) {
            Log.d(TAG, "\nSkipped rows (species not found in database):");
            for (String row : skippedRows) {
                Log.d(TAG, row);
            }
        }

        // Mark import as completed only if we got here without errors
        markImportComplete(context);
        Log.d(TAG, "Tree import completed successfully");

    }
}