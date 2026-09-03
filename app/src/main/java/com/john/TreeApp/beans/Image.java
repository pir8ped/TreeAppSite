package com.john.TreeApp.beans;

import java.util.Date;

public class Image {
    private int imageId;
    private int treeId;
    private Date dateTaken;
    private String imageUrlOrFileName;

    // Constructor
    public Image(int imageId, int treeId, Date dateTaken, String imageUrlOrFileName) {
        this.imageId = imageId;
        this.treeId = treeId;
        this.dateTaken = dateTaken;
        this.imageUrlOrFileName = imageUrlOrFileName;
    }

    // Default Constructor
    public Image() {
    }

    // Getters and Setters
    public int getImageId() {
        return imageId;
    }

    public void setImageID(int imageId) {
        this.imageId = imageId;
    }

    public int getTreeId() {
        return treeId;
    }

    public void setTreeId(int treeId) {
        this.treeId = treeId;
    }

    public Date getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(Date dateTaken) {
        this.dateTaken = dateTaken;
    }

    public String getImageUrlOrFileName() {
        return imageUrlOrFileName;
    }

    public void setImageUrlOrFileName(String imageUrlOrFileName) {
        this.imageUrlOrFileName = imageUrlOrFileName;
    }

    // Optionally, override toString, equals, and hashCode methods
    @Override
    public String toString() {
        return "Image{" +
                "imageId=" + imageId +
                ", treeId=" + treeId +
                ", dateTaken=" + dateTaken +
                ", imageUrlOrFileName='" + imageUrlOrFileName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Image image = (Image) o;

        return imageId == image.imageId;
    }

    @Override
    public int hashCode() {
        return (int) (imageId ^ (imageId >>> 32));
    }
}
