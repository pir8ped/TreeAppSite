package com.john.TreeApp.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.john.TreeApp.beans.Collection;

import java.util.List;

import db.CollectionDAO;
import db.CollectionDAOImpl;

public class ChangeCollectionDialog extends DialogFragment {
    private CollectionDAO collectionDAO;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        collectionDAO = new CollectionDAOImpl();
        List<Collection> collections = collectionDAO.getAllCollections();
        int currentCollectionId = collectionDAO.getSelectedCollectionId();

        String[] collectionNames = collections.stream()
                .map(Collection::getName)
                .toArray(String[]::new);

        // Find the index of the current collection
        int currentIndex = -1;
        for (int i = 0; i < collections.size(); i++) {
            if (collections.get(i).getId() == currentCollectionId) {
                currentIndex = i;
                break;
            }
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle("Change Collection")
                .setSingleChoiceItems(collectionNames, currentIndex, (dialog, which) -> {
                    int selectedCollectionId = collections.get(which).getId();
                    if (selectedCollectionId != currentCollectionId) {
                        collectionDAO.setSelectedCollectionId(selectedCollectionId);
                        Toast.makeText(requireContext(),
                                "Switched to collection: " + collectionNames[which],
                                Toast.LENGTH_SHORT).show();

                        // Notify activity to refresh
                        if (getActivity() instanceof OnCollectionChangedListener) {
                            ((OnCollectionChangedListener) getActivity())
                                    .onCollectionChanged(selectedCollectionId);
                        }
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .create();
    }
}