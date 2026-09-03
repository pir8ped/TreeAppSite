package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.john.TreeApp.beans.Note;
import com.john.TreeApp.beans.NoteSearchResult;
import com.john.TreeApp.beans.Tree;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NoteDAOImpl extends DAOBase implements NoteDAO {
    private static final String TAG = "NoteDAOImpl";
    private TreeDAO treeDAO;

    public NoteDAOImpl() {
        super();
        treeDAO = new TreeDAOImpl();
    }

    @Override
    public List<NoteSearchResult> searchNotes(String searchTerm) {
        List<NoteSearchResult> results = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Join Note table with Tree and TreeSpecies tables to get tree information
            String query = "SELECT n.noteID, n.treeId, n.dateWritten, n.description, " +
                    "COALESCE(ts.englishName, '') as englishName, " +
                    "COALESCE(t.latinName, '') as latinName, " +
                    "COALESCE(t.label, '') as label, " +
                    "t.collectionId " +
                    "FROM Note n " +
                    "LEFT JOIN Tree t ON n.treeId = t.treeId " +
                    "LEFT JOIN TreeSpecies ts ON t.latinName = ts.latinName " +
                    "WHERE LOWER(n.description) LIKE LOWER(?)";

            // Use a simple pattern that just matches the word anywhere
            String pattern = "%" + searchTerm + "%";
            String[] selectionArgs = new String[] { pattern };

            Log.d(TAG, "Search term: " + searchTerm);
            Log.d(TAG, "Search pattern: " + pattern);
            Log.d(TAG, "SQL Query: " + query);

            cursor = getDatabase().rawQuery(query, selectionArgs);
            Log.d(TAG, "Number of results found: " + cursor.getCount());

            // Process all results from the search cursor
            while (cursor.moveToNext()) {
                Log.d(TAG, "Processing cursor row at position: " + cursor.getPosition());
                int noteId = cursor.getInt(cursor.getColumnIndexOrThrow("noteID"));
                int treeId = cursor.getInt(cursor.getColumnIndexOrThrow("treeId"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                String englishName = cursor.getString(cursor.getColumnIndexOrThrow("englishName"));
                String latinName = cursor.getString(cursor.getColumnIndexOrThrow("latinName"));
                String label = cursor.getString(cursor.getColumnIndexOrThrow("label"));
                int collectionId = cursor.getInt(cursor.getColumnIndexOrThrow("collectionId"));

                Log.d(TAG, "Found note: ID=" + noteId + ", Tree=" + treeId +
                        ", Description=" + description);

                // Find the position of the search term in the description
                int startIndex = description.toLowerCase().indexOf(searchTerm.toLowerCase());
                Log.d(TAG, "Search term position: " + startIndex);

                // Get a fragment of the text around the search term
                int fragmentStart = Math.max(0, startIndex - 50);
                int fragmentEnd = Math.min(description.length(), startIndex + searchTerm.length() + 50);
                String fragment = description.substring(fragmentStart, fragmentEnd);

                // Add ellipsis if we're not showing the full text
                if (fragmentStart > 0)
                    fragment = "..." + fragment;
                if (fragmentEnd < description.length())
                    fragment = fragment + "...";

                Log.d(TAG, "Created fragment: " + fragment);

                NoteSearchResult result = new NoteSearchResult(
                        treeId,
                        collectionId,
                        englishName,
                        latinName,
                        label,
                        fragment,
                        noteId,
                        startIndex,
                        startIndex + searchTerm.length());

                results.add(result);
                Log.d(TAG, "Added result to list. Total results so far: " + results.size());
            }
            Log.d(TAG, "Finished processing cursor. Total results: " + results.size());
        } catch (Exception e) {
            Log.e(TAG, "Error searching notes", e);
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return results;
    }

    @Override
    public List<Note> getNotes(int treeId) {
        List<Note> notes = new ArrayList<>();
        Cursor cursor = null;

        String[] projection = {
                "noteID",
                "treeId",
                "dateWritten",
                "description",
                "imageId"
        };

        String selection = "treeId = ? AND imageId IS NULL";
        String[] selectionArgs = { String.valueOf(treeId) };

        try {
            cursor = getDatabase().query(
                    "Note", // Table name
                    projection, // Columns to return
                    selection, // WHERE clause
                    selectionArgs, // WHERE clause arguments
                    null, // Group by
                    null, // Having
                    "dateWritten DESC, noteID DESC" // Order by
            );

            while (cursor.moveToNext()) {
                notes.add(cursorToNote(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "getNotes for treeId " + treeId + " returned " + notes.size() + " items");
        return notes;
    }

    @Override
    public void deleteNote(int noteID) {
        try {
            String whereClause = "noteID = ?";
            String[] whereArgs = { String.valueOf(noteID) };

            int rowsAffected = getDatabase().delete("Note", whereClause, whereArgs);

            if (rowsAffected > 0) {
                Log.d("NoteDAOImpl", "Note with ID " + noteID + " successfully deleted.");
            } else {
                Log.d("NoteDAOImpl", "Note with ID " + noteID + " not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("NoteDAOImpl", "Error deleting note with ID " + noteID, e);
        }
    }

    @Override
    public void addNote(Note note) {
        try {
            ContentValues values = new ContentValues();
            values.put("treeId", note.getTreeId());
            values.put("dateWritten", note.getDateWritten() != null ? note.getDateWritten().getTime() : null);
            values.put("description", note.getDescription());
            values.put("imageId", note.getImageId());

            long newRowId = getDatabase().insert("Note", null, values);

            if (newRowId != -1) {
                note.setNoteID((int) newRowId);
                Log.d("NoteDAOImpl", "Note successfully added with ID: " + newRowId);
            } else {
                Log.d("NoteDAOImpl", "Failed to add note.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("NoteDAOImpl", "Error adding note", e);
        }
    }

    @Override
    public void editNote(int noteID, String newText) {
        try {
            ContentValues values = new ContentValues();
            values.put("description", newText);

            String whereClause = "noteID = ?";
            String[] whereArgs = { String.valueOf(noteID) };

            int rowsAffected = getDatabase().update("Note", values, whereClause, whereArgs);

            if (rowsAffected > 0) {
                Log.d("NoteDAOImpl", "Note with ID " + noteID + " successfully updated.");
            } else {
                Log.d("NoteDAOImpl", "Note with ID " + noteID + " not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("NoteDAOImpl", "Error updating note with ID " + noteID, e);
        }
    }

    @Override
    public void editNote(int noteID, String newText, java.util.Date newDate) {
        try {
            ContentValues values = new ContentValues();
            values.put("description", newText);
            values.put("dateWritten", newDate != null ? newDate.getTime() : null);

            String whereClause = "noteID = ?";
            String[] whereArgs = { String.valueOf(noteID) };

            int rowsAffected = getDatabase().update("Note", values, whereClause, whereArgs);

            if (rowsAffected > 0) {
                Log.d("NoteDAOImpl", "Note with ID " + noteID + " successfully updated with new date.");
            } else {
                Log.d("NoteDAOImpl", "Note with ID " + noteID + " not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("NoteDAOImpl", "Error updating note with ID " + noteID, e);
        }
    }

    @Override
    public Note getNoteForImage(int imageId) {
        Cursor cursor = null;
        try {
            String[] projection = { "noteID", "treeId", "dateWritten", "description", "imageId" };
            String selection = "imageId = ?";
            String[] selectionArgs = { String.valueOf(imageId) };
            
            cursor = getDatabase().query("Note", projection, selection, selectionArgs, null, null, null);
            
            if (cursor.moveToFirst()) {
                return cursorToNote(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    private Note cursorToNote(Cursor cursor) {
        int noteID = cursor.getInt(cursor.getColumnIndexOrThrow("noteID"));
        int treeId = cursor.getInt(cursor.getColumnIndexOrThrow("treeId"));
        long dateWrittenMillis = cursor.getLong(cursor.getColumnIndexOrThrow("dateWritten"));
        Date dateWritten = new Date(dateWrittenMillis);
        String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        int imageIdIndex = cursor.getColumnIndex("imageId");
        Integer imageId = null;
        if (imageIdIndex != -1 && !cursor.isNull(imageIdIndex)) {
            imageId = cursor.getInt(imageIdIndex);
        }

        return new Note(noteID, treeId, dateWritten, description, imageId);
    }
}
