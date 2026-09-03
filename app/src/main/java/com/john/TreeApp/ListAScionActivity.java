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

import com.john.TreeApp.beans.Scion;

import db.ScionDAO;
import db.ScionDAOImpl;

public class ListAScionActivity extends BaseActivity {

    private EditText editSource, editVariety, editQuantity;
    private TextView textEnglishName, textLatinName, textCharacteristics;
    private Button buttonAddScion;
    private ScionDAO scionDAO;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_list_a_scion);

        scionDAO = new ScionDAOImpl();

        // Initialize UI components
        textEnglishName = findViewById(R.id.text_english_name);
        textLatinName = findViewById(R.id.text_latin_name);
        textCharacteristics = findViewById(R.id.text_Characteristics);
        editSource = findViewById(R.id.edit_source);
        editVariety = findViewById(R.id.edit_variety);
        editQuantity = findViewById(R.id.edit_quantity);
        buttonAddScion = findViewById(R.id.button_add_scion);

        // Retrieve and update UI with data from Intent
        Intent intent = getIntent();
        if (intent != null) {
            updateUI(intent);
        }

        // Button action
        buttonAddScion.setOnClickListener(v -> {
            // Get user input from text fields
            String species = textLatinName.getText().toString();
            String variety = editVariety.getText().toString();
            String source = editSource.getText().toString();
            String quantityStr = editQuantity.getText().toString();

            if (quantityStr.isEmpty()) {
                editQuantity.setError("Quantity is required");
                return;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(quantityStr);
            } catch (NumberFormatException e) {
                editQuantity.setError("Invalid quantity");
                return;
            }

            // Create N separate Scion records (each defaults to attached=false)
            for (int i = 0; i < quantity; i++) {
                Scion scion = new Scion.Builder(species)
                        .variety(variety)
                        .source(source)
                        .build();

                long scionId = scionDAO.addScion(scion);

                if (scionId == -1) {
                    Toast.makeText(this, "Failed to add scion", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Show success message
            String message = quantity == 1 ? "Scion added successfully" : quantity + " scions added successfully";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            // Navigate to Scions list to show the inventory
            Intent scionListIntent = new Intent(this, ScionListActivity.class);
            startActivity(scionListIntent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Always update the UI with new data from the intent
        Intent intent = getIntent();
        if (intent != null) {
            updateUI(intent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent with new data
        updateUI(intent); // Refresh UI with new species data
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
