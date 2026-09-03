package com.tree.beans;

import java.util.Date;

public class Image {
    private int imageId;
    private int treeId;
    private Date dateTaken;
    private String imageUrlOrFileName;
    private String caption; // Linked caption from Note table

    public Image() {}

    public Image(int imageId, int treeId, Date dateTaken, String imageUrlOrFileName) {
        this.imageId = imageId;
        this.treeId = treeId;
        this.dateTaken = dateTaken;
        this.imageUrlOrFileName = imageUrlOrFileName;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
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

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
