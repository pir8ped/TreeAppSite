package db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.john.TreeApp.beans.Location;

import java.util.ArrayList;
import java.util.List;

public class LocationDAOImpl extends DAOBase implements LocationDAO {

    /**
     * Default constructor that uses a writable database since this DAO performs
     * write operations.
     */
    public LocationDAOImpl() {
        super();
    }

    @Override
    public int insertLocation(Location location) {
        ContentValues values = new ContentValues();
        values.put("latitude", location.getLatitude());
        values.put("longitude", location.getLongitude());

        return (int) getDatabase().insert("Location", null, values);
    }

    @Override
    public Location getLocationById(int locationId) {
        Cursor cursor = getDatabase().query("Location",
                null,
                "locationId = ?",
                new String[] { String.valueOf(locationId) },
                null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
            double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
            cursor.close();
            return new Location(locationId, latitude, longitude);
        }
        return null;
    }

    @Override
    public List<Location> getAllLocations() {
        List<Location> locations = new ArrayList<>();
        Cursor cursor = getDatabase().query("Location",
                null,
                null, null,
                null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int locationId = cursor.getInt(cursor.getColumnIndexOrThrow("locationId"));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));

                Location location = new Location(locationId, latitude, longitude);
                location.setLocationId(locationId);
                locations.add(location);
            }
            cursor.close();
        }
        return locations;
    }

    @Override
    public int deleteLocation(int locationId) {
        return getDatabase().delete("Location",
                "locationId = ?",
                new String[] { String.valueOf(locationId) });
    }

    @Override
    public int updateLocation(Location location) {
        ContentValues values = new ContentValues();
        values.put("latitude", location.getLatitude());
        values.put("longitude", location.getLongitude());

        return getDatabase().update("Location",
                values,
                "locationId = ?",
                new String[] { String.valueOf(location.getLocationId()) });
    }

}
