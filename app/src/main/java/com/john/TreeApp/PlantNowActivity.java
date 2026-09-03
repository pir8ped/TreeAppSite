package com.john.TreeApp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import db.CollectionDAOImpl;
import db.LocationDAO;
import db.LocationDAOImpl;
import db.TreeService;
import db.TreeDAO;
import db.TreeDAOImpl;
import gps.GPSAverager;
import com.john.TreeApp.beans.Tree;

public class PlantNowActivity extends BaseActivity implements LocationFragment.LocationListener {

    private static final String TAG = "PlantNowActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    // Adjustable Settings:
    private static final int MAX_READINGS = 60; // Maximum number of GPS readings
    private static final int MAX_RECORDING_TIME = 120000; // 2 minutes (in milliseconds)
    private static final int READ_INTERVAL = 2000; // Interval between readings (2 seconds)
    private final boolean useTimeLimit = true;

    private GPSAverager gpsAverager;

    // UI fields from your layout
    private TextView tvEnglishName, tvLatinName;
    private EditText etVariety, etRootstock;
    private EditText etLabel, etOrigin;
    private Button btnStartPlanting, btnFinished;

    // Planting state flags and timer
    private boolean isPlanting = false;
    private int readingsCount = 0;
    private CountDownTimer timer;
    private boolean isPlantingAnother = false; // Flag to indicate if coming from "Plant Another"

    // LocationFragment reference
    private LocationFragment locationFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_plant_now);

        // Initialize views (note: tv_accuracy is removed if locationFragment displays
        // accuracy)
        tvEnglishName = findViewById(R.id.tv_english_name);
        tvLatinName = findViewById(R.id.tv_latin_name);
        etLabel = findViewById(R.id.et_label);
        etVariety = findViewById(R.id.tv_variety);
        etRootstock = findViewById(R.id.tv_rootstock);
        etOrigin = findViewById(R.id.tv_origin);
        btnStartPlanting = findViewById(R.id.btn_start_planting);
        btnFinished = findViewById(R.id.btn_finished);

        // Retrieve tree details from Intent
        Intent intent = getIntent();
        if (intent != null) {
            tvEnglishName.setText(intent.getStringExtra("englishName"));
            tvLatinName.setText(intent.getStringExtra("latinName"));
            etVariety.setText(intent.getStringExtra("variety"));
            etRootstock.setText(intent.getStringExtra("rootstock"));
            etOrigin.setText(intent.getStringExtra("origin"));

            // Check if this is from "Plant Another"
            isPlantingAnother = intent.getBooleanExtra("isPlantingAnother", false);
        }

        // Initialize GPS Averager
        gpsAverager = new GPSAverager();

        // Set up button listeners
        btnStartPlanting.setOnClickListener(v -> {
            if (!isPlanting) {
                startPlantingProcess();
            } else {
                finishPlantingProcess();
                setUItoStart();
            }
        });

        btnFinished.setOnClickListener(v -> {
            Intent intent2 = new Intent(PlantNowActivity.this, PlantedTreeListActivity.class);
            startActivity(intent2);
            finish(); // Close this activity
        });

        // Initialize LocationFragment
        locationFragment = new LocationFragment();
        locationFragment.setLocationListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.location_fragment_holder, locationFragment)
                .commit();

        checkAndRequestLocationPermission();

        setUItoStart();
    }

    private void startPlantingProcess() {
        String label = etLabel.getText().toString().trim();
        if (TextUtils.isEmpty(label)) {
            Toast.makeText(this, "Please enter a label", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if label is already in use
        TreeDAO treeDAO = new TreeDAOImpl();
        int collectionId = new CollectionDAOImpl().getSelectedCollectionId();
        if (!treeDAO.isLabelUniqueForAdd(label, collectionId)) {
            Toast.makeText(this, "Label is already in use in this collection. Choose a unique label.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        isPlanting = true;
        setUItoMakeGPSAvailable();
    }

    // Called when "Finish Planting" is pressed
    private void finishPlantingProcess() {
        if (!isPlanting)
            return;
        stopPlanting();
    }

    // Stops planting: stops location updates (via fragment), calculates average
    // location, and saves the tree
    private void stopPlanting() {
        if (locationFragment != null) {
            locationFragment.stopLocationUpdates();
        }

        isPlanting = false;
        etLabel.setEnabled(true);
        etOrigin.setEnabled(true);
        etVariety.setEnabled(true);
        etRootstock.setEnabled(true);
        btnStartPlanting.setEnabled(true);
        btnStartPlanting.setText("Plant another?");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // Starts a countdown timer if using a time limit for recording
    private void startTimer() {
        timer = new CountDownTimer(MAX_RECORDING_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "Time remaining: " + millisUntilFinished / 1000 + " seconds");
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Recording time limit reached.");
                stopPlanting();
            }
        }.start();
    }

    // Checks for location permission and requests it if not granted
    private void checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted. You can now plant.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission required for Plant Now functionality.", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void saveTreeToDatabase(double latitude, double longitude) {
        TreeDAO treeDAO = new TreeDAOImpl();
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Intent is null. Could not get data from TreesToPlant table.");
            Toast.makeText(PlantNowActivity.this, "Intent Error", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get latinName from intent
        String latinName = intent.getStringExtra("latinName");
        if (latinName == null || latinName.isEmpty()) {
            Log.e(TAG, "Invalid latinName from intent");
            Toast.makeText(PlantNowActivity.this, "Error: Invalid species name", Toast.LENGTH_SHORT).show();
            return;
        }

        int collectionId = new CollectionDAOImpl().getSelectedCollectionId();
        if (collectionId == -1) {
            Log.e(TAG, "Invalid selectedCollectionId from Shared Preferences");
            Toast.makeText(PlantNowActivity.this, "Error: No collection selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new location record
        com.john.TreeApp.beans.Location location = new com.john.TreeApp.beans.Location();
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        // Get values from EditText fields
        String origin = etOrigin.getText().toString().trim();
        String variety = etVariety.getText().toString().trim();
        String rootstock = etRootstock.getText().toString().trim();
        String label = etLabel.getText().toString().trim();

        String message;
        if (isPlantingAnother) {
            // Plant Another: Create a brand new tree with all the editable values
            LocationDAO locationDAO = new LocationDAOImpl();
            int locationId = (int) locationDAO.insertLocation(location);
            if (locationId == -1) {
                Toast.makeText(PlantNowActivity.this, "Error: Failed to save location", Toast.LENGTH_SHORT).show();
                return;
            }

            Tree newTree = new Tree.Builder(latinName)
                    .collectionId(collectionId)
                    .locationId(locationId)
                    .label(label)
                    .origin(origin.isEmpty() ? null : origin)
                    .variety(variety.isEmpty() ? null : variety)
                    .rootstock(rootstock.isEmpty() ? null : rootstock)
                    .datePlanted(new java.sql.Date(System.currentTimeMillis()))
                    .build();

            long treeId = treeDAO.addTree(newTree);
            message = treeId > 0 ? "Tree planted successfully" : "Failed to plant tree";
        } else {
            // Regular plant flow: Update existing unplanted tree
            message = treeDAO.updateTreeWithLocationAndLabel(
                    latinName,
                    collectionId,
                    location,
                    label,
                    origin);
        }

        Toast.makeText(PlantNowActivity.this, message, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Tree planted: Species=" + latinName + ", Collection=" + collectionId +
                ", Variety=" + variety + ", Rootstock=" + rootstock);
    }

    @Override
    public void onLocationAveraged(Location averagedLocation) {
        if (averagedLocation != null) {
            double latitude = averagedLocation.getLatitude();
            double longitude = averagedLocation.getLongitude();
            float accuracy = averagedLocation.getAccuracy();
            String label = etLabel.getText().toString().trim();

            Log.d(TAG, "Final Averaged Location: Lat=" + latitude + ", Lon=" + longitude +
                    ", Accuracy=" + accuracy + " Label=" + label);
            saveTreeToDatabase(latitude, longitude);

            setUItoFinishedPlanting();
        }
    }

    @Override
    public void onLocationCancelled() {
        // Cancellation is handled by the main UI's cancel button
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ensure that location updates are stopped when the activity pauses
        if (locationFragment != null) {
            locationFragment.stopLocationUpdates();
        }
    }

    private void setUItoStart() {
        // This method resets the UI back to its initial state (for example, if the user
        // cancels)
        etLabel.setEnabled(true);
        etLabel.setText("");
        etOrigin.setEnabled(true);
        btnStartPlanting.setEnabled(true);
        btnStartPlanting.setText("Plant Now");
        btnFinished.setVisibility(View.GONE);
        // Disable GPS buttons in LocationFragment:
        if (locationFragment != null) {
            locationFragment.setStartGPSButtonEnabled(false);
            locationFragment.setSetLocationButtonState(false);
        }
        isPlanting = false;

        // Check if there are more trees of the same species without locations
        // Skip this check if we're planting another tree (user explicitly wants to add
        // a new one)
        if (!isPlantingAnother) {
            TreeDAO treeDAO = new TreeDAOImpl();
            String latinName = tvLatinName.getText().toString();
            if (treeDAO.areUnplantedTrees(latinName) == 0) {
                Toast.makeText(this, "No more trees of this species to plant", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity if no more trees to plant
            }
        }
    }

    private void setUItoMakeGPSAvailable() {
        etLabel.setEnabled(false);
        etOrigin.setEnabled(false);
        etVariety.setEnabled(false);
        etRootstock.setEnabled(false);
        btnStartPlanting.setText("Cancel");
        locationFragment.setStartGPSButtonEnabled(true);
        locationFragment.setSetLocationButtonState(false);
    }

    private void setUItoFinishedPlanting() {
        btnStartPlanting.setText("Plant another?");
        btnStartPlanting.setEnabled(true);
        btnFinished.setVisibility(View.VISIBLE);
        locationFragment.setSetLocationButtonState(false);
        locationFragment.setStartGPSButtonEnabled(false);
    }
}
