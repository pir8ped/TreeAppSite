package db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.john.TreeApp.beans.Collection;
import com.john.TreeApp.beans.Location;
import com.john.TreeApp.beans.Note;
import com.john.TreeApp.beans.Tree;

import java.util.Date;
import java.util.List;

public class TreeService extends DAOBase {

    private static final String TAG = "TreeService";
    private final LocationDAO locationDAO;
    private final TreeDAO treeDAO;
    private final NoteDAO noteDAO;
    private final CollectionDAO collectionDAO;

    /**
     * Constructor with injected DAOs.
     * This is the preferred constructor that avoids direct DAO instantiation.
     */
    private final ScionDAO scionDAO;
    private final TreeScionDAO treeScionDAO;

    /**
     * Constructor with injected DAOs.
     * This is the preferred constructor that avoids direct DAO instantiation.
     */
    public TreeService(TreeDAO treeDAO, LocationDAO locationDAO, NoteDAO noteDAO, CollectionDAO collectionDAO,
            ScionDAO scionDAO, TreeScionDAO treeScionDAO) {
        this.treeDAO = treeDAO;
        this.locationDAO = locationDAO;
        this.noteDAO = noteDAO;
        this.collectionDAO = collectionDAO;
        this.scionDAO = scionDAO;
        this.treeScionDAO = treeScionDAO;
        Log.d(TAG, "TreeService initialized with injected DAOs");
    }

    /**
     * Constructor that injects DAOs from DatabaseCreator.
     * This is the preferred constructor when DAOs aren't passed directly.
     */
    public TreeService() {
        // Get DAOs from DatabaseCreator
        DatabaseCreator dbCreator = DatabaseCreator.getInstance();
        this.treeDAO = dbCreator.getTreeDAO();
        this.locationDAO = dbCreator.getLocationDAO();
        this.noteDAO = dbCreator.getNoteDAO();
        this.collectionDAO = dbCreator.getCollectionDAO();
        this.scionDAO = dbCreator.getScionDAO();
        this.treeScionDAO = dbCreator.getTreeScionDAO();
        Log.d(TAG, "TreeService initialized with DAOs from DatabaseCreator");
    }

    /**
     * Constructor that accepts a database instance.
     * 
     * @deprecated Use the default constructor or the constructor with injected DAOs
     *             instead.
     */
    public TreeService(SQLiteDatabase db) {
        super(db);
        // Get DAOs from DatabaseCreator

        this.treeDAO = DatabaseCreator.getInstance().getTreeDAO();
        this.locationDAO = DatabaseCreator.getInstance().getLocationDAO();
        this.noteDAO = DatabaseCreator.getInstance().getNoteDAO();
        this.collectionDAO = DatabaseCreator.getInstance().getCollectionDAO();
        this.scionDAO = DatabaseCreator.getInstance().getScionDAO();
        this.treeScionDAO = DatabaseCreator.getInstance().getTreeScionDAO();
        Log.d(TAG, "TreeService initialized with DAOs from DatabaseCreator (db provided)");
    }

    /**
     * Finds a tree with the given latinName that has no location, then updates it
     * with the given location and label.
     */

    /**
     * Adds a new tree and assigns it a location.
     */
    /**
     * Adds a tree to the database, including its location and optional notes.
     * This method handles the transaction and ensures the tree is associated with
     * the correct collection.
     *
     * @param tree     The tree object to add.
     * @param location The location of the tree.
     * @param notes    Optional notes about the tree.
     * @return The ID of the newly added tree, or -1 if the operation failed.
     */
    public int addTree(Tree tree, Location location, String notes) {
        Log.d(TAG, "Starting addTree process");

        // Determine collection ID
        int collectionId = tree.getCollectionId() != null ? tree.getCollectionId()
                : collectionDAO.getSelectedCollectionId();
        Log.d(TAG, "Using Collection ID: " + collectionId);

        if (collectionId == -1) {
            Log.e(TAG, "No collection selected or provided");
            return -1;
        }

        int treeId = -1;
        try {
            getDatabase().beginTransaction();
            Log.d(TAG, "Transaction started");

            // Insert location and get its ID
            int locationId = (int) locationDAO.insertLocation(location);
            Log.d(TAG, "Location inserted with ID: " + locationId);

            if (locationId == -1) {
                throw new Exception("Failed to insert location.");
            }

            // Assign location and collection to tree and insert it
            tree.setLocationId(locationId);
            tree.setCollectionId(collectionId);

            // Use the consolidated addTree method from TreeDAO
            treeId = (int) treeDAO.addTree(tree);
            Log.d(TAG, "Tree inserted with ID: " + treeId);

            if (treeId == -1) {
                throw new Exception("Failed to insert tree.");
            }

            // Create and add note if notes exist
            if (notes != null && !notes.trim().isEmpty()) {
                Note note = new Note();
                note.setDescription(notes);
                note.setTreeId(treeId);
                note.setDateWritten(new Date());
                noteDAO.addNote(note);
                Log.d(TAG, "Note added for tree ID: " + treeId);
            }

            getDatabase().setTransactionSuccessful();
            Log.d(TAG, "Transaction completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error adding tree: " + e.getMessage());
            e.printStackTrace();
            treeId = -1; // Ensure we return failure on exception
        } finally {
            getDatabase().endTransaction();
            Log.d(TAG, "Transaction ended");
        }
        return treeId;
    }

    /**
     * Checks if a label is unique for adding a new tree in the current collection.
     *
     * @param label The label to check.
     * @return True if the label is unique, false otherwise.
     */
    public boolean isLabelUniqueForAdd(String label) {
        int collectionId = collectionDAO.getSelectedCollectionId();
        return treeDAO.isLabelUniqueForAdd(label, collectionId);
    }

    /**
     * Checks if a label is unique for updating an existing tree in the current
     * collection.
     *
     * @param label  The label to check.
     * @param treeId The ID of the tree being updated (to exclude from the check).
     * @return True if the label is unique, false otherwise.
     */
    public boolean isLabelUniqueForUpdate(String label, int treeId) {
        int collectionId = collectionDAO.getSelectedCollectionId();
        return treeDAO.isLabelUniqueForUpdate(label, treeId, collectionId);
    }

    /**
     * Generates a unique label for a tree based on its Latin name.
     * The format will be "LatinName 1", "LatinName 2", etc.
     *
     * @param latinName The Latin name of the tree.
     * @return A unique label.
     */
    public String generateUniqueLabel(String latinName) {
        int collectionId = collectionDAO.getSelectedCollectionId();
        int counter = 1;
        String label;
        do {
            label = latinName + " " + counter;
            counter++;
        } while (!treeDAO.isLabelUniqueForAdd(label, collectionId));
        return label;
    }

    public String updateTreeWithLocationAndLabel(String latinName, int collectionId, Location location, String label) {
        return treeDAO.updateTreeWithLocationAndLabel(latinName, collectionId, location, label);
    }

    public String updateTreeWithLocationAndLabel(String latinName, int collectionId, Location location, String label,
            String origin) {
        return treeDAO.updateTreeWithLocationAndLabel(latinName, collectionId, location, label, origin);
    }

    public String updateTreeWithLocationAndLabel(int treeId, int collectionId, Location location, String label) {
        try {
            getDatabase().beginTransaction();
            // Insert location and get its ID
            int locationId = (int) locationDAO.insertLocation(location);
            if (locationId == -1) {
                throw new Exception("Failed to insert location.");
            }

            String result = treeDAO.updateTreeWithLocationAndLabel(treeId, collectionId, locationId, label);
            getDatabase().setTransactionSuccessful();
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error updating tree location: " + e.getMessage());
            return "Error updating tree location.";
        } finally {
            getDatabase().endTransaction();
        }
    }

    /**
     * Discards a scion by removing it from the tree and deleting it from the
     * system.
     *
     * @param treeId  The ID of the tree the scion is attached to.
     * @param scionId The ID of the scion to delete.
     * @return True if the scion was successfully deleted, false otherwise.
     */
    public boolean discardScion(int treeId, int scionId) {
        Log.d(TAG, "Starting discard of scion " + scionId + " from tree " + treeId);

        try {
            // 1. Remove association from TreeScion table
            Log.d(TAG, "Removing scion-tree association...");
            boolean associationRemoved = treeScionDAO.removeScionFromTree(treeId, scionId);
            Log.d(TAG, "Association removal result: " + associationRemoved);

            // 2. Delete the scion from Scion table
            Log.d(TAG, "Deleting scion from Scion table...");
            boolean scionDeleted = scionDAO.deleteScion(scionId);
            Log.d(TAG, "Scion deletion result: " + scionDeleted);

            if (scionDeleted) {
                Log.i(TAG, "Scion " + scionId + " successfully discarded from tree " + treeId);
                return true;
            } else {
                Log.e(TAG, "Failed to delete scion " + scionId + " from Scion table");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error discarding scion " + scionId + ": " + e.getMessage(), e);
            return false;
        }
    }

}
