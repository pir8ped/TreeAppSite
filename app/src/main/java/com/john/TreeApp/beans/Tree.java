package com.john.TreeApp.beans;

import java.sql.Date;

public class Tree {
    // Fields:
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

    public void setLabel(String label) {
        this.label = label;
    }

    public int getTreeId() {
        return treeId;
    }

    public String getLatinName() {
        return latinName;
    }

    public Integer getLocationId() {
        return locationId;
    }

    public Integer getCollectionId() {
        return collectionId;
    }

    public Date getDatePlanted() {
        return datePlanted;
    }

    public String getOrigin() {
        return origin;
    }

    public String getRootstock() {
        return rootstock;
    }

    public String getVariety() {
        return variety;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocated() {
        return located;
    }

    public String getLabel() {
        return label;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getFrenchName() {
        return frenchName;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public String getOtherNames() {
        return otherNames;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public void setRootstock(String rootstock) {
        this.rootstock = rootstock;
    }

    public void setDatePlanted(Date datePlanted) {
        this.datePlanted = datePlanted;
    }

    public void setCollectionId(Integer collectionId) {
        this.collectionId = collectionId;
    }

    private String frenchName;
    private String characteristics;
    private String otherNames;

    // Private constructor that accepts a Builder instance
    private Tree(Builder builder) {
        this.treeId = builder.treeId;
        this.latinName = builder.latinName;
        this.locationId = builder.locationId;
        this.collectionId = builder.collectionId;
        this.datePlanted = builder.datePlanted;
        this.origin = builder.origin;
        this.rootstock = builder.rootstock;
        this.variety = builder.variety;
        this.located = builder.located;
        this.label = builder.label;
        this.englishName = builder.englishName;
        this.frenchName = builder.frenchName;
        this.characteristics = builder.characteristics;
        this.otherNames = builder.otherNames;
        this.status = builder.status;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    // Getters and setters ...

    // The Builder inner class
    public static class Builder {
        private int treeId;
        private String latinName;
        private Integer locationId;
        private Integer collectionId;
        private Date datePlanted;
        private String origin;
        private String rootstock;
        private String variety;
        private String located;



        private String label;
        private String englishName;
        private String frenchName;
        private String characteristics;
        private String otherNames;
        private String status = "unverified";

        // Builder constructor with required fields
        public Builder(String latinName) {
            this.latinName = latinName;
        }

        public Builder englishName(String englishName) {
            this.englishName = englishName;
            return this;
        }

        public Builder treeId(int treeId) {
            this.treeId = treeId;
            return this;
        }

        public Builder locationId(Integer locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder collectionId(Integer collectionId) {
            this.collectionId = collectionId;
            return this;
        }

        public Builder datePlanted(Date datePlanted) {
            this.datePlanted = datePlanted;
            return this;
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder rootstock(String rootstock) {
            this.rootstock = rootstock;
            return this;
        }

        public Builder variety(String variety) {
            this.variety = variety;
            return this;
        }

        public Builder located(String located) {
            this.located = located;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder frenchName(String frenchName) {
            this.frenchName = frenchName;
            return this;
        }

        public Builder characteristics(String characteristics) {
            this.characteristics = characteristics;
            return this;
        }

        public Builder otherNames(String otherNames) {
            this.otherNames = otherNames;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Tree build() {
            return new Tree(this);
        }
    }

    @Override
    public String toString() {
        return "Tree{" +
                "treeId=" + treeId +
                ", latinName='" + latinName + '\'' +
                ", locationId=" + locationId +
                ", collectionId=" + collectionId +
                ", datePlanted=" + datePlanted +
                ", origin='" + origin + '\'' +
                ", rootstock='" + rootstock + '\'' +
                ", variety='" + variety + '\'' +
                ", located='" + located + '\'' +
                ", label='" + label + '\'' +
                ", englishName='" + englishName + '\'' +
                ", frenchName='" + frenchName + '\'' +
                ", characteristics='" + characteristics + '\'' +
                ", otherNames='" + otherNames + '\'' +
                ", status='" + status + '\'' +
                '}';
    }


@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tree tree = (Tree) o;

        return treeId == tree.treeId;
    }

    @Override
    public int hashCode() {
        return (int) (treeId ^ (treeId >>> 32));
    }


}
