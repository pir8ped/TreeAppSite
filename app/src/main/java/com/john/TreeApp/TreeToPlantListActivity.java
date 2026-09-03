package com.john.TreeApp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.john.TreeApp.beans.Tree;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import db.TreeDAO;
import db.TreeDAOImpl;

import android.view.LayoutInflater;
import android.widget.ExpandableListView;
import java.util.ArrayList;
import java.util.HashMap;
import android.app.AlertDialog;
import android.widget.EditText;
import java.util.stream.Collectors;

public class TreeToPlantListActivity extends BaseActivity implements TreeToPlantListAdapter.OnBatchEditListener {

    private ExpandableListView expandableListView;
    private TreeToPlantListAdapter adapter;
    private List<TreeToPlantListAdapter.TreeBatch> batchList;
    private Map<TreeToPlantListAdapter.TreeBatch, List<Tree>> treeMap;
    private TreeDAO treeDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_trees_to_plant_list);

        treeDAO = new TreeDAOImpl();

        expandableListView = findViewById(R.id.expandable_view_trees);
        Button plantNowButton = findViewById(R.id.button_plant_now);
        Button deleteButton = findViewById(R.id.button_delete);
        TextView noTreesMessage = findViewById(R.id.text_no_trees);

        loadTrees();

        if (batchList.isEmpty()) {
            noTreesMessage.setVisibility(View.VISIBLE);
            expandableListView.setVisibility(View.GONE);
            plantNowButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        } else {
            noTreesMessage.setVisibility(View.GONE);
            adapter = new TreeToPlantListAdapter(this, batchList, treeMap);
            adapter.setOnBatchEditListener(this);
            expandableListView.setAdapter(adapter);

            expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
                adapter.selectBatch(groupPosition);
                return false;
            });
        }

        plantNowButton.setOnClickListener(v -> {
            Tree selectedTree = adapter != null ? adapter.getSelectedTree() : null;
            if (selectedTree != null) {
                Log.d("TreeListActivity", "Planting Tree: " + selectedTree.toString());

                Intent intent = new Intent(TreeToPlantListActivity.this, PlantNowActivity.class);
                intent.putExtra("englishName", selectedTree.getEnglishName());
                intent.putExtra("latinName", selectedTree.getLatinName());
                intent.putExtra("variety", selectedTree.getVariety());
                intent.putExtra("rootstock", selectedTree.getRootstock());
                intent.putExtra("origin", selectedTree.getOrigin());
                intent.putExtra("label", selectedTree.getLabel());

                startActivity(intent);
            } else {
                Toast.makeText(TreeToPlantListActivity.this, "No tree selected", Toast.LENGTH_SHORT).show();
            }
        });

        deleteButton.setOnClickListener(v -> {
            Tree selectedTree = adapter != null ? adapter.getSelectedTree() : null;
            if (selectedTree != null) {
                boolean isDeleted = treeDAO.deleteTree(selectedTree.getTreeId());
                if (isDeleted) {
                    loadTrees();
                    if (batchList.isEmpty()) {
                        noTreesMessage.setVisibility(View.VISIBLE);
                        expandableListView.setVisibility(View.GONE);
                        plantNowButton.setVisibility(View.GONE);
                        deleteButton.setVisibility(View.GONE);
                    } else {
                        adapter.updateData(batchList, treeMap);
                    }
                } else {
                    Toast.makeText(TreeToPlantListActivity.this, "Failed to delete tree", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(TreeToPlantListActivity.this, "No tree selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTrees() {
        List<Tree> allUnplanted = treeDAO.getAllTreesReadyToPlant();
        treeMap = new HashMap<>();
        batchList = new ArrayList<>();

        if (allUnplanted != null) {
            for (Tree tree : allUnplanted) {
                TreeToPlantListAdapter.TreeBatch batch = new TreeToPlantListAdapter.TreeBatch(tree);
                if (!treeMap.containsKey(batch)) {
                    batchList.add(batch);
                    treeMap.put(batch, new ArrayList<>());
                }
                treeMap.get(batch).add(tree);
            }
        }

        // Sort batches by date added (newest first)
        batchList.sort((b1, b2) -> {
            if (b1.dateAdded == null && b2.dateAdded == null)
                return 0;
            if (b1.dateAdded == null)
                return 1;
            if (b2.dateAdded == null)
                return -1;
            return b2.dateAdded.compareTo(b1.dateAdded);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrees();
        if (adapter != null) {
            adapter.updateData(batchList, treeMap);
        }
    }

    @Override
    public void onBatchEdit(List<Tree> trees) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bulk Labeling");
        builder.setMessage("Enter label for " + trees.size() + " trees:");

        final EditText input = new EditText(this);
        input.setPadding(50, 20, 50, 20);
        // Pre-fill with existing label if all trees have the same label
        String existingLabel = trees.get(0).getLabel();
        boolean allSame = true;
        for (Tree t : trees) {
            if (!Objects.equals(t.getLabel(), existingLabel)) {
                allSame = false;
                break;
            }
        }
        if (allSame) {
            input.setText(existingLabel);
        }

        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newLabel = input.getText().toString();
            List<Integer> treeIds = trees.stream()
                    .map(Tree::getTreeId)
                    .collect(Collectors.toList());

            boolean success = treeDAO.updateBatchLabels(treeIds, newLabel);
            if (success) {
                loadTrees();
                adapter.updateData(batchList, treeMap);
                Toast.makeText(this, "Batch updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update batch", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        builder.show();
    }
}
