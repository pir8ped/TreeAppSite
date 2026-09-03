package com.john.TreeApp.beans;

public class TreeStatistics {
    private int verifiedCount;
    private int unverifiedCount;
    private int lostCount;
    private int speciesCount;

    public TreeStatistics() {
    }

    public TreeStatistics(int verifiedCount, int unverifiedCount, int lostCount, int speciesCount) {
        this.verifiedCount = verifiedCount;
        this.unverifiedCount = unverifiedCount;
        this.lostCount = lostCount;
        this.speciesCount = speciesCount;
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
}
