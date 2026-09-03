package com.john.TreeApp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.john.TreeApp.R;
import com.john.TreeApp.beans.ScionGroup;

import java.util.List;

public class ScionAdapter extends RecyclerView.Adapter<ScionAdapter.ScionViewHolder> {

    private List<ScionGroup> scionGroups;
    private OnScionRemoveListener removeListener;

    public interface OnScionRemoveListener {
        void onScionRemove(ScionGroup group);
    }

    public ScionAdapter(List<ScionGroup> scionGroups) {
        this.scionGroups = scionGroups;
    }

    public void setOnScionRemoveListener(OnScionRemoveListener listener) {
        this.removeListener = listener;
    }

    public void updateScions(List<ScionGroup> scionGroups) {
        this.scionGroups = scionGroups;
        notifyDataSetChanged();
    }

    public void removeScionGroup(ScionGroup group) {
        int position = scionGroups.indexOf(group);
        if (position != -1) {
            scionGroups.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ScionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scion, parent, false);
        return new ScionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScionViewHolder holder, int position) {
        ScionGroup group = scionGroups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return scionGroups != null ? scionGroups.size() : 0;
    }

    class ScionViewHolder extends RecyclerView.ViewHolder {
        TextView textSpecies;
        TextView textVariety;
        TextView textSource;
        TextView textQuantity;
        TextView textFruiting;
        ImageButton buttonRemove;

        public ScionViewHolder(@NonNull View itemView) {
            super(itemView);
            textSpecies = itemView.findViewById(R.id.text_species);
            textVariety = itemView.findViewById(R.id.text_variety);
            textSource = itemView.findViewById(R.id.text_source);
            textQuantity = itemView.findViewById(R.id.text_quantity);
            textFruiting = itemView.findViewById(R.id.text_fruiting);
            buttonRemove = itemView.findViewById(R.id.button_remove_scion);
        }

        void bind(ScionGroup group) {
            textSpecies.setText(group.getSpecies());

            if (group.getVariety() != null && !group.getVariety().isEmpty()) {
                textVariety.setText(group.getVariety());
                textVariety.setVisibility(View.VISIBLE);
            } else {
                textVariety.setVisibility(View.GONE);
            }

            if (group.getSource() != null && !group.getSource().isEmpty()) {
                textSource.setText(group.getSource());
                textSource.setVisibility(View.VISIBLE);
            } else {
                textSource.setVisibility(View.GONE);
            }

            textQuantity.setText("Qty: " + group.getCount());

            if (group.getFruitingDescription() != null && !group.getFruitingDescription().isEmpty()) {
                textFruiting.setText("Expected: " + group.getFruitingDescription());
                textFruiting.setVisibility(View.VISIBLE);
            } else {
                textFruiting.setVisibility(View.GONE);
            }

            buttonRemove.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onScionRemove(group);
                }
            });
        }
    }
}
