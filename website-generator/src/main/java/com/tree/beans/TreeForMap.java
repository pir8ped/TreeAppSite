package com.tree.beans;

public class TreeForMap {
    private int id;
    private double latitude;
    private double longitude;
    private String label;
    private String englishName;
    private String latinName;
    private String variety;
    private String rootstock;
    private String status;
    private int collectionId;
    private String collectionName;
    private String latestImagePath;
    private String colorHex;

    public TreeForMap() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getLatinName() {
        return latinName;
    }

    public void setLatinName(String latinName) {
        this.latinName = latinName;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getRootstock() {
        return rootstock;
    }

    public void setRootstock(String rootstock) {
        this.rootstock = rootstock;
    }

    public String getStatus() {
        return status != null ? status : "unverified";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(int collectionId) {
        this.collectionId = collectionId;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getLatestImagePath() {
        return latestImagePath;
    }

    public void setLatestImagePath(String latestImagePath) {
        this.latestImagePath = latestImagePath;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        if (englishName != null && !englishName.trim().isEmpty()) {
            return englishName;
        }
        return latinName != null ? latinName : "Tree #" + id;
    }
}
