package com.john.TreeApp.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.john.TreeApp.beans.Image;
import com.john.TreeApp.beans.Note;
import com.john.TreeApp.beans.Tree;
import db.ImageDAO;
import db.ImageDAOImpl;
import db.NoteDAO;
import db.NoteDAOImpl;
import db.TreeDAO;
import db.TreeDAOImpl;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CollectionExporter {
    private static final String TAG = "CollectionExporter";
    private final Context context;
    private final TreeDAO treeDAO;
    private final ImageDAO imageDAO;
    private final NoteDAO noteDAO;

    public CollectionExporter(Context context) {
        this.context = context;
        this.treeDAO = new TreeDAOImpl();
        this.imageDAO = new ImageDAOImpl();
        this.noteDAO = new NoteDAOImpl();
    }

    public void cleanupOldExports() {
        File cacheDir = context.getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("Collection_Backup_") && name.endsWith(".zip"));
        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    Log.d(TAG, "Deleted old export: " + file.getName());
                }
            }
        }
    }

    public File exportCollectionPhotos(int collectionId, String collectionName) throws Exception {
        File zipFile = new File(context.getCacheDir(), "Collection_Backup_" + collectionName.replaceAll("[^a-zA-Z0-9]", "_") + ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            List<Tree> trees = treeDAO.getAllTrees(); // This is for the selected collection in TreeDAOImpl
            // If treeDAO.getAllTrees() doesn't filter by collectionId properly or if we want to be explicit:
            // For now, TreeDAOImpl.getAllTrees() uses collectionDAO.getSelectedCollectionId().
            
            JSONObject exportMetadata = new JSONObject();
            exportMetadata.put("collectionId", collectionId);
            exportMetadata.put("collectionName", collectionName);
            
            JSONArray treesArray = new JSONArray();

            for (Tree tree : trees) {
                if (tree.getCollectionId() != collectionId) continue;

                JSONObject treeJson = new JSONObject();
                treeJson.put("treeId", tree.getTreeId());
                treeJson.put("label", tree.getLabel());
                treeJson.put("species", tree.getEnglishName());
                treeJson.put("latinName", tree.getLatinName());
                treeJson.put("variety", tree.getVariety());

                JSONArray imagesArray = new JSONArray();
                List<Image> images = imageDAO.getAllImages(tree.getTreeId());
                
                for (Image image : images) {
                    JSONObject imageJson = new JSONObject();
                    imageJson.put("imageId", image.getImageId());
                    imageJson.put("filename", image.getImageUrlOrFileName());
                    
                    Note caption = noteDAO.getNoteForImage(image.getImageId());
                    if (caption != null) {
                        imageJson.put("caption", caption.getDescription());
                    }

                    // Add image file to ZIP
                    File imageFile = new File(
                        new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Trees"),
                        image.getImageUrlOrFileName()
                    );

                    if (imageFile.exists()) {
                        String zipEntryPath = "photos/" + (tree.getLabel() != null ? tree.getLabel() : "Tree_" + tree.getTreeId()) + "/" + image.getImageUrlOrFileName();
                        addImageToZip(zos, imageFile, zipEntryPath);
                        imagesArray.put(imageJson);
                    }
                }
                treeJson.put("photos", imagesArray);
                treesArray.put(treeJson);
            }

            exportMetadata.put("trees", treesArray);

            // Add metadata JSON to ZIP
            addTextToZip(zos, "collection_info.json", exportMetadata.toString(4));

            zos.finish();
        }

        return zipFile;
    }

    private void addImageToZip(ZipOutputStream zos, File file, String entryPath) throws IOException {
        ZipEntry entry = new ZipEntry(entryPath);
        zos.putNextEntry(entry);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
    }

    private void addTextToZip(ZipOutputStream zos, String filename, String content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content.getBytes());
        zos.closeEntry();
    }
}
