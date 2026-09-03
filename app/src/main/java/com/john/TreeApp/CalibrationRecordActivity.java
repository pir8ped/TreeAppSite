package com.john.TreeApp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import gps.GPSAverager;
import gps.GPSCalibrationManager;
import gps.ReferencePoint;

/**
 * Activity for recording GPS readings during calibration.
 * Records averaged GPS position and computes offset against known reference
 * point.
 */
public class CalibrationRecordActivity extends BaseActivity {

    private static final String TAG = "CalibrationRecordActivity";
    public static final String EXTRA_REF_INDEX = "ref_index";

    private static final int MIN_VALID_READINGS = 30;
    private static final int READ_INTERVAL = 2000; // 2 seconds

    private GPSCalibrationManager calibrationManager;
    private GPSAverager gpsAverager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private int refIndex = -1;
    private ReferencePoint referencePoint;
    private boolean isRecording = false;
    private int validReadingsCount = 0;

    private TextInputEditText etRefPointName;
    private TextInputEditText etRefPointCoords;
    private TextView tvGpsStatus;
    private TextView tvAccuracy;
    private Button btnStart;
    private Button btnFinish;
    private Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_calibration_record);
        setActionBarTitle("GPS Calibration");

        calibrationManager = GPSCalibrationManager.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        gpsAverager = new GPSAverager();

        // Find views
        etRefPointName = findViewById(R.id.et_ref_point_name);
        etRefPointCoords = findViewById(R.id.et_ref_point_coords);
        tvGpsStatus = findViewById(R.id.tv_gps_status);
        tvAccuracy = findViewById(R.id.tv_accuracy);
        btnStart = findViewById(R.id.btn_start);
        btnFinish = findViewById(R.id.btn_finish);
        btnCancel = findViewById(R.id.btn_cancel);

        // Get reference point index from intent
        refIndex = getIntent().getIntExtra(EXTRA_REF_INDEX, -1);

        if (refIndex >= 0 && refIndex < calibrationManager.getReferencePoints().size()) {
            // Edit mode: Pre-populate existing reference point
            referencePoint = calibrationManager.getReferencePoints().get(refIndex);
            etRefPointName.setText(referencePoint.getName());
            etRefPointCoords.setText(referencePoint.getCoordinatesString());
            calibrationManager.startCalibration(refIndex);
        } else {
            // Quick Calibrate mode: Empty fields
            referencePoint = null; // Will be created on finish
        }

        // Button listeners
        btnStart.setOnClickListener(v -> startRecording());
        btnFinish.setOnClickListener(v -> finishCalibration());
        btnCancel.setOnClickListener(v -> {
            stopRecording();
            finish();
        });
    }

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate inputs before starting
        String name = etRefPointName.getText().toString().trim();
        String coordsStr = etRefPointCoords.getText().toString().trim();

        if (name.isEmpty()) {
            etRefPointName.setError("Name required");
            return;
        }

        double[] coords = ReferencePoint.parseCoordinates(coordsStr);
        if (coords == null) {
            etRefPointCoords.setError("Invalid format. Use: lat, lon");
            return;
        }

        isRecording = true;
        validReadingsCount = 0;
        gpsAverager.startRecording();

        // Disable inputs during recording
        etRefPointName.setEnabled(false);
        etRefPointCoords.setEnabled(false);

        btnStart.setEnabled(false);
        btnStart.setText("Recording...");
        tvGpsStatus.setText("Recording GPS readings...");
        tvGpsStatus.setBackgroundColor(Color.parseColor("#FFEB3B")); // Yellow

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isRecording)
                    return;

                for (Location location : locationResult.getLocations()) {
                    gpsAverager.addLocationReading(location);
                    float accuracy = location.getAccuracy();

                    if (accuracy < 10) {
                        validReadingsCount++;
                    }

                    tvAccuracy.setText(String.format(Locale.getDefault(),
                            "Valid readings: %d | Accuracy: %.1f m", validReadingsCount, accuracy));

                    // Color code accuracy
                    if (accuracy > 10) {
                        tvAccuracy.setBackgroundColor(Color.parseColor("#FFCDD2")); // Light red
                    } else if (accuracy > 5) {
                        tvAccuracy.setBackgroundColor(Color.parseColor("#FFF9C4")); // Light yellow
                    } else {
                        tvAccuracy.setBackgroundColor(Color.parseColor("#C8E6C9")); // Light green
                    }

                    // Enable finish button when enough readings
                    if (validReadingsCount >= MIN_VALID_READINGS && !btnFinish.isEnabled()) {
                        btnFinish.setEnabled(true);
                        tvGpsStatus.setText("Ready to finish calibration");
                        tvGpsStatus.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                    }
                }
            }
        };

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, READ_INTERVAL)
                .setMinUpdateDistanceMeters(0)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

    }

    private void stopRecording() {
        isRecording = false;
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    private void finishCalibration() {
        stopRecording();

        Location averaged = gpsAverager.getAveragedLocation();
        if (averaged == null) {
            Toast.makeText(this, "Could not compute average location", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get final values from inputs
        String name = etRefPointName.getText().toString().trim();
        String coordsStr = etRefPointCoords.getText().toString().trim();
        double[] coords = ReferencePoint.parseCoordinates(coordsStr);

        if (coords == null) {
            Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        if (refIndex >= 0) {
            // Update existing reference point
            calibrationManager.updateReferencePoint(refIndex, name, coords[0], coords[1]);
        } else {
            // Create new temporary reference point for this calibration
            // Note: If we want to save it permanently, we'd use addReferencePoint
            // For now, we'll add it so it persists for future use
            calibrationManager.addReferencePoint(name, coords[0], coords[1]);
            // Get the index of the newly added point (last one)
            refIndex = calibrationManager.getReferencePoints().size() - 1;
            calibrationManager.startCalibration(refIndex);
        }

        // Complete calibration
        calibrationManager.finishCalibration(averaged);

        double offsetMeters = calibrationManager.getOffsetDistanceMeters();
        String message = String.format(Locale.getDefault(),
                "Calibration complete!\nOffset: %.1f meters", offsetMeters);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.i(TAG, "Calibration completed. Offset: " + offsetMeters + "m");

        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }

    @Override
    public void onBackPressed() {
        stopRecording();
        super.onBackPressed();
    }
}
