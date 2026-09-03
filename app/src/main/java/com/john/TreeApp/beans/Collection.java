package com.john.TreeApp.beans;

public class Collection {
    private int id;
    private String name;
    private boolean selected;  // New property

    // Constructor, getters, and setters
    public Collection(int id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public Collection (String name, boolean selected) {
        this.name = name;
        this.selected = selected;
    }

    public Collection (){};

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return name;
    }

    // ... rest of the code ...
}
