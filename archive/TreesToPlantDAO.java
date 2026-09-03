package db;

import com.john.TreeApp.beans.utilBean.TreeToPlant;

import java.util.List;

public interface TreesToPlantDAO {
    int addTrees(String latinName, String label, String variety, String rootstock, String origin, String location, int quantity);

    List<TreeToPlant> getAllTrees();

    void setQuantity(int id, int quantity);

    void decrementQuantity(int id);

    void remove(int id);

    void changeLocation(int id, String newLocation);
    boolean deleteTree(String speciesLatinName);

    TreeToPlant getTreeById(int id);

    TreeToPlant getTreeByLatinName(String latinName);
}
