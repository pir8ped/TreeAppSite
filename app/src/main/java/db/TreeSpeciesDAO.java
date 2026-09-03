package db;


import com.john.TreeApp.beans.TreeSpecies;

import java.util.List;

public interface TreeSpeciesDAO {
    String addASpecies(TreeSpecies species);

    boolean deleteTree(String speciesLatinName);

    TreeSpecies findTreesSpecies_Latin(String speciesLatinName);

    TreeSpecies findTreesSpecies_English(String speciesEnglishName);

    TreeSpecies findTreesSpecies_French(String speciesFrenchName);

    List<TreeSpecies> findTreeSpeciesByEnglishPrefix(String prefix);

    List<TreeSpecies> findTreeSpeciesByLatinPrefix(String prefix);
}
