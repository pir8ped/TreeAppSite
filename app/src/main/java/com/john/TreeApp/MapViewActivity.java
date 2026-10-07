package com.john.TreeApp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.john.TreeApp.beans.Location;
import com.john.TreeApp.beans.utilBean.TreeForMap;
import com.john.TreeApp.views.LegendView;
import com.john.TreeApp.adapters.PhotoAdapter;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import db.CollectionDAOImpl;
import db.LocationDAO;
import db.LocationDAOImpl;
import db.TreeDAO;
import db.TreeDAOImpl;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Button;
import android.content.Context;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.HashMap;
import java.util.stream.Collectors;


public class MapViewActivity extends BaseActivity implements OnMapReadyCallback {
    private static final String TAG = "MapViewActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int EDIT_TREE_REQUEST = 2;

    /**
     * Palette of well-separated HSV hues used to colour-code species.
     * These are spread around the colour wheel to remain distinguishable.
     */
    private static final float[] SPECIES_HUES = {
        0f,   // red
        30f,  // orange
        60f,  // yellow
        120f, // green
        180f, // cyan
        200f, // sky blue
        240f, // blue
        270f, // indigo
        300f, // magenta
        330f, // pink
        15f,  // red-orange
        45f,  // amber
        90f,  // yellow-green
        160f, // teal
        210f, // azure
    };

    // --- Map & UI ---
    private GoogleMap mMap;
    private LegendView legendView;
    private Button compassModeButton;

    // --- DAO ---
    private TreeDAO treeDAO;
    private LocationDAO locationDAO;

    // --- Compass ---
    private boolean isCompassMode = false;
    private float lastKnownBearing = 0f;
    private SensorManager sensorManager;

    // --- GPS correction ---
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback correctedPositionCallback;
    private Marker correctedPositionMarker;
    private boolean isTrackingCorrectedPosition = false;

    // --- Species colour map: latinName.toUpperCase() → hue ---
    private final Map<String, Float> speciesHueMap = new LinkedHashMap<>();

    // --- Marker state ---
    private Marker selectedMarker;
    private int currentCollectionId;

    // --- Image cache for info windows: treeId → Bitmap ---
    private final Map<Integer, Bitmap> thumbnailCache = new HashMap<>();
    private final ExecutorService imageLoader = Executors.newCachedThreadPool();

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_map_view);

        treeDAO      = new TreeDAOImpl();
        locationDAO  = new LocationDAOImpl();
        legendView   = findViewById(R.id.legend_view);
        compassModeButton = findViewById(R.id.compass_mode_button);
        compassModeButton.setOnClickListener(v -> toggleCompassMode());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isCompassMode) startCompassUpdates();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCompassUpdates();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageLoader.shutdownNow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Options menu
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
         if (id == R.id.action_create_map) {
            showCreateMapDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create Map dialog
    // ─────────────────────────────────────────────────────────────────────────

    private void showCreateMapDialog() {
        Intent intent = new Intent(this, MapExportActivity.class);
        if (mMap != null) {
            CameraPosition pos = mMap.getCameraPosition();
            intent.putExtra("EXTRA_LAT", pos.target.latitude);
            intent.putExtra("EXTRA_LNG", pos.target.longitude);
            intent.putExtra("EXTRA_ZOOM", pos.zoom);
        }
        startActivity(intent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Map ready
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        UiSettings settings = mMap.getUiSettings();
        settings.setCompassEnabled(true);
        settings.setRotateGesturesEnabled(false);
        settings.setScrollGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(true);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_LOCATION_PERMISSION);
        } else {
            enableMyLocation();
        }

        // Load trees and position camera according to collection bounds vs phone location
        loadAllTreesOnMap();

        setupMarkerClickListeners();
        setupInfoWindowAdapter();
    }

    /**
     * Positions camera:
     * - If phone is within collection bounds -> centers on current phone location
     * - If phone is outside collection bounds (or location unavailable) -> centers on center of current collection
     */
    private void positionCameraForCurrentCollection(int collectionId, LatLngBounds collectionBounds, boolean hasSelection, LatLngBounds selectionBounds) {
        if (mMap == null) return;

        // 1. If trees were explicitly pre-selected, focus on them
        if (hasSelection && selectionBounds != null) {
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(selectionBounds, 200));
            } catch (Exception ignored) {}
            return;
        }

        // 2. If no collection bounds exist, nothing to center on
        if (collectionBounds == null) return;

        LatLng collectionCenter = collectionBounds.getCenter();

        // 3. Query device location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient flc = LocationServices.getFusedLocationProviderClient(this);
            flc.getLastLocation().addOnSuccessListener(this, loc -> {
                if (loc != null) {
                    LatLng phoneLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());

                    // Add a small 0.0005 deg (~50m) buffer so standing at the gate/edge still counts as inside
                    double margin = 0.0005;
                    LatLngBounds bufferedBounds = new LatLngBounds(
                            new LatLng(collectionBounds.southwest.latitude - margin, collectionBounds.southwest.longitude - margin),
                            new LatLng(collectionBounds.northeast.latitude + margin, collectionBounds.northeast.longitude + margin)
                    );

                    if (bufferedBounds.contains(phoneLatLng)) {
                        Log.d(TAG, "Phone is WITHIN collection bounds. Centering on phone: " + phoneLatLng);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(phoneLatLng, 18f));
                    } else {
                        Log.d(TAG, "Phone is OUTSIDE collection bounds. Centering on collection center: " + collectionCenter);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(collectionCenter, 18f));
                    }
                } else {
                    Log.d(TAG, "Phone location unavailable. Centering on collection center: " + collectionCenter);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(collectionCenter, 18f));
                }
            }).addOnFailureListener(e -> {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(collectionCenter, 18f));
            });
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(collectionCenter, 18f));
        }
    }

    private void loadAllTreesOnMap() {
        allTrees = treeDAO.getTreesForMapAllCollections();
        Log.d(TAG, "loadAllTreesOnMap: " + allTrees.size() + " trees across all collections");

        if (allTrees.isEmpty()) return;

        buildSpeciesColourMap(allTrees);

        // Resolve current collection ID
        int selectedColId = getIntent().getIntExtra("collectionId", -1);
        if (selectedColId == -1) {
            selectedColId = new CollectionDAOImpl().getSelectedCollectionId();
        }
        currentCollectionId = selectedColId;

        ArrayList<Integer> selectedTreeIds = getIntent().getIntegerArrayListExtra("SELECTED_TREE_IDS");
        boolean hasSelection = selectedTreeIds != null && !selectedTreeIds.isEmpty();

        LatLngBounds.Builder selectionBoundsBuilder = new LatLngBounds.Builder();
        LatLngBounds.Builder collectionBoundsBuilder = new LatLngBounds.Builder();
        int treesInCurrentCollection = 0;

        for (TreeForMap tree : allTrees) {
            LatLng latLng = new LatLng(tree.getLatitude(), tree.getLongitude());

            // Track bounds for current collection
            if (tree.getCollectionId() == currentCollectionId) {
                collectionBoundsBuilder.include(latLng);
                treesInCurrentCollection++;
            }

            boolean isSelected = hasSelection && selectedTreeIds.contains(tree.getId());
            if (isSelected) {
                selectionBoundsBuilder.include(latLng);
            }

            MarkerOptions opts = new MarkerOptions()
                    .position(latLng)
                    .title(tree.getNameToUseOnMap())
                    .snippet(tree.getLabel())
                    .icon(isSelected ? BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                            : BitmapDescriptorFactory.fromResource(R.drawable.tree_icon_small))
                    .zIndex(isSelected ? 1.0f : 0.0f);

            Marker marker = mMap.addMarker(opts);
            if (marker != null) marker.setTag(tree);
        }

        if (legendView != null) {
            legendView.setVisibility(android.view.View.GONE);
        }

        LatLngBounds selectionBounds = hasSelection ? selectionBoundsBuilder.build() : null;
        LatLngBounds collectionBounds = treesInCurrentCollection > 0 ? collectionBoundsBuilder.build() : null;

        // Apply the requested bounds centering logic
        positionCameraForCurrentCollection(currentCollectionId, collectionBounds, hasSelection, selectionBounds);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            // Re-evaluate camera centering with newly granted location
            loadAllTreesOnMap();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load trees from ALL collections
    // ─────────────────────────────────────────────────────────────────────────

    private List<TreeForMap> allTrees = new ArrayList<>();


    // Keep backwards-compatible overload used by onActivityResult
    private void loadTreeLocations(ArrayList<Integer> selectedTreeIds) {
        if (mMap != null) mMap.clear();
        allTrees.clear();
        thumbnailCache.clear();
        loadAllTreesOnMap();
        // Re-apply selection highlight
        if (selectedTreeIds != null && !selectedTreeIds.isEmpty()) {
            getIntent().putExtra("SELECTED_TREE_IDS", selectedTreeIds);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Species → colour helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void buildSpeciesColourMap(List<TreeForMap> trees) {
        speciesHueMap.clear();
        int hueIndex = 0;
        for (TreeForMap tree : trees) {
            String key = tree.getLatinName() != null
                    ? tree.getLatinName().toUpperCase(Locale.ROOT) : "UNKNOWN";
            if (!speciesHueMap.containsKey(key)) {
                float hue = SPECIES_HUES[hueIndex % SPECIES_HUES.length];
                speciesHueMap.put(key, hue);
                hueIndex++;
            }
        }
    }

    private float getHueForSpecies(String latinName) {
        if (latinName == null) return 0f;
        Float h = speciesHueMap.get(latinName.toUpperCase(Locale.ROOT));
        return h != null ? h : 0f;
    }

    /** Convert a map hue to a full ARGB colour (for LegendView). */
    public static int hueToArgb(float hue) {
        float[] hsv = { hue, 0.85f, 0.90f };
        return Color.HSVToColor(hsv);
    }

    private void updateLegend() {
        if (legendView == null) return;
        List<LegendView.Entry> entries = new ArrayList<>();
        for (TreeForMap tree : allTrees) {
            String key = tree.getLatinName() != null
                    ? tree.getLatinName().toUpperCase(Locale.ROOT) : "UNKNOWN";
            // Only one entry per unique species
            boolean alreadyAdded = entries.stream().anyMatch(e -> e.label.equalsIgnoreCase(
                    (tree.getEnglishName() != null && !tree.getEnglishName().isEmpty())
                            ? tree.getEnglishName() : tree.getLatinName()));
            if (!alreadyAdded) {
                float hue = getHueForSpecies(tree.getLatinName());
                String displayName = (tree.getEnglishName() != null && !tree.getEnglishName().isEmpty())
                        ? tree.getEnglishName() : tree.getLatinName();
                entries.add(new LegendView.Entry(hueToArgb(hue), displayName));
            }
        }
        legendView.setEntries(entries);
        legendView.setVisibility(entries.isEmpty()
                ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Marker click & info window
    // ─────────────────────────────────────────────────────────────────────────

    private void setupMarkerClickListeners() {
        mMap.setOnMarkerClickListener(marker -> {
            selectedMarker = marker;
            Object tag = marker.getTag();
            if (tag instanceof TreeForMap) {
                TreeForMap tree = (TreeForMap) tag;
                // Trigger async image pre-load so info window can show it
                preloadThumbnail(tree, marker);
            }
            marker.showInfoWindow();
            return true;
        });

        mMap.setOnInfoWindowClickListener(this::openEditTreeActivity);
        mMap.setOnInfoWindowLongClickListener(this::openEditTreeActivity);

        mMap.setOnMapClickListener(latLng -> {
            if (selectedMarker != null) {
                selectedMarker.hideInfoWindow();
                selectedMarker = null;
            }
        });
    }

    private void setupInfoWindowAdapter() {
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public android.view.View getInfoWindow(Marker marker) {
                return null; // use default frame
            }

            @Override
            public android.view.View getInfoContents(Marker marker) {
                if (!(marker.getTag() instanceof TreeForMap)) return null;
                TreeForMap tree = (TreeForMap) marker.getTag();

                android.widget.LinearLayout root = new android.widget.LinearLayout(MapViewActivity.this);
                root.setOrientation(android.widget.LinearLayout.VERTICAL);
                root.setPadding(dp(12), dp(10), dp(12), dp(10));

                // ── Label / title ──
                android.widget.TextView tvLabel = new android.widget.TextView(MapViewActivity.this);
                tvLabel.setTextColor(Color.BLACK);
                tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
                tvLabel.setText(tree.getLabel() != null ? tree.getLabel() : "—");
                root.addView(tvLabel);

                // ── Species ──
                addInfoRow(root, "Species",
                        (tree.getEnglishName() != null && !tree.getEnglishName().isEmpty())
                                ? tree.getEnglishName() : tree.getLatinName());

                if (tree.getLatinName() != null && tree.getEnglishName() != null
                        && !tree.getEnglishName().isEmpty()) {
                    addInfoRow(root, "", tree.getLatinName()); // italic latin name on second line
                }

                // ── Variety ──
                if (tree.getVariety() != null && !tree.getVariety().isEmpty()) {
                    addInfoRow(root, "Variety", tree.getVariety());
                }

                // ── Rootstock ──
                if (tree.getRootstock() != null && !tree.getRootstock().isEmpty()) {
                    addInfoRow(root, "Rootstock", tree.getRootstock());
                }

                // ── Photo thumbnail ──
                Bitmap thumb = thumbnailCache.get(tree.getId());
                if (thumb != null) {
                    android.widget.ImageView iv = new android.widget.ImageView(MapViewActivity.this);
                    android.widget.LinearLayout.LayoutParams lp =
                            new android.widget.LinearLayout.LayoutParams(dp(160), dp(120));
                    lp.topMargin = dp(6);
                    iv.setLayoutParams(lp);
                    iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    iv.setImageBitmap(thumb);
                    root.addView(iv);
                } else if (tree.getLatestImagePath() != null) {
                    android.widget.TextView tvLoading = new android.widget.TextView(MapViewActivity.this);
                    tvLoading.setTextColor(Color.GRAY);
                    tvLoading.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
                    tvLoading.setText("Loading photo…");
                    root.addView(tvLoading);
                }

                addInfoRow(root, "", "Tap to edit");

                return root;
            }
        });
    }

    private void addInfoRow(android.widget.LinearLayout parent, String key, String value) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);

        if (!key.isEmpty()) {
            android.widget.TextView tvKey = new android.widget.TextView(this);
            tvKey.setTextColor(Color.DKGRAY);
            tvKey.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            tvKey.setTypeface(null, android.graphics.Typeface.BOLD);
            tvKey.setText(key + ": ");
            row.addView(tvKey);
        }

        android.widget.TextView tvVal = new android.widget.TextView(this);
        tvVal.setTextColor(Color.DKGRAY);
        tvVal.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        tvVal.setText(value);
        row.addView(tvVal);

        parent.addView(row);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Pre-loads the latest photo thumbnail for a tree on a background thread.
     * When done, refreshes the info window so the image appears.
     */
    private void preloadThumbnail(TreeForMap tree, Marker marker) {
        if (tree.getLatestImagePath() == null) return;
        if (thumbnailCache.containsKey(tree.getId())) return; // already loaded

        imageLoader.execute(() -> {
            try {
                File imgFile = PhotoAdapter.resolveImageFile(MapViewActivity.this, tree.getLatestImagePath());

                Bitmap bmp = Glide.with(MapViewActivity.this)
                        .asBitmap()
                        .load(imgFile.exists() ? imgFile : tree.getLatestImagePath())
                        .submit(320, 240)
                        .get();
                if (bmp != null) {
                    thumbnailCache.put(tree.getId(), bmp);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (marker.isInfoWindowShown()) {
                            marker.showInfoWindow(); // refresh
                        }
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not load thumbnail for tree " + tree.getId() + ": " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edit tree
    // ─────────────────────────────────────────────────────────────────────────

    private void openEditTreeActivity(Marker marker) {
        if (!(marker.getTag() instanceof TreeForMap)) return;
        TreeForMap tree = (TreeForMap) marker.getTag();
        Intent intent = new Intent(this, EditTreeActivity.class);
        intent.putExtra("treeId", tree.getId());
        intent.putExtra("collectionId", tree.getCollectionId());
        startActivityForResult(intent, EDIT_TREE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_TREE_REQUEST && mMap != null) {
            CameraPosition pos = mMap.getCameraPosition();
            mMap.clear();
            correctedPositionMarker = null;
            thumbnailCache.clear();
            loadAllTreesOnMap();
            new Handler().postDelayed(() -> {
                if (mMap != null && !isFinishing()) {
                    try { mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos)); }
                    catch (Exception ignored) {}
                }
            }, 150);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compass mode
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleCompassMode() {
        isCompassMode = !isCompassMode;
        if (isCompassMode) {
            compassModeButton.setText("Exit Compass Mode");
            enableCompassMode();
        } else {
            compassModeButton.setText("Enter Compass Mode");
            disableCompassMode();
        }
    }

    private void enableCompassMode() {
        if (mMap == null) return;
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        CameraPosition pos = new CameraPosition.Builder()
                .target(mMap.getCameraPosition().target)
                .zoom(mMap.getCameraPosition().zoom)
                .bearing(lastKnownBearing).tilt(45).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
        startCompassUpdates();
    }

    private void disableCompassMode() {
        if (mMap == null) return;
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        CameraPosition pos = new CameraPosition.Builder()
                .target(mMap.getCameraPosition().target)
                .zoom(mMap.getCameraPosition().zoom)
                .bearing(0).tilt(0).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
        stopCompassUpdates();
    }

    private final SensorEventListener compassListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] rot = new float[9], orient = new float[3];
                SensorManager.getRotationMatrixFromVector(rot, event.values);
                SensorManager.getOrientation(rot, orient);
                float bearing = (float) Math.toDegrees(orient[0]);
                if (bearing < 0) bearing += 360;
                lastKnownBearing = bearing;
                if (isCompassMode && mMap != null) {
                    CameraPosition pos = new CameraPosition.Builder()
                            .target(mMap.getCameraPosition().target)
                            .zoom(mMap.getCameraPosition().zoom)
                            .bearing(bearing).tilt(45).build();
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
                }
            }
        }
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void startCompassUpdates() {
        if (sensorManager == null)
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor rv = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rv != null)
            sensorManager.registerListener(compassListener, rv, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stopCompassUpdates() {
        if (sensorManager != null) sensorManager.unregisterListener(compassListener);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GPS location (corrected position marker)
    // ─────────────────────────────────────────────────────────────────────────

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }





}
