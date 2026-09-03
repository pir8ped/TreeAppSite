package com.tree.beans;

import java.util.Date;
import java.util.List;

public class Tree {
    private int treeId;
    private String latinName;
    private Integer locationId;
    private Integer collectionId;
    private Date datePlanted;
    private String origin;
    private String rootstock;
    private String variety;
    private String status;
    private String located;
    private String label;
    private String englishName;
    private String frenchName;
    private String characteristics;
    private String otherNames;

    // Joined / Extra fields for Web
    private String collectionName;
    private Location location;
    private List<Image> images;
    private List<Note> notes;
    private List<Scion> scions;
    private String primaryImagePath;
    private String fruitingDescription;

    public Tree() {}

    public int getTreeId() {
        return treeId;
    }

    public void setTreeId(int treeId) {
        this.treeId = treeId;
    }

    public String getLatinName() {
        return latinName;
    }

    public void setLatinName(String latinName) {
        this.latinName = latinName;
    }

    public Integer getLocationId() {
        return locationId;
    }

    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    public Integer getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Integer collectionId) {
        this.collectionId = collectionId;
    }

    public Date getDatePlanted() {
        return datePlanted;
    }

    public void setDatePlanted(Date datePlanted) {
        this.datePlanted = datePlanted;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getRootstock() {
        return rootstock;
    }

    public void setRootstock(String rootstock) {
        this.rootstock = rootstock;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getStatus() {
        return status != null ? status : "unverified";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocated() {
        return located;
    }

    public void setLocated(String located) {
        this.located = located;
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

    public String getFrenchName() {
        return frenchName;
    }

    public void setFrenchName(String frenchName) {
        this.frenchName = frenchName;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(String characteristics) {
        this.characteristics = characteristics;
    }

    public String getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(String otherNames) {
        this.otherNames = otherNames;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    public List<Scion> getScions() {
        return scions;
    }

    public void setScions(List<Scion> scions) {
        this.scions = scions;
    }

    public String getPrimaryImagePath() {
        return primaryImagePath;
    }

    public void setPrimaryImagePath(String primaryImagePath) {
        this.primaryImagePath = primaryImagePath;
    }

    public String getFruitingDescription() {
        return fruitingDescription;
    }

    public void setFruitingDescription(String fruitingDescription) {
        this.fruitingDescription = fruitingDescription;
    }

    public String getDisplayName() {
        if (englishName != null && !englishName.trim().isEmpty()) {
            return englishName;
        }
        return latinName != null ? latinName : "Tree #" + treeId;
    }
}
