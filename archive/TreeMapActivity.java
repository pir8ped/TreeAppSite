/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.john.treelocator;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.john.treelocator.beans.Tree;
import com.john.treelocator.db.Database;
import com.john.treelocator.db.TreeTable;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * This shows how to create a simple activity with a map and a marker on the map.
 */
public class TreeMapActivity extends AppCompatActivity implements OnMapReadyCallback,   GoogleMap.OnMarkerDragListener, GoogleMap.OnInfoWindowClickListener, LocationSource {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int CREATE_REQUEST_CODE = 40;
    private static final int OPEN_REQUEST_CODE = 41;

    private static final String TAG = TreeMapActivity.class.getSimpleName();
    public static int TREE_ID = 0;

    Context context;
    TreeTable treeTable;
    private Location currentLocation = null;
    Location mLastLocation;
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    Marker mCurrLocationMarker;
    IconGenerator iconFactory;


    //  TreeCollection treeCollection = null;
    List<Tree> treeList;
    //  List<Tree> sortedTreeList;
    boolean updateTreeSortingWhenLocationChanges = false;

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(TreeMapActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(TreeMapActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(TreeMapActivity.this, "Location data essential for mapping trees. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(TreeMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("value", "Permission Granted, Now you can use local drive .");
            } else {
                Log.e("value", "Permission Denied, You cannot use local drive .");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = super.getApplicationContext();
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        treeTable = TreeTable.getInstance(context);

        iconFactory = new IconGenerator(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            setCurrentLocation(location);
                        }
                    }
                });

        locationRequest = createLocationRequest();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        initialiseLocationCallback();

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    Activity#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for Activity#requestPermissions for more details.
                    return;
                }
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());


            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(TreeMapActivity.this, LocationRequest.PRIORITY_HIGH_ACCURACY);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });

        treeList = treeTable.getAllTrees();

        setContentView(R.layout.basic_demo);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void initialiseLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                List<Location> locationList = locationResult.getLocations();
                if (locationList.size() > 0) {
                    //The last location in the list is the newest
                    Location location = locationList.get(locationList.size() - 1);
                    setCurrentLocation(location);
                    mLastLocation = location;
                    if (mCurrLocationMarker != null) {
                        mCurrLocationMarker.remove();
                    }
                    map.clear();

                    //Place current location marker
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title("Current Position");
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                    mCurrLocationMarker = map.addMarker(markerOptions);

                    Intent intent = getIntent();
                    String nameOfSelectedTree = intent.getStringExtra("treeName");
                    Marker marker;
                    for (int i = 0; i < treeList.size(); i++) {
                        Tree tree = treeList.get(i);
                        if (nameOfSelectedTree != null && nameOfSelectedTree.equals(tree.getSpecies().getLatinName()))
                            marker = map.addMarker(new MarkerOptions().position(new LatLng(tree.getLatitude(), tree.getLongitude())).title(tree.getSpecies().getLatinName()).draggable(true).snippet("ID:"+tree.getTreeId()).icon(BitmapDescriptorFactory.fromResource(R.drawable.tree_icon_large_purple)));//iconFactory.makeIcon(""+tree.getTreeId())
                        else
                            marker = map.addMarker(new MarkerOptions().position(new LatLng(tree.getLatitude(), tree.getLongitude())).title(tree.getSpecies().getLatinName()).draggable(true).snippet("ID:"+tree.getTreeId()).icon(BitmapDescriptorFactory.fromResource(R.drawable.tree_icon_small)));//iconFactory.makeIcon(tree.getTreeId())
                        marker.setTag(tree);
                    }

                }
            }

        };

    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        this.map = map;
        map.setOnMarkerDragListener(this);
        map.setMyLocationEnabled(true);
        UiSettings settings = map.getUiSettings();
        settings.setCompassEnabled(true);
        settings.setRotateGesturesEnabled(false);
        settings.setScrollGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(true);
        Location location = getCurrentLocation();
        if (location != null) {
            float zoomLevel = 21.0f; //This goes up to 21
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), zoomLevel));
        }
      //  map.setOnMarkerClickListener(this);
        map.setOnInfoWindowClickListener(this);
        map.setOnMarkerDragListener(this);
    }

    private void updateCameraBearing(GoogleMap googleMap, float bearing) {
        if ( googleMap == null) return;
        CameraPosition camPos = CameraPosition
                .builder(
                        googleMap.getCameraPosition() // current Camera
                )
                .bearing(bearing)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }


    public synchronized Location getCurrentLocation() {
        return currentLocation;
    }

    private synchronized void setCurrentLocation(Location location) {
        currentLocation = location;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Tree treeAtThatMarker = (Tree) marker.getTag();
      //  Toast.makeText(getApplicationContext(), "treeAtThatMarker =  " + "ID = "+treeAtThatMarker.getTreeId()+", S"+treeAtThatMarker.getSpecies().getLatinName(), Toast.LENGTH_LONG).show();

        updateTreeSortingWhenLocationChanges = false;
    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng pos = marker.getPosition();
        if (treeList != null) {
            Tree treeAtThatMarker = (Tree) marker.getTag();
            Log.d(TAG, "tree.toString = " + treeAtThatMarker.toString());
            Toast.makeText(getApplicationContext(), "treeAtThatMarker =  " + "ID = "+treeAtThatMarker.getTreeId()+", S"+treeAtThatMarker.getSpecies().getLatinName(), Toast.LENGTH_LONG).show();
            LatLng latLng = marker.getPosition();
            treeAtThatMarker.setLatLng(latLng);
            treeTable.updatePosition(treeAtThatMarker);
        }
    }


    @Override
    public void onInfoWindowClick(Marker marker) {
        Intent intent = new Intent(context, EditTreeActivity.class);
        Tree tree = (Tree) marker.getTag();
        TREE_ID = tree.getTreeId();
        intent.putExtra("treeId", TREE_ID);
        startActivity(intent);
    }


    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {

    }

    @Override
    public void deactivate() {

    }

}
