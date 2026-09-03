package gps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class MyLocationListener implements LocationListener {

    private static final String TAG = "MyLocationListener"; // For logging

    private final Context context;
    private final LocationManager locationManager;
    private long minTimeMillis;    // Minimum time interval between updates (milliseconds)
    private float minDistanceMeters; // Minimum distance between updates (meters)
    private final LocationCallback locationCallback; // Interface for reporting location updates

    // Define a callback interface to report location updates to the calling activity/class
    public interface LocationCallback {
        void onLocationChanged(Location location);
    }

    // Constructor:
    public MyLocationListener(Context context, LocationManager locationManager, long minTimeMillis, float minDistanceMeters, LocationCallback locationCallback) {
        this.context = context;
        this.locationManager = locationManager;
        this.minTimeMillis = minTimeMillis;
        this.minDistanceMeters = 0;
        this.minDistanceMeters = minDistanceMeters;
        this.locationCallback = locationCallback; // Store callback
    }

    // Method to start listening for location updates
    public void startListening() {
        //Check for location permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted. Cannot start listening.");
            return; //  Cannot start listening without permission. Your Activity must handle permission requesting.
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMillis, minDistanceMeters, this);
            Log.d(TAG, "Started listening for location updates.");

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
    }

    //Method to stop listening for location updates
    public void stopListening() {
        locationManager.removeUpdates(this);
        Log.d(TAG, "Stopped listening for location updates.");
    }

    //------LocationListener Interface Methods--------
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed: " + location.getLatitude() + ", " + location.getLongitude() + ", Accuracy: " + location.getAccuracy());
        // Report the new location back to the calling activity/class using the callback
        if (locationCallback != null) {
            locationCallback.onLocationChanged(location);
        }
    }



    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Location provider disabled: " + provider);
    }

    //----Setters and Getters----

    public void setMinTimeMillis(long minTimeMillis) {
        this.minTimeMillis = minTimeMillis;
    }

    public long getMinTimeMillis() {
        return minTimeMillis;
    }

    public void setMinDistanceMeters(float minDistanceMeters) {
        this.minDistanceMeters = minDistanceMeters;
    }

    public float getMinDistanceMeters() {
        return minDistanceMeters;
    }

}