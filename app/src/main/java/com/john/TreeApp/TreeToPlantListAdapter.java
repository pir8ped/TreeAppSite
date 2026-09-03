package com.john.TreeApp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.john.TreeApp.beans.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TreeToPlantListAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private List<TreeBatch> batchList;
    private Map<TreeBatch, List<Tree>> treeMap;
    private TreeBatch selectedBatch;
    private OnBatchEditListener batchEditListener;

    public interface OnBatchEditListener {
        void onBatchEdit(List<Tree> trees);
    }

    public void setOnBatchEditListener(OnBatchEditListener listener) {
        this.batchEditListener = listener;
    }

    public TreeToPlantListAdapter(Context context, List<TreeBatch> batchList, Map<TreeBatch, List<Tree>> treeMap) {
        this.context = context;
        this.batchList = batchList;
        this.treeMap = treeMap;
    }

    public void updateData(List<TreeBatch> batchList, Map<TreeBatch, List<Tree>> treeMap) {
        // Try to preserve selection
        TreeBatch oldSelected = selectedBatch;
        this.batchList = batchList;
        this.treeMap = treeMap;

        selectedBatch = null;
        if (oldSelected != null) {
            for (TreeBatch batch : batchList) {
                if (batch.equals(oldSelected)) {
                    selectedBatch = batch;
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return batchList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        TreeBatch batch = batchList.get(groupPosition);
        List<Tree> trees = treeMap.get(batch);
        return (trees != null && trees.size() > 1) ? trees.size() : 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return batchList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        TreeBatch batch = batchList.get(groupPosition);
        return treeMap.get(batch).get(childPosition);
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
        TreeBatch batch = (TreeBatch) getGroup(groupPosition);
        List<Tree> treesInBatch = treeMap.get(batch);
        int totalTrees = treesInBatch.size();

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_group_species, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.species_name);
        TextView countView = convertView.findViewById(R.id.tree_count);
        ImageButton btnEdit = convertView.findViewById(R.id.btn_edit_single_tree);

        // Use a RadioButton for selection instead of background color if we want
        // precise selection
        // But for now, let's stick to background highlighting as in
        // PlantedTreeListAdapter
        // Alternatively, we can use a custom background.

        String englishName = batch.englishName != null ? batch.englishName : "Unknown Species";
        String variety = batch.variety != null ? " (" + batch.variety + ")" : "";
        String label = batch.label != null && !batch.label.isEmpty() ? "\nLabel: " + batch.label : "";
        String dateStr = batch.dateAdded != null ? batch.dateAdded.toString() : "Unknown Date";

        nameView.setText(String.format("%s: %s%s%s", dateStr, englishName, variety, label));
        countView.setText(String.valueOf(totalTrees));
        countView.setVisibility(View.VISIBLE);

        if (totalTrees == 1) {
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setFocusable(false);
            btnEdit.setOnClickListener(v -> openEditTreeActivity(treesInBatch.get(0)));
        } else if (totalTrees > 1) {
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setFocusable(false);
            btnEdit.setOnClickListener(v -> {
                if (batchEditListener != null) {
                    batchEditListener.onBatchEdit(treesInBatch);
                }
            });
        } else {
            btnEdit.setVisibility(View.GONE);
        }

        // Highlight selection
        if (batch.equals(selectedBatch)) {
            convertView.setBackgroundResource(R.color.selected_item_background);
        } else {
            convertView.setBackgroundResource(android.R.color.transparent);
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
            ViewGroup parent) {
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
            label = "Tree ID: " + tree.getTreeId();
        }
        labelView.setText(label);

        String dateStr = tree.getDatePlanted() != null ? "Added: " + tree.getDatePlanted().toString()
                : "Added: Unknown";
        dateView.setText(dateStr);

        btnEdit.setFocusable(false);
        btnEdit.setOnClickListener(v -> openEditTreeActivity(tree));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false; // Only groups are selectable for bulk actions
    }

    public void selectBatch(int groupPosition) {
        TreeBatch batch = batchList.get(groupPosition);
        if (batch.equals(selectedBatch)) {
            selectedBatch = null;
        } else {
            selectedBatch = batch;
        }
        notifyDataSetChanged();
    }

    public TreeBatch getSelectedBatch() {
        return selectedBatch;
    }

    public Tree getSelectedTree() {
        if (selectedBatch != null) {
            List<Tree> trees = treeMap.get(selectedBatch);
            if (trees != null && !trees.isEmpty()) {
                return trees.get(0); // For now, actions take the first tree from batch
            }
        }
        return null;
    }

    private void openEditTreeActivity(Tree tree) {
        Intent intent = new Intent(context, EditTreeActivity.class);
        intent.putExtra("treeId", tree.getTreeId());
        intent.putExtra("collectionId", tree.getCollectionId());
        context.startActivity(intent);
    }

    /**
     * Helper class to represent a batch of trees added on same date with same
     * attributes
     */
    public static class TreeBatch {
        public final java.sql.Date dateAdded;
        public final String latinName;
        public final String englishName;
        public final String variety;
        public final String rootstock;
        public final String origin;
        public final String label;

        public TreeBatch(Tree tree) {
            this.dateAdded = tree.getDatePlanted();
            this.latinName = tree.getLatinName();
            this.englishName = tree.getEnglishName();
            this.variety = tree.getVariety();
            this.rootstock = tree.getRootstock();
            this.origin = tree.getOrigin();
            this.label = tree.getLabel();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TreeBatch treeBatch = (TreeBatch) o;
            return Objects.equals(dateAdded, treeBatch.dateAdded) &&
                    ((latinName == null && treeBatch.latinName == null) || (latinName != null && latinName.equalsIgnoreCase(treeBatch.latinName))) &&
                    Objects.equals(variety, treeBatch.variety) &&
                    Objects.equals(rootstock, treeBatch.rootstock) &&
                    Objects.equals(origin, treeBatch.origin) &&
                    Objects.equals(label, treeBatch.label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dateAdded, latinName != null ? latinName.toUpperCase() : null, variety, rootstock, origin, label);
        }
    }
}
