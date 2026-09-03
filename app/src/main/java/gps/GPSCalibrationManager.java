package gps;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages GPS calibration state including reference points and current offset.
 * Stores data in SharedPreferences for persistence.
 */
public class GPSCalibrationManager {
    private static final String TAG = "GPSCalibrationManager";
    private static final String PREFS_NAME = "GPSCalibrationPrefs";
    private static final String KEY_REFERENCE_POINTS = "reference_points";
    private static final String KEY_OFFSET_LAT = "offset_lat";
    private static final String KEY_OFFSET_LON = "offset_lon";
    private static final String KEY_CALIBRATION_TIME = "calibration_time";
    private static final String KEY_CALIBRATING_REF_INDEX = "calibrating_ref_index";

    // Calibration validity windows (in milliseconds)
    private static final long WARNING_THRESHOLD_MS = 60 * 60 * 1000; // 1 hour
    private static final long EXPIRY_THRESHOLD_MS = 2 * 60 * 60 * 1000; // 2 hours

    private static GPSCalibrationManager instance;
    private final SharedPreferences prefs;
    private List<ReferencePoint> referencePoints;
    private double offsetLat = 0;
    private double offsetLon = 0;
    private long calibrationTime = 0;
    private int calibratingRefIndex = -1; // Index of ref point being calibrated against

    private GPSCalibrationManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    public static synchronized GPSCalibrationManager getInstance(Context context) {
        if (instance == null) {
            instance = new GPSCalibrationManager(context);
        }
        return instance;
    }

    private void loadFromPrefs() {
        // Load reference points
        referencePoints = new ArrayList<>();
        String json = prefs.getString(KEY_REFERENCE_POINTS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                referencePoints.add(new ReferencePoint(
                        obj.getString("name"),
                        obj.getDouble("lat"),
                        obj.getDouble("lon")));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading reference points", e);
        }

        // Add default reference points if none exist
        if (referencePoints.isEmpty()) {
            referencePoints.add(new ReferencePoint("Valley (cabin door)", 45.104680, 1.928994));
            referencePoints.add(new ReferencePoint("Acorn (back door)", 45.109033, 1.924138));
            saveReferencePoints();
        }

        // Load offset
        offsetLat = Double.longBitsToDouble(prefs.getLong(KEY_OFFSET_LAT, 0));
        offsetLon = Double.longBitsToDouble(prefs.getLong(KEY_OFFSET_LON, 0));
        calibrationTime = prefs.getLong(KEY_CALIBRATION_TIME, 0);
        calibratingRefIndex = prefs.getInt(KEY_CALIBRATING_REF_INDEX, -1);
    }

    private void saveReferencePoints() {
        JSONArray array = new JSONArray();
        try {
            for (ReferencePoint rp : referencePoints) {
                JSONObject obj = new JSONObject();
                obj.put("name", rp.getName());
                obj.put("lat", rp.getLatitude());
                obj.put("lon", rp.getLongitude());
                array.put(obj);
            }
            prefs.edit().putString(KEY_REFERENCE_POINTS, array.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving reference points", e);
        }
    }

    private void saveOffset() {
        prefs.edit()
                .putLong(KEY_OFFSET_LAT, Double.doubleToLongBits(offsetLat))
                .putLong(KEY_OFFSET_LON, Double.doubleToLongBits(offsetLon))
                .putLong(KEY_CALIBRATION_TIME, calibrationTime)
                .putInt(KEY_CALIBRATING_REF_INDEX, calibratingRefIndex)
                .apply();
    }

    // --- Reference Point Management ---

    public List<ReferencePoint> getReferencePoints() {
        return new ArrayList<>(referencePoints);
    }

    public void addReferencePoint(String name, double lat, double lon) {
        referencePoints.add(new ReferencePoint(name, lat, lon));
        saveReferencePoints();
    }

    public void updateReferencePoint(int index, String name, double lat, double lon) {
        if (index >= 0 && index < referencePoints.size()) {
            ReferencePoint rp = referencePoints.get(index);
            rp.setName(name);
            rp.setLatitude(lat);
            rp.setLongitude(lon);
            saveReferencePoints();
        }
    }

    public void deleteReferencePoint(int index) {
        if (index >= 0 && index < referencePoints.size()) {
            referencePoints.remove(index);
            saveReferencePoints();
        }
    }

    // --- Calibration ---

    /**
     * Marks that calibration is starting for the specified reference point.
     */
    public void startCalibration(int refPointIndex) {
        calibratingRefIndex = refPointIndex;
    }

    /**
     * Finishes calibration by computing offset between averaged GPS and known
     * reference point.
     * 
     * @param averagedGPS The averaged GPS location from GPSAverager
     */
    public void finishCalibration(Location averagedGPS) {
        if (calibratingRefIndex < 0 || calibratingRefIndex >= referencePoints.size()) {
            Log.e(TAG, "Invalid calibrating reference index: " + calibratingRefIndex);
            return;
        }

        ReferencePoint refPoint = referencePoints.get(calibratingRefIndex);

        // Offset = true position - GPS reading
        offsetLat = refPoint.getLatitude() - averagedGPS.getLatitude();
        offsetLon = refPoint.getLongitude() - averagedGPS.getLongitude();
        calibrationTime = System.currentTimeMillis();

        Log.i(TAG, String.format("Calibration complete. Offset: lat=%.7f, lon=%.7f", offsetLat, offsetLon));
        saveOffset();
        calibratingRefIndex = -1;
    }

    /**
     * Applies the current calibration offset to a raw location.
     * 
     * @return Corrected location, or the original if calibration is expired
     */
    public Location applyOffset(Location rawLocation) {
        if (!isCalibrationValid()) {
            return rawLocation;
        }

        Location corrected = new Location(rawLocation);
        corrected.setLatitude(rawLocation.getLatitude() + offsetLat);
        corrected.setLongitude(rawLocation.getLongitude() + offsetLon);
        return corrected;
    }

    /**
     * Checks if calibration is still valid (within 2-hour window).
     */
    public boolean isCalibrationValid() {
        if (calibrationTime == 0) {
            return false;
        }
        long age = System.currentTimeMillis() - calibrationTime;
        return age < EXPIRY_THRESHOLD_MS;
    }

    /**
     * Checks if calibration is nearing expiry (past 1-hour warning threshold).
     */
    public boolean isCalibrationWarning() {
        if (calibrationTime == 0) {
            return false;
        }
        long age = System.currentTimeMillis() - calibrationTime;
        return age >= WARNING_THRESHOLD_MS && age < EXPIRY_THRESHOLD_MS;
    }

    /**
     * Returns the age of the current calibration in minutes.
     */
    public int getCalibrationAgeMinutes() {
        if (calibrationTime == 0) {
            return -1;
        }
        long age = System.currentTimeMillis() - calibrationTime;
        return (int) (age / 60000);
    }

    /**
     * Clears the current calibration offset.
     */
    public void clearCalibration() {
        offsetLat = 0;
        offsetLon = 0;
        calibrationTime = 0;
        calibratingRefIndex = -1;
        saveOffset();
    }

    /**
     * Returns whether a calibration is currently active (has valid offset).
     */
    public boolean hasCalibration() {
        return calibrationTime > 0;
    }

    /**
     * Returns the current offset in meters (approximate).
     * Uses simple equirectangular approximation.
     */
    public double getOffsetDistanceMeters() {
        // Approximate meters per degree at mid-latitudes
        double latMetersPerDegree = 111000;
        double lonMetersPerDegree = 111000 * Math.cos(Math.toRadians(45)); // Approximate

        double latOffsetMeters = offsetLat * latMetersPerDegree;
        double lonOffsetMeters = offsetLon * lonMetersPerDegree;

        return Math.sqrt(latOffsetMeters * latOffsetMeters + lonOffsetMeters * lonOffsetMeters);
    }
}
