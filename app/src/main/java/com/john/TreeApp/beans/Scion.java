package com.john.TreeApp.beans;

public class Scion {
    // Fields
    private int scionId;
    private String species;
    private String variety;
    private String source;
    private boolean attached;
    private Integer fruitingStartMonth;
    private String fruitingDescription;

    // Private constructor that accepts a Builder instance
    private Scion(Builder builder) {
        this.scionId = builder.scionId;
        this.species = builder.species;
        this.variety = builder.variety;
        this.source = builder.source;
        this.attached = builder.attached;
        this.fruitingStartMonth = builder.fruitingStartMonth;
        this.fruitingDescription = builder.fruitingDescription;
    }

    // Getters
    public int getScionId() {
        return scionId;
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

    public boolean isAttached() {
        return attached;
    }

    public Integer getFruitingStartMonth() {
        return fruitingStartMonth;
    }

    public String getFruitingDescription() {
        return fruitingDescription;
    }

    // Setters (if needed for updates)
    public void setVariety(String variety) {
        this.variety = variety;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setAttached(boolean attached) {
        this.attached = attached;
    }

    public void setFruitingStartMonth(Integer fruitingStartMonth) {
        this.fruitingStartMonth = fruitingStartMonth;
    }

    public void setFruitingDescription(String fruitingDescription) {
        this.fruitingDescription = fruitingDescription;
    }

    // The Builder inner class
    public static class Builder {
        private int scionId;
        private String species;
        private String variety;
        private String source;
        private boolean attached = false; // Default not attached
        private Integer fruitingStartMonth;
        private String fruitingDescription;

        // Builder constructor with required field
        public Builder(String species) {
            this.species = species;
        }

        public Builder scionId(int scionId) {
            this.scionId = scionId;
            return this;
        }

        public Builder variety(String variety) {
            this.variety = variety;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder attached(boolean attached) {
            this.attached = attached;
            return this;
        }

        public Builder fruitingStartMonth(Integer fruitingStartMonth) {
            this.fruitingStartMonth = fruitingStartMonth;
            return this;
        }

        public Builder fruitingDescription(String fruitingDescription) {
            this.fruitingDescription = fruitingDescription;
            return this;
        }

        public Scion build() {
            return new Scion(this);
        }
    }

    @Override
    public String toString() {
        return "Scion{" +
                "scionId=" + scionId +
                ", species='" + species + '\'' +
                ", variety='" + variety + '\'' +
                ", source='" + source + '\'' +
                ", attached=" + attached +
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

        Scion scion = (Scion) o;

        return scionId == scion.scionId;
    }

    @Override
    public int hashCode() {
        return scionId;
    }
}
