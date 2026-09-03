package com.john.TreeApp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Button;
import android.net.Uri;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.IOException;

import com.john.TreeApp.utils.DatabaseBackupManager;
import com.john.TreeApp.utils.DatabaseBackupManager.BackupFileInfo;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupRestoreActivity extends BaseActivity {
    private static final String TAG = "BackupRestoreActivity";
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CODE_SELECT_BACKUP = 101;

    private DatabaseBackupManager backupManager;
    private ListView backupListView;
    private ArrayAdapter<String> adapter;
    private List<String> displayNames = new ArrayList<>();
    private List<String> backupPaths = new ArrayList<>();
    private boolean pendingBackupOperation = false;
    private String pendingRestorePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_backup_restore);

        // Request necessary permissions
        requestStoragePermissions();

        // Set up the ListView for displaying backups
        backupListView = findViewById(R.id.backup_list);
        displayNames = new ArrayList<>();
        backupPaths = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayNames);
        backupListView.setAdapter(adapter);

        // Initialize the activity but delay operations that need permissions
        Button createBackupButton = findViewById(R.id.create_backup_button);

        createBackupButton.setOnClickListener(v -> {
            pendingBackupOperation = true;
            checkPermissionsAndProceed();
        });

        backupListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < backupPaths.size()) {
                String backupPath = backupPaths.get(position);
                showBackupOptionsDialog(backupPath);
            }
        });

        backupListView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position < backupPaths.size()) {
                String backupPath = backupPaths.get(position);
                showDeleteDialog(backupPath);
            }
            return true;
        });

        // Check permissions before initializing backupManager
        checkPermissionsAndProceed();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the backup list every time we return to this activity
        if (backupManager != null) {
            refreshBackupList();
        }
    }

    private void checkPermissionsAndProceed() {
        // For Android 11+, we can skip the permission request as we'll use MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            initializeBackupManager();
            return;
        }

        // For older Android versions, request storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Permission not granted, request it
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE },
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted, proceed
                initializeBackupManager();
            }
        } else {
            // Permission granted by default for Android < 6.0
            initializeBackupManager();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
                initializeBackupManager();
            } else {
                // Permission denied
                Toast.makeText(this, "Storage permission denied. Backups may not work properly.",
                        Toast.LENGTH_LONG).show();
                // Use app-specific storage as fallback
                initializeBackupManager();
            }
        }
    }

    private void initializeBackupManager() {
        // Initialize or ensure backupManager is ready
        if (backupManager == null) {
            backupManager = new DatabaseBackupManager(this);
        }

        // Refresh the list
        refreshBackupList();

        // Process any pending operations
        if (pendingBackupOperation) {
            pendingBackupOperation = false;
            createBackup();
        } else if (pendingRestorePath != null) {
            String path = pendingRestorePath;
            pendingRestorePath = null;
            showRestoreDialog(path);
        }
    }

    private void createBackup() {
        if (backupManager == null) {
            Toast.makeText(this, "Backup manager not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        String backupPath = backupManager.createLocalBackup();
        if (backupPath != null) {
            Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show();
            refreshBackupList();
        } else {
            Toast.makeText(this, "Failed to create backup", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshBackupList() {
        if (backupManager == null) {
            return;
        }

        displayNames.clear();
        backupPaths.clear();

        // Use the new method to get all backups
        List<BackupFileInfo> backupInfos = backupManager.getAllBackups();

        // Sort by date (newest first)
        Collections.sort(backupInfos, (b1, b2) -> b2.getDateCreated().compareTo(b1.getDateCreated()));

        // Add backups to our lists
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (BackupFileInfo info : backupInfos) {
            backupPaths.add(info.getPath());

            // Ensure we have a valid date to display
            Date backupDate = info.getDateCreated();
            String displayDate;

            // Check if date is the Unix epoch (1970-01-01) which would indicate an error
            if (backupDate == null || backupDate.getTime() < 86400000) { // less than 1 day after epoch
                // If we have an invalid date, use the current time
                displayDate = sdf.format(new Date());
                Log.w("BackupRestoreActivity",
                        "Invalid date for backup: " + info.getDisplayName() + ", using current time");
            } else {
                displayDate = sdf.format(backupDate);
            }

            displayNames.add("Backup: " + displayDate);
        }

        adapter.notifyDataSetChanged();
    }

    private void showBackupOptionsDialog(String backupPath) {
        String stats = backupManager.getBackupStats(backupPath);
        String[] options = {
                "Restore Database",
                "Export for Website (DB + Photos ZIP)",
                "Export for PC (CSVs ZIP)",
                "Share .db File"
        };

        new AlertDialog.Builder(this)
                .setTitle("Backup Options\n\n" + stats)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Restore
                            pendingRestorePath = backupPath;
                            checkPermissionsAndProceed();
                            break;
                        case 1: // Export for Website
                            exportAndShareWebsiteZip(backupPath);
                            break;
                        case 2: // Export for PC
                            exportAndShareZip(backupPath);
                            break;
                        case 3: // Share .db
                            shareBackup(backupPath);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportAndShareWebsiteZip(String backupPath) {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Generating Website Export")
                .setMessage("Packaging database and photos for the website...")
                .setCancelable(false)
                .show();

        new Thread(() -> {
            try {
                File zipFile = backupManager.exportWebsiteZip(backupPath);
                Uri contentUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", zipFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/zip");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    startActivity(Intent.createChooser(shareIntent, "Save or Share Website Package to..."));
                });
            } catch (Exception e) {
                Log.e(TAG, "Error exporting website ZIP", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to export website data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void shareBackup(String backupPath) {
        try {
            File backupFile = backupManager.getBackupFileForSharing(backupPath);
            Uri contentUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", backupFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/octet-stream");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Backup to..."));
        } catch (IOException e) {
            Log.e(TAG, "Error sharing backup", e);
            Toast.makeText(this, "Failed to prepare backup for sharing", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportAndShareZip(String backupPath) {
        // Show progress dialog as ZIP generation might take a moment
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Generating Export")
                .setMessage("Please wait while we prepare your data for PC...")
                .setCancelable(false)
                .show();

        new Thread(() -> {
            try {
                File zipFile = backupManager.exportBackupToZip(backupPath);
                Uri contentUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", zipFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/zip");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    startActivity(Intent.createChooser(shareIntent, "Save ZIP to..."));
                });
            } catch (Exception e) {
                Log.e(TAG, "Error exporting ZIP", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to export data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showRestoreDialog(String backupPath) {
        String stats = backupManager.getBackupStats(backupPath);
        new AlertDialog.Builder(this)
                .setTitle("Confirm Restore")
                .setMessage("Are you sure you want to restore this backup? Current data will be replaced.\n\n" + stats)
                .setPositiveButton("Restore NOW", (dialog, which) -> restoreBackup(backupPath))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog(String backupPath) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Backup")
                .setMessage("Are you sure you want to delete this backup?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBackup(backupPath))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Takes persistent permissions for content URI if needed
     * 
     * @param backupPath The backup path to check and take permissions for
     * @return The backup path, possibly modified if needed
     */
    private String ensureBackupAccess(String backupPath) {
        if (backupPath != null && backupPath.startsWith("content://")) {
            try {
                Uri uri = Uri.parse(backupPath);
                int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, flags);
                Log.i(TAG, "Took persistent permissions for " + backupPath);
            } catch (SecurityException e) {
                // Just log the error - we'll do a proper access check later
                Log.e(TAG, "Failed to take persistent permissions for " + backupPath, e);
            }
        }
        return backupPath;
    }

    private void restoreBackup(String backupPath) {
        if (backupManager == null) {
            Toast.makeText(this, "Backup manager not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure we have access to the backup file
        String accessiblePath = ensureBackupAccess(backupPath);

        // If the path starts with content:// but we couldn't get permissions,
        // and the file doesn't exist directly, we need to let the user pick the file
        // again
        if (accessiblePath.startsWith("content://")) {
            try {
                // Try to open the file to check if we have access
                InputStream testStream = getContentResolver().openInputStream(Uri.parse(accessiblePath));
                if (testStream != null) {
                    testStream.close(); // We have access, close the stream
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot access backup file: " + e.getMessage());
                // Show a dialog to let the user know we need to select the file again
                new AlertDialog.Builder(this)
                        .setTitle("Backup Access Error")
                        .setMessage("Cannot access the backup file. This can happen if you reinstalled the app. " +
                                "Would you like to select the backup file again?")
                        .setPositiveButton("Select File", (dialog, which) -> selectBackupFile())
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }
        } else if (accessiblePath.startsWith("/")) {
            // Check if the file exists for a direct file path
            File backupFile = new File(accessiblePath);
            if (!backupFile.exists() || !backupFile.canRead()) {
                Log.e(TAG, "Backup file does not exist or is not readable: " + accessiblePath);
                new AlertDialog.Builder(this)
                        .setTitle("Backup Not Found")
                        .setMessage("The backup file could not be found. This can happen if you reinstalled the app. " +
                                "Would you like to select a backup file?")
                        .setPositiveButton("Select File", (dialog, which) -> selectBackupFile())
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }
        }

        // Show a progress dialog during restore
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Restoring Backup")
                .setMessage("Please wait while the database is being restored...")
                .setCancelable(false)
                .show();

        // Use a background thread for the restore operation
        new Thread(() -> {
            final boolean success = backupManager.restoreFromLocal(accessiblePath);

            // Update UI on the main thread
            runOnUiThread(() -> {
                progressDialog.dismiss();

                if (success) {
                    // Set a flag to indicate that we just restored a database
                    // This will prevent tree importing in next BaseActivity creation
                    SharedPreferences prefs = getSharedPreferences("TreeApp_Settings", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("JUST_RESTORED_DATABASE", true);
                    editor.apply();

                    Toast.makeText(BackupRestoreActivity.this,
                            "Database restored successfully. Restart the app for changes to take effect.",
                            Toast.LENGTH_LONG).show();

                    // Refresh the backup list
                    refreshBackupList();
                } else {
                    Toast.makeText(BackupRestoreActivity.this,
                            "Failed to restore database. Check logs for details.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void deleteBackup(String backupPath) {
        if (backupManager == null) {
            Toast.makeText(this, "Backup manager not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        if (backupManager.deleteBackup(backupPath)) {
            Toast.makeText(this, "Backup deleted", Toast.LENGTH_SHORT).show();
            refreshBackupList();
        } else {
            Toast.makeText(this, "Failed to delete backup", Toast.LENGTH_SHORT).show();
        }
    }

    // Add a method to let the user select a backup file
    private void selectBackupFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        // Set MIME types for database files
        String[] mimeTypes = { "application/octet-stream", "application/x-sqlite3", "application/vnd.sqlite3" };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        startActivityForResult(intent, REQUEST_CODE_SELECT_BACKUP);
    }

    // Handle the result of file selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_BACKUP && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();

                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException e) {
                    Log.e(TAG, "Failed to take persistent permissions", e);
                }

                // Get file display name for UI
                String displayName = getFileDisplayName(uri);

                // Show confirmation dialog
                new AlertDialog.Builder(this)
                        .setTitle("Restore Backup")
                        .setMessage("Do you want to restore from backup: " + displayName + "?\n\n" +
                                "WARNING: This will replace your current database. This action cannot be undone.")
                        .setPositiveButton("Restore", (dialog, which) -> restoreBackup(uri.toString()))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    // Helper method to get a file's display name from URI
    private String getFileDisplayName(Uri uri) {
        String displayName = "Unknown file";

        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file name", e);
        }

        return displayName;
    }

    /**
     * Request storage permissions based on Android version
     */
    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+, check if we can access all files
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    Toast.makeText(this, "Please grant 'All Files Access' permission for backup/restore",
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e("BackupRestoreActivity", "Error requesting all files access", e);
                    Toast.makeText(this, "Cannot request storage permission. Backups may not work properly.",
                            Toast.LENGTH_LONG).show();
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6-10, request runtime permissions
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[] {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        STORAGE_PERMISSION_REQUEST_CODE);
            }
        }
    }
}