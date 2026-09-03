package com.john.TreeApp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import gps.GPSAverager;
import gps.GPSCalibrationManager;

import java.util.Locale;

public class LocationFragment extends Fragment {

    // problem - when this updates the location, it resets the planting date to now.
    // This should happen
    // only when the tree is first planted.

    public interface LocationListener {
        void onLocationAveraged(Location averagedLocation);

        void onLocationCancelled();
    }

    private static final String TAG = "LocationFragment";
    private static final int MAX_RECORDING_TIME = 120000; // 2 minutes
    private static final int READ_INTERVAL = 2000; // 2 seconds
    private static final int MAX_READINGS = 60;

    private static final int MIN_VALID_READINGS = 5;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private GPSAverager gpsAverager;
    private int readingsCount = 0;
    private CountDownTimer timer;
    private boolean isRecording = false;
    private LocationListener listener;

    private TextView tvAccuracy;

    private Location averagedLocation = null;

    // Call this to set the listener in your activity
    public void setLocationListener(LocationListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_location, container, false);
        tvAccuracy = view.findViewById(R.id.tv_accuracy_fragment);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        gpsAverager = new GPSAverager();
        Button btnStartGps = view.findViewById(R.id.btn_start_gps);
        Button btnSetLocation = view.findViewById(R.id.btn_set_location);

        btnStartGps.setOnClickListener(v -> {
            if (!isRecording) {
                // Start GPS recording
                btnStartGps.setText("Cancel");
                btnStartGps.setEnabled(true);
                gpsAverager.setOnEnoughReadingsListener(() -> onSufficientGPSReadings());
                gpsAverager.startRecording();
                startLocationUpdates();
            } else {
                // Cancel GPS recording
                stopLocationUpdates();
                btnStartGps.setText("Start GPS");
                if (listener != null) {
                    listener.onLocationCancelled();
                }
            }
        });

        btnSetLocation.setOnClickListener(v -> {
            if (readingsCount >= MIN_VALID_READINGS) {
                stopLocationUpdates();
                setSetLocationButtonState(false);
                clearAccuracyMeter();
                // Reset Start GPS button text/state
                if (btnStartGps != null) {
                    btnStartGps.setText("Start GPS");
                }
                // Notify the activity of the averaged location
                if (averagedLocation != null && listener != null) {
                    listener.onLocationAveraged(averagedLocation);
                    Toast.makeText(getContext(), "Location set", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Could not compute averaged location", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "Not enough accurate GPS readings", Toast.LENGTH_LONG).show();
            }
        });

        // Now, check if we need to enable Start GPS based on the flag passed from the
        // activity
        boolean enableStartGPS = false;
        if (getArguments() != null) {
            enableStartGPS = getArguments().getBoolean("enableStartGPS", false);
        }
        btnStartGps.setEnabled(enableStartGPS);
    }

    public void onSufficientGPSReadings() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> setSetLocationButtonState(true));
        }
    }

    public void startLocationUpdates() {
        isRecording = true;
        gpsAverager.startRecording();
        readingsCount = 0;

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || !isRecording)
                    return;
                for (Location location : locationResult.getLocations()) {
                    gpsAverager.addLocationReading(location);
                    // Update accuracy display
                    float accuracy = location.getAccuracy();
                    if (accuracy < 5) {
                        readingsCount++;
                    }
                    tvAccuracy.setText(String.format(Locale.getDefault(),
                            "Accurate readings: %d. Accuracy: %.1f m", readingsCount, accuracy));
                    if (accuracy > 10) {
                        tvAccuracy.setBackgroundColor(0xFFFF0000); // Red
                    } else if (accuracy > 5) {
                        tvAccuracy.setBackgroundColor(0xFFFFFF00); // Yellow
                    } else {
                        tvAccuracy.setBackgroundColor(0xFF00FF00); // Green
                    }
                    if (readingsCount >= MIN_VALID_READINGS) {
                        setSetLocationButtonState(true);
                    }
                    // If maximum readings reached, stop updates
                    if (readingsCount >= MAX_READINGS) {
                        stopLocationUpdates();
                        return;
                    }
                }
            }
        };

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, READ_INTERVAL)
                .setMinUpdateDistanceMeters(0)
                .build();

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Optionally start a timer
        timer = new CountDownTimer(MAX_RECORDING_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "Time remaining: " + millisUntilFinished / 1000 + " sec");
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Time limit reached, stopping location updates.");
                stopLocationUpdates();
            }
        }.start();
    }

    public void stopLocationUpdates() {
        isRecording = false;
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        averagedLocation = gpsAverager.getAveragedLocation();

        // Apply GPS calibration offset if available
        if (averagedLocation != null && getContext() != null) {
            GPSCalibrationManager calibrationManager = GPSCalibrationManager.getInstance(getContext());
            if (calibrationManager.isCalibrationValid()) {
                averagedLocation = calibrationManager.applyOffset(averagedLocation);
                Log.d(TAG, "Applied GPS calibration offset to averaged location");
            }
        }
    }

    public void setStartGPSButtonEnabled(boolean enabled) {
        if (getView() != null) {
            Button btnStartGPS = getView().findViewById(R.id.btn_start_gps);
            btnStartGPS.setEnabled(enabled);
            if (!enabled) {
                btnStartGPS.setText("Start GPS");
            }
        }
    }

    public void setSetLocationButtonState(boolean enabled) {
        if (getView() != null) {
            Button btnSetLocation = getView().findViewById(R.id.btn_set_location);
            btnSetLocation.setEnabled(enabled);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    public Location getAveragedLocation() {
        return averagedLocation;
    }

    private void clearAccuracyMeter() {
        tvAccuracy.setText("");
        tvAccuracy.setBackgroundColor(Color.WHITE);
    }
}
