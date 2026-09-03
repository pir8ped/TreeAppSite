package com.john.TreeApp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.john.TreeApp.beans.Tree;
import com.john.TreeApp.beans.TreeSpecies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlantedTreeListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<TreeSpecies> speciesList;
    private Map<TreeSpecies, List<Tree>> treeMap;
    private TreeSpecies selectedSpecies;

    public PlantedTreeListAdapter(Context context, List<TreeSpecies> speciesList,
            Map<TreeSpecies, List<Tree>> treeMap) {
        this.context = context;
        this.speciesList = speciesList;
        this.treeMap = treeMap;
    }

    public void updateData(List<TreeSpecies> speciesList, Map<TreeSpecies, List<Tree>> treeMap) {
        // Store the currently selected species's latin name if any
        String selectedLatinName = selectedSpecies != null ? selectedSpecies.getLatinName() : null;

        this.speciesList = speciesList;
        this.treeMap = treeMap;

        // Restore the selection if the species still exists
        if (selectedLatinName != null) {
            for (TreeSpecies species : speciesList) {
                if (species.getLatinName().equals(selectedLatinName)) {
                    selectedSpecies = species;
                    break;
                }
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return speciesList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        TreeSpecies species = speciesList.get(groupPosition);
        List<Tree> trees = treeMap.get(species);
        // Only show children if there's more than one tree
        return (trees != null && trees.size() > 1) ? trees.size() : 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return speciesList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        TreeSpecies species = speciesList.get(groupPosition);
        return treeMap.get(species).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        TreeSpecies species = (TreeSpecies) getGroup(groupPosition);
        List<Tree> treesInSpecies = treeMap.get(species);
        int totalTrees = treesInSpecies.size();

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_group_species, parent, false);
        }

        TextView speciesNameView = convertView.findViewById(R.id.species_name);
        String displayText = String.format("%s\n%s (%d trees)",
                species.getEnglishName() != null ? species.getEnglishName() : "",
                species.getLatinName(),
                totalTrees);
        speciesNameView.setText(displayText);

        // Handle single tree edit button
        ImageButton btnEditSingle = convertView.findViewById(R.id.btn_edit_single_tree);
        if (totalTrees == 1) {
            btnEditSingle.setVisibility(View.VISIBLE);
            btnEditSingle.setFocusable(false);
            btnEditSingle.setOnClickListener(v -> {
                Tree tree = treesInSpecies.get(0);
                openEditTreeActivity(tree);
            });
        } else {
            btnEditSingle.setVisibility(View.GONE);
        }

        // Update background based on selection state
        if (species.equals(selectedSpecies)) {
            convertView.setBackgroundResource(R.color.selected_item_background);
        } else {
            convertView.setBackgroundResource(android.R.color.transparent);
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
        Tree tree = (Tree) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_planted_tree, parent, false);
        }

        TextView labelView = convertView.findViewById(R.id.tree_label);
        TextView dateView = convertView.findViewById(R.id.tree_date_planted);
        ImageButton btnEdit = convertView.findViewById(R.id.btn_edit_tree);

        String label = tree.getLabel();
        if (label == null || label.isEmpty()) {
            label = "Tree " + tree.getTreeId();
        }
        labelView.setText(label);

        // Color-code label background based on tree status
        String status = tree.getStatus() != null ? tree.getStatus() : "unverified";
        switch (status) {
            case "verified":
                labelView.setBackgroundColor(0xFFC8E6C9); // green
                break;
            case "lost":
                labelView.setBackgroundColor(0xFFE0E0E0); // grey
                break;
            default: // unverified
                labelView.setBackgroundColor(0xFFFFFFFF); // white
                break;
        }

        String dateStr = "Date Planted: Unknown";
        if (tree.getDatePlanted() != null) {
            dateStr = "Date Planted: " + tree.getDatePlanted().toString();
        }
        dateView.setText(dateStr);

        btnEdit.setFocusable(false);
        btnEdit.setOnClickListener(v -> openEditTreeActivity(tree));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public List<Tree> getSelectedTrees() {
        if (selectedSpecies == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(treeMap.get(selectedSpecies));
    }

    public void toggleSelection(int groupPosition) {
        TreeSpecies species = speciesList.get(groupPosition);
        if (species.equals(selectedSpecies)) {
            selectedSpecies = null;
        } else {
            selectedSpecies = species;
        }
        notifyDataSetChanged();
    }

    private void openEditTreeActivity(Tree tree) {
        Intent intent = new Intent(context, EditTreeActivity.class);
        intent.putExtra("treeId", tree.getTreeId());
        intent.putExtra("collectionId", tree.getCollectionId());
        context.startActivity(intent);
    }
}