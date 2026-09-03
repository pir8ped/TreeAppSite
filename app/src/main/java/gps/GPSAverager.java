package gps;

import android.location.Location;
import java.util.ArrayList;
import java.util.List;

public class GPSAverager {

    private final List<Location> locationReadings = new ArrayList<>();
    private boolean isRecording = false;

    // Adjustable parameters:
    private long recordingIntervalMillis = 5000; // Record every 5 seconds
    private double accuracyThresholdMeters = 20;  // Readings with accuracy > 20m are outliers
    private int minimumReadings = 5;             // Need at least 5 good readings for averaging



    // Constructor
    public GPSAverager() {
        // Default constructor
    }

    // Setters for parameters (optional, if you want to customize from your activity)
    public void setRecordingIntervalMillis(long interval) {
        this.recordingIntervalMillis = interval;
    }

    public void setAccuracyThresholdMeters(double threshold) {
        this.accuracyThresholdMeters = threshold;
    }

    public void setMinimumReadings(int minReadings) {
        this.minimumReadings = minReadings;
    }

    public interface OnEnoughReadingsListener {
        void onEnoughReadings();
    }

    private OnEnoughReadingsListener listener;

    public void setOnEnoughReadingsListener(OnEnoughReadingsListener listener) {
        this.listener = listener;
    }

    private void checkReadings() {
        if (locationReadings.size() >= minimumReadings) {
            if (listener != null) {
                listener.onEnoughReadings();
            }
        }
    }


    // Method to start recording GPS locations.  Call this when you press "Start Planting"
    public void startRecording() {
        locationReadings.clear(); // Clear any previous readings
        isRecording = true;
    }

    // Method to add a new GPS reading. Call this from your location listener.
    public void addLocationReading(Location location) {
        if (isRecording) {
            locationReadings.add(location);
        }
    }


    // Method to stop recording, remove outliers, and calculate the average location.
    // Call this when you press "Finish Planting". Returns null if there are not enough valid readings.
    public Location getAveragedLocation() {
        isRecording = false;

        // Remove outliers based on accuracy
        List<Location> filteredReadings = removeOutliers();

        if (filteredReadings.size() < minimumReadings) {
            return null; // Not enough valid readings
        }

        // Calculate average latitude and longitude (using Cartesian conversion for accuracy)
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;

        for (Location location : filteredReadings) {
            double latitudeRad = Math.toRadians(location.getLatitude());
            double longitudeRad = Math.toRadians(location.getLongitude());

            sumX += Math.cos(latitudeRad) * Math.cos(longitudeRad);
            sumY += Math.cos(latitudeRad) * Math.sin(longitudeRad);
            sumZ += Math.sin(latitudeRad);
        }

        int numReadings = filteredReadings.size();
        double avgX = sumX / numReadings;
        double avgY = sumY / numReadings;
        double avgZ = sumZ / numReadings;

        double avgLongitudeRad = Math.atan2(avgY, avgX);
        double avgHypotenuse = Math.sqrt(avgX * avgX + avgY * avgY);
        double avgLatitudeRad = Math.atan2(avgZ, avgHypotenuse);

        double avgLatitude = Math.toDegrees(avgLatitudeRad);
        double avgLongitude = Math.toDegrees(avgLongitudeRad);

        Location averagedLocation = new Location("GPSAverager");
        averagedLocation.setLatitude(avgLatitude);
        averagedLocation.setLongitude(avgLongitude);

        // Calculate average accuracy (simple average, might not be the most robust)
        double sumAccuracy = 0;
        for (Location location : filteredReadings) {
            sumAccuracy += location.getAccuracy();
        }
        double avgAccuracy = sumAccuracy / numReadings;
        averagedLocation.setAccuracy((float)avgAccuracy);  // Accuracy stored as float

        return averagedLocation;
    }


    // Helper method to remove outlier readings based on accuracy
    private List<Location> removeOutliers() {
        List<Location> filteredList = new ArrayList<>();
        for (Location location : locationReadings) {
            if (location.getAccuracy() <= accuracyThresholdMeters) {
                filteredList.add(location);
            }
        }
        return filteredList;
    }


    // Getter for recording status (optional, for UI updates)
    public boolean isRecording() {
        return isRecording;
    }


    //Getters for Recording time settings

    public long getRecordingIntervalMillis() {
        return recordingIntervalMillis;
    }
    public double getAccuracyThresholdMeters(){
        return accuracyThresholdMeters;
    }

    public int getMinimumReadings(){
        return minimumReadings;
    }



}