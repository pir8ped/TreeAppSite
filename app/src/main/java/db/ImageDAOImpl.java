package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.john.TreeApp.beans.Image;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class ImageDAOImpl extends DAOBase implements ImageDAO {
    private static final String TAG = "ImageDAOImpl";
    private static final String TABLE_IMAGE = "Image";

    public ImageDAOImpl() {
        super();
    }

    @Override
    public List<Image> getAllImages(long treeId) {
        String selection = "treeId = ?";
        String[] selectionArgs = { String.valueOf(treeId) };
        return queryImages(selection, selectionArgs);
    }

    @Override
    public void addImage(long treeId, Date dateTaken, String imageUrlOrFileName) {
        ContentValues values = new ContentValues();
        values.put("treeId", treeId);
        values.put("dateAdded", dateTaken != null ? dateTaken.toString() : null);
        values.put("imagePath", imageUrlOrFileName);

        executeInsert(TABLE_IMAGE, values);
    }

    @Override
    public void deleteImage(int imageID) {
        // Delete any associated notes (captions) first
        try {
            getDatabase().delete("Note", "imageId = ?", new String[]{String.valueOf(imageID)});
            Log.d(TAG, "Associated notes for image " + imageID + " deleted");
        } catch (Exception e) {
            Log.e(TAG, "Error deleting associated notes for image " + imageID + ": " + e.getMessage());
        }

        String whereClause = "imageId = ?";
        String[] whereArgs = { String.valueOf(imageID) };
        executeDelete(TABLE_IMAGE, whereClause, whereArgs);
    }

    private List<Image> queryImages(String selection, String[] selectionArgs) {
        List<Image> images = new ArrayList<>();
        Cursor cursor = null;

        try {
            String[] projection = { "imageId", "treeId", "dateAdded", "imagePath" };
            cursor = getDatabase().query(TABLE_IMAGE, projection, selection, selectionArgs, null, null, "dateAdded DESC");

            int indexImageID = cursor.getColumnIndexOrThrow("imageId");
            int indexTreeId = cursor.getColumnIndexOrThrow("treeId");
            int indexDateTaken = cursor.getColumnIndexOrThrow("dateAdded");
            int indexImagePath = cursor.getColumnIndexOrThrow("imagePath");

            while (cursor.moveToNext()) {
                int imageID = cursor.getInt(indexImageID);
                int treeId = cursor.getInt(indexTreeId);
                String dateTakenStr = cursor.getString(indexDateTaken);
                Date dateTaken = dateTakenStr != null ? Date.valueOf(dateTakenStr) : null;
                String imagePath = cursor.getString(indexImagePath);

                Image image = new Image(imageID, treeId, dateTaken, imagePath);
                images.add(image);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying images: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return images;
    }

    private void executeInsert(String tableName, ContentValues values) {
        try {
            long newRowId = getDatabase().insert(tableName, null, values);
            if (newRowId == -1) {
                Log.e(TAG, "Failed to add image");
            } else {
                Log.d(TAG, "Image added successfully with ID: " + newRowId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeDelete(String tableName, String whereClause, String[] whereArgs) {
        try {
            int rowsAffected = getDatabase().delete(tableName, whereClause, whereArgs);
            if (rowsAffected > 0) {
                Log.d(TAG, "Image deleted successfully");
            } else {
                Log.d(TAG, "No image found with the given ID");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting image: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
