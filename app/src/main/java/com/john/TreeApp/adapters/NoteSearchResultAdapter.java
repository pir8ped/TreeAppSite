package com.john.TreeApp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.john.TreeApp.R;
import com.john.TreeApp.beans.NoteSearchResult;

import java.util.ArrayList;
import java.util.List;

public class NoteSearchResultAdapter extends RecyclerView.Adapter<NoteSearchResultAdapter.ViewHolder> {
    private List<NoteSearchResult> results = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(NoteSearchResult result);
    }

    public NoteSearchResultAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoteSearchResult result = results.get(position);
        holder.treeNameTextView.setText(result.getFullName());
        holder.treeLabelTextView.setText("Label: " + result.getLabel());
        holder.noteFragmentTextView.setText(result.getNoteFragment());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(result);
            }
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void setResults(List<NoteSearchResult> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView treeNameTextView;
        TextView treeLabelTextView;
        TextView noteFragmentTextView;

        ViewHolder(View itemView) {
            super(itemView);
            treeNameTextView = itemView.findViewById(R.id.treeNameTextView);
            treeLabelTextView = itemView.findViewById(R.id.treeLabelTextView);
            noteFragmentTextView = itemView.findViewById(R.id.noteFragmentTextView);
        }
    }
} 