package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.john.TreeApp.beans.Collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectionDAOImpl extends DAOBase implements CollectionDAO {
    private static final String TAG = "CollectionDAOImpl";

    /**
     * Private constructor that uses a writable database since this DAO performs
     * write operations.
     */
    public CollectionDAOImpl() {
        super(); // Need writable database
    }

    @Override
    public int addCollection(Collection collection) {
        Log.d(TAG, "Adding new collection: " + collection.getName());

        try {
            ContentValues values = new ContentValues();
            values.put("name", collection.getName());
            values.put("selected", collection.isSelected() ? 1 : 0);

            Log.d(TAG, "Attempting to insert collection into database");
            // Insert the new row, returning the primary key value of the new row
            int id = (int) getDatabase().insert("Collection", null, values);

            if (id != -1) {
                Log.d(TAG, "Successfully added collection with ID: " + id);
            } else {
                Log.e(TAG, "Failed to add collection: " + collection.getName());
            }

            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error adding collection: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // Get all collections
    @Override
    public List<Collection> getAllCollections() {
        ArrayList<Collection> collections = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = getDatabase().query("Collection", new String[] { "id", "name", "selected" }, null, null, null,
                    null,
                    null);

            // Log column names for debugging
            String[] columnNames = cursor.getColumnNames();
            Log.d("DatabaseColumns", "Columns: " + Arrays.toString(columnNames));

            int idIndex = cursor.getColumnIndex("id");
            int nameIndex = cursor.getColumnIndex("name");
            int selectedIndex = cursor.getColumnIndex("selected");

            if (idIndex == -1 || nameIndex == -1 || selectedIndex == -1) {
                Log.e("DatabaseError", "One or more columns not found in the cursor.");
                return collections;
            }

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    // Convert integer to boolean
                    boolean selected = cursor.getInt(selectedIndex) > 0; // or (cursor.getInt(selectedIndex) != 0)

                    collections.add(new Collection(id, name, selected)); // Ensure Collection constructor matches
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return collections;
    }

    @Override
    public Collection getCollection(long id) {
        Collection collection = null;
        Cursor cursor = null;
        try {
            cursor = getDatabase().query(
                    "Collection",
                    new String[] { "id", "name", "selected" },
                    "id = ?",
                    new String[] { String.valueOf(id) },
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("id");
                int nameIndex = cursor.getColumnIndex("name");
                int selectedIndex = cursor.getColumnIndex("selected");
                if (idIndex != -1 && nameIndex != -1 && selectedIndex != -1) {
                    int collectionId = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    boolean selected = cursor.getInt(selectedIndex) > 0;
                    collection = new Collection(collectionId, name, selected);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return collection;
    }

    @Override
    public boolean deleteCollection(int collectionId) {
        if (!isDatabaseReady()) {
            throw new RuntimeException("DB not ready");
        }

        // Fetch name for fallback deletion if ID lookup fails
        String nameFallback = null;
        Collection current = getCollection(collectionId);
        if (current != null) {
            nameFallback = current.getName();
        }

        SQLiteDatabase db = getDatabase();
        try {
            db.beginTransaction();

            // 1. Check selection status before deletion
            int selectedId = getSelectedCollectionId();
            boolean wasSelected = (selectedId == collectionId);

            // 2. Delete the collection record
            int rowsDeleted = db.delete("Collection", "id = ?",
                    new String[] { String.valueOf(collectionId) });

            // If ID deletion failed but we have a name, try deleting by name
            if (rowsDeleted == 0 && nameFallback != null) {
                rowsDeleted = db.delete("Collection", "name = ?", new String[] { nameFallback });
            }

            if (rowsDeleted > 0) {
                // 3. Update selection if necessary
                if (wasSelected) {
                    Cursor nextCursor = db.query("Collection", new String[] { "id" }, "id != ?",
                            new String[] { String.valueOf(collectionId) }, null, null, "id ASC", "1");
                    if (nextCursor != null && nextCursor.moveToFirst()) {
                        int nextId = nextCursor.getInt(0);
                        ContentValues values = new ContentValues();
                        values.put("selected", 1);
                        db.update("Collection", values, "id = ?", new String[] { String.valueOf(nextId) });
                        nextCursor.close();
                    } else if (nextCursor != null) {
                        nextCursor.close();
                    }
                }

                db.setTransactionSuccessful();
            } else {
                throw new RuntimeException("Deletion target not found in database (ID: " + collectionId + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting collection " + collectionId, e);
            throw new RuntimeException("DAO Error: " + e.getMessage());
        } finally {
            if (db != null && db.inTransaction()) {
                db.endTransaction();
            }
        }

        return true;
    }

    @Override
    public boolean moveTreesToCollection(int fromCollectionId, int toCollectionId) {
        try {
            getDatabase().beginTransaction();

            // Get the source collection name
            Collection fromCollection = getCollection(fromCollectionId);
            if (fromCollection == null) {
                Log.e(TAG, "Source collection not found");
                return false;
            }

            // First, get all trees from the source collection that have labels
            Cursor cursor = getDatabase().query("Tree",
                    new String[] { "treeId", "label" },
                    "collectionId = ? AND label IS NOT NULL",
                    new String[] { String.valueOf(fromCollectionId) },
                    null, null, null);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int treeId = cursor.getInt(cursor.getColumnIndexOrThrow("treeId"));
                        String label = cursor.getString(cursor.getColumnIndexOrThrow("label"));

                        // Check if this label exists in the target collection
                        Cursor checkCursor = getDatabase().query("Tree",
                                new String[] { "treeId" },
                                "collectionId = ? AND label = ?",
                                new String[] { String.valueOf(toCollectionId), label },
                                null, null, null);

                        try {
                            if (checkCursor != null && checkCursor.getCount() > 0) {
                                // Label conflict exists, update the label
                                ContentValues labelUpdate = new ContentValues();
                                labelUpdate.put("label", label + " - from " + fromCollection.getName());
                                getDatabase().update("Tree", labelUpdate, "treeId = ?",
                                        new String[] { String.valueOf(treeId) });
                            }
                        } finally {
                            if (checkCursor != null) {
                                checkCursor.close();
                            }
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            // Now move all trees to the target collection
            ContentValues values = new ContentValues();
            values.put("collectionId", toCollectionId);
            int result = getDatabase().update("Tree", values, "collectionId = ?",
                    new String[] { String.valueOf(fromCollectionId) });

            getDatabase().setTransactionSuccessful();
            return result > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error moving trees: " + e.getMessage());
            return false;
        } finally {
            getDatabase().endTransaction();
        }
    }

    @Override
    public int getSelectedCollectionId() {
        Cursor cursor = null;
        try {
            cursor = getDatabase().query("Collection",
                    new String[] { "id" },
                    "selected = 1",
                    null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting selected collection: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    @Override
    public void setSelectedCollectionId(int collectionId) {
        try {
            getDatabase().beginTransaction();

            // First clear all selections
            ContentValues clearValues = new ContentValues();
            clearValues.put("selected", 0);
            getDatabase().update("Collection", clearValues, null, null);

            // Then set the new selection
            ContentValues selectValues = new ContentValues();
            selectValues.put("selected", 1);
            getDatabase().update("Collection", selectValues, "id = ?",
                    new String[] { String.valueOf(collectionId) });

            getDatabase().setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error setting selected collection: " + e.getMessage());
        } finally {
            getDatabase().endTransaction();
        }
    }
}
