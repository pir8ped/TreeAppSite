package com.tree.beans;

public class TreeStatistics {
    private int totalCount;
    private int verifiedCount;
    private int unverifiedCount;
    private int lostCount;
    private int speciesCount;
    private int scionCount;
    private int photoCount;

    public TreeStatistics() {}

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getVerifiedCount() {
        return verifiedCount;
    }

    public void setVerifiedCount(int verifiedCount) {
        this.verifiedCount = verifiedCount;
    }

    public int getUnverifiedCount() {
        return unverifiedCount;
    }

    public void setUnverifiedCount(int unverifiedCount) {
        this.unverifiedCount = unverifiedCount;
    }

    public int getLostCount() {
        return lostCount;
    }

    public void setLostCount(int lostCount) {
        this.lostCount = lostCount;
    }

    public int getSpeciesCount() {
        return speciesCount;
    }

    public void setSpeciesCount(int speciesCount) {
        this.speciesCount = speciesCount;
    }

    public int getScionCount() {
        return scionCount;
    }

    public void setScionCount(int scionCount) {
        this.scionCount = scionCount;
    }

    public int getPhotoCount() {
        return photoCount;
    }

    public void setPhotoCount(int photoCount) {
        this.photoCount = photoCount;
    }
}
