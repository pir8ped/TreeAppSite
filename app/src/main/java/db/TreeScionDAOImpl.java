package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.john.TreeApp.beans.Scion;
import com.john.TreeApp.beans.ScionGroup;
import com.john.TreeApp.beans.Tree;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class TreeScionDAOImpl extends DAOBase implements TreeScionDAO {
    private static final String TAG = "TreeScionDAOImpl";
    private static final String TABLE_TREE_SCION = "TreeScion";
    private ScionDAO scionDAO;

    public TreeScionDAOImpl() {
        super();
        this.scionDAO = new ScionDAOImpl(); // Initialize ScionDAO
    }

    @Override
    public boolean addScionToTree(int treeId, int scionId) {
        Log.d(TAG, "Adding scion " + scionId + " to tree " + treeId);

        // Allow duplicates - user may graft multiple scions from same source
        // to one tree, expecting some to fail

        try {
            ContentValues values = new ContentValues();
            values.put("treeId", treeId);
            values.put("scionId", scionId);

            long id = getDatabase().insert(TABLE_TREE_SCION, null, values);

            if (id != -1) {
                Log.d(TAG, "Successfully created association");

                // Mark scion as attached
                Scion scion = scionDAO.getScion(scionId);
                if (scion != null) {
                    scion.setAttached(true);
                    boolean updated = scionDAO.updateScion(scion);
                    if (updated) {
                        Log.d(TAG, "Successfully marked scion as attached");
                    } else {
                        Log.w(TAG, "Failed to mark scion as attached");
                    }
                }

                return true;
            } else {
                Log.e(TAG, "Failed to create association");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding scion to tree: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeScionFromTree(int treeId, int scionId) {
        Log.d(TAG, "Removing scion " + scionId + " from tree " + treeId);

        try {
            int rowsAffected = getDatabase().delete(
                    TABLE_TREE_SCION,
                    "treeId = ? AND scionId = ?",
                    new String[] { String.valueOf(treeId), String.valueOf(scionId) });

            boolean success = rowsAffected > 0;
            if (success) {
                Log.d(TAG, "Successfully removed association");
            } else {
                Log.w(TAG, "No association found to remove");
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error removing scion from tree: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Scion> getScionsForTree(int treeId) {
        List<Scion> scions = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT s.* FROM Scion s " +
                    "INNER JOIN TreeScion ts ON s.scionId = ts.scionId " +
                    "WHERE ts.treeId = ? " +
                    "ORDER BY s.species ASC, s.variety ASC";

            cursor = getDatabase().rawQuery(query, new String[] { String.valueOf(treeId) });

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("scionId");
                int speciesIndex = cursor.getColumnIndex("species");
                int varietyIndex = cursor.getColumnIndex("variety");
                int sourceIndex = cursor.getColumnIndex("source");

                do {
                    if (idIndex != -1 && speciesIndex != -1) {
                        int id = cursor.getInt(idIndex);
                        String species = cursor.getString(speciesIndex);
                        String variety = varietyIndex != -1 ? cursor.getString(varietyIndex) : null;
                        String source = sourceIndex != -1 ? cursor.getString(sourceIndex) : null;

                        Scion scion = new Scion.Builder(species)
                                .scionId(id)
                                .variety(variety)
                                .source(source)
                                .build();
                        scions.add(scion);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting scions for tree: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return scions;
    }

    @Override
    public List<ScionGroup> getScionsForTreeGrouped(int treeId) {
        List<ScionGroup> groups = new ArrayList<>();
        Cursor cursor = null;

        try {
            // SQL to group and count scions attached to a specific tree
            String query = "SELECT s.species, s.variety, s.source, s.fruitingDescription, COUNT(*) as count " +
                    "FROM Scion s " +
                    "INNER JOIN TreeScion ts ON s.scionId = ts.scionId " +
                    "WHERE ts.treeId = ? " +
                    "GROUP BY s.species, s.variety, s.source, s.fruitingDescription " +
                    "ORDER BY s.species ASC, s.variety ASC";

            cursor = getDatabase().rawQuery(query, new String[] { String.valueOf(treeId) });

            if (cursor != null && cursor.moveToFirst()) {
                int speciesIndex = cursor.getColumnIndex("species");
                int varietyIndex = cursor.getColumnIndex("variety");
                int sourceIndex = cursor.getColumnIndex("source");
                int countIndex = cursor.getColumnIndex("count");
                int fruitingDescIndex = cursor.getColumnIndex("fruitingDescription");

                do {
                    if (speciesIndex != -1) {
                        String species = cursor.getString(speciesIndex);
                        String variety = varietyIndex != -1 ? cursor.getString(varietyIndex) : null;
                        String source = sourceIndex != -1 ? cursor.getString(sourceIndex) : null;
                        int count = countIndex != -1 ? cursor.getInt(countIndex) : 0;
                        String fruitingDescription = fruitingDescIndex != -1 ? cursor.getString(fruitingDescIndex)
                                : null;

                        ScionGroup group = new ScionGroup(species, variety, source, count, fruitingDescription);
                        groups.add(group);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting grouped scions for tree: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return groups;
    }

    @Override
    public List<Tree> getTreesForScion(int scionId) {
        List<Tree> trees = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT t.*, ts.englishName, ts.frenchName, ts.characteristics, ts.otherNames " +
                    "FROM Tree t " +
                    "INNER JOIN TreeScion tsc ON t.treeId = tsc.treeId " +
                    "INNER JOIN TreeSpecies ts ON UPPER(t.latinName) = UPPER(ts.latinName) " +
                    "WHERE tsc.scionId = ? " +
                    "ORDER BY t.latinName ASC";

            cursor = getDatabase().rawQuery(query, new String[] { String.valueOf(scionId) });

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Tree tree = getTreeFromCursor(cursor);
                    if (tree != null) {
                        trees.add(tree);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting trees for scion: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return trees;
    }

    @Override
    public boolean isTreeScionAssociationExists(int treeId, int scionId) {
        Cursor cursor = null;

        try {
            cursor = getDatabase().query(
                    TABLE_TREE_SCION,
                    new String[] { "treeScionId" },
                    "treeId = ? AND scionId = ?",
                    new String[] { String.valueOf(treeId), String.valueOf(scionId) },
                    null, null, null);

            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error checking association: " + e.getMessage(), e);
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Helper method to build a Tree object from a cursor
     * Based on TreeDAOImpl.getTreeFromCursor()
     */
    private Tree getTreeFromCursor(Cursor cursor) {
        try {
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

            int englishNameIndex = cursor.getColumnIndex("englishName");
            if (englishNameIndex != -1) {
                builder.englishName(cursor.getString(englishNameIndex));
            }

            int frenchNameIndex = cursor.getColumnIndex("frenchName");
            if (frenchNameIndex != -1) {
                builder.frenchName(cursor.getString(frenchNameIndex));
            }

            int characteristicsIndex = cursor.getColumnIndex("characteristics");
            if (characteristicsIndex != -1) {
                builder.characteristics(cursor.getString(characteristicsIndex));
            }

            int otherNamesIndex = cursor.getColumnIndex("otherNames");
            if (otherNamesIndex != -1) {
                builder.otherNames(cursor.getString(otherNamesIndex));
            }

            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error building tree from cursor: " + e.getMessage(), e);
            return null;
        }
    }
}
