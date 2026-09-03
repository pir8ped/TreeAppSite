package com.john.TreeApp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class ButtonsFragment extends Fragment {

    // Define an interface for button events
    public interface ButtonsFragmentListener {
        void onUpdateLocationPressed();

        void onSaveChangesPressed();

        void onDeleteTreePressed();

        void onPlantAnotherPressed();
    }

    private ButtonsFragmentListener listener;

    // Setter for the listener
    public void setButtonsFragmentListener(ButtonsFragmentListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for the buttons fragment
        View view = inflater.inflate(R.layout.buttons_container, container, false);

        // Assuming the Update Location button has ID button_update_location
        Button btnUpdateLocation = view.findViewById(R.id.button_update_location);
        btnUpdateLocation.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpdateLocationPressed();
            }
        });

        Button btnSaveChanges = view.findViewById(R.id.button_save);
        btnSaveChanges.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSaveChangesPressed();
            }
        });

        Button btnDeleteTree = view.findViewById(R.id.button_delete);
        btnDeleteTree.setOnClickListener(v -> {
            if (listener != null) {
                // Show confirmation dialog before deleting
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Tree")
                        .setMessage("Are you sure you want to delete this tree? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            listener.onDeleteTreePressed();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        Button btnPlantAnother = view.findViewById(R.id.button_plant_another);
        btnPlantAnother.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlantAnotherPressed();
            }
        });

        return view;
    }
}
