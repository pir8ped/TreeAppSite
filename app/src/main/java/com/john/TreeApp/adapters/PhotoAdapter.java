package com.john.TreeApp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.john.TreeApp.R;
import com.john.TreeApp.beans.Image;
import com.john.TreeApp.views.ZoomableImageView;
import db.ImageDAO;
import db.ImageDAOImpl;
import db.NoteDAO;
import db.NoteDAOImpl;
import com.john.TreeApp.beans.Note;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private List<Image> images;
    private final SimpleDateFormat dateFormat;
    private final ImageDAO imageDAO;
    private final NoteDAO noteDAO;

    public PhotoAdapter(List<Image> images) {
        this.images = images;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        this.imageDAO = new ImageDAOImpl();
        this.noteDAO = new NoteDAOImpl();
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        Image image = images.get(position);
        Context context = holder.itemView.getContext();
        
        // Get the image file from the app's private Pictures directory
        File imageFile = new File(
            new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Trees"),
            image.getImageUrlOrFileName()
        );

        // Load the image using Glide
        Glide.with(context)
            .load(imageFile)
            .centerCrop()
            .into(holder.imageView);

        if (image.getDateTaken() != null) {
            holder.dateView.setText(dateFormat.format(image.getDateTaken()));
        }

        // Show caption preview if it exists
        Note captionNote = noteDAO.getNoteForImage(image.getImageId());
        if (captionNote != null && captionNote.getDescription() != null && !captionNote.getDescription().isEmpty()) {
            holder.captionPreview.setText(captionNote.getDescription());
            holder.captionPreview.setVisibility(View.VISIBLE);
        } else {
            holder.captionPreview.setVisibility(View.GONE);
        }

        // Set click listener to show full-size image with options
        holder.imageView.setOnClickListener(v -> showFullSizeImageWithOptions(context, imageFile, image));
    }

    private void showFullSizeImageWithOptions(Context context, File imageFile, Image image) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_full_image, null);
        ZoomableImageView fullImageView = dialogView.findViewById(R.id.full_image_view);

        // Load full-size image with specific configuration
        Glide.with(context)
            .load(imageFile)
            .fitCenter()
            .override(1000, 1000) // Set a reasonable size limit
            .into(fullImageView);

        TextView captionView = dialogView.findViewById(R.id.caption_view);
        
        // Fetch and show caption if it exists
        Note captionNote = noteDAO.getNoteForImage(image.getImageId());
        if (captionNote != null && captionNote.getDescription() != null && !captionNote.getDescription().isEmpty()) {
            captionView.setText(captionNote.getDescription());
            captionView.setVisibility(View.VISIBLE);
        }

        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(dialogView)
            .create();

        // Handle custom buttons
        dialogView.findViewById(R.id.btn_close).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_edit_caption).setOnClickListener(v -> {
            Note currentNote = noteDAO.getNoteForImage(image.getImageId());
            showEditCaptionDialog(context, image, currentNote, captionView);
        });
        dialogView.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Yes", (confirmDialog, which) -> {
                    // Delete from database
                    imageDAO.deleteImage(image.getImageId());
                    
                    // Delete the file
                    if (imageFile.exists()) {
                        if (imageFile.delete()) {
                            // Remove from the list and notify adapter
                            int position = images.indexOf(image);
                            if (position != -1) {
                                images.remove(position);
                                notifyItemRemoved(position);
                            }
                            dialog.dismiss();
                            Toast.makeText(context, "Photo deleted successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to delete photo file", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("No", null)
                .show();
        });

        // Add logging to debug the image loading
        Log.d("PhotoAdapter", "Loading image from: " + imageFile.getAbsolutePath());
        Log.d("PhotoAdapter", "File exists: " + imageFile.exists());
        Log.d("PhotoAdapter", "File size: " + imageFile.length());

        dialog.show();
    }

    private void showEditCaptionDialog(Context context, Image image, Note existingNote, TextView captionView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(existingNote == null ? "Add Caption" : "Edit Caption");

        final android.widget.EditText input = new android.widget.EditText(context);
        input.setInputType(android.view.inputmethod.EditorInfo.TYPE_CLASS_TEXT | android.view.inputmethod.EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
        if (existingNote != null) {
            input.setText(existingNote.getDescription());
        }
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String captionText = input.getText().toString();
            if (existingNote == null) {
                if (!captionText.isEmpty()) {
                    Note newNote = new Note(image.getTreeId(), new java.util.Date(), captionText, image.getImageId());
                    noteDAO.addNote(newNote);
                    captionView.setText(captionText);
                    captionView.setVisibility(View.VISIBLE);
                }
            } else {
                if (captionText.isEmpty()) {
                    noteDAO.deleteNote(existingNote.getNoteID());
                    captionView.setVisibility(View.GONE);
                } else {
                    noteDAO.editNote(existingNote.getNoteID(), captionText);
                    captionView.setText(captionText);
                    captionView.setVisibility(View.VISIBLE);
                }
            }
            
            // Notify adapter to refresh the gallery view preview
            int position = images.indexOf(image);
            if (position != -1) {
                notifyItemChanged(position);
            }
            
            Toast.makeText(context, "Caption saved", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public int getItemCount() {
        return images != null ? images.size() : 0;
    }

    public void updatePhotos(List<Image> newImages) {
        this.images = newImages;
        notifyDataSetChanged();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView dateView;
        TextView captionPreview;

        PhotoViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photo_image);
            dateView = itemView.findViewById(R.id.photo_date);
            captionPreview = itemView.findViewById(R.id.photo_caption_preview);
        }
    }
} 