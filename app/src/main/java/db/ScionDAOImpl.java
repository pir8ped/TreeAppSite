package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.john.TreeApp.beans.Scion;
import com.john.TreeApp.beans.ScionGroup;

import java.util.ArrayList;
import java.util.List;

public class ScionDAOImpl extends DAOBase implements ScionDAO {
    private static final String TAG = "ScionDAOImpl";
    private static final String TABLE_SCION = "Scion";

    public ScionDAOImpl() {
        super();
    }

    @Override
    public long addScion(Scion scion) {
        Log.d(TAG, "Adding new scion: " + scion.getSpecies());

        try {
            ContentValues values = new ContentValues();
            values.put("species", scion.getSpecies());
            values.put("variety", scion.getVariety());
            values.put("source", scion.getSource());
            values.put("attached", scion.isAttached() ? 1 : 0);
            values.put("fruitingStartMonth", scion.getFruitingStartMonth());
            values.put("fruitingDescription", scion.getFruitingDescription());

            long id = getDatabase().insert(TABLE_SCION, null, values);

            if (id != -1) {
                Log.d(TAG, "Successfully added scion with ID: " + id);
            } else {
                Log.e(TAG, "Failed to add scion");
            }

            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error adding scion: " + e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public Scion getScion(int scionId) {
        Scion scion = null;
        Cursor cursor = null;

        try {
            cursor = getDatabase().query(
                    TABLE_SCION,
                    new String[] { "scionId", "species", "variety", "source", "attached", "fruitingStartMonth",
                            "fruitingDescription" },
                    "scionId = ?",
                    new String[] { String.valueOf(scionId) },
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("scionId");
                int speciesIndex = cursor.getColumnIndex("species");
                int varietyIndex = cursor.getColumnIndex("variety");
                int sourceIndex = cursor.getColumnIndex("source");
                int attachedIndex = cursor.getColumnIndex("attached");
                int fruitingStartMonthIndex = cursor.getColumnIndex("fruitingStartMonth");
                int fruitingDescriptionIndex = cursor.getColumnIndex("fruitingDescription");

                if (idIndex != -1 && speciesIndex != -1) {
                    int id = cursor.getInt(idIndex);
                    String species = cursor.getString(speciesIndex);
                    String variety = varietyIndex != -1 ? cursor.getString(varietyIndex) : null;
                    String source = sourceIndex != -1 ? cursor.getString(sourceIndex) : null;
                    boolean attached = attachedIndex != -1 && cursor.getInt(attachedIndex) == 1;
                    Integer fruitingStartMonth = fruitingStartMonthIndex != -1
                            && !cursor.isNull(fruitingStartMonthIndex) ? cursor.getInt(fruitingStartMonthIndex) : null;
                    String fruitingDescription = fruitingDescriptionIndex != -1
                            ? cursor.getString(fruitingDescriptionIndex)
                            : null;

                    scion = new Scion.Builder(species)
                            .scionId(id)
                            .variety(variety)
                            .source(source)
                            .attached(attached)
                            .fruitingStartMonth(fruitingStartMonth)
                            .fruitingDescription(fruitingDescription)
                            .build();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting scion: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return scion;
    }

    @Override
    public List<Scion> getAllScions() {
        List<Scion> scions = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = getDatabase().query(
                    TABLE_SCION,
                    new String[] { "scionId", "species", "variety", "source", "attached", "fruitingStartMonth",
                            "fruitingDescription" },
                    null, null, null, null,
                    "species ASC, variety ASC");

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("scionId");
                int speciesIndex = cursor.getColumnIndex("species");
                int varietyIndex = cursor.getColumnIndex("variety");
                int sourceIndex = cursor.getColumnIndex("source");
                int attachedIndex = cursor.getColumnIndex("attached");
                int fruitingStartMonthIndex = cursor.getColumnIndex("fruitingStartMonth");
                int fruitingDescriptionIndex = cursor.getColumnIndex("fruitingDescription");

                do {
                    if (idIndex != -1 && speciesIndex != -1) {
                        int id = cursor.getInt(idIndex);
                        String species = cursor.getString(speciesIndex);
                        String variety = varietyIndex != -1 ? cursor.getString(varietyIndex) : null;
                        String source = sourceIndex != -1 ? cursor.getString(sourceIndex) : null;
                        boolean attached = attachedIndex != -1 && cursor.getInt(attachedIndex) == 1;
                        Integer fruitingStartMonth = (fruitingStartMonthIndex != -1
                                && !cursor.isNull(fruitingStartMonthIndex)) ? cursor.getInt(fruitingStartMonthIndex)
                                        : null;
                        String fruitingDescription = (fruitingDescriptionIndex != -1)
                                ? cursor.getString(fruitingDescriptionIndex)
                                : null;

                        Scion scion = new Scion.Builder(species)
                                .scionId(id)
                                .variety(variety)
                                .source(source)
                                .attached(attached)
                                .fruitingStartMonth(fruitingStartMonth)
                                .fruitingDescription(fruitingDescription)
                                .build();
                        scions.add(scion);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all scions: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return scions;
    }

    @Override
    public boolean updateScion(Scion scion) {
        Log.d(TAG, "Updating scion: " + scion.getScionId());

        try {
            ContentValues values = new ContentValues();
            values.put("species", scion.getSpecies());
            values.put("variety", scion.getVariety());
            values.put("source", scion.getSource());
            values.put("attached", scion.isAttached() ? 1 : 0);
            values.put("fruitingStartMonth", scion.getFruitingStartMonth());
            values.put("fruitingDescription", scion.getFruitingDescription());

            int rowsAffected = getDatabase().update(
                    TABLE_SCION,
                    values,
                    "scionId = ?",
                    new String[] { String.valueOf(scion.getScionId()) });

            boolean success = rowsAffected > 0;
            if (success) {
                Log.d(TAG, "Successfully updated scion");
            } else {
                Log.w(TAG, "No rows affected when updating scion");
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error updating scion: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteScion(int scionId) {
        Log.d(TAG, "Deleting scion: " + scionId);

        try {
            int rowsAffected = getDatabase().delete(
                    TABLE_SCION,
                    "scionId = ?",
                    new String[] { String.valueOf(scionId) });

            boolean success = rowsAffected > 0;
            if (success) {
                Log.d(TAG, "Successfully deleted scion");
            } else {
                Log.w(TAG, "No rows affected when deleting scion");
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting scion: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Scion> getScionsBySpecies(String species) {
        List<Scion> scions = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = getDatabase().query(
                    TABLE_SCION,
                    new String[] { "scionId", "species", "variety", "source", "attached", "fruitingStartMonth",
                            "fruitingDescription" },
                    "species = ?",
                    new String[] { species },
                    null, null,
                    "variety ASC");

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("scionId");
                int speciesIndex = cursor.getColumnIndex("species");
                int varietyIndex = cursor.getColumnIndex("variety");
                int sourceIndex = cursor.getColumnIndex("source");
                int attachedIndex = cursor.getColumnIndex("attached");
                int fruitingStartMonthIndex = cursor.getColumnIndex("fruitingStartMonth");
                int fruitingDescriptionIndex = cursor.getColumnIndex("fruitingDescription");

                do {
                    if (idIndex != -1 && speciesIndex != -1) {
                        int id = cursor.getInt(idIndex);
                        String speciesName = cursor.getString(speciesIndex);
                        String variety = varietyIndex != -1 ? cursor.getString(varietyIndex) : null;
                        String source = sourceIndex != -1 ? cursor.getString(sourceIndex) : null;
                        boolean attached = attachedIndex != -1 && cursor.getInt(attachedIndex) == 1;
                        Integer fruitingStartMonth = (fruitingStartMonthIndex != -1
                                && !cursor.isNull(fruitingStartMonthIndex)) ? cursor.getInt(fruitingStartMonthIndex)
                                        : null;
                        String fruitingDescription = (fruitingDescriptionIndex != -1)
                                ? cursor.getString(fruitingDescriptionIndex)
                                : null;

                        Scion scion = new Scion.Builder(speciesName)
                                .scionId(id)
                                .variety(variety)
                                .source(source)
                                .attached(attached)
                                .fruitingStartMonth(fruitingStartMonth)
                                .fruitingDescription(fruitingDescription)
                                .build();
                        scions.add(scion);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting scions by species: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return scions;
    }

    @Override
    public List<Scion> getUnattachedScions() {
        List<Scion> scions = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query for scions that are not attached
            String query = "SELECT scionId, species, variety, source, attached, fruitingStartMonth, fruitingDescription FROM Scion "
                    +
                    "WHERE attached = 0 " +
                    "ORDER BY species ASC, variety ASC";

            cursor = getDatabase().rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("scionId");
                int speciesIndex = cursor.getColumnIndex("species");
                int varietyIndex = cursor.getColumnIndex("variety");
                int sourceIndex = cursor.getColumnIndex("source");
                int attachedIndex = cursor.getColumnIndex("attached");

                do {
                    if (idIndex != -1 && speciesIndex != -1) {
                        int id = cursor.getInt(idIndex);
                        String species = cursor.getString(speciesIndex);
                        String variety = varietyIndex != -1 ? cursor.getString(varietyIndex) : null;
                        String source = sourceIndex != -1 ? cursor.getString(sourceIndex) : null;
                        boolean attached = attachedIndex != -1 && cursor.getInt(attachedIndex) == 1;
                        int fruitingStartMonthIndex = cursor.getColumnIndex("fruitingStartMonth");
                        int fruitingDescriptionIndex = cursor.getColumnIndex("fruitingDescription");
                        Integer fruitingStartMonth = (fruitingStartMonthIndex != -1
                                && !cursor.isNull(fruitingStartMonthIndex)) ? cursor.getInt(fruitingStartMonthIndex)
                                        : null;
                        String fruitingDescription = (fruitingDescriptionIndex != -1)
                                ? cursor.getString(fruitingDescriptionIndex)
                                : null;

                        Scion scion = new Scion.Builder(species)
                                .scionId(id)
                                .variety(variety)
                                .source(source)
                                .attached(attached)
                                .fruitingStartMonth(fruitingStartMonth)
                                .fruitingDescription(fruitingDescription)
                                .build();
                        scions.add(scion);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting unattached scions: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return scions;
    }

    @Override
    public List<ScionGroup> getUnattachedScionsGrouped() {
        List<ScionGroup> groups = new ArrayList<>();
        Cursor cursor = null;

        try {
            // SQL to group and count unattached scions by type
            String query = "SELECT species, variety, source, fruitingDescription, COUNT(*) as count " +
                    "FROM Scion " +
                    "WHERE attached = 0 " +
                    "GROUP BY species, variety, source, fruitingDescription " +
                    "ORDER BY species ASC, variety ASC";

            cursor = getDatabase().rawQuery(query, null);

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
            Log.e(TAG, "Error getting grouped scions: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return groups;
    }
}
