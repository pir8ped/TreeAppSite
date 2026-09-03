package com.john.TreeApp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.john.TreeApp.beans.Location;
import com.john.TreeApp.beans.Tree;
import com.john.TreeApp.LocationFragment;

//import android.location.Location;

import db.TreeService;

public class AddExistingTreeActivity extends BaseActivity implements LocationFragment.LocationListener {
    private static final String TAG = "AddExistingTreeActivity";

    private TreeService treeService;
    private LocationFragment locationFragment;

    // UI elements
    private TextInputEditText editLatinName;
    private TextInputEditText editEnglishName;
    private TextInputEditText editLabel;
    private TextInputEditText editLocated;
    private TextInputEditText editNotes;
    private Button btnCancel;
    private Button btnAddTree;
    private Button btnChooseSpecies;

    // Location data
    private android.location.Location currentLocation;
    private boolean isLocationReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_add_existing_tree);
        setActionBarTitle("Add Existing Tree");

        // Initialize TreeService
        treeService = new TreeService();

        // Initialize UI elements
        initializeViews();

        // Set up location fragment
        setupLocationFragment();

        // Set up button listeners
        setupButtonListeners();
    }

    private com.google.android.material.textfield.TextInputLayout layoutLatinName;
    private com.google.android.material.textfield.TextInputLayout layoutEnglishName;

    private void initializeViews() {
        layoutLatinName = findViewById(R.id.layout_latin_name);
        layoutEnglishName = findViewById(R.id.layout_english_name);
        editLatinName = findViewById(R.id.edit_latin_name);
        editEnglishName = findViewById(R.id.edit_english_name);
        editLabel = findViewById(R.id.edit_label);
        editLocated = findViewById(R.id.edit_located);
        editNotes = findViewById(R.id.edit_notes);
        btnCancel = findViewById(R.id.btn_cancel);
        btnAddTree = findViewById(R.id.btn_add_tree);
        btnChooseSpecies = findViewById(R.id.btn_choose_species);
    }

    private void setupLocationFragment() {
        locationFragment = new LocationFragment();
        locationFragment.setLocationListener(this);
        Bundle args = new Bundle();
        args.putBoolean("enableStartGPS", false);
        locationFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.location_fragment_holder, locationFragment)
                .commit();
    }

    private void setupButtonListeners() {
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnAddTree.setOnClickListener(v -> addExistingTree());
        btnChooseSpecies.setOnClickListener(v -> openSpeciesPicker());

        // Enable Start GPS when species is populated (prefer Latin; or English present)
        TextWatcher speciesWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean hasLatin = editLatinName.getText() != null
                        && !editLatinName.getText().toString().trim().isEmpty();
                boolean hasEnglish = editEnglishName.getText() != null
                        && !editEnglishName.getText().toString().trim().isEmpty();
                if (locationFragment != null) {
                    locationFragment.setStartGPSButtonEnabled(hasLatin || hasEnglish);
                }
            }
        };
        editLatinName.addTextChangedListener(speciesWatcher);
        editEnglishName.addTextChangedListener(speciesWatcher);
    }

    private void openSpeciesPicker() {
        Intent intent = new Intent(this, SearchSpeciesActivity.class);
        intent.putExtra("returnResult", true);
        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            String englishName = data.getStringExtra("englishName");
            String latinName = data.getStringExtra("latinName");
            if (englishName != null) {
                editEnglishName.setText(englishName);
            }
            if (latinName != null) {
                editLatinName.setText(latinName);
            }

            // Show the fields and update button text
            layoutLatinName.setVisibility(android.view.View.VISIBLE);
            layoutEnglishName.setVisibility(android.view.View.VISIBLE);
            btnChooseSpecies.setText("Change Species");

            if (locationFragment != null) {
                locationFragment.setStartGPSButtonEnabled(true);
            }
        }
    }

    private void addExistingTree() {
        // Validate required fields
        if (!validateInput()) {
            return;
        }

        if (!isLocationReady) {
            Toast.makeText(this, "Please wait for location to be ready", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create Location object from GPS coordinates
            Location location = new Location();
            location.setLatitude(currentLocation.getLatitude());
            location.setLongitude(currentLocation.getLongitude());

            // Generate label if not provided
            String label = editLabel.getText().toString().trim();
            if (label.isEmpty()) {
                label = treeService.generateUniqueLabel(editLatinName.getText().toString().trim());
                Log.d(TAG, "Generated label: " + label);
            }

            // Create Tree object
            Tree tree = new Tree.Builder(editLatinName.getText().toString().trim())
                    .located(editLocated.getText().toString().trim())
                    .label(label)
                    .build();

            // Add the tree using TreeService
            String notes = editNotes.getText().toString().trim();
            int treeId = treeService.addTree(tree, location, notes.isEmpty() ? null : notes);

            if (treeId != -1) {
                String message = "Tree added successfully with ID: " + treeId;
                if (editLabel.getText().toString().trim().isEmpty()) {
                    message += "\nGenerated label: " + label;
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                // Return success result
                Intent resultIntent = new Intent();
                resultIntent.putExtra("treeId", treeId);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Failed to add tree. Please try again.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding existing tree: " + e.getMessage(), e);
            Toast.makeText(this, "Error adding tree: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean validateInput() {
        boolean isValid = true;

        // User can provide either Latin or English name. If Latin is empty but English
        // provided,
        // attempt to resolve Latin name from English species table.
        String latin = editLatinName.getText() != null ? editLatinName.getText().toString().trim() : "";
        String english = editEnglishName.getText() != null ? editEnglishName.getText().toString().trim() : "";

        if (latin.isEmpty() && english.isEmpty()) {
            editLatinName.setError("Provide Latin or English name");
            editEnglishName.setError("Provide English or Latin name");
            isValid = false;
        } else {
            // Clear previous errors
            editLatinName.setError(null);
            editEnglishName.setError(null);

            if (latin.isEmpty() && !english.isEmpty()) {
                try {
                    db.TreeSpeciesDAOImpl speciesDAO = new db.TreeSpeciesDAOImpl();
                    com.john.TreeApp.beans.TreeSpecies species = speciesDAO.findTreesSpecies_English(english);
                    if (species != null && species.getLatinName() != null && !species.getLatinName().trim().isEmpty()) {
                        editLatinName.setText(species.getLatinName().trim());
                        latin = species.getLatinName().trim();
                    } else {
                        editEnglishName.setError("Unknown English name – pick a known species");
                        isValid = false;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error resolving latin name from english: " + e.getMessage(), e);
                    editEnglishName.setError("Error looking up species");
                    isValid = false;
                }
            }
        }

        // Label is optional - will be auto-generated if empty
        editLabel.setError(null);

        // Check if provided label is unique (only if user provided one)
        if (!TextUtils.isEmpty(editLabel.getText())) {
            String label = editLabel.getText().toString().trim();
            if (!label.isEmpty() && !treeService.isLabelUniqueForAdd(label)) {
                editLabel.setError("Label is already in use in this collection");
                isValid = false;
            } else {
                editLabel.setError(null);
            }
        }

        return isValid;
    }

    // LocationFragment.LocationListener implementation
    @Override
    public void onLocationAveraged(android.location.Location location) {
        Log.d(TAG, "Location averaged: " + location.getLatitude() + ", " + location.getLongitude());
        currentLocation = location;
        isLocationReady = true;

        // Update UI to show location is ready
        btnAddTree.setEnabled(true);
        btnAddTree.setText("Add Tree");
    }

    @Override
    public void onLocationCancelled() {
        Log.w(TAG, "Location cancelled");
        Toast.makeText(this, "Location cancelled. Please try again.", Toast.LENGTH_SHORT).show();
        isLocationReady = false;
        btnAddTree.setEnabled(false);
        btnAddTree.setText("Waiting for Location...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationFragment != null) {
            locationFragment.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationFragment != null) {
            locationFragment.onPause();
        }
    }

}
