package com.john.TreeApp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.john.TreeApp.adapters.NoteSearchResultAdapter;
import com.john.TreeApp.beans.NoteSearchResult;

import java.util.List;

import db.NoteDAO;
import db.NoteDAOImpl;

public class SearchNotesFragment extends Fragment {
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView resultsRecyclerView;
    private LinearLayout noResultsLayout;
    private NoteSearchResultAdapter adapter;
    private NoteDAO noteDAO;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteDAO = new NoteDAOImpl();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchEditText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView);
        noResultsLayout = view.findViewById(R.id.noResultsLayout);

        // Setup RecyclerView
        adapter = new NoteSearchResultAdapter(this::onResultClick);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        resultsRecyclerView.setAdapter(adapter);

        // Setup search button
        searchButton.setOnClickListener(v -> performSearch());
    }

    private void performSearch() {
        String searchTerm = searchEditText.getText().toString().trim();
        if (searchTerm.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        List<NoteSearchResult> results = noteDAO.searchNotes(searchTerm);
        if (results.isEmpty()) {
            showNoResults();
            return;
        }

        adapter.setResults(results);
        showResults();
    }

    private void showNoResults() {
        resultsRecyclerView.setVisibility(View.GONE);
        noResultsLayout.setVisibility(View.VISIBLE);
    }

    private void showResults() {
        noResultsLayout.setVisibility(View.GONE);
        resultsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void onResultClick(NoteSearchResult result) {
        Intent intent = new Intent(getContext(), EditTreeActivity.class);
        intent.putExtra("treeId", result.getTreeId());
        intent.putExtra("collectionId", result.getCollectionId());
        intent.putExtra("NOTE_ID", result.getNoteId());
        intent.putExtra("NOTE_START_INDEX", result.getStartIndex());
        intent.putExtra("NOTE_END_INDEX", result.getEndIndex());
        startActivity(intent);
    }
}