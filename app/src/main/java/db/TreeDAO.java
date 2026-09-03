package db;

import com.john.TreeApp.beans.Tree;
import com.john.TreeApp.beans.TreeStatistics;
import com.john.TreeApp.beans.utilBean.TreeForMap;
import com.john.TreeApp.beans.utilBean.TreeGroup;

import java.util.List;

public interface TreeDAO {
    TreeStatistics getTreeStatistics(int collectionId);
        boolean updateTree(Tree tree);

        boolean updateTreeStatus(long treeId, String status);

        List<TreeForMap> getTreesForMap(int collectionId);

        List<TreeForMap> getTreesForMapAllCollections();

        boolean updateTreeLocationId(long treeId, int locationId);

        String updateTreeWithLocationAndLabel(long treeId, int collectionId, int locationId, String label);

        boolean isLabelUniqueForAdd(String label, int collectionId);

        long addTree(Tree tree);

        int addTreesToPlant(Tree tree, int quantity);

        Tree getTree(int treeId);

        List<Tree> getAllTrees();

        List<Tree> getAllTreesReadyToPlant();

        boolean deleteTree(long treeId);

        Tree findATree_fromLabel(String label);
        Tree findATree_fromLabel(String label, int collectionId);

        Tree findATree_fromId(int treeId);

        List<Tree> findAllTrees_Latin(String speciesLatinName, String[] collectionNames);

        List<Tree> findAllTrees_Latin(String speciesLatinName);

        List<Tree> findAllTrees_Located(String located);

        List<Tree> findAllTrees_English(String speciesEnglishName, String[] collectionNames);

        List<Tree> findAllTrees(String speciesEnglishName);

        boolean deleteOneTreeToPlant(String speciesLatinName);

        String deleteTree(String label);

        List<Tree> findAllTreesOnMap(double maxLat, double minLat, double maxLong, double minLong);

        List<Tree> findAllTreesOnMap_bySpeciesLatin(double maxLat, double minLat, double maxLong, double minLong,
                        String speciesLatinName);

        List<Tree> findAllTreesOnMap_bySpeciesEnglish(double maxLat, double minLat, double maxLong, double minLong,
                        String speciesEnglishName);

        String updateTreeWithLocationAndLabel(String latinName, int collectionId,
                        com.john.TreeApp.beans.Location location,
                        String label);

        String updateTreeWithLocationAndLabel(String latinName, int collectionId,
                        com.john.TreeApp.beans.Location location,
                        String label, String origin);

        List<Tree> getAllTreesInACollection(int collectionId);

        int areUnplantedTrees(String latinName);

        List<TreeGroup> getTreesWithoutLocationIdGrouped();

        List<TreeGroup> listTreesInACollectionBySpecies(int collectionId);

        boolean isLabelUniqueForUpdate(String label, long treeIdToExclude, int collectionId);

        List<Tree> getTreesWithoutCollection();

        boolean updateBatchLabels(List<Integer> treeIds, String label);
}
