package com.john.TreeApp.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.john.TreeApp.R;
import com.john.TreeApp.beans.Collection;

import db.CollectionDAO;
import db.CollectionDAOImpl;

public class NewCollectionDialog extends DialogFragment {
    private CollectionDAO collectionDAO;
    private OnCollectionChangedListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        collectionDAO = new CollectionDAOImpl();
        try {
            listener = (OnCollectionChangedListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement OnCollectionChangedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_new_collection, null);
        EditText collectionNameInput = view.findViewById(R.id.collectionNameInput);

        builder.setView(view)
                .setTitle("Create New Collection")
                .setPositiveButton("Create", (dialog, id) -> {
                    String name = collectionNameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Collection name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Collection newCollection = new Collection();
                    newCollection.setName(name);
                    long collectionId = collectionDAO.addCollection(newCollection);
                    
                    if (collectionId != -1) {
                        collectionDAO.setSelectedCollectionId((int) collectionId);
                        if (listener != null) {
                            listener.onCollectionChanged((int) collectionId);
                        }
                        Toast.makeText(getContext(), "Collection created successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to create collection", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        return builder.create();
    }
} 