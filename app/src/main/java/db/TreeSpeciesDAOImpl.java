package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.john.TreeApp.beans.TreeSpecies;

import java.util.ArrayList;
import java.util.List;

public class TreeSpeciesDAOImpl extends DAOBase implements TreeSpeciesDAO {

    private static final String TABLE_NAME = "TreeSpecies";

    public TreeSpeciesDAOImpl() {
        super();
    }

    @Override
    public String addASpecies(TreeSpecies species) {
        ContentValues values = new ContentValues();
        // The following lines seem to be incorrectly placed here, possibly from another
        // method.
        // They refer to a 'scion' object which is not available in this method's scope.
        // values.put("source", scion.getSource());
        // values.put("attached", scion.isAttached() ? 1 : 0);
        // values.put("fruitingStartMonth", scion.getFruitingStartMonth());
        // values.put("fruitingDescription", scion.getFruitingDescription());
        values.put("latinName", species.getLatinName());
        values.put("englishName", species.getEnglishName());
        values.put("frenchName", species.getFrenchName());
        values.put("characteristics", species.getCharacteristics());
        values.put("otherNames", species.getOtherNames());
        values.put("fruitingStartMonth", species.getFruitingStartMonth());
        values.put("fruitingDescription", species.getFruitingDescription());

        try {
            // Use the constant for the table name
            long result = getDatabase().insert(TABLE_NAME, null, values);
            if (result == -1) {
                return "Failed to add species.";
                // This line also appears to be incorrectly placed here, possibly from another
                // method.
                // TABLE_SCION,
                // new String[] { "scionId", "species", "variety", "source", "attached",
                // "fruitingStartMonth", "fruitingDescription" },
            } else {
                return "Species added successfully.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }

    @Override
    public boolean deleteTree(String speciesLatinName) {
        return getDatabase().delete(TABLE_NAME, "latinName = ?", new String[] { speciesLatinName }) > 0;
    }

    // public boolean deleteTree(String speciesLatinName) {
    // return getDatabase().delete(TABLE_NAME, "treeId = "+speciesLatinName);
    // }

    @Override
    public TreeSpecies findTreesSpecies_Latin(String speciesLatinName) {
        TreeSpecies treeSpecies = null;
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE latinName = ?";
        Cursor cursor = getDatabase().rawQuery(query, new String[] { speciesLatinName });
        if (cursor.moveToFirst()) {
            treeSpecies = cursorToTreeSpecies(cursor);
        }
        cursor.close();
        return treeSpecies;
    }

    @Override
    public TreeSpecies findTreesSpecies_English(String speciesEnglishName) {
        TreeSpecies treeSpecies = null;
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE englishName = ?";
        Cursor cursor = getDatabase().rawQuery(query, new String[] { speciesEnglishName });
        if (cursor.moveToFirst()) {
            treeSpecies = cursorToTreeSpecies(cursor);
        }
        cursor.close();
        return treeSpecies;
    }

    @Override
    public TreeSpecies findTreesSpecies_French(String speciesFrenchName) {
        TreeSpecies treeSpecies = null;
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE frenchName = ?";
        Cursor cursor = getDatabase().rawQuery(query, new String[] { speciesFrenchName });
        if (cursor.moveToFirst()) {
            treeSpecies = cursorToTreeSpecies(cursor);
        }
        cursor.close();
        return treeSpecies;
    }

    @Override
    public List<TreeSpecies> findTreeSpeciesByEnglishPrefix(String prefix) {
        List<TreeSpecies> speciesList = new ArrayList<>();
        // Use the actual table name (assuming it's "TreeSpecies")
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE englishName LIKE ?";
        // Append "%" to the prefix to match any characters that follow
        Cursor cursor = getDatabase().rawQuery(query, new String[] { "%" + prefix + "%" });
        if (cursor.moveToFirst()) {
            do {
                TreeSpecies species = cursorToTreeSpecies(cursor);
                speciesList.add(species);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return speciesList;
    }

    @Override
    public List<TreeSpecies> findTreeSpeciesByLatinPrefix(String prefix) {
        List<TreeSpecies> speciesList = new ArrayList<>();
        // Use the actual table name (assuming it's "TreeSpecies")
        // Use the actual table name (assuming it's "TreeSpecies")
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE latinName LIKE ?";
        // Append "%" to the prefix to match any characters that follow
        Cursor cursor = getDatabase().rawQuery(query, new String[] { "%" + prefix + "%" });
        if (cursor.moveToFirst()) {
            do {
                TreeSpecies species = cursorToTreeSpecies(cursor);
                speciesList.add(species);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return speciesList;
    }

    private TreeSpecies cursorToTreeSpecies(Cursor cursor) {
        TreeSpecies treeSpecies = null;

        int latinNameIndex = cursor.getColumnIndexOrThrow("latinName");
        int englishNameIndex = cursor.getColumnIndexOrThrow("englishName");
        int frenchNameIndex = cursor.getColumnIndexOrThrow("frenchName");
        int characteristicsIndex = cursor.getColumnIndexOrThrow("characteristics");
        int otherNamesIndex = cursor.getColumnIndexOrThrow("otherNames");
        int fruitingStartMonthIndex = cursor.getColumnIndexOrThrow("fruitingStartMonth");
        int fruitingDescriptionIndex = cursor.getColumnIndexOrThrow("fruitingDescription");

        String latinName = cursor.getString(latinNameIndex);
        String englishName = cursor.getString(englishNameIndex);
        String frenchName = cursor.isNull(frenchNameIndex) ? null : cursor.getString(frenchNameIndex);
        String description = cursor.isNull(characteristicsIndex) ? null : cursor.getString(characteristicsIndex);
        String otherNames = cursor.isNull(otherNamesIndex) ? null : cursor.getString(otherNamesIndex);
        Integer fruitingStartMonth = cursor.isNull(fruitingStartMonthIndex) ? null
                : cursor.getInt(fruitingStartMonthIndex);
        String fruitingDescription = cursor.isNull(fruitingDescriptionIndex) ? null
                : cursor.getString(fruitingDescriptionIndex);

        treeSpecies = new TreeSpecies(latinName, englishName, frenchName, description, otherNames);
        treeSpecies.setFruitingStartMonth(fruitingStartMonth);
        treeSpecies.setFruitingDescription(fruitingDescription);

        return treeSpecies;
    }

}
