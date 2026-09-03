package com.john.TreeApp.beans;

import java.util.Objects;

public class TreeSpecies {
    private String latinName;
    private String englishName;
    private String frenchName;
    private String characteristics;
    private String otherNames;
    private Integer fruitingStartMonth;
    private String fruitingDescription;

    // Constructor
    public TreeSpecies(String latinName, String englishName, String frenchName, String characteristics,
            String otherNames) {
        this.latinName = latinName;
        this.englishName = englishName;
        this.frenchName = frenchName;
        this.characteristics = characteristics;
        this.otherNames = otherNames;
    }

    // Default Constructor
    public TreeSpecies() {
    }

    // Getters and Setters
    public String getLatinName() {
        return latinName;
    }

    public void setLatinName(String latinName) {
        this.latinName = latinName;
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

    public Integer getFruitingStartMonth() {
        return fruitingStartMonth;
    }

    public void setFruitingStartMonth(Integer fruitingStartMonth) {
        this.fruitingStartMonth = fruitingStartMonth;
    }

    public String getFruitingDescription() {
        return fruitingDescription;
    }

    public void setFruitingDescription(String fruitingDescription) {
        this.fruitingDescription = fruitingDescription;
    }

    // Optionally, override toString, equals, and hashCode methods
    @Override
    public String toString() {
        return "TreeSpecies{" +
                "latinName='" + latinName + '\'' +
                ", englishName='" + englishName + '\'' +
                ", frenchName='" + frenchName + '\'' +
                ", characteristics='" + characteristics + '\'' +
                ", otherNames='" + otherNames + '\'' +
                ", fruitingStartMonth=" + fruitingStartMonth +
                ", fruitingDescription='" + fruitingDescription + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TreeSpecies that = (TreeSpecies) o;

        if (latinName == null || that.latinName == null) {
            return latinName == that.latinName;
        }

        return latinName.equalsIgnoreCase(that.latinName);
    }

    @Override
    public int hashCode() {
        return latinName != null ? latinName.toUpperCase().hashCode() : 0;
    }
}
