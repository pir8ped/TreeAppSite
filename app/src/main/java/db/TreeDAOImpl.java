package db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.john.TreeApp.beans.Location;
import com.john.TreeApp.beans.Tree;
import com.john.TreeApp.beans.TreeStatistics;
import com.john.TreeApp.beans.utilBean.TreeForMap;
import com.john.TreeApp.beans.utilBean.TreeGroup;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TreeDAOImpl extends DAOBase implements TreeDAO {
    private Context context;
    private CollectionDAO collectionDAO;

    /**
     * Default constructor that uses a writable database since this DAO peite
     * operations.
     * Note: This constructor should only be used when context is not needed for
     * collection operations.
     */
    public TreeDAOImpl() {
        super();
        collectionDAO = new CollectionDAOImpl();
    }

    @Override
    public TreeStatistics getTreeStatistics(int collectionId) {
        if (!isDatabaseReady()) {
            return new TreeStatistics();
        }

        TreeStatistics stats = new TreeStatistics();
        Cursor cursor = null;

        try {
            // Get status counts for PLANTED trees (locationId is NOT NULL)
            String statusQuery = "SELECT status, COUNT(*) FROM Tree WHERE collectionId = ? AND locationId IS NOT NULL GROUP BY status";
            cursor = getDatabase().rawQuery(statusQuery, new String[]{String.valueOf(collectionId)});
            while (cursor.moveToNext()) {
                String status = cursor.getString(0);
                int count = cursor.getInt(1);
                if ("verified".equalsIgnoreCase(status)) {
                    stats.setVerifiedCount(count);
                } else if ("unverified".equalsIgnoreCase(status)) {
                    stats.setUnverifiedCount(count);
                } else if ("lost".equalsIgnoreCase(status)) {
                    stats.setLostCount(count);
                }
            }
            closeCursor(cursor);

            // Get unique species count for PLANTED trees
            String speciesQuery = "SELECT COUNT(DISTINCT UPPER(latinName)) FROM Tree WHERE collectionId = ? AND locationId IS NOT NULL";
            cursor = getDatabase().rawQuery(speciesQuery, new String[]{String.valueOf(collectionId)});
            if (cursor.moveToFirst()) {
                stats.setSpeciesCount(cursor.getInt(0));
            }
        } catch (Exception e) {
            Log.e("TreeDAO", "Error getting tree statistics", e);
        } finally {
            closeCursor(cursor);
        }

        return stats;
    }

    @Override
    public boolean updateTree(Tree tree) {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return false;
        }

        final boolean[] success = { false };
        executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("locationId", tree.getLocationId());
            values.put("collectionId", tree.getCollectionId());
            values.put("origin", tree.getOrigin());
            values.put("rootstock", tree.getRootstock());
            values.put("variety", tree.getVariety());
            values.put("label", tree.getLabel());
            values.put("located", tree.getLocated());
            values.put("datePlanted", tree.getDatePlanted() != null ? tree.getDatePlanted().toString() : null);
            values.put("status", "verified");

            String selection = "treeID = ?";
            String[] selectionArgs = { String.valueOf(tree.getTreeId()) };

            int rowsAffected = getDatabase().update("Tree", values, selection, selectionArgs);
            success[0] = rowsAffected > 0;
        });

        return success[0];
    }

    @Override
    public boolean updateTreeStatus(long treeId, String status) {
        if (!isDatabaseReady()) {
            return false;
        }
        final boolean[] success = { false };
        executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("status", status);
            int rows = getDatabase().update("Tree", values, "treeId = ?", new String[] { String.valueOf(treeId) });
            success[0] = rows > 0;
        });
        return success[0];
    }

    @Override
    public List<TreeForMap> getTreesForMap(int collectionId) {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return new ArrayList<>();
        }

        List<TreeForMap> trees = new ArrayList<>();
        String query = "SELECT t.treeId, t.latinName, t.collectionId, s.englishName, t.variety, t.rootstock, t.label, l.latitude, l.longitude, " +
                "(SELECT imagePath FROM Image WHERE treeId = t.treeId ORDER BY dateAdded DESC LIMIT 1) AS latestImagePath " +
                "FROM Tree t " +
                "JOIN Location l ON t.locationId = l.locationId " +
                "JOIN TreeSpecies s ON UPPER(t.latinName) = UPPER(s.latinName) " +
                "WHERE t.locationId IS NOT NULL " +
                "AND t.collectionId IS NOT NULL " +
                "AND (t.status IS NULL OR LOWER(t.status) != 'lost') " +
                "AND t.collectionId = ?";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, new String[] { String.valueOf(collectionId) });
            Log.d("TreeDAO", "getTreesForMap: collectionId=" + collectionId + ", found " + cursor.getCount() + " trees");
            while (cursor.moveToNext()) {
                trees.add(treeForMapFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public List<TreeForMap> getTreesForMapAllCollections() {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return new ArrayList<>();
        }

        List<TreeForMap> trees = new ArrayList<>();
        String query = "SELECT t.treeId, t.latinName, t.collectionId, s.englishName, t.variety, t.rootstock, t.label, l.latitude, l.longitude, " +
                "(SELECT imagePath FROM Image WHERE treeId = t.treeId ORDER BY dateAdded DESC LIMIT 1) AS latestImagePath " +
                "FROM Tree t " +
                "JOIN Location l ON t.locationId = l.locationId " +
                "JOIN TreeSpecies s ON UPPER(t.latinName) = UPPER(s.latinName) " +
                "WHERE t.locationId IS NOT NULL " +
                "AND t.collectionId IS NOT NULL " +
                "AND LOWER(t.status) = 'verified'";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, null);
            Log.d("TreeDAO", "getTreesForMapAllCollections: found " + cursor.getCount() + " trees");
            while (cursor.moveToNext()) {
                trees.add(treeForMapFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    /** Shared helper: build a TreeForMap from the current cursor row. */
    private TreeForMap treeForMapFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("treeId"));
        String latinName = cursor.getString(cursor.getColumnIndexOrThrow("latinName"));
        String englishName = cursor.getString(cursor.getColumnIndexOrThrow("englishName"));
        int collectionId = cursor.getInt(cursor.getColumnIndexOrThrow("collectionId"));
        int varCol = cursor.getColumnIndex("variety");
        String variety = varCol != -1 ? cursor.getString(varCol) : null;
        int rsCol = cursor.getColumnIndex("rootstock");
        String rootstock = rsCol != -1 ? cursor.getString(rsCol) : null;
        String label = cursor.getString(cursor.getColumnIndexOrThrow("label"));
        double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
        double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
        int imgCol = cursor.getColumnIndex("latestImagePath");
        String latestImagePath = imgCol != -1 ? cursor.getString(imgCol) : null;

        Location location = new Location(latitude, longitude);
        return new TreeForMap(location, id, latinName, englishName, variety, rootstock,
                label, collectionId, latestImagePath);
    }

    public List<Tree> getTreesWithoutCollection() {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return new ArrayList<>();
        }

        List<Tree> trees = new ArrayList<>();
        String query = "SELECT * FROM Tree WHERE collectionId IS NULL OR collectionId = 0 ORDER BY treeId";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, null);
            Log.d("TreeDAO", "Found " + cursor.getCount() + " trees without collection assignment");

            while (cursor.moveToNext()) {
                Tree tree = getTreeFromCursor(cursor);
                trees.add(tree);
                Log.i("TreeDAO", "Unassigned tree: ID=" + tree.getTreeId() +
                        ", Label=" + tree.getLabel() +
                        ", Species=" + tree.getLatinName());
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    public boolean updateTreeLocationId(long treeId, int locationId) {
        // First verify the tree belongs to the current collection
        Tree tree = getTree((int) treeId);
        if (tree == null || tree.getCollectionId() != new CollectionDAOImpl().getSelectedCollectionId()) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("locationId", locationId);

        String selection = "treeId = ? AND collectionId = ?";
        String[] selectionArgs = {
                String.valueOf(treeId),
                String.valueOf(new CollectionDAOImpl().getSelectedCollectionId())
        };

        int rowsAffected = getDatabase().update("Tree", values, selection, selectionArgs);
        return rowsAffected > 0;
    }

    @Override
    public String updateTreeWithLocationAndLabel(long treeId, int collectionId, int locationId, String label) {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return "Database is not ready";
        }

        // Check if label is unique (excluding the current tree)
        if (!isLabelUniqueForUpdate(label, treeId, collectionId)) {
            Log.e("TreeDAO", "Label '" + label + "' is already in use in this collection");
            return "Label '" + label + "' is already in use in this collection.";
        }

        final String[] result = { "" };
        executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("locationId", locationId);
            values.put("collectionId", collectionId);
            values.put("label", label);
            values.put("status", "verified");

            String whereClause = "treeId = ?";
            String[] whereArgs = { String.valueOf(treeId) };

            int rowsAffected = getDatabase().update("Tree", values, whereClause, whereArgs);

            if (rowsAffected > 0) {
                result[0] = "Tree location and label updated successfully.";
                Log.d("TreeDAO", "Tree " + treeId + " updated with location " + locationId + " and label " + label);
            } else {
                result[0] = "Failed to update tree.";
                Log.e("TreeDAO", "Failed to update tree " + treeId);
            }
        });

        return result[0];
    }

    @Override
    public String updateTreeWithLocationAndLabel(String latinName, int collectionId, Location location, String label) {
        return updateTreeWithLocationAndLabel(latinName, collectionId, location, label, null);
    }

    @Override
    public String updateTreeWithLocationAndLabel(String latinName, int collectionId, Location location, String label,
            String origin) {
        try {
            getDatabase().beginTransaction();

            // Insert location and get its ID
            LocationDAO locationDAO = new LocationDAOImpl();
            int locationId = (int) locationDAO.insertLocation(location);
            if (locationId == -1) {
                throw new Exception("Failed to insert location.");
            }

            // Find the first unplanted tree of this species in this collection
            String selection = "latinName = ? AND collectionId = ? AND locationId IS NULL";
            String[] selectionArgs = { latinName, String.valueOf(collectionId) };
            Cursor cursor = getDatabase().query("Tree", null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // Update the tree with the location and label
                ContentValues values = new ContentValues();
                values.put("locationId", locationId);
                values.put("label", label);
                values.put("status", "verified");
                if (origin != null && !origin.trim().isEmpty()) {
                    values.put("origin", origin.trim());
                }

                String whereClause = "treeId = ?";
                String[] whereArgs = { String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("treeId"))) };
                int rowsAffected = getDatabase().update("Tree", values, whereClause, whereArgs);

                if (rowsAffected > 0) {
                    getDatabase().setTransactionSuccessful();
                    cursor.close();
                    return "Tree planted successfully";
                }
            }

            cursor.close();
            getDatabase().setTransactionSuccessful();
            return "No unplanted trees found for this species";
        } catch (Exception e) {
            Log.e("TreeDAOImpl", "Error updating tree with location and label", e);
            return "Error: " + e.getMessage();
        } finally {
            getDatabase().endTransaction();
        }
    }

    @Override
    public boolean isLabelUniqueForUpdate(String label, long treeIdToExclude, int collectionId) {
        // Only enforce uniqueness for PLANTED trees (those with locationId)
        String query = "SELECT COUNT(*) FROM Tree WHERE label = ? AND treeId != ? AND collectionId = ? AND locationId IS NOT NULL";
        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query,
                    new String[] { label, String.valueOf(treeIdToExclude), String.valueOf(collectionId) });
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) == 0;
            }
            return true;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean isLabelUniqueForAdd(String label, int collectionId) {
        // Only enforce uniqueness for PLANTED trees (those with locationId)
        String query = "SELECT COUNT(*) FROM Tree WHERE label = ? AND collectionId = ? AND locationId IS NOT NULL";
        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, new String[] { label, String.valueOf(collectionId) });
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) == 0;
            }
            return true;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public long addTree(Tree tree) {
        ContentValues values = new ContentValues();
        values.put("latinName", tree.getLatinName());
        values.put("locationId", tree.getLocationId());
        values.put("collectionId",
                tree.getCollectionId() != null ? tree.getCollectionId() : collectionDAO.getSelectedCollectionId());
        values.put("origin", tree.getOrigin());
        values.put("rootstock", tree.getRootstock());
        values.put("variety", tree.getVariety());
        values.put("label", tree.getLabel());
        values.put("datePlanted", tree.getDatePlanted() != null ? tree.getDatePlanted().toString() : null);
        values.put("status", "verified");

        return getDatabase().insert("Tree", null, values);
    }

    @Override
    public int addTreesToPlant(Tree tree, int quantity) {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return 0;
        }

        final int[] successCount = { 0 };
        executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("latinName", tree.getLatinName());
            values.put("origin", tree.getOrigin());
            values.put("rootstock", tree.getRootstock());
            values.put("variety", tree.getVariety());
            values.put("located", tree.getLocated());
            values.put("collectionId", collectionDAO.getSelectedCollectionId()); // Set current collection
            values.put("status", "verified");

            for (int i = 0; i < quantity; i++) {
                long id = getDatabase().insert("Tree", null, values);
                if (id != -1) {
                    successCount[0]++;
                }
            }
        });

        return successCount[0];
    }

    @Override
    public Tree getTree(int treeId) {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return null;
        }

        Tree tree = null;
        Cursor cursor = null;
        try {
            cursor = getDatabase().query(
                    "Tree t INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName)",
                    null,
                    "t.treeId = ?",
                    new String[] { String.valueOf(treeId) },
                    null,
                    null,
                    null);
            if (cursor.moveToFirst()) {
                tree = getTreeFromCursor(cursor);
            }
        } finally {
            closeCursor(cursor);
        }
        return tree;
    }

    @Override
    public List<Tree> getAllTrees() {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return new ArrayList<>();
        }

        List<Tree> trees = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getDatabase().query(
                    "Tree t INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName)",
                    null,
                    "t.collectionId = ?",
                    new String[] { String.valueOf(collectionDAO.getSelectedCollectionId()) },
                    null,
                    null,
                    null);
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public List<Tree> getAllTreesReadyToPlant() {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return new ArrayList<>();
        }

        List<Tree> trees = new ArrayList<>();
        Cursor cursor = null;
        try {
            String selection = "(t.locationId IS NULL OR t.locationId = 0) AND t.collectionId = ?";
            cursor = getDatabase().query(
                    "Tree t INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName)",
                    null,
                    selection,
                    new String[] { String.valueOf(collectionDAO.getSelectedCollectionId()) },
                    null,
                    null,
                    null);
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public boolean deleteTree(long treeId) {
        return getDatabase().delete("Tree",
                "treeId = ? AND collectionId = ?",
                new String[] { String.valueOf(treeId), String.valueOf(collectionDAO.getSelectedCollectionId()) }) > 0;
    }

    @Override
    public Tree findATree_fromLabel(String label) {
        return findATree_fromLabel(label, -1);
    }

    @Override
    public Tree findATree_fromLabel(String label, int collectionId) {
        Tree tree = null;
        String query;
        String[] selectionArgs;

        if (collectionId == -1) {
            query = "SELECT * FROM Tree t " +
                    "INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                    "WHERE UPPER(t.label) = UPPER(?)";
            selectionArgs = new String[] { label };
        } else {
            query = "SELECT * FROM Tree t " +
                    "INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                    "WHERE UPPER(t.label) = UPPER(?) AND t.collectionId = ?";
            selectionArgs = new String[] { label, String.valueOf(collectionId) };
        }

        Cursor cursor = getDatabase().rawQuery(query, selectionArgs);
        if (cursor.moveToFirst()) {
            tree = getTreeFromCursor(cursor);
        }
        if (cursor != null)
            cursor.close();
        return tree;
    }

    @Override
    public Tree findATree_fromId(int treeId) {
        Tree tree = null;
        String query = "SELECT * FROM Tree t " +
                "INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                "WHERE t.treeId = ?";

        Log.d("TreeDAOImpl",
                "Finding tree with ID: " + treeId);
        Cursor cursor = getDatabase().rawQuery(query,
                new String[] { String.valueOf(treeId) });

        if (cursor.moveToFirst()) {
            tree = getTreeFromCursor(cursor);
            Log.d("TreeDAOImpl", "Found tree: "
                    + (tree != null ? "ID=" + tree.getTreeId() + ", Latin=" + tree.getLatinName() : "null"));
        } else {
            Log.d("TreeDAOImpl", "No tree found with ID: " + treeId);
        }
        if (cursor != null)
            cursor.close();
        return tree;
    }

    @Override
    public List<Tree> findAllTrees_Latin(String speciesLatinName, String[] collectionNames) {
        List<Tree> trees = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM Tree t ")
                .append("INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) ")
                .append("INNER JOIN Collection c ON t.collectionId = c.collectionId ")
                .append("WHERE UPPER(t.latinName) = UPPER(?) AND c.name IN (");

        // Add placeholders for collection names
        String[] placeholders = new String[collectionNames.length];
        Arrays.fill(placeholders, "?");
        queryBuilder.append(TextUtils.join(",", placeholders)).append(")");

        // Combine parameters
        String[] selectionArgs = new String[collectionNames.length + 1];
        selectionArgs[0] = speciesLatinName;
        System.arraycopy(collectionNames, 0, selectionArgs, 1, collectionNames.length);

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(queryBuilder.toString(), selectionArgs);
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public List<Tree> findAllTrees_Latin(String speciesLatinName) {
        List<Tree> trees = new ArrayList<>();
        String query = "SELECT * FROM Tree t " +
                "INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                "WHERE UPPER(t.latinName) = UPPER(?)";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, new String[] { speciesLatinName });
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public List<Tree> findAllTrees_Located(String located) {
        List<Tree> trees = new ArrayList<>();
        String query = "SELECT * FROM Tree t " +
                "JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                "WHERE t.located = ?";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, new String[] { located });
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public List<Tree> findAllTrees_English(String speciesEnglishName, String[] collectionNames) {
        List<Tree> trees = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM Tree t ")
                .append("INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) ")
                .append("INNER JOIN Collection c ON t.collectionId = c.collectionId ")
                .append("WHERE ts.englishName = ? AND c.name IN (");

        // Add placeholders for collection names
        String[] placeholders = new String[collectionNames.length];
        Arrays.fill(placeholders, "?");
        queryBuilder.append(TextUtils.join(",", placeholders)).append(")");

        // Combine parameters
        String[] selectionArgs = new String[collectionNames.length + 1];
        selectionArgs[0] = speciesEnglishName;
        System.arraycopy(collectionNames, 0, selectionArgs, 1, collectionNames.length);

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(queryBuilder.toString(), selectionArgs);
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public List<Tree> findAllTrees(String speciesEnglishName) {
        List<Tree> trees = new ArrayList<>();
        String query = "SELECT * FROM Tree t " +
                "JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                "WHERE ts.englishName = ?";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, new String[] { speciesEnglishName });
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public boolean deleteOneTreeToPlant(String speciesLatinName) {
        if (!isDatabaseReady()) {
            return false;
        }

        final boolean[] success = { false };
        executeInTransaction(() -> {
            // 1. Query to find ONE treeId that matches the criteria
            Cursor cursor = getDatabase().query(
                    "Tree",
                    new String[] { "treeId" },
                    "latinName = ? AND locationId IS NULL AND collectionId = ?",
                    new String[] { speciesLatinName, String.valueOf(collectionDAO.getSelectedCollectionId()) },
                    null, null, null,
                    "1");

            if (cursor != null && cursor.moveToFirst()) {
                long treeIdToDelete = cursor.getLong(cursor.getColumnIndexOrThrow("treeId"));
                closeCursor(cursor);

                // 2. Delete the specific tree
                int deletedRows = getDatabase().delete(
                        "Tree",
                        "treeId = ? AND collectionId = ?",
                        new String[] { String.valueOf(treeIdToDelete),
                                String.valueOf(collectionDAO.getSelectedCollectionId()) });
                success[0] = deletedRows > 0;
            }
        });

        return success[0];
    }

    @Override
    public String deleteTree(String label) {
        if (!isDatabaseReady()) {
            return "Database is not ready";
        }

        final String[] message = { "" };
        executeInTransaction(() -> {
            String whereClause = "label = ? AND collectionId = ?";
            String[] whereArgs = new String[] { label, String.valueOf(collectionDAO.getSelectedCollectionId()) };

            int rowsDeleted = getDatabase().delete("Tree", whereClause, whereArgs);

            if (rowsDeleted > 0) {
                message[0] = "Tree with label '" + label + "' successfully deleted.";
            } else {
                message[0] = "No tree found with label '" + label + "' in this collection.";
            }
        });

        return message[0];
    }

    @Override
    public List<Tree> findAllTreesOnMap(double maxLat, double minLat, double maxLong, double minLong) {
        List<Tree> trees = new ArrayList<>();
        String query = "SELECT t.*, ts.* FROM Tree t " +
                "JOIN Location l ON t.locationId = l.locationId " +
                "JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                "WHERE l.latitude BETWEEN ? AND ? " +
                "AND l.longitude BETWEEN ? AND ? " +
                "AND t.collectionId = ?";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, new String[] {
                    String.valueOf(minLat),
                    String.valueOf(maxLat),
                    String.valueOf(minLong),
                    String.valueOf(maxLong),
                    String.valueOf(collectionDAO.getSelectedCollectionId())
            });
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public List<Tree> findAllTreesOnMap_bySpeciesLatin(double maxLat, double minLat, double maxLong,
            double minLong, String speciesLatinName) {
        List<Tree> trees = new ArrayList<>();
        String query = "SELECT t.*, ts.* FROM Tree t " +
                "JOIN Location l ON t.locationId = l.locationId " +
                "JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                "WHERE l.latitude BETWEEN ? AND ? " +
                "AND l.longitude BETWEEN ? AND ? " +
                "AND UPPER(t.latinName) = UPPER(?) " +
                "AND t.collectionId = ?";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, new String[] {
                    String.valueOf(minLat),
                    String.valueOf(maxLat),
                    String.valueOf(minLong),
                    String.valueOf(maxLong),
                    speciesLatinName,
                    String.valueOf(collectionDAO.getSelectedCollectionId())
            });
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public List<Tree> findAllTreesOnMap_bySpeciesEnglish(double maxLat, double minLat, double maxLong,
            double minLong, String speciesEnglishName) {
        List<Tree> trees = new ArrayList<>();
        String query = "SELECT t.*, ts.* FROM Tree t " +
                "JOIN Location l ON t.locationId = l.locationId " +
                "JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                "WHERE l.latitude BETWEEN ? AND ? " +
                "AND l.longitude BETWEEN ? AND ? " +
                "AND ts.englishName = ? " +
                "AND t.collectionId = ?";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, new String[] {
                    String.valueOf(minLat),
                    String.valueOf(maxLat),
                    String.valueOf(minLong),
                    String.valueOf(maxLong),
                    speciesEnglishName,
                    String.valueOf(collectionDAO.getSelectedCollectionId())
            });
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public List<TreeGroup> listTreesInACollectionBySpecies(int collectionId) {
        List<TreeGroup> trees = new ArrayList<>();

        String query = "SELECT ts.latinName, ts.englishName, variety, rootstock, origin, located, COUNT(t.treeId) AS treeCount "
                +
                "FROM Tree t " +
                "JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                "WHERE t.collectionId = ? " +
                "GROUP BY ts.latinName, ts.englishName";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, new String[] { String.valueOf(collectionId) });

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    TreeGroup treeGroup = getTreeGroupFromCursor(cursor);
                    trees.add(treeGroup);
                } while (cursor.moveToNext());
            }

            return trees;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    @Override
    public List<TreeGroup> getTreesWithoutLocationIdGrouped() {
        List<TreeGroup> treeToListPlant = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getDatabase().query(
                    "Tree t INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName)",
                    new String[] { "t.latinName", "ts.englishName",
                            "variety, rootstock, origin, located, COUNT(*) as treeCount" },
                    "t.locationId IS NULL AND t.collectionId = ?",
                    new String[] { String.valueOf(collectionDAO.getSelectedCollectionId()) },
                    "t.latinName, ts.englishName",
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    TreeGroup treeGroup = getTreeGroupFromCursor(cursor);
                    treeToListPlant.add(treeGroup);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("TreeDAO", "Error getting grouped trees without locationId (JOIN): " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return treeToListPlant;
    }

    private TreeGroup getTreeGroupFromCursor(Cursor cursor) {
        int latinNameIndex = cursor.getColumnIndexOrThrow("latinName");
        int englishNameIndex = cursor.getColumnIndexOrThrow("englishName");
        int countIndex = cursor.getColumnIndexOrThrow("treeCount");
        int varietyIndex = cursor.getColumnIndexOrThrow("variety");
        int rootstockIndex = cursor.getColumnIndexOrThrow("rootstock");
        int originIndex = cursor.getColumnIndexOrThrow("origin");
        int locatedIndex = cursor.getColumnIndexOrThrow("located");

        String latinName = cursor.getString(latinNameIndex);
        String englishName = cursor.getString(englishNameIndex);
        int quantity = cursor.getInt(countIndex);
        String variety = cursor.isNull(varietyIndex) ? null : cursor.getString(varietyIndex);
        String rootstock = cursor.isNull(rootstockIndex) ? null : cursor.getString(rootstockIndex);
        String origin = cursor.isNull(originIndex) ? null : cursor.getString(originIndex);
        String located = cursor.isNull(locatedIndex) ? null : cursor.getString(locatedIndex);

        return new TreeGroup(
                new Tree.Builder(latinName)
                        .englishName(englishName)
                        .variety(variety)
                        .rootstock(rootstock)
                        .origin(origin)
                        .located(located)
                        .build(),
                quantity);
    }

    private Tree getTreeFromCursor(Cursor cursor) {
        Tree.Builder builder = new Tree.Builder(cursor.getString(cursor.getColumnIndexOrThrow("latinName")))
                .treeId(cursor.getInt(cursor.getColumnIndexOrThrow("treeId")))
                .collectionId(cursor.getInt(cursor.getColumnIndexOrThrow("collectionId")));

        // Handle nullable fields
        int locationIdIndex = cursor.getColumnIndex("locationId");
        if (locationIdIndex != -1 && !cursor.isNull(locationIdIndex)) {
            builder.locationId(cursor.getInt(locationIdIndex));
        }

        int datePlantedIndex = cursor.getColumnIndex("datePlanted");
        if (datePlantedIndex != -1 && !cursor.isNull(datePlantedIndex)) {
            String dateTimeStr = cursor.getString(datePlantedIndex);
            // Extract just the date portion (first 10 characters) from the datetime string
            String dateOnly = dateTimeStr.substring(0, Math.min(dateTimeStr.length(), 10));
            builder.datePlanted(Date.valueOf(dateOnly));
        }

        int originIndex = cursor.getColumnIndex("origin");
        if (originIndex != -1 && !cursor.isNull(originIndex)) {
            builder.origin(cursor.getString(originIndex));
        }

        int rootstockIndex = cursor.getColumnIndex("rootstock");
        if (rootstockIndex != -1 && !cursor.isNull(rootstockIndex)) {
            builder.rootstock(cursor.getString(rootstockIndex));
        }

        int varietyIndex = cursor.getColumnIndex("variety");
        if (varietyIndex != -1 && !cursor.isNull(varietyIndex)) {
            builder.variety(cursor.getString(varietyIndex));
        }

        int locatedIndex = cursor.getColumnIndex("located");
        if (locatedIndex != -1 && !cursor.isNull(locatedIndex)) {
            builder.located(cursor.getString(locatedIndex));
        }

        int labelIndex = cursor.getColumnIndex("label");
        if (labelIndex != -1 && !cursor.isNull(labelIndex)) {
            builder.label(cursor.getString(labelIndex));
        }

        // Handle TreeSpecies fields if they exist in the cursor
        int englishNameIndex = cursor.getColumnIndex("englishName");
        if (englishNameIndex != -1 && !cursor.isNull(englishNameIndex)) {
            builder.englishName(cursor.getString(englishNameIndex));
        }

        int frenchNameIndex = cursor.getColumnIndex("frenchName");
        if (frenchNameIndex != -1 && !cursor.isNull(frenchNameIndex)) {
            builder.frenchName(cursor.getString(frenchNameIndex));
        }

        int characteristicsIndex = cursor.getColumnIndex("characteristics");
        if (characteristicsIndex != -1 && !cursor.isNull(characteristicsIndex)) {
            builder.characteristics(cursor.getString(characteristicsIndex));
        }

        int otherNamesIndex = cursor.getColumnIndex("otherNames");
        if (otherNamesIndex != -1 && !cursor.isNull(otherNamesIndex)) {
            builder.otherNames(cursor.getString(otherNamesIndex));
        }

        int statusIndex = cursor.getColumnIndex("status");
        if (statusIndex != -1 && !cursor.isNull(statusIndex)) {
            builder.status(cursor.getString(statusIndex));
        }

        return builder.build();
    }

    private Date getDateFromString(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            String dateOnly = dateString.split(" ")[0];
            return Date.valueOf(dateOnly);
        } catch (IllegalArgumentException e) {
            Log.w("TreeDAO", "Error parsing date string in getDateFromString: " + dateString + ". Returning null.", e);
            return null;
        }
    }

    @Override
    public List<Tree> getAllTreesInACollection(int collectionId) {
        if (!isDatabaseReady()) {
            Log.e("TreeDAO", "Database is not ready");
            return new ArrayList<>();
        }

        List<Tree> trees = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getDatabase().query(
                    "Tree t INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName)",
                    null,
                    "t.collectionId = ?",
                    new String[] { String.valueOf(collectionId) },
                    null,
                    null,
                    null);
            while (cursor.moveToNext()) {
                trees.add(getTreeFromCursor(cursor));
            }
        } finally {
            closeCursor(cursor);
        }
        return trees;
    }

    @Override
    public int areUnplantedTrees(String latinName) {
        int count = 0;
        Cursor cursor = null;
        try {
            String query = "SELECT COUNT(*) FROM Tree WHERE latinName = ? AND locationId IS NULL AND collectionId = ?";
            cursor = getDatabase().rawQuery(query,
                    new String[] { latinName, String.valueOf(collectionDAO.getSelectedCollectionId()) });
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            closeCursor(cursor);
        }
        return count;
    }

    @Override
    public boolean updateBatchLabels(List<Integer> treeIds, String label) {
        if (!isDatabaseReady() || treeIds == null || treeIds.isEmpty()) {
            return false;
        }

        final boolean[] success = { true };
        executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("label", label);

            for (Integer id : treeIds) {
                String whereClause = "treeId = ?";
                String[] whereArgs = { String.valueOf(id) };
                int rows = getDatabase().update("Tree", values, whereClause, whereArgs);
                if (rows == 0) {
                    success[0] = false;
                }
            }
        });

        return success[0];
    }
}
