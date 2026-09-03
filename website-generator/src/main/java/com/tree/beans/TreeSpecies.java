package com.tree.beans;

public class TreeSpecies {
    private String latinName;
    private String englishName;
    private String frenchName;
    private String characteristics;
    private String otherNames;
    private Integer fruitingStartMonth;
    private String fruitingDescription;

    public TreeSpecies() {}

    public TreeSpecies(String latinName, String englishName, String frenchName, String characteristics, String otherNames) {
        this.latinName = latinName;
        this.englishName = englishName;
        this.frenchName = frenchName;
        this.characteristics = characteristics;
        this.otherNames = otherNames;
    }

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

    public String getDisplayName() {
        if (englishName != null && !englishName.trim().isEmpty()) {
            return englishName;
        }
        return latinName != null ? latinName : "Unknown Species";
    }

    public String getFruitingMonthName() {
        if (fruitingStartMonth == null || fruitingStartMonth < 1 || fruitingStartMonth > 12) {
            return "";
        }
        String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        return months[fruitingStartMonth - 1];
    }
}
