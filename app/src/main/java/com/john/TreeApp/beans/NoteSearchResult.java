package com.john.TreeApp.beans;

public class NoteSearchResult {
    private int treeId;
    private int collectionId;
    private String englishName;
    private String latinName;
    private String label;
    private String noteFragment;
    private int noteId;
    private int startIndex;
    private int endIndex;

    public NoteSearchResult(int treeId, int collectionId, String englishName, String latinName, String label,
            String noteFragment, int noteId, int startIndex, int endIndex) {
        this.treeId = treeId;
        this.collectionId = collectionId;
        this.englishName = englishName;
        this.latinName = latinName;
        this.label = label;
        this.noteFragment = noteFragment;
        this.noteId = noteId;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public int getTreeId() {
        return treeId;
    }

    public int getCollectionId() {
        return collectionId;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getLatinName() {
        return latinName;
    }

    public String getLabel() {
        return label;
    }

    public String getNoteFragment() {
        return noteFragment;
    }

    public int getNoteId() {
        return noteId;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getFullName() {
        return englishName + " (" + latinName + ")";
    }
}