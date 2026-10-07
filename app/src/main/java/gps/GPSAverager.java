package gps;

import android.location.Location;
import java.util.ArrayList;
import java.util.List;

public class GPSAverager {

    private final List<Location> locationReadings = new ArrayList<>();
    private boolean isRecording = false;

    private long recordingIntervalMillis = 3000; // Throttle to 1 reading per 3 seconds
    private double accuracyThresholdMeters = 15.0; // Tighter accuracy filter
    private int minimumReadings = 5;
    private long lastReadingTime = 0;

    // Drop all readings taken in the first 15 seconds of the session
    private static final long WARMUP_PERIOD_MS = 15000; // 15 seconds

    public GPSAverager() {}

    public void startRecording() {
        locationReadings.clear();
        lastReadingTime = 0;
        isRecording = true;
    }

    public void addLocationReading(Location location) {
        if (!isRecording || location == null) return;

        // 1. Filter out fixes with poor accuracy immediately
        if (location.hasAccuracy() && location.getAccuracy() > accuracyThresholdMeters) {
            return;
        }

        // 2. Enforce time throttling (ignore rapid-fire duplicate callbacks)
        long currentTime = location.getTime();
        if (currentTime - lastReadingTime < recordingIntervalMillis) {
            return;
        }

        // 3. Prevent duplicate coordinate flooding
        if (!locationReadings.isEmpty()) {
            Location last = locationReadings.get(locationReadings.size() - 1);
            if (last.getLatitude() == location.getLatitude() &&
                    last.getLongitude() == location.getLongitude()) {
                return; // Skip identical fix
            }
        }

        locationReadings.add(new Location(location));
        lastReadingTime = currentTime;

        if (locationReadings.size() >= minimumReadings && listener != null) {
            listener.onEnoughReadings();
        }
    }

    /**
     * Stops recording, filters out early warm-up fixes, and calculates an
     * accuracy-weighted average location.
     *
     * @return Averaged Location, or null if insufficient valid readings exist.
     */
    public Location getAveragedLocation() {
        isRecording = false;

        if (locationReadings.size() < minimumReadings) {
            return null; // Not enough total readings collected
        }

        // --- Step 1: Discard early warm-up noise (Option 1) ---
        // The EKF Kalman filter takes 10–15s to settle after placing the phone down.
        long WARMUP_PERIOD_MS = 15000; // 15 seconds
        long startTime = locationReadings.get(0).getTime();

        List<Location> settledReadings = new ArrayList<>();
        for (Location loc : locationReadings) {
            if (loc.getTime() - startTime >= WARMUP_PERIOD_MS) {
                settledReadings.add(loc);
            }
        }

        // Fallback: If the recording session was under 15 seconds total,
        // use all collected readings rather than discarding everything.
        List<Location> validReadings = (settledReadings.size() >= minimumReadings)
                ? settledReadings
                : locationReadings;

        // Double check minimum threshold against the active dataset
        if (validReadings.size() < minimumReadings) {
            return null;
        }

        // --- Step 2: Accuracy-weighted averaging (Option 3) ---
        double totalWeight = 0;
        double weightedLatSum = 0;
        double weightedLonSum = 0;
        double totalAccuracy = 0;

        for (Location loc : validReadings) {
            // Inverse-variance weighting: weight = 1 / (accuracy^2)
            // Clamp accuracy floor to 0.5m to avoid zero-division or extreme weight spikes.
            double accuracy = loc.hasAccuracy() ? Math.max(loc.getAccuracy(), 0.5f) : 10.0;
            double weight = 1.0 / (accuracy * accuracy);

            weightedLatSum += loc.getLatitude() * weight;
            weightedLonSum += loc.getLongitude() * weight;
            totalWeight += weight;
            totalAccuracy += accuracy;
        }

        double avgLat = weightedLatSum / totalWeight;
        double avgLon = weightedLonSum / totalWeight;

        Location averagedLocation = new Location("GPSAverager");
        averagedLocation.setLatitude(avgLat);
        averagedLocation.setLongitude(avgLon);
        // Store arithmetic average of accuracy for general reference
        averagedLocation.setAccuracy((float) (totalAccuracy / validReadings.size()));
        averagedLocation.setTime(System.currentTimeMillis());

        return averagedLocation;
    }
    public interface OnEnoughReadingsListener {
        void onEnoughReadings();
    }

    private OnEnoughReadingsListener listener;

    public void setOnEnoughReadingsListener(OnEnoughReadingsListener listener) {
        this.listener = listener;
    }

    public boolean isRecording() { return isRecording; }
    public void setRecordingIntervalMillis(long interval) { this.recordingIntervalMillis = interval; }
    public void setAccuracyThresholdMeters(double threshold) { this.accuracyThresholdMeters = threshold; }
    public void setMinimumReadings(int minReadings) { this.minimumReadings = minReadings; }
}