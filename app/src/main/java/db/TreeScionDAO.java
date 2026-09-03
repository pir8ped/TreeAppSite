package db;

import com.john.TreeApp.beans.Scion;
import com.john.TreeApp.beans.Tree;

import java.util.List;

public interface TreeScionDAO {
    /**
     * Associate a scion with a tree
     * 
     * @param treeId  The ID of the tree
     * @param scionId The ID of the scion
     * @return true if association was created successfully, false otherwise
     */
    boolean addScionToTree(int treeId, int scionId);

    /**
     * Remove the association between a tree and a scion
     * 
     * @param treeId  The ID of the tree
     * @param scionId The ID of the scion
     * @return true if association was removed successfully, false otherwise
     */
    boolean removeScionFromTree(int treeId, int scionId);

    /**
     * Get all scions associated with a specific tree
     * 
     * @param treeId The ID of the tree
     * @return List of scions associated with the tree
     */
    List<Scion> getScionsForTree(int treeId);

    /**
     * Get all scions associated with a specific tree, grouped by type
     * 
     * @param treeId The ID of the tree
     * @return List of scion groups associated with the tree
     */
    List<com.john.TreeApp.beans.ScionGroup> getScionsForTreeGrouped(int treeId);

    /**
     * Get all trees that have a specific scion
     * 
     * @param scionId The ID of the scion
     * @return List of trees that have this scion
     */
    List<Tree> getTreesForScion(int scionId);

    /**
     * Check if an association exists between a tree and a scion
     * 
     * @param treeId  The ID of the tree
     * @param scionId The ID of the scion
     * @return true if association exists, false otherwise
     */
    boolean isTreeScionAssociationExists(int treeId, int scionId);
}
