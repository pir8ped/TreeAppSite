package com.john.TreeApp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.john.TreeApp.beans.utilBean.TreeForMap;
import com.john.TreeApp.views.LegendView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import db.TreeDAOImpl;

/**
 * Activity that lets the user frame a satellite map view, then generates
 * a PDF (A3 or A2, portrait or landscape) of their tree collection.
 *
 * The satellite background comes from the Maps Static API (1280×1280 px).
 * Trees are drawn as coloured dots with label + species annotations.
 * A legend box is drawn in the bottom-left corner.
 */
public class MapExportActivity extends BaseActivity implements OnMapReadyCallback {

    private static final String TAG = "MapExportActivity";

    // Maps Static API — same key as manifest
    private static final String MAPS_API_KEY = "AIzaSyC6MTM_wygL2hHHcgERdB-jHeoW9os9WbM";
    private static final int STATIC_LOGICAL_SIZE = 640; // logical px (scale=2 → 1280 physical)
    private static final int STATIC_SCALE       = 2;
    private static final int STATIC_IMAGE_SIZE  = STATIC_LOGICAL_SIZE * STATIC_SCALE; // 1280

    // PDF page sizes in points (1 pt = 1/72 inch)
    // A3 landscape = 1191 × 842 pt, A2 landscape = 1684 × 1191 pt
    private static final int A3_SHORT = 842;  private static final int A3_LONG = 1191;
    private static final int A2_SHORT = 1191; private static final int A2_LONG = 1684;

    private GoogleMap mMap;
    private RadioGroup rgPaperSize, rgOrientation;
    private Button btnGenerate;
    private TextView tvStatus;

    private List<TreeForMap> allTrees = new ArrayList<>();
    // latinName.upper() → hue
    private final Map<String, Float> speciesHueMap = new LinkedHashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_map_export);

        rgPaperSize   = findViewById(R.id.rg_paper_size);
        rgOrientation = findViewById(R.id.rg_orientation);
        btnGenerate   = findViewById(R.id.btn_generate_pdf);
        tvStatus      = findViewById(R.id.tv_export_status);

        btnGenerate.setOnClickListener(v -> startGeneration());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.export_map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Load trees in background so we can draw dots later
        executor.execute(() -> {
            allTrees = new TreeDAOImpl().getTreesForMapAllCollections();
            buildSpeciesColourMap(allTrees);
            new Handler(Looper.getMainLooper()).post(() -> {
                addMarkersToLiveMap();
                zoomToAllTrees();
            });
        });
    }

    private void addMarkersToLiveMap() {
        if (mMap == null || allTrees.isEmpty()) return;
        for (TreeForMap tree : allTrees) {
            float hue = getHueForSpecies(tree.getLatinName());
            com.google.android.gms.maps.model.MarkerOptions opts = new com.google.android.gms.maps.model.MarkerOptions()
                    .position(new LatLng(tree.getLatitude(), tree.getLongitude()))
                    .title(tree.getNameToUseOnMap())
                    .snippet(tree.getLabel())
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hue));
            com.google.android.gms.maps.model.Marker m = mMap.addMarker(opts);
            if (m != null) m.setTag(tree);
        }
    }

    private float getHueForSpecies(String latinName) {
        if (latinName == null) return 0f;
        Float h = speciesHueMap.get(latinName.toUpperCase(Locale.ROOT));
        return h != null ? h : 0f;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Map ready
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        
        addMarkersToLiveMap();

        if (getIntent().hasExtra("EXTRA_LAT") && getIntent().hasExtra("EXTRA_LNG")) {
            double lat = getIntent().getDoubleExtra("EXTRA_LAT", 0.0);
            double lng = getIntent().getDoubleExtra("EXTRA_LNG", 0.0);
            float zoom = getIntent().getFloatExtra("EXTRA_ZOOM", 18f);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), zoom));
        } else {
            zoomToAllTrees();
        }
    }

    private void zoomToAllTrees() {
        if (mMap == null || allTrees.isEmpty()) return;
        if (getIntent().hasExtra("EXTRA_LAT")) return; // respect passed coordinates
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (TreeForMap t : allTrees) b.include(new LatLng(t.getLatitude(), t.getLongitude()));
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 120));
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Species colour helpers (mirrors MapViewActivity)
    // ─────────────────────────────────────────────────────────────────────────

    private static final float[] SPECIES_HUES = {
        0f, 30f, 60f, 120f, 180f, 200f, 240f, 270f, 300f, 330f,
        15f, 45f, 90f, 160f, 210f
    };

    private void buildSpeciesColourMap(List<TreeForMap> trees) {
        speciesHueMap.clear();
        int idx = 0;
        for (TreeForMap tree : trees) {
            String key = tree.getLatinName() != null
                    ? tree.getLatinName().toUpperCase(Locale.ROOT) : "UNKNOWN";
            if (!speciesHueMap.containsKey(key)) {
                speciesHueMap.put(key, SPECIES_HUES[idx % SPECIES_HUES.length]);
                idx++;
            }
        }
    }

    private int colourForSpecies(String latinName) {
        if (latinName == null) return Color.RED;
        Float hue = speciesHueMap.get(latinName.toUpperCase(Locale.ROOT));
        if (hue == null) return Color.RED;
        float[] hsv = { hue, 0.85f, 0.90f };
        return Color.HSVToColor(hsv);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF Generation
    // ─────────────────────────────────────────────────────────────────────────

    private void startGeneration() {
        if (mMap == null) {
            Toast.makeText(this, "Map not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);
        tvStatus.setText("Fetching satellite image…");

        // Capture current map state on main thread
        double centerLat = mMap.getCameraPosition().target.latitude;
        double centerLng = mMap.getCameraPosition().target.longitude;
        float  zoom      = mMap.getCameraPosition().zoom;

        boolean isA2        = rgPaperSize.getCheckedRadioButtonId() == R.id.rb_a2;
        boolean isLandscape = rgOrientation.getCheckedRadioButtonId() == R.id.rb_landscape;

        executor.execute(() -> {
            try {
                // 1. Download satellite image from Maps Static API
                Bitmap satBitmap = downloadStaticMap(centerLat, centerLng, zoom);
                if (satBitmap == null) throw new Exception("Failed to download satellite image");

                post("Drawing tree markers…");

                // 2. Annotate: draw coloured dots + labels on the bitmap
                Bitmap annotated = annotateBitmap(satBitmap, centerLat, centerLng, zoom);

                // 3. Draw legend
                drawLegend(annotated);

                // 4. Build PDF
                post("Generating PDF…");
                int pageW, pageH;
                if (isA2) {
                    pageW = isLandscape ? A2_LONG : A2_SHORT;
                    pageH = isLandscape ? A2_SHORT : A2_LONG;
                } else {
                    pageW = isLandscape ? A3_LONG : A3_SHORT;
                    pageH = isLandscape ? A3_SHORT : A3_LONG;
                }

                PdfDocument pdfDoc = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageW, pageH, 1).create();
                PdfDocument.Page page = pdfDoc.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Scale annotated bitmap to fill the page
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                float scaleX = (float) pageW / annotated.getWidth();
                float scaleY = (float) pageH / annotated.getHeight();
                float scale  = Math.min(scaleX, scaleY); // maintain aspect ratio
                float offsetX = (pageW - annotated.getWidth()  * scale) / 2f;
                float offsetY = (pageH - annotated.getHeight() * scale) / 2f;
                matrix.setScale(scale, scale);
                matrix.postTranslate(offsetX, offsetY);

                Paint imgPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
                canvas.drawBitmap(annotated, matrix, imgPaint);

                pdfDoc.finishPage(page);

                // 5. Save & share
                String sizeName = isA2 ? "A2" : "A3";
                String orient   = isLandscape ? "landscape" : "portrait";
                File outputFile = new File(getCacheDir(),
                        "tree_map_" + sizeName + "_" + orient + ".pdf");
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    pdfDoc.writeTo(fos);
                }
                pdfDoc.close();

                new Handler(Looper.getMainLooper()).post(() -> sharePdf(outputFile));

            } catch (Exception e) {
                Log.e(TAG, "PDF generation failed", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    tvStatus.setText("Error: " + e.getMessage());
                    btnGenerate.setEnabled(true);
                });
            }
        });
    }

    private void post(String message) {
        new Handler(Looper.getMainLooper()).post(() -> tvStatus.setText(message));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Maps Static API download
    // ─────────────────────────────────────────────────────────────────────────

    private Bitmap downloadStaticMap(double lat, double lng, float zoom) {
        // Clamp zoom to integer (Static API accepts integer zoom 0-21)
        int zoomInt = Math.round(zoom);

        String urlStr = String.format(Locale.US,
                "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=%.7f,%.7f" +
                "&zoom=%d" +
                "&size=%dx%d" +
                "&scale=%d" +
                "&maptype=hybrid" +
                "&key=%s",
                lat, lng, zoomInt,
                STATIC_LOGICAL_SIZE, STATIC_LOGICAL_SIZE,
                STATIC_SCALE,
                MAPS_API_KEY);

        Log.d(TAG, "Static map URL: " + urlStr);

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(20_000);
            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Static map HTTP error: " + conn.getResponseCode());
                return null;
            }

            try (InputStream is = conn.getInputStream()) {
                return android.graphics.BitmapFactory.decodeStream(is);
            }
        } catch (Exception e) {
            Log.e(TAG, "downloadStaticMap failed", e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Annotate bitmap with coloured dots + labels
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Draws a coloured dot and label text next to each tree on the bitmap.
     * Returns a mutable copy.
     */
    private Bitmap annotateBitmap(Bitmap src, double centerLat, double centerLng, float zoom) {
        Bitmap mutable = src.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutable);
        int imgSize = mutable.getWidth(); // 1280

        // Paints
        Paint dotPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(1.0f);

        float dotRadius = 4f; // Small dots matching legend size

        for (TreeForMap tree : allTrees) {
            int[] px = latLngToPixel(
                    tree.getLatitude(), tree.getLongitude(),
                    centerLat, centerLng, zoom, imgSize);
            if (px == null) continue;

            int x = px[0], y = px[1];

            // Skip if outside bitmap
            if (x < 0 || x >= imgSize || y < 0 || y >= imgSize) continue;

            // Dot only (no text labels beside trees per user request)
            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(colourForSpecies(tree.getLatinName()));
            canvas.drawCircle(x, y, dotRadius, dotPaint);
            canvas.drawCircle(x, y, dotRadius, borderPaint);
        }

        return mutable;
    }

    /**
     * Converts a lat/lng to a pixel position in the static map image.
     *
     * Uses the Web Mercator projection formula consistent with Maps Static API.
     * With size=640&scale=2 the image is 1280px but the coordinate system
     * uses 640 logical pixels, then we multiply by scale.
     *
     * Returns null if the position would be outside the image bounds.
     */
    private int[] latLngToPixel(double lat, double lng,
                                double centerLat, double centerLng,
                                float zoom, int imageSize) {
        // World size at this zoom level (in world pixels where a tile = 256 world px)
        double worldSize = 256.0 * Math.pow(2, zoom);

        double cx = lngToWorldX(centerLng, worldSize);
        double cy = latToWorldY(centerLat, worldSize);
        double tx = lngToWorldX(lng, worldSize);
        double ty = latToWorldY(lat, worldSize);

        // Logical pixel offset from image centre
        double dxLogical = tx - cx;
        double dyLogical = ty - cy;

        // Scale to physical pixels (scale=2 → multiply logical by 2)
        int px = (int) Math.round(imageSize / 2.0 + dxLogical * STATIC_SCALE);
        int py = (int) Math.round(imageSize / 2.0 + dyLogical * STATIC_SCALE);

        return new int[]{ px, py };
    }

    private double lngToWorldX(double lng, double worldSize) {
        return (lng + 180.0) / 360.0 * worldSize;
    }

    private double latToWorldY(double lat, double worldSize) {
        double sinLat = Math.sin(Math.toRadians(lat));
        sinLat = Math.max(-0.9999, Math.min(0.9999, sinLat)); // clamp
        return (0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)) * worldSize;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Legend box (bottom-left of the bitmap)
    // ─────────────────────────────────────────────────────────────────────────

    private void drawLegend(Bitmap bitmap) {
        Canvas canvas = new Canvas(bitmap);

        // Collect unique species
        List<String[]> entries = new ArrayList<>(); // { displayName, latinName }
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (TreeForMap tree : allTrees) {
            String key = tree.getLatinName() != null
                    ? tree.getLatinName().toUpperCase(Locale.ROOT) : "UNKNOWN";
            if (seen.add(key)) {
                String display = (tree.getEnglishName() != null && !tree.getEnglishName().isEmpty())
                        ? tree.getEnglishName() : tree.getLatinName();
                entries.add(new String[]{ display, tree.getLatinName() });
            }
        }

        if (entries.isEmpty()) return;

        // Reduced legend scale (~25% of previous dimensions for compact fit)
        float padding   = 6f;
        float dotR      = 3.5f;
        float textSize  = 8f;
        float rowH      = textSize + 4f;
        float titleSize = 9f;

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.argb(210, 255, 255, 255));

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(titleSize);
        titlePaint.setFakeBoldText(true);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(textSize);

        Paint dotPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.argb(120, 0, 0, 0));
        borderPaint.setStrokeWidth(1.0f);

        // Measure max label width
        float maxW = titlePaint.measureText("Species Key");
        for (String[] e : entries) {
            float w = textPaint.measureText(e[0]);
            if (w > maxW) maxW = w;
        }

        float boxW = padding + dotR * 2 + 5f + maxW + padding;
        float boxH = padding + titleSize + 3f + rowH * entries.size() + padding;

        float left   = 12f;
        float bottom = bitmap.getHeight() - 12f;
        float top    = bottom - boxH;
        float right  = left + boxW;

        // Background
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 5f, 5f, bgPaint);

        // Title
        canvas.drawText("Species Key", left + padding, top + padding + titleSize, titlePaint);

        // Entries
        float y = top + padding + titleSize + 3f + rowH * 0.85f;
        for (String[] entry : entries) {
            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(colourForSpecies(entry[1]));
            float cx = left + padding + dotR;
            canvas.drawCircle(cx, y - dotR * 0.3f, dotR, dotPaint);
            canvas.drawCircle(cx, y - dotR * 0.3f, dotR, borderPaint);
            canvas.drawText(entry[0], left + padding + dotR * 2 + 5f, y, textPaint);
            y += rowH;
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Share PDF via FileProvider
    // ─────────────────────────────────────────────────────────────────────────

    private void sharePdf(File pdfFile) {
        tvStatus.setText("PDF ready — sharing…");
        btnGenerate.setEnabled(true);

        Uri contentUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", pdfFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Save or Share Tree Map PDF"));
    }
}
