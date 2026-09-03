package com.john.TreeApp.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import db.DatabaseCreator;
import db.TreeSpeciesBulkUploader;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class DatabaseBackupManager {
    private static final String TAG = "DatabaseBackupManager";
    private static final String BACKUP_DIR = "TreeAppBackups";
    private static final String DATABASE_NAME = "mydatabase.db";
    private static final String DATABASE_JOURNAL = "mydatabase.db-journal";
    private final Context context;
    private final String databasePath;
    private final String databaseJournalPath;
    private final File backupDir;
    private final File legacyBackupDir; // For checking old backups from previous versions

    public DatabaseBackupManager(Context context) {
        this.context = context.getApplicationContext();

        // Be more careful getting the database path
        try {
            this.databasePath = context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
            this.databaseJournalPath = context.getDatabasePath(DATABASE_JOURNAL).getAbsolutePath();
            Log.d(TAG, "Database path: " + databasePath);
        } catch (Exception e) {
            Log.e(TAG, "Error getting database path", e);
            throw e; // Re-throw to prevent partial initialization
        }

        // CHANGED: For Android 10 and below, use the root of external storage
        // (not app-specific), which persists across reinstallations
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            this.backupDir = new File(Environment.getExternalStorageDirectory(), BACKUP_DIR);
            Log.d(TAG, "Using external storage root directory for backups: " + backupDir.getAbsolutePath());
        } else {
            // For Android 11+, use the Documents directory and MediaStore
            this.backupDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), BACKUP_DIR);
            Log.d(TAG, "Using documents directory for backups: " + backupDir.getAbsolutePath());
        }

        // Also keep track of the old backup location for migration purposes
        this.legacyBackupDir = new File(context.getExternalFilesDir(null), BACKUP_DIR);

        // Only create directories when actually needed (not during constructor)
        Log.d(TAG, "DatabaseBackupManager initialized");
    }

    /**
     * Ensure backup directories exist
     */
    private void ensureBackupDirectoriesExist() {
        // For Android 10 and below, create directory in external storage root
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (!backupDir.exists()) {
                boolean created = backupDir.mkdirs();
                if (!created) {
                    Log.w(TAG, "Failed to create backup directory in external storage: " + backupDir.getAbsolutePath());
                } else {
                    Log.d(TAG, "Created backup directory: " + backupDir.getAbsolutePath());
                }
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // For Android Q (10), create in Documents but not with MediaStore
            if (!backupDir.exists()) {
                boolean created = backupDir.mkdirs();
                if (!created) {
                    Log.w(TAG, "Failed to create backup directory in Documents folder: " + backupDir.getAbsolutePath());
                } else {
                    Log.d(TAG, "Created backup directory: " + backupDir.getAbsolutePath());
                }
            }
        }
        // For Android 11+, directories are created automatically with MediaStore

        // Always ensure the legacy directory exists for backward compatibility
        if (!legacyBackupDir.exists()) {
            boolean created = legacyBackupDir.mkdirs();
            if (!created) {
                Log.w(TAG, "Failed to create backup directory in app's external files: "
                        + legacyBackupDir.getAbsolutePath());
            } else {
                Log.d(TAG, "Created legacy backup directory: " + legacyBackupDir.getAbsolutePath());
            }
        }
    }

    /**
     * Migrate backups from the app-specific directory to the persistent directory
     */
    private void migrateOldBackups() {
        // Skip migration on Android 11+ as we'll use MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return;
        }

        if (legacyBackupDir.exists() && legacyBackupDir.isDirectory()) {
            File[] oldBackups = legacyBackupDir.listFiles((dir, name) -> name.endsWith(".db"));
            if (oldBackups != null && oldBackups.length > 0) {
                Log.i(TAG, "Found " + oldBackups.length + " backups to migrate");
                for (File oldBackup : oldBackups) {
                    File newBackup = new File(backupDir, oldBackup.getName());
                    if (!newBackup.exists()) {
                        try {
                            copyFile(oldBackup, newBackup);
                            Log.i(TAG, "Migrated backup: " + oldBackup.getName());
                            // Optionally delete the old backup after successful migration
                            oldBackup.delete();
                        } catch (IOException e) {
                            Log.e(TAG, "Error migrating backup: " + oldBackup.getName(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a backup of the database to local storage
     * 
     * @return The path to the backup file, or null if backup failed
     */
    public String createLocalBackup() {
        // Ensure directories exist before backup
        ensureBackupDirectoriesExist();

        // Create a reliable timestamp for the backup filename
        Date now = new Date();
        SimpleDateFormat filenameDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timeStamp = filenameDateFormat.format(now);

        // Log the timestamp for debugging
        Log.d(TAG, "Creating backup with timestamp: " + timeStamp);

        String backupFileName = "trees_" + timeStamp + ".db";

        // Close all database connections before backup
        closeAllDatabaseConnections();

        try {
            // Make sure there's no journal file present during backup
            File journalFile = new File(databaseJournalPath);
            if (journalFile.exists()) {
                boolean deleted = journalFile.delete();
                if (!deleted) {
                    Log.w(TAG, "Could not delete journal file before backup");
                }
            }

            File sourceDb = new File(databasePath);

            // For Android 11+, use MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return createMediaStoreBackup(sourceDb, backupFileName);
            } else {
                // For Android 10 and below, use direct file access to persistent storage
                // Make sure backup directory exists
                if (!backupDir.exists()) {
                    boolean created = backupDir.mkdirs();
                    if (!created) {
                        Log.w(TAG, "Failed to create backup directory: " + backupDir.getAbsolutePath());
                    }
                }

                File backupFile = new File(backupDir, backupFileName);

                // If we can't write to the main directory, fall back to app-specific (legacy)
                if (!backupDir.canWrite()) {
                    Log.w(TAG, "Cannot write to main backup directory, falling back to legacy location");
                    backupFile = new File(legacyBackupDir, backupFileName);

                    if (!legacyBackupDir.exists()) {
                        boolean created = legacyBackupDir.mkdirs();
                        if (!created) {
                            Log.w(TAG, "Failed to create legacy backup directory");
                        }
                    }
                }

                Log.i(TAG, "Creating backup at: " + backupFile.getAbsolutePath());
                copyFile(sourceDb, backupFile);
                Log.i(TAG, "Database backed up successfully to " + backupFile.getAbsolutePath());
                return backupFile.getAbsolutePath();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error backing up database", e);
            return null;
        } finally {
            // Re-initialize the database connection after backup
            try {
                DatabaseCreator.initialize(context);
            } catch (Exception e) {
                Log.w(TAG, "Error re-initializing database after backup", e);
            }
        }
    }

    /**
     * Create a backup using MediaStore (for Android 10+)
     */
    private String createMediaStoreBackup(File sourceDb, String fileName) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");

        // Starting with Android 10, we can specify a relative path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + File.separator + BACKUP_DIR);
        }

        Uri uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), values);
        if (uri == null) {
            throw new IOException("Failed to create MediaStore entry");
        }

        try (OutputStream os = resolver.openOutputStream(uri);
                FileInputStream fis = new FileInputStream(sourceDb)) {

            if (os == null) {
                throw new IOException("Failed to open output stream");
            }

            byte[] buffer = new byte[1024 * 8];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            Log.i(TAG, "Database backed up successfully to MediaStore: " + uri);
            return uri.toString();
        } catch (IOException e) {
            // Clean up on failure
            resolver.delete(uri, null, null);
            throw e;
        }
    }

    /**
     * Clear all tables in the database
     */
    private boolean clearAllTables(SQLiteDatabase db) {
        String[] tables = { "Tree", "TreeSpecies", "Collection", "Location", "Note" };
        db.beginTransaction();
        try {
            // First disable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=OFF");

            // Delete all data from each table
            for (String table : tables) {
                db.execSQL("DELETE FROM " + table);
            }

            // Re-enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON");

            db.setTransactionSuccessful();
            Log.i(TAG, "Successfully cleared all tables");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error clearing tables: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Restore the database from a backup file
     * 
     * @param backupPath Path or Uri string of the backup file
     * @return true if restore was successful
     */
    public boolean restoreFromLocal(String backupPath) {
        Log.d(TAG, "Restoring database from local backup: " + backupPath);
        boolean success = false;

        try {
            // Get database path
            String dbPath = context.getDatabasePath(DATABASE_NAME).getAbsolutePath();

            // First clear all tables while we have a valid database connection
            SQLiteDatabase db = DatabaseCreator.getInstance().getWritableDatabase();
            if (!clearAllTables(db)) {
                Log.e(TAG, "Failed to clear existing tables");
                return false;
            }

            // Now close all database connections
            db.close();
            DatabaseCreator.getInstance().close();
            closeAllDatabaseConnections();

            // Force resource release by Android system
            System.gc();

            // Pause briefly to allow Android to release database resources
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.w(TAG, "Sleep interrupted during database resource release");
            }

            // Check if we're dealing with a content:// URI
            if (backupPath.startsWith("content://")) {
                Log.d(TAG, "Backup is a content URI, using ContentResolver");
                File dbFile = new File(dbPath);
                success = restoreFromContentUri(Uri.parse(backupPath), dbFile);
            } else {
                // Regular file path
                File backupFile = new File(backupPath);
                File dbFile = new File(dbPath);
                try (FileInputStream input = new FileInputStream(backupFile);
                        FileOutputStream output = new FileOutputStream(dbFile)) {

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                    output.flush();
                    output.getFD().sync(); // Force write to disk

                    Log.d(TAG, "Backup file copied successfully from file path to database location");
                    success = true;
                }
            }

            if (success) {
                // Set the flag to indicate we just restored a database
                SharedPreferences prefs = context.getSharedPreferences("TreeApp_Settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("JUST_RESTORED_DATABASE", true);
                editor.putBoolean("INITIAL_IMPORT_DONE", true); // Prevent automatic imports
                editor.apply();
                Log.d(TAG, "Set JUST_RESTORED_DATABASE flag to true");

                // Reset DatabaseCreator to use the newly restored database
                try {
                    Log.d(TAG, "Resetting DatabaseCreator instance");
                    Field instance = DatabaseCreator.class.getDeclaredField("instance");
                    instance.setAccessible(true);
                    instance.set(null, null);

                    // Reinitialize database
                    DatabaseCreator.initialize(context);

                    // Verify the database was restored correctly
                    db = DatabaseCreator.getInstance().getReadableDatabase();
                    if (!validateDatabase(db)) {
                        Log.e(TAG, "Database validation failed after restore");
                        success = false;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error resetting DatabaseCreator: " + e.getMessage(), e);
                    success = false;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error during database restore: " + e.getMessage(), e);
            success = false;
        }

        Log.i(TAG, "Database restore " + (success ? "successful" : "failed"));
        return success;
    }

    /**
     * Helper method to handle restoring from a content URI
     */
    private boolean restoreFromContentUri(Uri uri, File dbFile) {
        Log.d(TAG, "Restoring from content URI: " + uri);

        try (InputStream input = context.getContentResolver().openInputStream(uri);
                FileOutputStream output = new FileOutputStream(dbFile)) {

            if (input == null) {
                Log.e(TAG, "Failed to open content URI: " + uri);
                return false;
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.flush();

            Log.d(TAG, "Backup file copied successfully from content URI to database location");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error copying from content URI: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete all database-related files
     */
    private boolean deleteDatabase(File... files) {
        boolean allDeleted = true;
        for (File file : files) {
            if (!deleteIfExists(file)) {
                allDeleted = false;
            }
        }
        return allDeleted;
    }

    /**
     * Validate the database structure to ensure it's a valid TreeApp database
     */
    private boolean validateDatabase(SQLiteDatabase db) {
        try {
            // Check if required tables exist
            String[] requiredTables = { "TreeSpecies", "Tree", "Collection", "Location", "Note" };
            for (String table : requiredTables) {
                Cursor cursor = db.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                        new String[] { table });

                boolean tableExists = cursor != null && cursor.moveToFirst();
                if (cursor != null)
                    cursor.close();

                if (!tableExists) {
                    Log.e(TAG, "Required table not found in restored database: " + table);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating database: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Ensure the TreeSpecies table is populated
     */
    private void ensureTreeSpeciesPopulated(SQLiteDatabase db) {
        int speciesCount = getTableCount(db, "TreeSpecies");
        Log.d(TAG, "TreeSpecies table has " + speciesCount + " entries");

        if (speciesCount == 0) {
            Log.w(TAG, "TreeSpecies table is empty, populating it");
            db.beginTransaction();
            try {
                TreeSpeciesBulkUploader.bulkUploadTreeSpecies(context, db);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Error populating TreeSpecies table: " + e.getMessage(), e);
            } finally {
                db.endTransaction();
            }
        }
    }

    /**
     * Get the count of rows in a table
     */
    private int getTableCount(SQLiteDatabase db, String tableName) {
        try {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            int count = 0;
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
                cursor.close();
            }
            return count;
        } catch (Exception e) {
            Log.e(TAG, "Error getting count for table " + tableName + ": " + e.getMessage(), e);
            return 0;
        }
    }

    private boolean deleteIfExists(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                Log.w(TAG, "Could not delete file: " + file.getAbsolutePath());
            } else {
                Log.d(TAG, "Successfully deleted file: " + file.getAbsolutePath());
            }
            return deleted;
        }
        return true; // File didn't exist, so technically it's deleted
    }

    private void clearTreeImportPreferences() {
        try {
            // Clear TreeImporter preferences
            SharedPreferences prefs = context.getSharedPreferences("TreeImporterPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            // Clear TreeSpecies preferences too
            SharedPreferences speciesPrefs = context.getSharedPreferences("TreeSpeciesImporterPrefs",
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor speciesEditor = speciesPrefs.edit();
            speciesEditor.clear();
            speciesEditor.apply();

            Log.d(TAG, "Cleared tree import preferences");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing tree import preferences: " + e.getMessage());
        }
    }

    /**
     * Copy data from a content:// URI to a file
     */
    private void copyFromContentUri(Uri sourceUri, File destFile) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        try (InputStream is = resolver.openInputStream(sourceUri);
                FileOutputStream fos = new FileOutputStream(destFile)) {

            if (is == null) {
                throw new IOException("Failed to open input stream for: " + sourceUri);
            }

            byte[] buffer = new byte[1024 * 8];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Close all database connections
     */
    private void closeAllDatabaseConnections() {
        try {
            // Get the database without forcing creation
            try {
                SQLiteDatabase db = DatabaseCreator.getInstance().getWritableDatabase();
                if (db != null && db.isOpen()) {
                    db.close();
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not close database through DatabaseCreator: " + e.getMessage());
                // No need to throw, we'll try other methods
            }

            // Skip the releaseMemory call as it might be causing issues with initial
            // database creation
            // We'll only use it if absolutely necessary for backup/restore operations

            // Just to be safe, let's give a moment for any pending database operations to
            // complete
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignored
            }
        } catch (Exception e) {
            Log.w(TAG, "Error closing database connections", e);
        }
    }

    /**
     * Class to hold backup information
     */
    public static class BackupFileInfo {
        private final String path; // file path or Uri string
        private final String displayName;
        private final Date dateCreated;

        public BackupFileInfo(String path, String displayName, Date dateCreated) {
            this.path = path;
            this.displayName = displayName;
            this.dateCreated = dateCreated;
        }

        public String getPath() {
            return path;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Date getDateCreated() {
            return dateCreated;
        }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return "Backup: " + sdf.format(dateCreated);
        }
    }

    /**
     * List all available local backups
     * 
     * @return List of backup file information
     */
    public List<BackupFileInfo> getAllBackups() {
        // Ensure directories exist before listing backups
        ensureBackupDirectoriesExist();

        List<BackupFileInfo> results = new ArrayList<>();

        // First try to get backups from MediaStore (for Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            results.addAll(getMediaStoreBackups());
        }

        // For pre-Android 11, also check the filesystem
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Check the public directory first
            if (backupDir.exists() && backupDir.isDirectory()) {
                File[] backups = backupDir.listFiles((dir, name) -> name.endsWith(".db"));
                if (backups != null) {
                    for (File backup : backups) {
                        // Try to get a valid date
                        Date fileDate = new Date(); // Default to now
                        long lastModified = backup.lastModified();

                        if (lastModified > 0) {
                            fileDate = new Date(lastModified);
                            Log.d(TAG, "Using lastModified for " + backup.getName() + ": " + fileDate);
                        } else {
                            // Try to parse date from filename if it contains a timestamp
                            String name = backup.getName();
                            if (name.contains("_")) {
                                try {
                                    String dateString = name.split("_")[0];
                                    SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                                    fileDate = parser.parse(dateString);
                                    Log.d(TAG, "Using parsed date from filename for " + name + ": " + fileDate);
                                } catch (Exception e) {
                                    Log.w(TAG, "Could not parse date from filename: " + name);
                                }
                            }
                        }

                        results.add(new BackupFileInfo(
                                backup.getAbsolutePath(),
                                backup.getName(),
                                fileDate));
                    }
                }
            }

            // Check if we need to migrate old backups
            boolean shouldMigrate = false;

            // Also check the legacy directory
            if (legacyBackupDir.exists() && legacyBackupDir.isDirectory()) {
                File[] backups = legacyBackupDir.listFiles((dir, name) -> name.endsWith(".db"));
                if (backups != null) {
                    if (backups.length > 0 && results.isEmpty()) {
                        // We have legacy backups but no backups in the new location
                        shouldMigrate = true;
                    }

                    for (File backup : backups) {
                        // Try to get a valid date
                        Date fileDate = new Date(); // Default to now
                        long lastModified = backup.lastModified();

                        if (lastModified > 0) {
                            fileDate = new Date(lastModified);
                            Log.d(TAG, "Using lastModified for " + backup.getName() + ": " + fileDate);
                        } else {
                            // Try to parse date from filename if it contains a timestamp
                            String name = backup.getName();
                            if (name.contains("_")) {
                                try {
                                    String dateString = name.split("_")[0];
                                    SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                                    fileDate = parser.parse(dateString);
                                    Log.d(TAG, "Using parsed date from filename for " + name + ": " + fileDate);
                                } catch (Exception e) {
                                    Log.w(TAG, "Could not parse date from filename: " + name);
                                }
                            }
                        }

                        results.add(new BackupFileInfo(
                                backup.getAbsolutePath(),
                                backup.getName(),
                                fileDate));
                    }
                }
            }

            // Only migrate if we found old backups but no new ones
            if (shouldMigrate) {
                migrateOldBackups();
            }
        }

        return results;
    }

    /**
     * Get backups from MediaStore (Android 10+)
     */
    private List<BackupFileInfo> getMediaStoreBackups() {
        List<BackupFileInfo> results = new ArrayList<>();

        ContentResolver resolver = context.getContentResolver();
        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.DATE_ADDED // Also get DATE_ADDED as a fallback
        };

        String selection;
        String[] selectionArgs;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " LIKE ? AND " +
                    MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?";
            selectionArgs = new String[] {
                    "%" + BACKUP_DIR + "%",
                    "%.db"
            };
        } else {
            selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?";
            selectionArgs = new String[] { "%.db" };
        }

        Uri queryUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);

        try (Cursor cursor = resolver.query(queryUri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
                int dateAddedColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED); // This might not
                                                                                                      // exist in all
                                                                                                      // Android
                                                                                                      // versions

                do {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);

                    // Try to get a valid date
                    Date fileDate = new Date(); // Default to current date if none available

                    try {
                        long dateModified = cursor.getLong(dateModifiedColumn);
                        if (dateModified > 0) {
                            // DATE_MODIFIED is in seconds, convert to milliseconds
                            fileDate = new Date(dateModified * 1000);
                            Log.d(TAG, "Using DATE_MODIFIED for " + name + ": " + fileDate);
                        } else if (dateAddedColumn != -1) {
                            // Try DATE_ADDED as fallback
                            long dateAdded = cursor.getLong(dateAddedColumn);
                            if (dateAdded > 0) {
                                fileDate = new Date(dateAdded * 1000);
                                Log.d(TAG, "Using DATE_ADDED for " + name + ": " + fileDate);
                            }
                        } else {
                            // Try to parse date from filename if it contains a timestamp
                            if (name.contains("_")) {
                                try {
                                    String dateString = name.split("_")[0];
                                    SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                                    fileDate = parser.parse(dateString);
                                    Log.d(TAG, "Using parsed date from filename for " + name + ": " + fileDate);
                                } catch (Exception e) {
                                    Log.w(TAG, "Could not parse date from filename: " + name);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error retrieving date for " + name + ": " + e.getMessage());
                    }

                    Uri uri = Uri.withAppendedPath(queryUri, String.valueOf(id));
                    results.add(new BackupFileInfo(
                            uri.toString(),
                            name,
                            fileDate));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore for backups", e);
        }

        return results;
    }

    /**
     * List all available local backups (legacy method)
     * 
     * @return Array of backup file paths
     */
    public File[] listLocalBackups() {
        // Ensure directories exist
        ensureBackupDirectoriesExist();

        // For Android 11+, convert our new method to the old format
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            List<BackupFileInfo> backupInfos = getAllBackups();
            List<File> fileList = new ArrayList<>();

            for (BackupFileInfo info : backupInfos) {
                String path = info.getPath();
                if (!path.startsWith("content://")) {
                    fileList.add(new File(path));
                } else {
                    // For content:// URIs, create a dummy file with the URI as the path
                    // This is a bit of a hack but necessary for backward compatibility
                    File tempDir = context.getCacheDir();
                    File tempFile = new File(tempDir, info.getDisplayName());
                    try {
                        copyFromContentUri(Uri.parse(path), tempFile);
                        fileList.add(tempFile);
                    } catch (IOException e) {
                        Log.e(TAG, "Error copying from content URI", e);
                    }
                }
            }

            return fileList.toArray(new File[0]);
        }

        // For older Android versions, use direct file access
        File[] backups = backupDir.listFiles((dir, name) -> name.endsWith(".db"));

        // If we don't find any backups in the new location, check the old location
        if ((backups == null || backups.length == 0) && !backupDir.equals(legacyBackupDir)) {
            File[] legacyBackups = legacyBackupDir.listFiles((dir, name) -> name.endsWith(".db"));
            if (legacyBackups != null && legacyBackups.length > 0) {
                return legacyBackups; // Just return the legacy backups directly
            }
        }

        return backups;
    }

    /**
     * Delete a specific backup file
     * 
     * @param backupPath Path to the backup file to delete
     * @return true if deletion was successful
     */
    public boolean deleteBackup(String backupPath) {
        if (backupPath.startsWith("content://")) {
            // Delete from MediaStore
            try {
                ContentResolver resolver = context.getContentResolver();
                int deleted = resolver.delete(Uri.parse(backupPath), null, null);
                return deleted > 0;
            } catch (Exception e) {
                Log.e(TAG, "Error deleting MediaStore backup", e);
                return false;
            }
        } else {
            // Delete regular file
            File backupFile = new File(backupPath);
            boolean deleted = backupFile.delete();
            if (deleted) {
                Log.i(TAG, "Backup deleted: " + backupPath);
            } else {
                Log.e(TAG, "Failed to delete backup: " + backupPath);
            }
            return deleted;
        }
    }

    /**
     * Copy file from source to destination
     */
    private void copyFile(File src, File dst) throws IOException {
        try (FileChannel inChannel = new FileInputStream(src).getChannel();
                FileChannel outChannel = new FileOutputStream(dst).getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
            outChannel.force(true); // Ensure all data is flushed to disk
        }
    }

    /**
     * Get statistics from a backup file without restoring it
     * 
     * @param backupPath Path or Uri string of the backup file
     * @return String containing stats, or an error message
     */
    public String getBackupStats(String backupPath) {
        File tempFile = null;
        SQLiteDatabase db = null;
        try {
            if (backupPath.startsWith("content://")) {
                tempFile = new File(context.getCacheDir(), "temp_stats.db");
                copyFromContentUri(Uri.parse(backupPath), tempFile);
            } else {
                tempFile = new File(backupPath);
            }

            if (!tempFile.exists() || !tempFile.canRead()) {
                return "Error: Cannot read backup file.";
            }

            // Open the database in read-only mode
            db = SQLiteDatabase.openDatabase(tempFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

            // Check if it's a valid database by validating structure
            if (!validateDatabase(db)) {
                return "Error: Unsupported or corrupt database format.";
            }

            int treeCount = getTableCount(db, "Tree");
            int collectionCount = getTableCount(db, "Collection");
            int noteCount = getTableCount(db, "Note");
            int locationCount = getTableCount(db, "Location");
            int scionCount = getTableCount(db, "Scion");
            int reminderCount = getTableCount(db, "Reminder");
            int imageCount = getTableCount(db, "Image");

            return String.format(Locale.getDefault(),
                    "Backup Content:\n\n" +
                            "• Trees: %d\n" +
                            "• Collections: %d\n" +
                            "• Notes: %d\n" +
                            "• GPS Points: %d\n" +
                            "• Scions: %d\n" +
                            "• Reminders: %d\n" +
                            "• Images Linked: %d",
                    treeCount, collectionCount, noteCount, locationCount,
                    scionCount, reminderCount, imageCount);

        } catch (Exception e) {
            Log.e(TAG, "Error getting backup stats", e);
            return "Error: Failed to read backup data (" + e.getMessage() + ")";
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
            // If we created a temp file in cache, delete it
            if (backupPath.startsWith("content://") && tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Get a File object for a backup path, copying to cache if it's a content URI
     * Useful for sharing.
     */
    public File getBackupFileForSharing(String backupPath) throws IOException {
        if (backupPath.startsWith("content://")) {
            // Get the original filename from MediaStore if possible
            String displayName = "backup.db";
            try (Cursor cursor = context.getContentResolver().query(Uri.parse(backupPath),
                    new String[] { MediaStore.MediaColumns.DISPLAY_NAME }, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    displayName = cursor.getString(0);
                }
            }

            File tempFile = new File(context.getCacheDir(), displayName);
            copyFromContentUri(Uri.parse(backupPath), tempFile);
            return tempFile;
        } else {
            return new File(backupPath);
        }
    }

    /**
     * Export all data from a backup to a human-readable ZIP file containing CSVs
     * 
     * @param backupPath Path or Uri of the backup .db file
     * @return File object pointing to the generated ZIP in cache
     */
    public File exportBackupToZip(String backupPath) throws Exception {
        File databaseFile = null;
        SQLiteDatabase db = null;
        File zipFile = new File(context.getCacheDir(), "TreeApp_Data_Export.zip");

        try {
            if (backupPath.startsWith("content://")) {
                databaseFile = new File(context.getCacheDir(), "temp_export.db");
                copyFromContentUri(Uri.parse(backupPath), databaseFile);
            } else {
                databaseFile = new File(backupPath);
            }

            db = SQLiteDatabase.openDatabase(databaseFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 1. Planted Trees CSV
                addToZip(zos, "Planted_Trees.csv", queryToCsv(db,
                        "SELECT t.treeId, t.label, t.latinName, t.variety, t.rootstock, t.datePlanted, t.origin, " +
                                "c.name as collection, l.latitude, l.longitude " +
                                "FROM Tree t " +
                                "LEFT JOIN Collection c ON t.collectionId = c.id " +
                                "LEFT JOIN Location l ON t.locationId = l.locationId"));

                // 2. Scions CSV (shows which tree each scion is on, if any)
                addToZip(zos, "Scions.csv", queryToCsv(db,
                        "SELECT s.species, s.variety, s.source, s.attached, t.label as attached_to_tree " +
                                "FROM Scion s " +
                                "LEFT JOIN TreeScion ts ON s.scionId = ts.scionId " +
                                "LEFT JOIN Tree t ON ts.treeId = t.treeId"));

                // 3. Notes CSV
                addToZip(zos, "Notes.csv", queryToCsv(db,
                        "SELECT n.dateWritten, n.description, t.label as tree_label " +
                                "FROM Note n " +
                                "JOIN Tree t ON n.treeId = t.treeId"));

                // 4. Reminders CSV
                addToZip(zos, "Reminders.csv", queryToCsv(db,
                        "SELECT r.reminderDate, r.description, t.label as tree_label " +
                                "FROM Reminder r " +
                                "JOIN Tree t ON r.treeId = t.treeId"));

                // 5. Images Index CSV
                addToZip(zos, "Images_Index.csv", queryToCsv(db,
                        "SELECT i.imagePath, i.dateAdded, t.label as tree_label " +
                                "FROM Image i " +
                                "JOIN Tree t ON i.treeId = t.treeId"));

                zos.finish();
            }

            return zipFile;

        } finally {
            if (db != null && db.isOpen())
                db.close();
            if (backupPath.startsWith("content://") && databaseFile != null && databaseFile.exists()) {
                databaseFile.delete();
            }
        }
    }

    /**
     * Export database and all tree photos to a single ZIP file for website generation
     * 
     * @param backupPath Path or Uri of the backup .db file
     * @return File object pointing to the generated website package ZIP in cache
     */
    public File exportWebsiteZip(String backupPath) throws Exception {
        File databaseFile = null;
        File zipFile = new File(context.getCacheDir(), "TreeApp_Website_Export.zip");
        if (zipFile.exists()) {
            zipFile.delete();
        }

        try {
            if (backupPath.startsWith("content://")) {
                databaseFile = new File(context.getCacheDir(), "temp_web_export.db");
                copyFromContentUri(Uri.parse(backupPath), databaseFile);
            } else {
                databaseFile = new File(backupPath);
            }

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 1. Add database file as "database.db"
                if (databaseFile.exists() && databaseFile.canRead()) {
                    addFileToZip(zos, databaseFile, "database.db");
                    Log.i(TAG, "Added database to website export ZIP");
                }

                // 2. Add all tree images from Pictures/Trees
                File photosDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Trees");
                if (photosDir.exists() && photosDir.isDirectory()) {
                    File[] photos = photosDir.listFiles((dir, name) -> {
                        String lower = name.toLowerCase(Locale.ROOT);
                        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
                    });

                    if (photos != null) {
                        for (File photo : photos) {
                            addFileToZip(zos, photo, "images/" + photo.getName());
                        }
                        Log.i(TAG, "Added " + photos.length + " photos to website export ZIP");
                    }
                }

                zos.finish();
            }

            return zipFile;
        } finally {
            if (backupPath.startsWith("content://") && databaseFile != null && databaseFile.exists()) {
                databaseFile.delete();
            }
        }
    }

    private void addFileToZip(ZipOutputStream zos, File file, String zipEntryName) throws IOException {
        ZipEntry entry = new ZipEntry(zipEntryName);
        zos.putNextEntry(entry);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
    }

    private void addToZip(ZipOutputStream zos, String filename, String content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    private String queryToCsv(SQLiteDatabase db, String query) {
        StringBuilder csv = new StringBuilder();
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                // Headers
                String[] columnNames = cursor.getColumnNames();
                for (int i = 0; i < columnNames.length; i++) {
                    csv.append(escapeCsv(columnNames[i]));
                    if (i < columnNames.length - 1)
                        csv.append(",");
                }
                csv.append("\n");

                // Data
                do {
                    for (int i = 0; i < columnNames.length; i++) {
                        csv.append(escapeCsv(cursor.getString(i)));
                        if (i < columnNames.length - 1)
                            csv.append(",");
                    }
                    csv.append("\n");
                } while (cursor.moveToNext());
            } else {
                csv.append("No data found for this table.\n");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating CSV", e);
            csv.append("Error generating data: ").append(e.getMessage());
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}