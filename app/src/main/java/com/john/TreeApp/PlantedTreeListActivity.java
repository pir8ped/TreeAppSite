package com.john.TreeApp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;
import android.util.Log;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.john.TreeApp.beans.Tree;
import com.john.TreeApp.beans.TreeSpecies;

import db.CollectionDAO;
import db.CollectionDAOImpl;
import db.TreeDAO;
import db.TreeDAOImpl;
import db.TreeSpeciesDAO;
import db.TreeSpeciesDAOImpl;

public class PlantedTreeListActivity extends BaseActivity {
    private static final String TAG = "PlantedTreeListActivity";
    private ExpandableListView expandableListView;
    private PlantedTreeListAdapter listAdapter;
    private List<TreeSpecies> speciesList;
    private Map<TreeSpecies, List<Tree>> treeMap;
    private FloatingActionButton showOnMapFab;
    private TreeDAO treeDAO;
    private TreeSpeciesDAO treeSpeciesDAO;
    private TextView totalTreesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_planted_tree_list);

        treeDAO = new TreeDAOImpl();
        treeSpeciesDAO = new TreeSpeciesDAOImpl();

        expandableListView = findViewById(R.id.expandableListView);
        showOnMapFab = findViewById(R.id.show_on_map_fab);
        totalTreesText = findViewById(R.id.total_trees_text);

        loadTreeData();

        listAdapter = new PlantedTreeListAdapter(this, speciesList, treeMap);
        expandableListView.setAdapter(listAdapter);

        // Handle group click for selection (for map view)
        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            listAdapter.toggleSelection(groupPosition);
            return false; // Return false to allow default expand/collapse behavior
        });

        // Collapse other groups when one is expanded (only one expanded at a time)
        expandableListView.setOnGroupExpandListener(groupPosition -> {
            int groupCount = listAdapter.getGroupCount();
            for (int i = 0; i < groupCount; i++) {
                if (i != groupPosition && expandableListView.isGroupExpanded(i)) {
                    expandableListView.collapseGroup(i);
                }
            }
        });

        showOnMapFab.setOnClickListener(v -> showSelectedTreesOnMap());
    }

    private void loadTreeData() {
        CollectionDAO collectionDAO = new CollectionDAOImpl();
        int collectionId = collectionDAO.getSelectedCollectionId();
        Log.d(TAG, "Loading trees for collection ID: " + collectionId);

        // Log the name of the selected collection
        String collectionName = collectionDAO.getCollection(collectionId).getName();
        Log.d(TAG, "Selected collection name: " + collectionName);

        List<Tree> allTrees = treeDAO.getAllTreesInACollection(collectionId);
        Log.d(TAG, "Retrieved " + (allTrees != null ? allTrees.size() : "null") + " trees from database");

        if (allTrees == null || allTrees.isEmpty()) {
            Log.w(TAG, "No trees found in the database for collection " + collectionId);
            speciesList = new ArrayList<>();
            treeMap = new HashMap<>();
            updateTotalTreesCount(0);
            return;
        }

        // Create a map of species and their trees
        treeMap = new HashMap<>();
        Set<TreeSpecies> speciesSet = new HashSet<>();

        // Group trees by species and collect unique species
        for (Tree tree : allTrees) {
            Log.d(TAG, "Processing tree: ID=" + tree.getTreeId() +
                    ", Latin=" + tree.getLatinName() +
                    ", English=" + tree.getEnglishName() +
                    ", Label=" + tree.getLabel());

            TreeSpecies species = new TreeSpecies(
                    tree.getLatinName(),
                    tree.getEnglishName(),
                    tree.getFrenchName(),
                    tree.getCharacteristics(),
                    tree.getOtherNames());
            speciesSet.add(species);

            treeMap.computeIfAbsent(species, k -> new ArrayList<>()).add(tree);
        }

        // Convert set to list and sort by English name
        speciesList = new ArrayList<>(speciesSet);
        speciesList.sort((s1, s2) -> {
            String eng1 = s1.getEnglishName() != null ? s1.getEnglishName() : "";
            String eng2 = s2.getEnglishName() != null ? s2.getEnglishName() : "";
            return eng1.compareToIgnoreCase(eng2);
        });

        Log.d(TAG, "Grouped trees into " + speciesList.size() + " species");

        // Calculate total trees
        int totalTrees = 0;
        for (TreeSpecies species : speciesList) {
            List<Tree> trees = treeMap.get(species);
            totalTrees += trees.size();
            Log.d(TAG, "Species: " + species.getLatinName() +
                    " (" + species.getEnglishName() + ") has " +
                    trees.size() + " trees");
        }
        updateTotalTreesCount(totalTrees);
    }

    private void updateTotalTreesCount(int totalTrees) {
        if (totalTreesText != null) {
            if (totalTrees == 0) {
                totalTreesText.setText("No trees planted yet");
            } else {
                totalTreesText.setText(String.format("Total planted trees: %d", totalTrees));
            }
        }
    }

    private void showSelectedTreesOnMap() {
        List<Tree> selectedTrees = listAdapter.getSelectedTrees();
        if (selectedTrees.isEmpty()) {
            Toast.makeText(this, "Please select at least one tree", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Integer> selectedTreeIds = new ArrayList<>();
        for (Tree tree : selectedTrees) {
            selectedTreeIds.add(tree.getTreeId());
        }

        Intent intent = new Intent(this, MapViewActivity.class);
        intent.putIntegerArrayListExtra("SELECTED_TREE_IDS", selectedTreeIds);

        // Pass current collection ID to ensure map uses the same context
        CollectionDAO collectionDAO = new CollectionDAOImpl();
        int collectionId = collectionDAO.getSelectedCollectionId();
        intent.putExtra("collectionId", collectionId);

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTreeData();
        if (listAdapter != null) {
            listAdapter.updateData(speciesList, treeMap);
        }
    }
}
