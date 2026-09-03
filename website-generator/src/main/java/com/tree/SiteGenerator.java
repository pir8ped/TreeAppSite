package com.tree;

import com.google.gson.Gson;
import com.tree.beans.Collection;
import com.tree.beans.Image;
import com.tree.beans.Location;
import com.tree.beans.Note;
import com.tree.beans.Scion;
import com.tree.beans.Tree;
import com.tree.beans.TreeForMap;
import com.tree.beans.TreeSpecies;
import com.tree.beans.TreeStatistics;
import com.tree.db.WebTreeDAO;
import com.tree.db.WebTreeDAOImpl;
import com.tree.util.ZipUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SiteGenerator {

    private static final float[] SPECIES_HUES = {
            0f, 30f, 60f, 120f, 180f, 200f, 240f, 270f, 300f, 330f,
            15f, 45f, 90f, 160f, 210f
    };

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   Tree Website Static Site Generator (v1.0.0)    ");
        System.out.println("==================================================");

        try {
            File dataDir = new File("data");
            File outputDir = new File("dist");
            File tempDir = new File("build/temp_data");

            if (outputDir.exists()) {
                deleteDirectory(outputDir);
            }
            outputDir.mkdirs();

            // 1. Locate and prepare database and photos
            File dbFile = null;
            File imagesSourceDir = null;

            // Check if there is a ZIP file in data/
            File[] zipFiles = dataDir.exists() ? dataDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip")) : null;
            if (zipFiles != null && zipFiles.length > 0) {
                File zipFile = zipFiles[0];
                System.out.println("Found data archive: " + zipFile.getName());
                if (tempDir.exists()) deleteDirectory(tempDir);
                tempDir.mkdirs();
                System.out.println("Extracting archive to " + tempDir.getAbsolutePath() + "...");
                ZipUtils.unzip(zipFile, tempDir);

                // Look for database inside unzipped files
                File[] foundDb = tempDir.listFiles((dir, name) -> name.endsWith(".db"));
                if (foundDb != null && foundDb.length > 0) {
                    dbFile = foundDb[0];
                }
                File imgDir = new File(tempDir, "images");
                if (!imgDir.exists()) imgDir = new File(tempDir, "Trees");
                if (imgDir.exists()) imagesSourceDir = imgDir;
            } else {
                // Check if uncompressed database exists in data/ or root
                if (new File(dataDir, "database.db").exists()) {
                    dbFile = new File(dataDir, "database.db");
                } else if (new File(dataDir, "mydatabase.db").exists()) {
                    dbFile = new File(dataDir, "mydatabase.db");
                } else if (new File("TreeDatabase.db").exists()) {
                    dbFile = new File("TreeDatabase.db");
                }
                File imgDir = new File(dataDir, "images");
                if (!imgDir.exists()) imgDir = new File(dataDir, "Trees");
                if (imgDir.exists()) imagesSourceDir = imgDir;
            }

            if (dbFile == null || !dbFile.exists()) {
                System.err.println("Error: No SQLite database file found! Please provide a ZIP or .db file in the 'data/' folder.");
                return;
            }

            System.out.println("Using database: " + dbFile.getAbsolutePath());

            // 2. Connect to Database DAO
            WebTreeDAO dao = new WebTreeDAOImpl(dbFile.getAbsolutePath());

            // 3. Initialize Thymeleaf Template Engine
            TemplateEngine templateEngine = createTemplateEngine();
            Gson gson = new Gson();

            // Load data
            List<Collection> collections = dao.getAllCollections();
            TreeStatistics globalStats = dao.getStatistics();
            List<TreeForMap> allMapTrees = dao.getAllTreesForMap();
            List<TreeSpecies> allSpecies = dao.getAllSpecies();

            // Assign species colors
            Map<String, String> speciesColorMap = buildSpeciesColorMap(allMapTrees);
            for (TreeForMap tree : allMapTrees) {
                String key = tree.getLatinName() != null ? tree.getLatinName().toUpperCase(Locale.ROOT) : "UNKNOWN";
                tree.setColorHex(speciesColorMap.getOrDefault(key, "#4CAF50"));
            }

            System.out.println("Loaded " + collections.size() + " collections, " + allMapTrees.size() + " planted trees, " + allSpecies.size() + " species.");

            // 4. Generate Dashboard (index.html)
            System.out.println("Generating index.html...");
            Context indexContext = new Context();
            indexContext.setVariable("collections", collections);
            indexContext.setVariable("stats", globalStats);
            indexContext.setVariable("treesJson", gson.toJson(allMapTrees));
            indexContext.setVariable("speciesColors", speciesColorMap);
            indexContext.setVariable("pageTitle", "Tree Collection Dashboard");
            renderTemplate(templateEngine, "index", indexContext, new File(outputDir, "index.html"));

            // 5. Generate Collection Pages (collection_<id>.html)
            for (Collection col : collections) {
                System.out.println("Generating collection_" + col.getId() + ".html (" + col.getName() + ")...");
                List<Tree> collectionTrees = dao.getTreesForCollection(col.getId());
                List<TreeForMap> collectionMapTrees = dao.getTreesForMap(col.getId());
                for (TreeForMap tfm : collectionMapTrees) {
                    String key = tfm.getLatinName() != null ? tfm.getLatinName().toUpperCase(Locale.ROOT) : "UNKNOWN";
                    tfm.setColorHex(speciesColorMap.getOrDefault(key, "#4CAF50"));
                }
                TreeStatistics colStats = dao.getCollectionStatistics(col.getId());

                Context colContext = new Context();
                colContext.setVariable("collection", col);
                colContext.setVariable("collections", collections);
                colContext.setVariable("trees", collectionTrees);
                colContext.setVariable("treesJson", gson.toJson(collectionMapTrees));
                colContext.setVariable("stats", colStats);
                colContext.setVariable("speciesColors", speciesColorMap);
                colContext.setVariable("pageTitle", col.getName() + " - Tree Collection");
                renderTemplate(templateEngine, "collection", colContext, new File(outputDir, "collection_" + col.getId() + ".html"));
            }

            // 6. Generate Individual Tree Pages (trees/tree_<id>.html)
            File treesDir = new File(outputDir, "trees");
            treesDir.mkdirs();

            List<Tree> allTrees = dao.getAllPlantedTrees();
            for (Tree tree : allTrees) {
                Tree fullTree = dao.getTreeById(tree.getTreeId());
                if (fullTree != null) {
                    Context treeContext = new Context();
                    treeContext.setVariable("tree", fullTree);
                    treeContext.setVariable("collections", collections);
                    treeContext.setVariable("pageTitle", fullTree.getDisplayName() + " (" + fullTree.getLabel() + ")");

                    // Mini map JSON
                    if (fullTree.getLocation() != null) {
                        TreeForMap miniMapTree = new TreeForMap();
                        miniMapTree.setId(fullTree.getTreeId());
                        miniMapTree.setLabel(fullTree.getLabel());
                        miniMapTree.setEnglishName(fullTree.getEnglishName());
                        miniMapTree.setLatinName(fullTree.getLatinName());
                        miniMapTree.setVariety(fullTree.getVariety());
                        miniMapTree.setLatitude(fullTree.getLocation().getLatitude());
                        miniMapTree.setLongitude(fullTree.getLocation().getLongitude());
                        String key = fullTree.getLatinName() != null ? fullTree.getLatinName().toUpperCase(Locale.ROOT) : "UNKNOWN";
                        miniMapTree.setColorHex(speciesColorMap.getOrDefault(key, "#4CAF50"));
                        treeContext.setVariable("treeJson", gson.toJson(Collections.singletonList(miniMapTree)));
                    }

                    renderTemplate(templateEngine, "tree_detail", treeContext, new File(treesDir, "tree_" + fullTree.getTreeId() + ".html"));
                }
            }
            System.out.println("Generated " + allTrees.size() + " tree detail pages in /trees/");

            // 7. Generate Species Page (species.html)
            System.out.println("Generating species.html...");
            Context speciesContext = new Context();
            speciesContext.setVariable("speciesList", allSpecies);
            speciesContext.setVariable("collections", collections);
            speciesContext.setVariable("pageTitle", "Species Reference & Fruiting Calendar");
            renderTemplate(templateEngine, "species", speciesContext, new File(outputDir, "species.html"));

            // 8. Copy Images
            File outputImagesDir = new File(outputDir, "images");
            outputImagesDir.mkdirs();
            if (imagesSourceDir != null && imagesSourceDir.exists()) {
                System.out.println("Copying photos from " + imagesSourceDir.getAbsolutePath() + " to " + outputImagesDir.getAbsolutePath() + "...");
                ZipUtils.copyDirectory(imagesSourceDir, outputImagesDir);
            }

            // 9. Copy Static CSS/JS assets from resources
            copyStaticAssets(outputDir);

            System.out.println("==================================================");
            System.out.println("   SUCCESS! Website generated in: " + outputDir.getAbsolutePath());
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("Fatal error generating website: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static void renderTemplate(TemplateEngine engine, String templateName, Context context, File outputFile) throws IOException {
        String html = engine.process(templateName, context);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            writer.write(html);
        }
    }

    private static Map<String, String> buildSpeciesColorMap(List<TreeForMap> trees) {
        Map<String, String> map = new LinkedHashMap<>();
        int idx = 0;
        for (TreeForMap t : trees) {
            String key = t.getLatinName() != null ? t.getLatinName().toUpperCase(Locale.ROOT) : "UNKNOWN";
            if (!map.containsKey(key)) {
                float hue = SPECIES_HUES[idx % SPECIES_HUES.length];
                map.put(key, hueToHex(hue));
                idx++;
            }
        }
        return map;
    }

    private static String hueToHex(float hue) {
        float s = 0.85f;
        float v = 0.90f;
        float c = v * s;
        float x = c * (1 - Math.abs((hue / 60f) % 2 - 1));
        float m = v - c;
        float r = 0, g = 0, b = 0;
        if (hue >= 0 && hue < 60) { r = c; g = x; b = 0; }
        else if (hue >= 60 && hue < 120) { r = x; g = c; b = 0; }
        else if (hue >= 120 && hue < 180) { r = 0; g = c; b = x; }
        else if (hue >= 180 && hue < 240) { r = 0; g = x; b = c; }
        else if (hue >= 240 && hue < 300) { r = x; g = 0; b = c; }
        else if (hue >= 300 && hue < 360) { r = c; g = 0; b = x; }
        int red = Math.round((r + m) * 255);
        int green = Math.round((g + m) * 255);
        int blue = Math.round((b + m) * 255);
        return String.format("#%02x%02x%02x", red, green, blue);
    }

    private static void copyStaticAssets(File outputDir) throws IOException {
        File cssDir = new File(outputDir, "css");
        cssDir.mkdirs();

        // Copy style.css from resources
        try (InputStream is = SiteGenerator.class.getResourceAsStream("/static/css/style.css")) {
            if (is != null) {
                File targetCss = new File(cssDir, "style.css");
                try (FileOutputStream fos = new FileOutputStream(targetCss)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
