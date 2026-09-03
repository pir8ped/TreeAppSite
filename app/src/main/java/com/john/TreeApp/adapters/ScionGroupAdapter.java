package com.john.TreeApp.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.john.TreeApp.R;
import com.john.TreeApp.beans.ScionGroup;

import java.util.List;

public class ScionGroupAdapter extends RecyclerView.Adapter<ScionGroupAdapter.ScionGroupViewHolder> {

    private List<ScionGroup> scionGroups;
    private OnQuantityChangeListener quantityChangeListener;

    public interface OnQuantityChangeListener {
        void onQuantityChanged(ScionGroup group, int newQuantity);
    }

    public ScionGroupAdapter(List<ScionGroup> scionGroups) {
        this.scionGroups = scionGroups;
    }

    public void setQuantityChangeListener(OnQuantityChangeListener listener) {
        this.quantityChangeListener = listener;
    }

    public void setScionGroups(List<ScionGroup> scionGroups) {
        this.scionGroups = scionGroups;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScionGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scion_inventory, parent, false);
        return new ScionGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScionGroupViewHolder holder, int position) {
        ScionGroup group = scionGroups.get(position);
        holder.bind(group, quantityChangeListener);
    }

    @Override
    public int getItemCount() {
        return scionGroups != null ? scionGroups.size() : 0;
    }

    static class ScionGroupViewHolder extends RecyclerView.ViewHolder {
        TextView textSpecies;
        TextView textVariety;
        TextView textSource;
        EditText editQuantity;
        TextWatcher quantityWatcher;

        public ScionGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            textSpecies = itemView.findViewById(R.id.text_species);
            textVariety = itemView.findViewById(R.id.text_variety);
            textSource = itemView.findViewById(R.id.text_source);
            editQuantity = itemView.findViewById(R.id.edit_quantity);
        }

        void bind(ScionGroup group, OnQuantityChangeListener listener) {
            textSpecies.setText(group.getSpecies());
            textVariety.setText(group.getVariety() != null ? group.getVariety() : "");
            textSource.setText(group.getSource() != null ? group.getSource() : "");

            // Remove previous listener
            if (quantityWatcher != null) {
                editQuantity.removeTextChangedListener(quantityWatcher);
            }

            // Set current quantity
            editQuantity.setText(String.valueOf(group.getCount()));

            // Add editable listener
            quantityWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().isEmpty())
                        return;

                    try {
                        int newQuantity = Integer.parseInt(s.toString());
                        if (newQuantity != group.getCount() && listener != null) {
                            listener.onQuantityChanged(group, newQuantity);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid input
                    }
                }
            };

            editQuantity.addTextChangedListener(quantityWatcher);
        }
    }
}
