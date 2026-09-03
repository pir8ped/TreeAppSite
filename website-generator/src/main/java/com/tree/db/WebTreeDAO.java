package com.tree.db;

import com.tree.beans.*;

import java.util.List;

public interface WebTreeDAO {
    List<Collection> getAllCollections();
    Collection getCollectionById(int collectionId);
    TreeStatistics getStatistics();
    TreeStatistics getCollectionStatistics(int collectionId);
    List<Tree> getAllPlantedTrees();
    List<Tree> getTreesForCollection(int collectionId);
    Tree getTreeById(int treeId);
    List<TreeForMap> getAllTreesForMap();
    List<TreeForMap> getTreesForMap(int collectionId);
    List<Image> getImagesForTree(int treeId);
    List<Note> getNotesForTree(int treeId);
    List<Scion> getScionsForTree(int treeId);
    List<TreeSpecies> getAllSpecies();
    TreeSpecies getSpeciesByLatinName(String latinName);
}
