package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.john.TreeApp.beans.utilBean.TreeListItem;
import com.john.TreeApp.beans.utilBean.TreeToPlant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TreesToPlantDAOImpl extends DAOBase implements TreesToPlantDAO {

    private static final String TAG = "TreesToPlantDAO";

    public TreesToPlantDAOImpl(){
        super();
    }


    @Override
    public int addTrees(String latinName, String label, String variety, String rootstock, String origin, String location, int quantity) {
        ContentValues values = new ContentValues();
        values.put("latinName", latinName);
        values.put("label", label);
        values.put("variety", variety);
        values.put("rootstock", rootstock);
        values.put("origin", origin);
        values.put("location", location);
        values.put("quantity", quantity);

        int newRowId = database.insert("TreesToPlant", null, values);
        if (newRowId == -1) {
            Log.e(TAG, "Error adding tree data: Insertion failed.");
        } else {
            Log.d(TAG, String.format(Locale.getDefault(), "Successfully inserted tree data. New row ID: %d", newRowId));
        }
        return newRowId;
    }
    @Override
    public TreeToPlant getTreeById(int id) {
        TreeToPlant tree = null;

        String query = "SELECT * FROM TreesToPlant WHERE id = ?";
        Cursor cursor = database.rawQuery(query, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    tree = new TreeToPlant();
                    tree.setId(cursor.getInt(0));
                    tree.setLatinName(cursor.getString(1));
                    tree.setEnglishName(cursor.getString(2));
                    tree.setLabel(cursor.getString(3));
                    tree.setVariety(cursor.getString(4));
                    tree.setRootstock(cursor.getString(5));
                    tree.setOrigin(cursor.getString(6));
                    tree.setLocation(cursor.getString(7));
                    tree.setQuantity(cursor.getInt(8));
                }
            } finally {
                cursor.close();
            }
        }
        return tree;
    }

    @Override
    public TreeToPlant getTreeByLatinName(String latinName) {
        TreeToPlant tree = null;

        String query = "SELECT * FROM TreesToPlant WHERE latinName = ?";
        Cursor cursor = database.rawQuery(query, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    tree = new TreeToPlant();
                    tree.setId(cursor.getInt(0));
                    tree.setLatinName(cursor.getString(1));
                    tree.setEnglishName(cursor.getString(2));
                    tree.setLabel(cursor.getString(3));
                    tree.setVariety(cursor.getString(4));
                    tree.setRootstock(cursor.getString(5));
                    tree.setOrigin(cursor.getString(6));
                    tree.setLocation(cursor.getString(7));
                    tree.setQuantity(cursor.getInt(8));
                }
            } finally {
                cursor.close();
            }
        }
        return tree;
    }

    @Override
    public List<TreeListItem> getAllTrees() {
        List<TreeListItem > trees = new ArrayList<>();
        String query = "SELECT tp.id, tp.latinName, ts.englishName, " +
                "tp.label, tp.variety, tp.rootstock, tp.origin, tp.location, tp.quantity " +
                "FROM TreesToPlant tp " +
                "LEFT JOIN TreeSpecies ts ON tp.latinName = ts.latinName";

        Cursor cursor = database.rawQuery(query, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    TreeToPlant tree = new TreeToPlant();
                    tree.setId(cursor.getInt(0));
                    tree.setLatinName(cursor.getString(1));
                    tree.setEnglishName(cursor.getString(2));
                    tree.setLabel(cursor.getString(3));
                    tree.setVariety(cursor.getString(4));
                    tree.setRootstock(cursor.getString(5));
                    tree.setOrigin(cursor.getString(6));
                    tree.setLocation(cursor.getString(7));
                    tree.setQuantity(cursor.getInt(8));

                    trees.add(tree);
                }
            } finally {
                cursor.close();
            }
        }
        return trees;
    }

    @Override
    public void setQuantity(int id, int quantity) {
        ContentValues values = new ContentValues();
        values.put("quantity", quantity);
        int rowsAffected = database.update("TreesToPlant", values, "id = ?", new String[]{String.valueOf(id)});
        if (rowsAffected > 0) {
            Log.d(TAG, "Updated quantity for tree ID: " + id);
        } else {
            Log.e(TAG, "Failed to update quantity for tree ID: " + id);
        }
        deleteIfZero(id);
    }

    @Override
    public void decrementQuantity(int id) {
        database.execSQL("UPDATE TreesToPlant SET quantity = quantity - 1 WHERE id = ?", new Object[]{id});
        Log.d(TAG, "Decremented quantity for tree ID: " + id);
        deleteIfZero(id);
    }

    @Override
    public void remove(int id) {
        int rowsDeleted = database.delete("TreesToPlant", "id = ?", new String[]{String.valueOf(id)});
        if (rowsDeleted > 0) {
            Log.d(TAG, "Deleted tree with ID: " + id);
        } else {
            Log.e(TAG, "Failed to delete tree with ID: " + id);
        }
    }

    @Override
    public void changeLocation(int id, String newLocation) {
        ContentValues values = new ContentValues();
        values.put("location", newLocation);
        int rowsAffected = database.update("TreesToPlant", values, "id = ?", new String[]{String.valueOf(id)});
        if (rowsAffected > 0) {
            Log.d(TAG, "Updated location for tree ID: " + id);
        } else {
            Log.e(TAG, "Failed to update location for tree ID: " + id);
        }
    }

    @Override
    public boolean deleteTree(String speciesLatinName) {
        try {
            int rowsDeleted = database.delete("TreesToPlant", "latinName = ?", new String[]{speciesLatinName});
            if (rowsDeleted > 0) {
                return true;
            } else {
                Log.e("DeleteTree", "No rows deleted for latinName: " + speciesLatinName);
                return false; // No rows deleted, possible issue with the latinName or constraints
            }
        } catch (Exception e) {
            Log.e("DeleteTree", "Error deleting tree with latinName: " + speciesLatinName, e);
            return false; // Handle any database exceptions
        }
    }


    private void deleteIfZero(int id) {
        int rowsDeleted = database.delete("TreesToPlant", "id = ? AND quantity <= 0", new String[]{String.valueOf(id)});
        if (rowsDeleted > 0) {
            Log.d(TAG, "Deleted tree with ID: " + id + " due to zero quantity.");
        }
    }
}
