package com.john.TreeApp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.util.Log;
import java.io.File;

import com.john.TreeApp.beans.Collection;
import com.john.TreeApp.beans.TreeStatistics;
import com.john.TreeApp.dialogs.ChangeCollectionDialog;
import com.john.TreeApp.dialogs.DeleteCollectionDialog;
import com.john.TreeApp.dialogs.NewCollectionDialog;
import com.john.TreeApp.dialogs.OnCollectionChangedListener;
import db.CollectionDAO;
import db.CollectionDAOImpl;
import db.DatabaseCreator;

public class StartActivity extends BaseActivity implements OnCollectionChangedListener {
    private TextView currentCollectionText;
    private TextView treeStatusStatsText;
    private TextView speciesStatsText;
    private Button changeCollectionButton;
    private Button deleteCollectionButton;
    private Button newCollectionButton;
    private Button backupCollectionButton;
    private File lastExportedFile;
    private CollectionDAO collectionDAO;
    private db.TreeDAO treeDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_start);

        // Get the collection DAO from DatabaseCreator
        collectionDAO = DatabaseCreator.getInstance().getCollectionDAO();
        treeDAO = DatabaseCreator.getInstance().getTreeDAO();

        // Initialize collection management views
        currentCollectionText = findViewById(R.id.currentCollectionText);
        treeStatusStatsText = findViewById(R.id.treeStatusStatsText);
        speciesStatsText = findViewById(R.id.speciesStatsText);
        changeCollectionButton = findViewById(R.id.changeCollectionButton);
        deleteCollectionButton = findViewById(R.id.deleteCollectionButton);
        newCollectionButton = findViewById(R.id.newCollectionButton);
        backupCollectionButton = findViewById(R.id.backupCollectionButton);

        // Set up collection management buttons
        changeCollectionButton.setOnClickListener(v -> showChangeCollectionDialog());
        deleteCollectionButton.setOnClickListener(v -> showDeleteCollectionDialog());
        newCollectionButton.setOnClickListener(v -> showNewCollectionDialog());
        backupCollectionButton.setOnClickListener(v -> startCollectionBackup());

        // Check if we need to create a default collection
        if (collectionDAO.getAllCollections().isEmpty()) {
            createDefaultCollection();
        }

        // Update the current collection display
        updateCollectionDisplay();
    }

    private void createDefaultCollection() {
        Collection defaultCollection = new Collection();
        defaultCollection.setName("Valley");
        long id = collectionDAO.addCollection(defaultCollection);
        collectionDAO.setSelectedCollectionId((int) id);
    }

    private void showChangeCollectionDialog() {
        new ChangeCollectionDialog().show(getSupportFragmentManager(), "changeCollection");
    }

    private void showDeleteCollectionDialog() {
        int currentCollectionId = collectionDAO.getSelectedCollectionId();
        if (collectionDAO.getAllCollections().size() <= 1) {
            Toast.makeText(this,
                    "Cannot delete the last collection. Create a new collection first.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        DeleteCollectionDialog.newInstance(currentCollectionId)
                .show(getSupportFragmentManager(), "deleteCollection");
    }

    private void showNewCollectionDialog() {
        new NewCollectionDialog().show(getSupportFragmentManager(), "newCollection");
    }

    @Override
    public void onCollectionChanged(int newCollectionId) {
        updateCollectionDisplay();
        refreshCollectionSubtitle();
    }

    private void updateCollectionDisplay() {
        int currentCollectionId = collectionDAO.getSelectedCollectionId();
        Collection currentCollection = collectionDAO.getCollection(currentCollectionId);
        if (currentCollection != null) {
            currentCollectionText.setText(currentCollection.getName());
            
            // Get and display statistics
            TreeStatistics stats = treeDAO.getTreeStatistics(currentCollectionId);
            treeStatusStatsText.setText(String.format("Verified: %d  Unverified: %d  Lost: %d",
                    stats.getVerifiedCount(), stats.getUnverifiedCount(), stats.getLostCount()));
            speciesStatsText.setText(String.format("Species: %d", stats.getSpeciesCount()));
        } else {
            currentCollectionText.setText("No Collection Selected");
            treeStatusStatsText.setText("Verified: 0  Unverified: 0  Lost: 0");
            speciesStatsText.setText("Species: 0");
        }
    }

    private void startCollectionBackup() {
        int currentCollectionId = collectionDAO.getSelectedCollectionId();
        Collection currentCollection = collectionDAO.getCollection(currentCollectionId);
        
        if (currentCollection == null) {
            Toast.makeText(this, "No collection selected", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Backing up Collection");
        progressDialog.setMessage("Preparing photos and captions for " + currentCollection.getName() + "...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                com.john.TreeApp.utils.CollectionExporter exporter = new com.john.TreeApp.utils.CollectionExporter(this);
                // Clean up any old remaining exports first
                exporter.cleanupOldExports();
                
                File zipFile = exporter.exportCollectionPhotos(currentCollectionId, currentCollection.getName());
                lastExportedFile = zipFile;
                
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    shareZipFile(zipFile);
                });
            } catch (Exception e) {
                Log.e("StartActivity", "Backup failed", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void shareZipFile(File zipFile) {
        android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", zipFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Save or Share Backup"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCollectionDisplay();
        
        // Clean up the last exported file if the user has returned to the app
        if (lastExportedFile != null && lastExportedFile.exists()) {
            if (lastExportedFile.delete()) {
                Log.d("StartActivity", "Deleted temporary export file: " + lastExportedFile.getName());
            }
            lastExportedFile = null;
        }
    }
}
