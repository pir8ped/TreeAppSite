package db;

import com.john.TreeApp.beans.Scion;
import com.john.TreeApp.beans.ScionGroup;

import java.util.List;

public interface ScionDAO {
    /**
     * Add a new scion to the database
     * 
     * @param scion The scion to add
     * @return The auto-generated scion ID, or -1 if the operation failed
     */
    long addScion(Scion scion);

    /**
     * Get a scion by its ID
     * 
     * @param scionId The scion ID
     * @return The scion, or null if not found
     */
    Scion getScion(int scionId);

    /**
     * Get all scions from the database
     * 
     * @return List of all scions
     */
    List<Scion> getAllScions();

    /**
     * Update an existing scion
     * 
     * @param scion The scion with updated information
     * @return true if update was successful, false otherwise
     */
    boolean updateScion(Scion scion);

    /**
     * Delete a scion from the database
     * 
     * @param scionId The ID of the scion to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteScion(int scionId);

    /**
     * Find scions by species name
     * 
     * @param species The species name to search for
     * @return List of scions with matching species
     */
    List<Scion> getScionsBySpecies(String species);

    /**
     * Get all scions that are not currently attached to any tree
     * 
     * @return List of unattached scions
     */
    List<Scion> getUnattachedScions();

    /**
     * Get grouped unattached scions with counts
     * Groups scions by species/variety/source and returns count of each type
     * 
     * @return List of ScionGroup objects with counts
     */
    List<ScionGroup> getUnattachedScionsGrouped();
}
