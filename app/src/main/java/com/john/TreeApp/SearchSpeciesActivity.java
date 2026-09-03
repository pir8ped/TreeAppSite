package com.john.TreeApp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.john.TreeApp.beans.TreeSpecies;

import java.util.ArrayList;
import java.util.List;

import db.TreeSpeciesDAOImpl;

public class SearchSpeciesActivity extends BaseActivity {

    private EditText searchInput;
    private ListView suggestionListView;
    private Button selectSpeciesButton;
    private MaterialButtonToggleGroup searchModeToggle;
    private TextView selectedSpeciesTextView;
    private Button clearSearchButton;

    // Our custom adapter and list for suggestion strings
    private SuggestionAdapter suggestionAdapter;
    private final List<String> suggestionList = new ArrayList<>();

    // List holding the matching TreeSpecies objects returned from the database
    // query
    private List<TreeSpecies> matchingSpeciesList = new ArrayList<>();
    private boolean isEnglishMode = true;
    private TreeSpeciesDAOImpl treeSpeciesDAO;
    private TreeSpecies selectedSpecies;
    private boolean returnResult;
    private boolean forScion; // Flag to indicate if we're selecting species for a scion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_search_species);

        setActionBarTitle("Choose Your Tree Species");

        returnResult = getIntent() != null && getIntent().getBooleanExtra("returnResult", false);
        forScion = getIntent() != null && getIntent().getBooleanExtra("forScion", false);

        // Find views
        searchInput = findViewById(R.id.search_input);
        suggestionListView = findViewById(R.id.suggestion_list);
        selectSpeciesButton = findViewById(R.id.btn_select_species);
        searchModeToggle = findViewById(R.id.search_mode_toggle);
        selectedSpeciesTextView = findViewById(R.id.selected_species_text_view);
        clearSearchButton = findViewById(R.id.btn_clear_search);

        treeSpeciesDAO = new TreeSpeciesDAOImpl();

        // Set up our custom adapter using the suggestionList
        suggestionAdapter = new SuggestionAdapter(suggestionList);
        suggestionListView.setAdapter(suggestionAdapter);

        // Handle list item clicks
        suggestionListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < matchingSpeciesList.size()) {
                selectedSpecies = matchingSpeciesList.get(position);
                Log.d("SearchSpecies", "Selected species: " + selectedSpecies.getLatinName() + " - "
                        + selectedSpecies.getEnglishName());

                String suggestion = suggestionAdapter.getItem(position);
                String[] names = suggestion.split("\n");

                if (names.length == 2) {
                    String englishName = isEnglishMode ? names[0] : names[1];
                    String latinName = isEnglishMode ? names[1] : names[0];

                    selectedSpeciesTextView.setText("Selected: " + englishName + " (" + latinName + ")");
                    selectedSpeciesTextView.setVisibility(View.VISIBLE);
                    clearSearchButton.setVisibility(View.VISIBLE);
                }
            }
        });

        // Set up the toggle control
        searchModeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked)
                return;

            clearSearchUI();

            if (checkedId == R.id.toggle_english) {
                isEnglishMode = true;
                searchInput.setHint("Type an English name...");
            } else if (checkedId == R.id.toggle_latin) {
                isEnglishMode = false;
                searchInput.setHint("Type a Latin name...");
            }
        });

        // Listen for changes in the search input text
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();

                selectedSpecies = null;
                selectedSpeciesTextView.setVisibility(View.GONE);
                clearSearchButton.setVisibility(View.GONE);

                if (text.length() >= 3) {
                    updateSuggestions(text);
                } else {
                    suggestionList.clear();
                    suggestionAdapter.notifyDataSetChanged();
                    suggestionListView.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Handle select species button click
        selectSpeciesButton.setOnClickListener(v -> {
            if (selectedSpecies != null) {
                if (returnResult) {
                    Log.d("SearchSpecies", "Returning selected species to caller: " +
                            selectedSpecies.getLatinName() + " - " + selectedSpecies.getEnglishName());
                    Intent data = new Intent();
                    data.putExtra("englishName", selectedSpecies.getEnglishName());
                    data.putExtra("latinName", selectedSpecies.getLatinName());
                    data.putExtra("characteristics", selectedSpecies.getCharacteristics());
                    setResult(RESULT_OK, data);
                    finish();
                } else if (forScion) {
                    // Navigate to ListAScionActivity for scion entry
                    Log.d("SearchSpecies", "Sending selected species to ListAScionActivity: " +
                            selectedSpecies.getLatinName() + " - " + selectedSpecies.getEnglishName());
                    Intent intent = new Intent(SearchSpeciesActivity.this, ListAScionActivity.class);
                    intent.putExtra("englishName", selectedSpecies.getEnglishName());
                    intent.putExtra("latinName", selectedSpecies.getLatinName());
                    intent.putExtra("characteristics", selectedSpecies.getCharacteristics());
                    startActivity(intent);
                } else {
                    // Navigate to ListATreeToPlantActivity for tree entry
                    Log.d("SearchSpecies", "Sending selected species to ListATreeToPlantActivity: " +
                            selectedSpecies.getLatinName() + " - " + selectedSpecies.getEnglishName());
                    Intent intent = new Intent(SearchSpeciesActivity.this, ListATreeToPlantActivity.class);
                    intent.putExtra("englishName", selectedSpecies.getEnglishName());
                    intent.putExtra("latinName", selectedSpecies.getLatinName());
                    intent.putExtra("characteristics", selectedSpecies.getCharacteristics());
                    startActivity(intent);
                }
            } else {
                Log.e("SearchSpecies", "No species selected");
                Toast.makeText(SearchSpeciesActivity.this, "Please select a species from the list", Toast.LENGTH_SHORT)
                        .show();
            }
        });

        clearSearchButton.setOnClickListener(v -> clearSearchUI());
    }

    private void clearSearchUI() {
        searchInput.setText("");
        selectedSpeciesTextView.setVisibility(View.GONE);
        clearSearchButton.setVisibility(View.GONE);
        suggestionList.clear();
        suggestionAdapter.notifyDataSetChanged();
        suggestionListView.setVisibility(View.GONE);
        selectedSpecies = null;

        TextView emptyText = findViewById(R.id.empty_list_text);
        emptyText.setText("Type 3 or more characters to search");
        emptyText.setVisibility(View.VISIBLE);
    }

    private void updateSuggestions(String query) {
        TextView emptyText = findViewById(R.id.empty_list_text);

        if (isEnglishMode) {
            matchingSpeciesList = treeSpeciesDAO.findTreeSpeciesByEnglishPrefix(query);
        } else {
            matchingSpeciesList = treeSpeciesDAO.findTreeSpeciesByLatinPrefix(query);
        }

        suggestionList.clear();
        if (matchingSpeciesList != null && !matchingSpeciesList.isEmpty()) {
            for (TreeSpecies species : matchingSpeciesList) {
                String suggestion;
                if (isEnglishMode) {
                    suggestion = species.getEnglishName() + "\n" + species.getLatinName();
                } else {
                    suggestion = species.getLatinName() + "\n" + species.getEnglishName();
                }
                suggestionList.add(suggestion);
            }
            suggestionAdapter.notifyDataSetChanged();
            suggestionListView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        } else {
            suggestionAdapter.notifyDataSetChanged();
            suggestionListView.setVisibility(View.GONE);
            emptyText.setText("No matches found");
            emptyText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearSearchUI();
    }

    // Custom adapter class for displaying multi-line suggestions in the list
    private class SuggestionAdapter extends BaseAdapter {
        private final List<String> suggestions;

        public SuggestionAdapter(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        @Override
        public int getCount() {
            return suggestions.size();
        }

        @Override
        public String getItem(int position) {
            return suggestions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(SearchSpeciesActivity.this)
                        .inflate(R.layout.spinner_item_layout, parent, false);
            }

            String[] names = getItem(position).split("\n");
            TextView primaryText = convertView.findViewById(R.id.suggestion_text_primary);
            TextView secondaryText = convertView.findViewById(R.id.suggestion_text_secondary);

            if (names.length == 2) {
                primaryText.setText(names[0]);
                secondaryText.setText(names[1]);
            }

            // Highlight if this is the selected item
            if (selectedSpecies != null && position < matchingSpeciesList.size()) {
                TreeSpecies species = matchingSpeciesList.get(position);
                boolean isSelected = species.getLatinName().equals(selectedSpecies.getLatinName());
                convertView.setBackgroundColor(isSelected ? 0xFFE8F5E9 : 0xFFFFFFFF);
            } else {
                convertView.setBackgroundColor(0xFFFFFFFF);
            }

            return convertView;
        }
    }
}