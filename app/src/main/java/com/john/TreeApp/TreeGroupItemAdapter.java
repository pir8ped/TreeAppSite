package com.john.TreeApp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.john.TreeApp.beans.utilBean.TreeGroup;

import java.util.List;

public class TreeGroupItemAdapter extends RecyclerView.Adapter<TreeGroupItemAdapter.ViewHolder> {
    private final List<TreeGroup> itemList;
    private int selectedPosition = -1;

    public TreeGroupItemAdapter(List<TreeGroup> itemList) {
        this.itemList = itemList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tree_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TreeGroup item = itemList.get(position);
        holder.englishName.setText(item.getEnglishName());
        holder.latinName.setText(item.getLatinName());
        holder.quantity.setText(String.valueOf(item.getQuantity()));

        // Prevent unwanted calls during recycling
        holder.toggle.setOnCheckedChangeListener(null);
        holder.toggle.setChecked(position == selectedPosition);

        holder.toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            if (isChecked) {
                selectedPosition = currentPosition;
                notifyDataSetChanged();
            } else if (selectedPosition == currentPosition) {
                selectedPosition = -1;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView englishName, latinName, quantity;
        Switch toggle;

        public ViewHolder(View itemView) {
            super(itemView);
            englishName = itemView.findViewById(R.id.text_english_name);
            latinName = itemView.findViewById(R.id.text_latin_name);
            quantity = itemView.findViewById(R.id.text_quantity);
            toggle = itemView.findViewById(R.id.toggle_select);
        }
    }
}
