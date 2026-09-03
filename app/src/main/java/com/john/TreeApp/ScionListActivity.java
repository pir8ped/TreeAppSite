package com.john.TreeApp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.john.TreeApp.adapters.ScionGroupAdapter;
import com.john.TreeApp.beans.Scion;
import com.john.TreeApp.beans.ScionGroup;

import java.util.List;

import db.ScionDAO;
import db.ScionDAOImpl;

public class ScionListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private Button buttonAddScion;
    private ScionGroupAdapter adapter;
    private ScionDAO scionDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_scion_list);
        setActionBarTitle("Scions");

        scionDAO = new ScionDAOImpl();

        recyclerView = findViewById(R.id.recycler_view_scions);
        buttonAddScion = findViewById(R.id.button_add_new_scion);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScionGroupAdapter(null);
        adapter.setQuantityChangeListener(this::handleQuantityChange);
        recyclerView.setAdapter(adapter);

        buttonAddScion.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchSpeciesActivity.class);
            intent.putExtra("forScion", true);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadScions();
    }

    private void loadScions() {
        // Get grouped unattached scions with counts
        List<ScionGroup> scionGroups = scionDAO.getUnattachedScionsGrouped();

        // Update adapter if it's the group adapter
        if (adapter instanceof ScionGroupAdapter) {
            ((ScionGroupAdapter) adapter).setScionGroups(scionGroups);
        }
    }

    private void handleQuantityChange(ScionGroup group, int newQuantity) {
        int currentCount = group.getCount();
        int difference = newQuantity - currentCount;

        if (difference == 0)
            return;

        if (difference > 0) {
            // Add new scions
            for (int i = 0; i < difference; i++) {
                Scion newScion = new Scion.Builder(group.getSpecies())
                        .variety(group.getVariety())
                        .source(group.getSource())
                        .build();
                scionDAO.addScion(newScion);
            }
            Toast.makeText(this, "Added " + difference + " scion(s)", Toast.LENGTH_SHORT).show();
        } else {
            // Delete scions (difference is negative)
            List<Scion> scionsToDelete = scionDAO.getUnattachedScions();
            int deleted = 0;
            for (Scion scion : scionsToDelete) {
                if (deleted >= Math.abs(difference))
                    break;

                // Match by species, variety, and source
                boolean matches = scion.getSpecies().equals(group.getSpecies()) &&
                        ((scion.getVariety() == null && group.getVariety() == null) ||
                                (scion.getVariety() != null && scion.getVariety().equals(group.getVariety())))
                        &&
                        ((scion.getSource() == null && group.getSource() == null) ||
                                (scion.getSource() != null && scion.getSource().equals(group.getSource())));

                if (matches) {
                    scionDAO.deleteScion(scion.getScionId());
                    deleted++;
                }
            }
            Toast.makeText(this, "Removed " + deleted + " scion(s)", Toast.LENGTH_SHORT).show();
        }

        // Reload to show updated counts
        loadScions();
    }
}
