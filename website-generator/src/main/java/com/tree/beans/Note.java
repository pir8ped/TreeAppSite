package com.tree.beans;

import java.util.Date;

public class Note {
    private int noteId;
    private int treeId;
    private Date dateWritten;
    private String description;
    private Integer imageId;

    public Note() {}

    public Note(int noteId, int treeId, Date dateWritten, String description, Integer imageId) {
        this.noteId = noteId;
        this.treeId = treeId;
        this.dateWritten = dateWritten;
        this.description = description;
        this.imageId = imageId;
    }

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public int getTreeId() {
        return treeId;
    }

    public void setTreeId(int treeId) {
        this.treeId = treeId;
    }

    public Date getDateWritten() {
        return dateWritten;
    }

    public void setDateWritten(Date dateWritten) {
        this.dateWritten = dateWritten;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }
}
