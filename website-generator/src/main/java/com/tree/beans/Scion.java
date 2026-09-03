package com.tree.beans;

public class Scion {
    private int scionId;
    private String species;
    private String variety;
    private String source;
    private boolean attached;
    private Integer fruitingStartMonth;
    private String fruitingDescription;

    public Scion() {}

    public Scion(int scionId, String species, String variety, String source, boolean attached, Integer fruitingStartMonth, String fruitingDescription) {
        this.scionId = scionId;
        this.species = species;
        this.variety = variety;
        this.source = source;
        this.attached = attached;
        this.fruitingStartMonth = fruitingStartMonth;
        this.fruitingDescription = fruitingDescription;
    }

    public int getScionId() {
        return scionId;
    }

    public void setScionId(int scionId) {
        this.scionId = scionId;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isAttached() {
        return attached;
    }

    public void setAttached(boolean attached) {
        this.attached = attached;
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
}
