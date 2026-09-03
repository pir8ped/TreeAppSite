package com.john.TreeApp.beans;

/**
 * Represents a grouped set of scions of the same type (species/variety/source).
 * Used for displaying scion inventory with counts.
 */
public class ScionGroup {
    private String species;
    private String variety;
    private String source;
    private int count; // Number of unattached scions of this type
    private String fruitingDescription;

    public ScionGroup(String species, String variety, String source, int count) {
        this(species, variety, source, count, null);
    }

    public ScionGroup(String species, String variety, String source, int count, String fruitingDescription) {
        this.species = species;
        this.variety = variety;
        this.source = source;
        this.count = count;
        this.fruitingDescription = fruitingDescription;
    }

    public String getSpecies() {
        return species;
    }

    public String getVariety() {
        return variety;
    }

    public String getSource() {
        return source;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getFruitingDescription() {
        return fruitingDescription;
    }

    public void setFruitingDescription(String fruitingDescription) {
        this.fruitingDescription = fruitingDescription;
    }

    @Override
    public String toString() {
        return "ScionGroup{" +
                "species='" + species + '\'' +
                ", variety='" + variety + '\'' +
                ", source='" + source + '\'' +
                ", count=" + count +
                ", fruitingDescription='" + fruitingDescription + '\'' +
                '}';
    }
}
