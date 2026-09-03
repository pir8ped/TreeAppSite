package com.john.TreeApp.beans.utilBean;

import com.google.android.gms.maps.model.LatLng;
import com.john.TreeApp.beans.Location;

public class TreeForMap {
    Location location;
    int id;
    String latinName;
    String englishName;
    String variety;
    String rootstock;
    String label;
    int collectionId;
    String nameToUseOnMap;
    String latestImagePath; // may be null

    public TreeForMap(Location location, int id, String latinName, String englishName,
                      String variety, String rootstock, String label, int collectionId,
                      String latestImagePath) {
        this.location = location;
        this.id = id;
        this.latinName = latinName;
        this.englishName = englishName;
        this.variety = variety;
        this.rootstock = rootstock;
        this.label = label;
        this.collectionId = collectionId;
        this.latestImagePath = latestImagePath;
        if (englishName != null && !englishName.equals(""))
            nameToUseOnMap = englishName;
        else
            nameToUseOnMap = latinName;
    }

    public LatLng getLatLng() {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public int getId() {
        return id;
    }

    public int getCollectionId() {
        return collectionId;
    }

    public String getLabel() {
        return label;
    }

    public String getNameToUseOnMap() {
        return nameToUseOnMap;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getLatinName() {
        return latinName;
    }

    public String getVariety() {
        return variety;
    }

    public String getRootstock() {
        return rootstock;
    }

    public String getLatestImagePath() {
        return latestImagePath;
    }

    public Location getLocation() {
        return location;
    }

    public double getLatitude() {
        return location.getLatitude();
    }

    public double getLongitude() {
        return location.getLongitude();
    }
}
