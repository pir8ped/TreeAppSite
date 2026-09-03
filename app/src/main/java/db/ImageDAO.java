package db;


import com.john.TreeApp.beans.Image;

import java.sql.Date;
import java.util.List;

public interface ImageDAO {
    // Retrieves all images for a specific tree
    List<Image> getAllImages(long treeId);

    // Adds a new image to the database
    void addImage(long treeId, Date dateTaken, String imageUrlOrFileName);

    // Deletes an image from the database
    void deleteImage(int imageID);
}
