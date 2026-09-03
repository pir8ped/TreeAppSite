package com.john.TreeApp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.john.TreeApp.beans.Collection;

import db.CollectionDAO;
import db.CollectionDAOImpl;
import db.DatabaseCreator;

public class BaseActivity extends AppCompatActivity {

    private String TAG = "BaseActivity";

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;
    private ActionBarDrawerToggle drawerToggle;

    private boolean initialisationHasRun = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the database creator
        DatabaseCreator.initialize(getApplicationContext());

        // Check if a database restore just happened
        SharedPreferences prefs = getSharedPreferences("TreeApp_Settings", Context.MODE_PRIVATE);
        boolean justRestoredDatabase = prefs.getBoolean("JUST_RESTORED_DATABASE", false);

        if (justRestoredDatabase) {
            // Clear the flag so it doesn't affect future app starts
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("JUST_RESTORED_DATABASE", false);
            editor.apply();

            Log.i(TAG, "Database was just restored, skipping tree import");
            // Skip tree import since we just restored a database that already contains
            // trees
            initialisationHasRun = true;
        }

        /*
         * Tree import functionality preserved but commented out as it's no longer
         * needed
         * Only import trees if they haven't been imported before in this session
         * and we didn't just restore a database
         * if (!initialisationHasRun) {
         * try {
         * Log.d(TAG,
         * "First BaseActivity creation in this session, checking if trees need to be imported"
         * );
         * DatabaseCreator.getInstance().importTreesIfNeeded();
         * initialisationHasRun = true;
         * } catch (Exception e) {
         * Log.e(TAG, "Error importing trees: " + e.getMessage(), e);
         * }
         * }
         */

        // Set the base layout as the content view.
        setContentView(R.layout.activity_base);

        // Initialize drawer and navigation view.
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Initialize the toolbar and set it as the action bar.
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set up navigation item selection handling.
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.StartActivity) {
                // Launch StartActivity if not already on it
                if (!(getClass().equals(StartActivity.class))) {
                    Intent intent = new Intent(this, StartActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Collections", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.ScionListActivity) {
                // Launch ScionListActivity if not already on it
                if (!(getClass().equals(ScionListActivity.class))) {
                    Intent intent = new Intent(this, ScionListActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Scions", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.SearchSpeciesActivity) {
                // Launch SearchSpeciesActivity if not already on it.
                if (!(getClass().equals(SearchSpeciesActivity.class))) {
                    Intent intent = new Intent(this, SearchSpeciesActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Search Species", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.AddExistingTreeActivity) {
                // Launch AddExistingTreeActivity if not already on it
                if (!(getClass().equals(AddExistingTreeActivity.class))) {
                    Intent intent = new Intent(this, AddExistingTreeActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Add Existing Tree", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.TreeToPlantListActivity) {
                // Launch TreeToPlantListActivity if not already on it.
                if (!(getClass().equals(TreeToPlantListActivity.class))) {
                    Intent intent = new Intent(this, TreeToPlantListActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Trees to Plant", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.PlantedTreeListActivity) {
                // Launch PlantedTreeListActivity if not already on it.
                if (!(getClass().equals(PlantedTreeListActivity.class))) {
                    Intent intent = new Intent(this, PlantedTreeListActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Planted Trees", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.MapViewActivity) {
                // Launch MapViewActivity if not already on it.
                if (!(getClass().equals(MapViewActivity.class))) {
                    Intent intent = new Intent(this, MapViewActivity.class);
                    intent.setAction("android.intent.action.MAIN");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Map View", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.ReminderListActivity) {
                // Launch ReminderListActivity if not already on it.
                if (!(getClass().equals(ReminderListActivity.class))) {
                    Intent intent = new Intent(this, ReminderListActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Reminders", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.BackupRestoreActivity) {
                // Launch BackupRestoreActivity if not already on it
                if (!(getClass().equals(BackupRestoreActivity.class))) {
                    Intent intent = new Intent(this, BackupRestoreActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Backup/Restore", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.SearchNotesActivity) {
                // Launch SearchNotesActivity if not already on it
                if (!(getClass().equals(SearchNotesActivity.class))) {
                    Intent intent = new Intent(this, SearchNotesActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Search Notes", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.findTree) {
                // Launch FindTreeActivity if not already on it
                if (!(getClass().equals(FindTreeActivity.class))) {
                    Intent intent = new Intent(this, FindTreeActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on Find Tree", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.nav_calibrate_gps) {
                // Launch GPSCalibrationActivity to list reference points
                if (!(getClass().equals(GPSCalibrationActivity.class))) {
                    Intent intent = new Intent(this, GPSCalibrationActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Already on GPS Calibration", Toast.LENGTH_SHORT).show();
                }
            }
            drawerLayout.closeDrawers();
            return true;
        });

        refreshCollectionSubtitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCollectionSubtitle();
    }

    /**
     * Updates the action bar subtitle to show the currently selected collection.
     */
    protected void refreshCollectionSubtitle() {
        CollectionDAO collectionDAO = new CollectionDAOImpl();
        try {
            int selectedId = collectionDAO.getSelectedCollectionId();
            if (selectedId != -1) {
                Collection selected = collectionDAO.getCollection(selectedId);
                if (selected != null && getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle("Collection: " + selected.getName());
                }
            } else if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing collection subtitle: " + e.getMessage());
        }
    }

    /**
     * Use this method in your activities to insert their content into the common
     * layout.
     */
    protected void setActivityLayout(int layoutResID) {
        FrameLayout contentFrame = findViewById(R.id.content_frame);
        getLayoutInflater().inflate(layoutResID, contentFrame, true);
    }

    // Custom method to set title from child activity.
    public void setActionBarTitle(String title) {
        Log.d("BaseActivity", "Setting title: " + title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    /**
     * @deprecated Tree import is now handled directly in DatabaseCreator
     */
    public void runPostInitializationTasks() {
        Log.i(TAG, "Tree initialization is now handled directly in DatabaseCreator");
    }
}