package com.john.TreeApp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.john.TreeApp.R;
import com.john.TreeApp.beans.Note;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private List<Note> notes;
    private OnNoteClickListener listener;
    private OnNoteDeleteListener deleteListener;
    private final SimpleDateFormat dateFormat;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }
    
    public interface OnNoteDeleteListener {
        void onNoteDelete(Note note);
    }

    public NoteAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }
    
    public NoteAdapter(List<Note> notes, OnNoteClickListener listener, OnNoteDeleteListener deleteListener) {
        this.notes = notes;
        this.listener = listener;
        this.deleteListener = deleteListener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }
    
    public void setOnNoteDeleteListener(OnNoteDeleteListener listener) {
        this.deleteListener = listener;
    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        String previewText = note.getDescription();
        if (previewText.length() > 50) {
            previewText = previewText.substring(0, 47) + "...";
        }
        holder.contentView.setText(previewText);
        holder.dateView.setText(dateFormat.format(note.getDateWritten()));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onNoteDelete(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    public void updateNotes(List<Note> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }
    
    public void removeNote(Note note) {
        int position = notes.indexOf(note);
        if (position != -1) {
            notes.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView contentView;
        TextView dateView;
        ImageButton deleteButton;

        NoteViewHolder(View itemView) {
            super(itemView);
            contentView = itemView.findViewById(R.id.text_note_description);
            dateView = itemView.findViewById(R.id.text_note_date);
            deleteButton = itemView.findViewById(R.id.button_delete_note);
        }
    }
} 