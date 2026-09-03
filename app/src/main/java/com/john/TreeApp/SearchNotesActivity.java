package com.john.TreeApp;

import android.os.Bundle;

public class SearchNotesActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_search_notes);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new SearchNotesFragment())
                .commit();
        }
    }
} 