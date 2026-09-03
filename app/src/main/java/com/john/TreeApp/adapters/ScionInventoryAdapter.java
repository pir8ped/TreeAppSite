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
import com.john.TreeApp.beans.Scion;

import java.util.List;

public class ScionInventoryAdapter extends RecyclerView.Adapter<ScionInventoryAdapter.ScionViewHolder> {

    private List<Scion> scions;
    private OnScionQuantityChangeListener listener;

    public interface OnScionQuantityChangeListener {
        void onQuantityChanged(Scion scion, int newQuantity);
    }

    public ScionInventoryAdapter(List<Scion> scions, OnScionQuantityChangeListener listener) {
        this.scions = scions;
        this.listener = listener;
    }

    public void setScions(List<Scion> scions) {
        this.scions = scions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scion_inventory, parent, false);
        return new ScionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScionViewHolder holder, int position) {
        Scion scion = scions.get(position);
        holder.bind(scion);
    }

    @Override
    public int getItemCount() {
        return scions != null ? scions.size() : 0;
    }

    class ScionViewHolder extends RecyclerView.ViewHolder {
        TextView textSpecies;
        TextView textVariety;
        TextView textSource;
        EditText editQuantity;
        TextWatcher quantityWatcher;

        public ScionViewHolder(@NonNull View itemView) {
            super(itemView);
            textSpecies = itemView.findViewById(R.id.text_species);
            textVariety = itemView.findViewById(R.id.text_variety);
            textSource = itemView.findViewById(R.id.text_source);
            editQuantity = itemView.findViewById(R.id.edit_quantity);
        }

        void bind(Scion scion) {
            textSpecies.setText(scion.getSpecies());
            textVariety.setText(scion.getVariety());
            textSource.setText(scion.getSource());

            // Each scion is a separate record (not quantity-based)
            editQuantity.setText("1");
        }
    }
}
