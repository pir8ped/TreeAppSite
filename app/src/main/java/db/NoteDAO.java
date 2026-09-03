package db;

import com.john.TreeApp.beans.Note;
import com.john.TreeApp.beans.NoteSearchResult;
import java.util.List;

public interface NoteDAO {
    List<Note> getNotes(int treeId);

    void deleteNote(int noteID);

    void addNote(Note note);

    void editNote(int noteID, String newText);

    void editNote(int noteID, String newText, java.util.Date newDate);

    Note getNoteForImage(int imageId);

    List<NoteSearchResult> searchNotes(String searchTerm);
}
