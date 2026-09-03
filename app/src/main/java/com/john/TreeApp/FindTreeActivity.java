package com.john.TreeApp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.john.TreeApp.beans.Tree;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import db.TreeDAO;
import db.TreeDAOImpl;
import db.TreeService;
import db.CollectionDAO;
import db.CollectionDAOImpl;
import android.widget.CheckBox;

public class FindTreeActivity extends BaseActivity {
    private TreeDAO treeDAO;
    private TreeService treeService;
    private EditText labelInput;
    private CheckBox globalSearchCheckbox;
    private Button searchButton;
    private CollectionDAO collectionDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_find_tree);
        setActionBarTitle("Find Tree");

        // Initialize DAOs and services
        treeDAO = new TreeDAOImpl();
        treeService = new TreeService();
        collectionDAO = new CollectionDAOImpl();

        // Initialize views
        labelInput = findViewById(R.id.label_input);
        globalSearchCheckbox = findViewById(R.id.global_search_checkbox);
        searchButton = findViewById(R.id.search_button);

        // Set up search button click listener
        searchButton.setOnClickListener(v -> searchForTree());
    }

    private void searchForTree() {
        String label = labelInput.getText().toString().trim();

        if (TextUtils.isEmpty(label)) {
            Toast.makeText(this, "Please enter a label", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search for tree by label
        Tree tree;
        if (globalSearchCheckbox.isChecked()) {
            // Global search
            tree = treeDAO.findATree_fromLabel(label);
        } else {
            // Current collection search
            int currentCollectionId = collectionDAO.getSelectedCollectionId();
            tree = treeDAO.findATree_fromLabel(label, currentCollectionId);
        }

        if (tree != null) {
            // Tree found, launch EditTreeActivity
            Intent intent = new Intent(this, EditTreeActivity.class);
            intent.putExtra("treeId", tree.getTreeId());
            intent.putExtra("collectionId", tree.getCollectionId());
            startActivity(intent);
            finish();
        } else {
            String scope = globalSearchCheckbox.isChecked() ? "across all collections" : "in this collection";
            Toast.makeText(this, "No tree found with label '" + label + "' " + scope, Toast.LENGTH_SHORT).show();
        }
    }
}