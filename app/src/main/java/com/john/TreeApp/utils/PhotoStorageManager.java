package com.john.TreeApp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PhotoStorageManager {
    private static final String TAG = "PhotoStorageManager";
    public static final String TREES_FOLDER_NAME = "Trees";

    /**
     * Public Pictures/Trees directory (/storage/emulated/0/Pictures/Trees/)
     */
    public static File getPublicTreesDir() {
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                TREES_FOLDER_NAME
        );
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Legacy app-specific private directory (/storage/emulated/0/Android/data/com.john.TreeApp/files/Pictures/Trees/)
     */
    public static File getPrivateTreesDir(Context context) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), TREES_FOLDER_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Resolves an image file name or path safely:
     * 1. Checks if it's already an existing absolute file.
     * 2. Checks the public Pictures/Trees directory.
     * 3. Fallback: checks legacy app-specific private directory so old photos are NEVER lost.
     */
    public static File resolveImageFile(Context context, String storedPathOrName) {
        if (storedPathOrName == null || storedPathOrName.trim().isEmpty()) {
            return null;
        }

        File rawFile = new File(storedPathOrName);
        if (rawFile.isAbsolute() && rawFile.exists()) {
            return rawFile;
        }

        String fileName = rawFile.getName();

        // 1. Primary target: Public Pictures/Trees
        File publicDir = getPublicTreesDir();
        File publicFile = new File(publicDir, fileName);
        if (publicFile.exists()) {
            return publicFile;
        }

        // 2. Fallback: legacy app-specific private directory
        File privateDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), TREES_FOLDER_NAME);
        File appSpecificFile = new File(privateDir, fileName);
        if (appSpecificFile.exists()) {
            return appSpecificFile;
        }

        return publicFile;
    }

    /**
     * Copies or moves a photo file to the public Pictures/Trees directory.
     * Safely verifies the destination copy before deleting the source.
     *
     * @param context Application context
     * @param sourceFile Source image file
     * @param deleteSource If true, deletes sourceFile ONLY after copy succeeds
     * @return Filename in public storage, or null if failed
     */
    public static String movePhotoToPublicPictures(Context context, File sourceFile, boolean deleteSource) {
        if (sourceFile == null || !sourceFile.exists() || sourceFile.length() == 0) {
            return null;
        }

        String fileName = sourceFile.getName();
        File publicTreesDir = getPublicTreesDir();
        File targetPublicFile = new File(publicTreesDir, fileName);

        // If target file already exists in public storage with valid content
        if (targetPublicFile.exists() && targetPublicFile.length() > 0) {
            if (deleteSource && !sourceFile.getAbsolutePath().equals(targetPublicFile.getAbsolutePath())) {
                sourceFile.delete();
            }
            MediaScannerConnection.scanFile(
                    context.getApplicationContext(),
                    new String[]{ targetPublicFile.getAbsolutePath() },
                    new String[]{ "image/jpeg" },
                    null
            );
            return fileName;
        }

        boolean copied = false;

        // Method 1: Direct File copy (works with MANAGE_EXTERNAL_STORAGE or direct filesystem access)
        try {
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(targetPublicFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }
            if (targetPublicFile.exists() && targetPublicFile.length() > 0) {
                copied = true;
                Log.i(TAG, "Copied photo to public storage via direct File API: " + targetPublicFile.getAbsolutePath());
            }
        } catch (Exception directEx) {
            Log.w(TAG, "Direct file copy failed (" + directEx.getMessage() + "), falling back to MediaStore API...");
            if (targetPublicFile.exists() && targetPublicFile.length() == 0) {
                targetPublicFile.delete();
            }
        }

        // Method 2: Fallback to MediaStore API (Scoped Storage compliant)
        if (!copied) {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + TREES_FOLDER_NAME + "/");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.Images.Media.IS_PENDING, 1);
                }

                Uri publicUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (publicUri == null) {
                    Log.e(TAG, "Failed to create MediaStore entry for " + fileName);
                    return null;
                }

                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = context.getContentResolver().openOutputStream(publicUri)) {
                    if (out == null) {
                        context.getContentResolver().delete(publicUri, null, null);
                        return null;
                    }

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    context.getContentResolver().update(publicUri, values, null, null);
                }

                try (Cursor cursor = context.getContentResolver().query(
                        publicUri,
                        new String[]{ MediaStore.Images.Media.DISPLAY_NAME },
                        null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                        if (nameIdx != -1) {
                            fileName = cursor.getString(nameIdx);
                            targetPublicFile = new File(publicTreesDir, fileName);
                        }
                    }
                }

                copied = true;
                Log.i(TAG, "Copied photo to public storage via MediaStore: " + publicUri);
            } catch (Exception mediaEx) {
                Log.e(TAG, "MediaStore insertion failed", mediaEx);
                return null;
            }
        }

        if (copied) {
            if (deleteSource && !sourceFile.getAbsolutePath().equals(targetPublicFile.getAbsolutePath())) {
                boolean deleted = sourceFile.delete();
                Log.i(TAG, "Deleted source file after successful copy (deleted=" + deleted + "): " + sourceFile.getAbsolutePath());
            }

            MediaScannerConnection.scanFile(
                    context.getApplicationContext(),
                    new String[]{ targetPublicFile.getAbsolutePath() },
                    new String[]{ "image/jpeg" },
                    (path, uri) -> Log.i(TAG, "MediaScanner indexed: " + path + " -> " + uri)
            );

            return fileName;
        }

        return null;
    }

    /**
     * Safely migrates all legacy photos from the app-specific private directory
     * (/Android/data/com.john.TreeApp/files/Pictures/Trees/) to the public Pictures/Trees/ directory.
     *
     * @param context Application context
     * @return Number of photos successfully migrated
     */
    public static int migratePrivatePhotosToPublic(Context context) {
        int migratedCount = 0;
        try {
            File privateDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), TREES_FOLDER_NAME);
            if (!privateDir.exists() || !privateDir.isDirectory()) {
                return 0;
            }

            File[] files = privateDir.listFiles();
            if (files == null || files.length == 0) {
                return 0;
            }

            for (File file : files) {
                if (file.isFile() && !file.getName().startsWith(".")) {
                    String saved = movePhotoToPublicPictures(context, file, true);
                    if (saved != null) {
                        migratedCount++;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during migration of private photos: " + e.getMessage(), e);
        }
        return migratedCount;
    }
}
