package db;

import com.john.TreeApp.beans.Collection;

import java.util.List;

public interface CollectionDAO {
    int addCollection(Collection collection);

    // Get all collections
    List<Collection> getAllCollections();

    // Update collection's selected status
    void setSelectedCollectionId(int collectionId);

    Collection getCollection(long id);

    boolean deleteCollection(int collectionId);

    boolean moveTreesToCollection(int fromCollectionId, int toCollectionId);

    /**
     * Get the ID of the currently selected collection
     * @return The ID of the selected collection, or -1 if none is selected
     */
    int getSelectedCollectionId();


}
