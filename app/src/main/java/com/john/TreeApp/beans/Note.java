package com.john.TreeApp.beans;

import java.util.Date;

    public class Note {
        private int noteID;
        private int treeId;
        private Date dateWritten;
        private String description;
        private Integer imageId; // Can be null

        // Constructor
        public Note(int noteID, int treeId, Date dateWritten, String description, Integer imageId) {
            this.noteID = noteID;
            this.treeId = treeId;
            this.dateWritten = dateWritten;
            this.description = description;
            this.imageId = imageId;
        }

        public Note(int noteID, int treeId, Date dateWritten, String description) {
            this(noteID, treeId, dateWritten, description, null);
        }

        public Note(int treeId, Date dateWritten, String description, Integer imageId) {
            this.treeId = treeId;
            this.dateWritten = dateWritten;
            this.description = description;
            this.imageId = imageId;
        }

        public Note(int treeId, Date dateWritten, String description) {
            this(treeId, dateWritten, description, null);
        }

        // Default Constructor
        public Note() {
        }

        // Getters and Setters
        public int getNoteID() {
            return noteID;
        }

        public void setNoteID(int noteID) {
            this.noteID = noteID;
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

        // Optionally, override toString, equals, and hashCode methods
        @Override
        public String toString() {
            return "Note{" +
                    "noteID=" + noteID +
                    ", treeId=" + treeId +
                    ", dateWritten=" + dateWritten +
                    ", description='" + description + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Note note = (Note) o;

            return noteID == note.noteID;
        }

        @Override
        public int hashCode() {
            return (int) (noteID ^ (noteID >>> 32));
        }
    }


