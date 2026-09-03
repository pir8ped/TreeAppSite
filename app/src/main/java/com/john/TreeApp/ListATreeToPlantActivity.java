package com.john.TreeApp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

import com.john.TreeApp.beans.Tree;

import db.TreeDAO;
import db.TreeDAOImpl;

public class ListATreeToPlantActivity extends BaseActivity {

    private EditText  editRootstock, editVariety, editLocation, editOrigin, editQuantity;
    private TextView textEnglishName, textLatinName, textCharacteristics;

    private Button buttonPlantNow, buttonPlantLater;

    private TreeDAO treesDAO;


    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_list_a_tree_to_plant);

        treesDAO = new TreeDAOImpl();

        // Initialize UI components
        textEnglishName = findViewById(R.id.text_english_name);
        textLatinName = findViewById(R.id.text_latin_name);
        textCharacteristics = findViewById(R.id.text_Characteristics);
        editRootstock = findViewById(R.id.edit_rootstock);
        editVariety = findViewById(R.id.edit_variety);
        editOrigin = findViewById(R.id.edit_origin);
        editLocation = findViewById(R.id.edit_location);
        editQuantity = findViewById(R.id.edit_quantity);
        buttonPlantNow = findViewById(R.id.button_plant_now);
        buttonPlantLater = findViewById(R.id.button_plant_later);

        // Retrieve and update UI with data from Intent
        Intent intent = getIntent();
        if (intent != null) {
            updateUI(intent);  // Update the UI when activity is first created or resumed
        }

        // Button actions
        buttonPlantLater.setOnClickListener(v -> {
            // Get user input from text fields
            String latinName = textLatinName.getText().toString();
            String variety = editVariety.getText().toString();
            String rootstock = editRootstock.getText().toString();
            String origin = editOrigin.getText().toString();
            String located = editLocation.getText().toString();
            int quantity = Integer.parseInt(editQuantity.getText().toString());

            // Create a Tree object using the Builder pattern.
            Tree tree = new Tree.Builder(latinName)
                    .variety(variety)
                    .rootstock(rootstock)
                    .origin(origin)
                    .located(located)
                    .build();

            // Pass the Tree object to your DAO, along with the quantity,
            // so that the DAO inserts that number of rows.
            int treeId = treesDAO.addTreesToPlant(tree, quantity);

            // Start TreeToPlantListActivity, passing along the treeId.
            Intent newIntent = new Intent(ListATreeToPlantActivity.this, TreeToPlantListActivity.class);
            newIntent.putExtra("treeId", treeId);
            startActivity(newIntent);
        });

        // Add click listener for Plant Now button
        buttonPlantNow.setOnClickListener(v -> {
            // Get user input from text fields
            String latinName = textLatinName.getText().toString();
            String variety = editVariety.getText().toString();
            String rootstock = editRootstock.getText().toString();
            String origin = editOrigin.getText().toString();
            String located = editLocation.getText().toString();
            int quantity = Integer.parseInt(editQuantity.getText().toString());

            // Create a Tree object using the Builder pattern
            Tree tree = new Tree.Builder(latinName)
                    .variety(variety)
                    .rootstock(rootstock)
                    .origin(origin)
                    .located(located)
                    .build();

            // Add the trees to the database
            for (int i = 0; i < quantity; i++) {
                treesDAO.addTree(tree);
            }

            // Navigate to PlantNowActivity with necessary tree information
            Intent plantNowIntent = new Intent(ListATreeToPlantActivity.this, PlantNowActivity.class);
            plantNowIntent.putExtra("latinName", latinName);
            plantNowIntent.putExtra("englishName", textEnglishName.getText().toString());
            plantNowIntent.putExtra("variety", variety);
            plantNowIntent.putExtra("rootstock", rootstock);
            plantNowIntent.putExtra("origin", origin);
            startActivity(plantNowIntent);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Always update the UI with new data from the intent
        Intent intent = getIntent();
        if (intent != null) {
            updateUI(intent);  // Update the UI with new species details
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);  // Update the intent with new data
        updateUI(intent);   // Refresh UI with new species data
    }

    private void updateUI(Intent intent) {
        if (intent != null) {
            String englishName = intent.getStringExtra("englishName");
            String latinName = intent.getStringExtra("latinName");
            String characteristics = intent.getStringExtra("characteristics");

            // Update the UI fields
            textEnglishName.setText(englishName);
            textLatinName.setText(latinName);
            textCharacteristics.setText(characteristics);
        }
    }


}
