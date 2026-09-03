package com.john.TreeApp.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.john.TreeApp.beans.Collection;
import com.john.TreeApp.beans.Tree;

import java.util.List;
import java.util.stream.Collectors;

import db.CollectionDAO;
import db.CollectionDAOImpl;
import db.TreeDAO;
import db.TreeDAOImpl;

public class DeleteCollectionDialog extends DialogFragment {
    private CollectionDAO collectionDAO;
    private TreeDAO treeDAO;
    private int collectionToDelete;
    private Collection collectionToDeleteObj;
    private Context safeContext;

    public static DeleteCollectionDialog newInstance(int collectionId) {
        DeleteCollectionDialog dialog = new DeleteCollectionDialog();
        Bundle args = new Bundle();
        args.putInt("collectionId", collectionId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        safeContext = context;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        collectionDAO = new CollectionDAOImpl();
        treeDAO = new TreeDAOImpl();
        collectionToDelete = getArguments().getInt("collectionId");
        collectionToDeleteObj = collectionDAO.getCollection(collectionToDelete);

        if (collectionToDeleteObj == null) {
            return new AlertDialog.Builder(safeContext)
                    .setTitle("Error")
                    .setMessage("Collection not found")
                    .setPositiveButton("OK", (dialog, which) -> dismiss())
                    .create();
        }

        // Get list of other collections for potential tree transfer
        List<Collection> otherCollections = collectionDAO.getAllCollections().stream()
                .filter(c -> c.getId() != collectionToDelete)
                .collect(Collectors.toList());

        // If there are trees in this collection, show transfer option
        List<Tree> treesInCollection = treeDAO.getAllTreesInACollection(collectionToDelete);
        if (!treesInCollection.isEmpty()) {
            return createTransferDialog(otherCollections, treesInCollection.size());
        } else {
            return createSimpleDeleteDialog();
        }
    }

    private Dialog createTransferDialog(List<Collection> otherCollections, int treeCount) {
        String[] collectionNames = otherCollections.stream()
                .map(Collection::getName)
                .toArray(String[]::new);

        AlertDialog.Builder builder = new AlertDialog.Builder(safeContext);
        builder.setTitle("Delete Collection '" + collectionToDeleteObj.getName() + "'")
                .setMessage(String.format("This collection contains %d trees. What would you like to do with them?",
                        treeCount))
                .setPositiveButton("Move to Another Collection", (dialog, which) -> {
                    if (collectionNames.length > 0) {
                        showCollectionChooser(otherCollections);
                    } else {
                        showToast("No other collections available. Create a new collection first.");
                    }
                })
                .setNegativeButton("Delete Trees", (dialog, which) -> {
                    showConfirmDeleteAllDialog();
                })
                .setNeutralButton("Cancel", (dialog, which) -> dismiss());

        return builder.create();
    }

    private void showCollectionChooser(List<Collection> collections) {
        if (!isAdded())
            return;

        String[] collectionNames = collections.stream()
                .map(Collection::getName)
                .toArray(String[]::new);

        new AlertDialog.Builder(safeContext)
                .setTitle("Move Trees From '" + collectionToDeleteObj.getName() + "'")
                .setItems(collectionNames, (dialog, which) -> {
                    Collection targetCollection = collections.get(which);
                    moveTreesAndDeleteCollection(targetCollection);
                })
                .show();
    }

    private void moveTreesAndDeleteCollection(Collection targetCollection) {
        try {
            // Move trees to target collection
            if (collectionDAO.moveTreesToCollection(collectionToDelete, targetCollection.getId())) {
                // Delete the collection (DAO handles selection update if it was selected)
                if (collectionDAO.deleteCollection(collectionToDelete)) {
                    showToast(String.format("Trees moved to '%s' and collection '%s' deleted successfully",
                            targetCollection.getName(), collectionToDeleteObj.getName()));

                    // Selection might have changed, notify the activity
                    notifyCollectionChanged(collectionDAO.getSelectedCollectionId());
                    dismiss();
                } else {
                    showToast("Error deleting collection");
                }
            } else {
                showToast("Error moving trees to new collection");
            }
        } catch (Exception e) {
            showErrorDialog("Error moving trees or deleting collection", e.getMessage());
        }
    }

    private void showConfirmDeleteAllDialog() {
        new AlertDialog.Builder(safeContext)
                .setTitle("Confirm Delete")
                .setMessage(String.format(
                        "This will permanently delete the collection '%s' and ALL its trees. This action cannot be undone. Are you sure?",
                        collectionToDeleteObj.getName()))
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    deleteCollection();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Dialog createSimpleDeleteDialog() {
        return new AlertDialog.Builder(safeContext)
                .setTitle("Delete Collection")
                .setMessage(String.format("Are you sure you want to delete the collection '%s'?",
                        collectionToDeleteObj.getName()))
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCollection();
                })
                .setNegativeButton("Cancel", null)
                .create();
    }

    private void deleteCollection() {
        try {
            if (collectionDAO.deleteCollection(collectionToDelete)) {
                showToast(String.format("Collection '%s' deleted successfully",
                        collectionToDeleteObj.getName()));

                // Always notify to refresh UI, using the current selected ID
                notifyCollectionChanged(collectionDAO.getSelectedCollectionId());
                dismiss();
            } else {
                showToast("Error deleting collection");
            }
        } catch (Exception e) {
            showErrorDialog("Error deleting collection", e.getMessage());
        }
    }

    private void showToast(String message) {
        if (isAdded() && safeContext != null) {
            Toast.makeText(safeContext, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyCollectionChanged(int newCollectionId) {
        if (safeContext instanceof OnCollectionChangedListener) {
            ((OnCollectionChangedListener) safeContext)
                    .onCollectionChanged(newCollectionId);
        } else if (getActivity() instanceof OnCollectionChangedListener) {
            ((OnCollectionChangedListener) getActivity())
                    .onCollectionChanged(newCollectionId);
        }
    }

    private void showErrorDialog(String title, String message) {
        if (!isAdded() || safeContext == null)
            return;
        new AlertDialog.Builder(safeContext)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}