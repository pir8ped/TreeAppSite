package com.john.TreeApp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.app.AlertDialog;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;

import com.john.TreeApp.adapters.NoteAdapter;
import com.john.TreeApp.adapters.PhotoAdapter;
import com.john.TreeApp.beans.Note;
import com.john.TreeApp.beans.Image;
import com.john.TreeApp.beans.Tree;
import com.john.TreeApp.beans.Collection; // Added
import android.widget.Spinner; // Added
import android.widget.ArrayAdapter; // Added
import android.widget.CheckBox; // Added
import com.john.TreeApp.beans.Reminder; // Added

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.File;
import java.io.IOException;
import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.MediaStore;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;

import db.CollectionDAO;
import db.CollectionDAOImpl;
import db.TreeDAO;
import db.TreeDAOImpl;
import db.TreeService;
import db.ImageDAO;
import db.ImageDAOImpl;
import db.NoteDAO;
import db.NoteDAOImpl;
import db.LocationDAO;
import db.LocationDAOImpl;

public class EditTreeActivity extends BaseActivity implements LocationFragment.LocationListener {

    private static final String TAG = "EditTreeActivity";
    // Add result code for tree deletion
    public static final int RESULT_TREE_DELETED = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;

    private String currentPhotoPath;
    private Uri photoURI;
    private ImageDAO imageDAO;
    private PhotoAdapter photoAdapter;

    private TreeDAO treeDAO;
    private TreeService treeService;
    private int treeId;
    private Tree tree;

    // UI components for basic info
    private TextView collectionNameView;
    private TextView labelView;
    private TextView englishNameView;
    private TextView latinNameView;
    private TextView rootstockView;
    private TextView varietyView;
    private Spinner statusSpinner;
    private boolean statusSpinnerInitialized = false;

    // Section cards
    private MaterialCardView basicInfoCard;
    private MaterialCardView locationCard;
    private MaterialCardView scionsCard;
    private MaterialCardView photosCard;
    private MaterialCardView notesCard;

    // RecyclerViews
    private RecyclerView photosRecyclerView;
    private RecyclerView scionsRecyclerView;
    private RecyclerView notesRecyclerView;
    private TextView notesPageIndicator;

    // Scion support
    private db.ScionDAO scionDAO;
    private db.TreeScionDAO treeScionDAO;
    private db.ReminderDAO reminderDAO;

    // Action buttons
    private ViewGroup actionButtonsContainer;

    // Fragments
    private LocationFragment locationFragment;
    private EditTree_primaryFields primaryFragment;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_edit_tree);

        // Initialize DAOs and services with writable database
        treeDAO = new TreeDAOImpl();
        treeService = new TreeService();
        imageDAO = new ImageDAOImpl();
        scionDAO = new db.ScionDAOImpl();
        treeScionDAO = new db.TreeScionDAOImpl();
        reminderDAO = new db.ReminderDAOImpl();

        // Initialize tree from Intent; if unsuccessful, finish the activity
        if (!initTreeDataFromIntent()) {
            finish();
            return;
        }

        // Set the action bar title with English name and label (if available)
        String title = "Edit Tree: " + tree.getEnglishName();
        if (tree.getLabel() != null && !tree.getLabel().isEmpty()) {
            title += " " + tree.getLabel();
        }
        setActionBarTitle(title);

        initializeViews();
        setupRecyclerViews();

        // Load data into views
        populateTreeData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_tree_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit_basic_info) {
            showEditBasicInfoDialog();
            return true;
        } else if (id == R.id.action_add_scion) {
            showAddScionDialog();
            return true;
        } else if (id == R.id.action_plant_another) {
            Intent intent = new Intent(EditTreeActivity.this, PlantNowActivity.class);
            intent.putExtra("englishName", tree.getEnglishName());
            intent.putExtra("latinName", tree.getLatinName());
            intent.putExtra("variety", tree.getVariety());
            intent.putExtra("rootstock", tree.getRootstock());
            intent.putExtra("origin", tree.getOrigin());
            intent.putExtra("isPlantingAnother", true);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_move_collection) {
            showMoveCollectionDialog();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        // Find views
        basicInfoCard = findViewById(R.id.basic_info_card);
        locationCard = findViewById(R.id.location_card);
        scionsCard = findViewById(R.id.scions_card);
        photosCard = findViewById(R.id.photos_card);
        notesCard = findViewById(R.id.notes_card);

        // Basic info views
        collectionNameView = findViewById(R.id.tv_collection_name);
        labelView = findViewById(R.id.tv_label);
        englishNameView = findViewById(R.id.tv_english_name);
        latinNameView = findViewById(R.id.tv_latin_name);
        rootstockView = findViewById(R.id.tv_rootstock);
        varietyView = findViewById(R.id.tv_variety);
        statusSpinner = findViewById(R.id.spinner_status);

        // Set up "See on Map" link
        TextView seeOnMapLink = findViewById(R.id.see_on_map_link);
        seeOnMapLink.setOnClickListener(v -> openMapViewWithTree());

        // RecyclerViews
        photosRecyclerView = findViewById(R.id.photos_recycler);
        scionsRecyclerView = findViewById(R.id.scions_recycler);
        notesRecyclerView = findViewById(R.id.notes_recycler);
        notesRecyclerView.setNestedScrollingEnabled(false);
        new PagerSnapHelper().attachToRecyclerView(notesRecyclerView);
        notesPageIndicator = findViewById(R.id.notes_page_indicator);

        // Set up "Update Location" link
        TextView updateLocationLink = findViewById(R.id.update_location_link);
        updateLocationLink.setOnClickListener(v -> startLocationUpdate());

        // Action buttons container
        actionButtonsContainer = findViewById(R.id.action_buttons_container);

        // Initialize action buttons
        setupActionButtons();
    }

    private void setupActionButtons() {
        // This method is now called once during initialization
        // No more dynamic updates based on scroll position
        createActionButtons();
    }

    private void createActionButtons() {
        // Clear any existing buttons
        actionButtonsContainer.removeAllViews();

        // Convert dp to pixels for margins
        int marginDp = 8;
        float density = getResources().getDisplayMetrics().density;
        int marginPx = (int) (marginDp * density);
        int buttonWidthPx = (int) (105 * density); // Slightly narrower to fit 3 buttons nicely

        // Only Add Photo, Add Note, and Add Reminder remain as bottom buttons
        // Other actions moved to overflow menu (⋮)

        // Add Photo Button
        Button addPhotoButton = createButton("+Photo", v -> dispatchTakePictureIntent());
        addButtonToContainer(addPhotoButton, marginPx, buttonWidthPx);

        // Add Note Button
        Button addNoteButton = createButton("+Note", v -> showAddNoteDialog());
        addButtonToContainer(addNoteButton, marginPx, buttonWidthPx);

        // Add Reminder Button
        Button addReminderButton = createButton("+Remind", v -> showAddReminderDialog());
        addButtonToContainer(addReminderButton, marginPx, buttonWidthPx);
    }

    private Button createButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setBackgroundResource(R.drawable.button_green_rounded);
        button.setTextColor(0xFFFFFFFF);
        button.setOnClickListener(listener);
        return button;
    }

    private void addButtonToContainer(Button button, int marginPx, int widthPx) {
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                widthPx,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, marginPx, 0);
        button.setLayoutParams(params);
        actionButtonsContainer.addView(button);
    }

    private void showAddScionDialog() {
        // Get unattached scions grouped by type
        List<com.john.TreeApp.beans.ScionGroup> scionGroups = scionDAO.getUnattachedScionsGrouped();

        // Inflate dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_scion, null);
        ListView scionListView = dialogView.findViewById(R.id.scion_list);
        TextView emptyMessage = dialogView.findViewById(R.id.empty_message);

        if (scionGroups.isEmpty()) {
            // Show empty state message
            scionListView.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);

            // Show dialog with just the message
            new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            // Create adapter for grouped scions
            android.widget.BaseAdapter adapter = new android.widget.BaseAdapter() {
                @Override
                public int getCount() {
                    return scionGroups.size();
                }

                @Override
                public Object getItem(int position) {
                    return scionGroups.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(
                                R.layout.list_item_scion_selectable, parent, false);
                    }

                    com.john.TreeApp.beans.ScionGroup group = scionGroups.get(position);

                    TextView speciesText = convertView.findViewById(R.id.text_species);
                    TextView varietyText = convertView.findViewById(R.id.text_variety);
                    TextView sourceText = convertView.findViewById(R.id.text_source);
                    TextView countText = convertView.findViewById(R.id.text_count);

                    speciesText.setText(group.getSpecies());

                    if (group.getVariety() != null && !group.getVariety().isEmpty()) {
                        varietyText.setText("Variety: " + group.getVariety());
                        varietyText.setVisibility(View.VISIBLE);
                    } else {
                        varietyText.setVisibility(View.GONE);
                    }

                    if (group.getSource() != null && !group.getSource().isEmpty()) {
                        sourceText.setText("Source: " + group.getSource());
                        sourceText.setVisibility(View.VISIBLE);
                    } else {
                        sourceText.setVisibility(View.GONE);
                    }

                    // Show count (read-only)
                    if (countText != null) {
                        countText.setText("Available: " + group.getCount());
                        countText.setVisibility(View.VISIBLE);
                    }

                    return convertView;
                }
            };

            scionListView.setAdapter(adapter);

            // Create the dialog
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setNegativeButton("Cancel", null)
                    .create();

            // Handle scion group selection
            scionListView.setOnItemClickListener((parent, view, position, id) -> {
                com.john.TreeApp.beans.ScionGroup selectedGroup = scionGroups.get(position);

                // Find ONE unattached scion of this type
                List<com.john.TreeApp.beans.Scion> unattachedScions = scionDAO.getUnattachedScions();
                com.john.TreeApp.beans.Scion scionToAttach = null;

                for (com.john.TreeApp.beans.Scion scion : unattachedScions) {
                    boolean matches = scion.getSpecies().equals(selectedGroup.getSpecies()) &&
                            ((scion.getVariety() == null && selectedGroup.getVariety() == null) ||
                                    (scion.getVariety() != null
                                            && scion.getVariety().equals(selectedGroup.getVariety())))
                            &&
                            ((scion.getSource() == null && selectedGroup.getSource() == null) ||
                                    (scion.getSource() != null && scion.getSource().equals(selectedGroup.getSource())));

                    if (matches) {
                        scionToAttach = scion;
                        break;
                    }
                }

                if (scionToAttach != null) {
                    // Add scion to tree
                    boolean success = treeScionDAO.addScionToTree(tree.getTreeId(), scionToAttach.getScionId());

                    if (success) {
                        Toast.makeText(this, "Scion added successfully", Toast.LENGTH_SHORT).show();

                        // Refresh the scions RecyclerView
                        List<com.john.TreeApp.beans.ScionGroup> updatedScions = treeScionDAO
                                .getScionsForTreeGrouped(tree.getTreeId());
                        com.john.TreeApp.adapters.ScionAdapter scionAdapter = (com.john.TreeApp.adapters.ScionAdapter) scionsRecyclerView
                                .getAdapter();
                        if (scionAdapter != null) {
                            scionAdapter.updateScions(updatedScions);
                        }

                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, "Failed to add scion", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No scion found", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show();
        }
    }

    private void setupRecyclerViews() {
        // Set up photos RecyclerView
        LinearLayoutManager photosLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        photosRecyclerView.setLayoutManager(photosLayoutManager);
        List<Image> images = imageDAO.getAllImages(tree.getTreeId());
        Log.d("EditTreeActivity", "Loaded " + images.size() + " photos for tree " + tree.getTreeId());
        photoAdapter = new PhotoAdapter(images);
        photosRecyclerView.setAdapter(photoAdapter);

        // Set up scions RecyclerView
        LinearLayoutManager scionsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        scionsRecyclerView.setLayoutManager(scionsLayoutManager);

        // Use grouped scions
        List<com.john.TreeApp.beans.ScionGroup> scionGroups = treeScionDAO.getScionsForTreeGrouped(tree.getTreeId());
        com.john.TreeApp.adapters.ScionAdapter scionAdapter = new com.john.TreeApp.adapters.ScionAdapter(scionGroups);

        scionAdapter.setOnScionRemoveListener(group -> {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Delete Scion")
                    .setMessage(
                            "Are you sure you want to delete one scion of this type from the tree? This will remove the association and delete the scion record.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Find ONE scion of this group attached to the tree
                        List<com.john.TreeApp.beans.Scion> attachedScions = treeScionDAO
                                .getScionsForTree(tree.getTreeId());
                        com.john.TreeApp.beans.Scion scionToRemove = null;

                        for (com.john.TreeApp.beans.Scion scion : attachedScions) {
                            boolean matches = scion.getSpecies().equals(group.getSpecies()) &&
                                    ((scion.getVariety() == null && group.getVariety() == null) ||
                                            (scion.getVariety() != null
                                                    && scion.getVariety().equals(group.getVariety())))
                                    &&
                                    ((scion.getSource() == null && group.getSource() == null) ||
                                            (scion.getSource() != null && scion.getSource().equals(group.getSource())));

                            if (matches) {
                                scionToRemove = scion;
                                break; // Found one
                            }
                        }

                        if (scionToRemove != null) {
                            // Use TreeService to discard the scion (removes association and deletes scion)
                            boolean success = treeService.discardScion(tree.getTreeId(), scionToRemove.getScionId());

                            if (success) {
                                // Update the list (reload from DB to be safe and accurate with counts)
                                List<com.john.TreeApp.beans.ScionGroup> updatedGroups = treeScionDAO
                                        .getScionsForTreeGrouped(tree.getTreeId());
                                scionAdapter.updateScions(updatedGroups);
                                Toast.makeText(this, "Scion removed", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to remove scion", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Could not find scion to remove", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        scionsRecyclerView.setAdapter(scionAdapter);

        // Set up notes RecyclerView
        LinearLayoutManager notesLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        notesRecyclerView.setLayoutManager(notesLayoutManager);
        NoteDAO noteDAO = new NoteDAOImpl();
        List<Note> notes = noteDAO.getNotes(tree.getTreeId());
        Log.d("EditTreeActivity", "Loaded " + notes.size() + " notes for tree " + tree.getTreeId());
        NoteAdapter noteAdapter = new NoteAdapter(notes, note -> {
            // Show full note content in a dialog
            showNoteDialog(note);
        });

        // Set up delete listener
        noteAdapter.setOnNoteDeleteListener(note -> {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Delete the note from database
                        noteDAO.deleteNote(note.getNoteID());

                        // Remove from adapter
                        noteAdapter.removeNote(note);

                        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        notesRecyclerView.setAdapter(noteAdapter);

        // Update page indicator
        updateNotesPageIndicator(0, notes.size());
        notesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (lm != null) {
                        int pos = lm.findFirstCompletelyVisibleItemPosition();
                        if (pos == RecyclerView.NO_POSITION) {
                            pos = lm.findFirstVisibleItemPosition();
                        }
                        updateNotesPageIndicator(pos, noteAdapter.getItemCount());
                    }
                }
            }
        });
    }

    private void updateNotesPageIndicator(int position, int total) {
        if (notesPageIndicator != null) {
            if (total <= 1) {
                notesPageIndicator.setVisibility(View.GONE);
            } else {
                notesPageIndicator.setVisibility(View.VISIBLE);
                notesPageIndicator.setText((position + 1) + " / " + total);
            }
        }
    }

    private void showNoteDialog(Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Note from " + dateFormat.format(note.getDateWritten()))
                .setMessage(note.getDescription())
                .setPositiveButton("Close", null)
                .setNeutralButton("Edit", (dialog, which) -> showEditNoteDialog(note));
        builder.show();
    }

    private void showEditNoteDialog(Note note) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_note, null);
        EditText noteInput = dialogView.findViewById(R.id.input_note);
        Button dateButton = dialogView.findViewById(R.id.button_date);

        // Set current note text
        noteInput.setText(note.getDescription());

        // Set current date
        dateButton.setText(dateFormat.format(note.getDateWritten()));

        // Store the current date
        java.util.Date[] selectedDate = { note.getDateWritten() };

        // Set up date picker
        dateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate[0] = calendar.getTime();
                        dateButton.setText(dateFormat.format(selectedDate[0]));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Edit Note")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newText = noteInput.getText().toString();
                    if (!TextUtils.isEmpty(newText)) {
                        // Update note in database
                        NoteDAO noteDAO = new NoteDAOImpl();
                        noteDAO.editNote(note.getNoteID(), newText, selectedDate[0]);

                        // Update RecyclerView
                        List<Note> updatedNotes = noteDAO.getNotes(tree.getTreeId());
                        ((NoteAdapter) notesRecyclerView.getAdapter()).updateNotes(updatedNotes);

                        Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditBasicInfoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_basic_info, null);
        EditText labelInput = dialogView.findViewById(R.id.input_label);
        EditText rootstockInput = dialogView.findViewById(R.id.input_rootstock);
        EditText varietyInput = dialogView.findViewById(R.id.input_variety);
        EditText datePlantedInput = dialogView.findViewById(R.id.input_date_planted);

        // Pre-fill current values
        labelInput.setText(tree.getLabel());
        rootstockInput.setText(tree.getRootstock());
        varietyInput.setText(tree.getVariety());
        if (tree.getDatePlanted() != null) {
            datePlantedInput.setText(dateFormat.format(tree.getDatePlanted()));
        }

        // Set up date picker for datePlantedInput
        datePlantedInput.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            if (tree.getDatePlanted() != null) {
                calendar.setTime(tree.getDatePlanted());
            }

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        datePlantedInput.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Edit Basic Information")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Update tree data
                    String newLabel = labelInput.getText().toString();
                    String newRootstock = rootstockInput.getText().toString();
                    String newVariety = varietyInput.getText().toString();
                    String newDatePlantedStr = datePlantedInput.getText().toString();

                    // Check if label is unique (only if it's different from current label)
                    if (!newLabel.equals(tree.getLabel()) && !treeService.isLabelUniqueForAdd(newLabel)) {
                        Toast.makeText(this, "Label is already in use. Choose a unique label.", Toast.LENGTH_LONG)
                                .show();
                        return;
                    }

                    tree.setLabel(newLabel);
                    tree.setRootstock(newRootstock);
                    tree.setVariety(newVariety);

                    // Parse and set date planted
                    if (!TextUtils.isEmpty(newDatePlantedStr)) {
                        try {
                            java.util.Date parsedDate = dateFormat.parse(newDatePlantedStr);
                            tree.setDatePlanted(new java.sql.Date(parsedDate.getTime()));
                        } catch (Exception e) {
                            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // Save to database
                    boolean updated = treeDAO.updateTree(tree);
                    if (!updated) {
                        Toast.makeText(this, "Failed to update tree information", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update UI
                    populateTreeData();
                    Toast.makeText(this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                    finish();

                    // Update action bar title
                    String title = "Edit Tree: " + tree.getEnglishName();
                    if (newLabel != null && !newLabel.isEmpty()) {
                        title += " (" + newLabel + ")";
                    }
                    setActionBarTitle(title);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Tree")
                .setMessage("Are you sure you want to delete this tree? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = treeDAO.deleteTree(treeId);
                    if (deleted) {
                        Toast.makeText(this, "Tree deleted successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_TREE_DELETED);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete tree", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddNoteDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_note, null);
        EditText noteInput = dialogView.findViewById(R.id.input_note);

        new AlertDialog.Builder(this)
                .setTitle("Add Note")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String noteContent = noteInput.getText().toString();
                    if (!TextUtils.isEmpty(noteContent)) {
                        // Create and save new note
                        NoteDAO noteDAO = new NoteDAOImpl();
                        Note newNote = new Note(tree.getTreeId(), new Date(System.currentTimeMillis()), noteContent);
                        noteDAO.addNote(newNote);

                        // Update RecyclerView
                        List<Note> updatedNotes = noteDAO.getNotes(tree.getTreeId());
                        ((NoteAdapter) notesRecyclerView.getAdapter()).updateNotes(updatedNotes);

                        // Auto-verify tree
                        verifyTree();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddReminderDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_reminder, null);
        EditText descriptionInput = dialogView.findViewById(R.id.input_reminder_description);
        EditText dateInput = dialogView.findViewById(R.id.input_reminder_date);
        CheckBox urgentCheckbox = dialogView.findViewById(R.id.checkbox_urgent);

        final Calendar calendar = Calendar.getInstance();
        dateInput.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                dateInput.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Add Reminder")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String description = descriptionInput.getText().toString();
                    String dateStr = dateInput.getText().toString();
                    boolean isUrgent = urgentCheckbox.isChecked();

                    if (!TextUtils.isEmpty(description)) {
                        Date reminderDate = null;
                        if (!TextUtils.isEmpty(dateStr)) {
                            try {
                                reminderDate = dateFormat.parse(dateStr);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        Reminder reminder = new Reminder(0, (int) tree.getTreeId(), new Date(), reminderDate,
                                description, isUrgent);
                        reminderDAO.addReminder(reminder);

                        Toast.makeText(this, "Reminder added successfully", Toast.LENGTH_SHORT).show();

                        // Auto-verify tree
                        verifyTree();
                    } else {
                        Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMoveCollectionDialog() {
        CollectionDAO collectionDAO = new CollectionDAOImpl();
        List<Collection> allCollections = collectionDAO.getAllCollections();

        if (allCollections.isEmpty()) {
            Toast.makeText(this, "No collections found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a Spinner in a new simple layout
        final Spinner spinner = new Spinner(this);
        // Add some padding
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        spinner.setPadding(padding, padding, padding, padding);

        ArrayAdapter<Collection> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, allCollections);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Pre-select current collection
        for (int i = 0; i < allCollections.size(); i++) {
            if (allCollections.get(i).getId() == tree.getCollectionId()) {
                spinner.setSelection(i);
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Move to Collection")
                .setView(spinner)
                .setPositiveButton("Move", (dialog, which) -> {
                    Collection selectedCollection = (Collection) spinner.getSelectedItem();
                    if (selectedCollection != null) {
                        if (selectedCollection.getId() == tree.getCollectionId()) {
                            Toast.makeText(this, "Tree is already in this collection", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Update tree
                        tree.setCollectionId(selectedCollection.getId());
                        boolean success = treeDAO.updateTree(tree);
                        if (success) {
                            Toast.makeText(this, "Moved to " + selectedCollection.getName(), Toast.LENGTH_SHORT).show();
                            // Finish to force refresh when returning
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to move tree", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Initialize tree data by retrieving the treeId from the intent,
     * then loading the corresponding Tree object.
     * 
     * @return true if successful, false otherwise.
     */
    private boolean initTreeDataFromIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "Intent is null", Toast.LENGTH_SHORT).show();
            return false;
        }

        treeId = intent.getIntExtra("treeId", -1);
        if (treeId == -1) {
            Toast.makeText(this, "Invalid tree ID", Toast.LENGTH_SHORT).show();
            return false;
        }

        Log.d(TAG, "Initializing tree data for tree ID: " + treeId);
        tree = treeDAO.findATree_fromId(treeId);

        if (tree == null) {
            Log.e(TAG, "Tree not found with ID: " + treeId);
            Toast.makeText(this, "Tree not found", Toast.LENGTH_SHORT).show();
            return false;
        }

        Log.d(TAG, "Successfully loaded tree: ID=" + tree.getTreeId() +
                ", Label=" + tree.getLabel() +
                ", Latin=" + tree.getLatinName() +
                ", Collection=" + tree.getCollectionId());
        return true;
    }

    /**
     * Create a Bundle containing the tree data.
     */
    private Bundle createTreeBundle() {
        Bundle args = new Bundle();
        args.putString("LABEL", tree.getLabel());
        args.putString("ENGLISH_NAME", tree.getEnglishName());
        args.putString("LATIN_NAME", tree.getLatinName());
        args.putString("ROOTSTOCK", tree.getRootstock());
        args.putString("VARIETY", tree.getVariety());
        return args;
    }

    /**
     * Load the primary fields fragment into its container.
     */
    private void loadPrimaryFragment(Bundle args) {
        primaryFragment = new EditTree_primaryFields();
        primaryFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_edit_primary, primaryFragment)
                .commit();
    }

    private void startLocationUpdate() {
        // When "Update Location" is pressed, load the LocationFragment.
        LocationFragment newLocationFragment = new LocationFragment();
        // Pass a flag to enable the Start GPS button once the view is created.
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putBoolean("enableStartGPS", true);
        newLocationFragment.setArguments(fragmentArgs);
        newLocationFragment.setLocationListener(this);

        Log.d(TAG, "Swapping for LocationFragment and hiding other cards...");
        
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_edit_primary, newLocationFragment)
                .commit();
        
        // Hide UI elements to focus on GPS
        basicInfoCard.setVisibility(View.GONE);
        locationCard.setVisibility(View.GONE);
        scionsCard.setVisibility(View.GONE);
        photosCard.setVisibility(View.GONE);
        notesCard.setVisibility(View.GONE);
        findViewById(R.id.action_buttons_scroll).setVisibility(View.GONE);
        
        locationFragment = newLocationFragment;
    }

    private void restoreUIfromLocation() {
        // Remove LocationFragment
        if (locationFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(locationFragment)
                    .commit();
            locationFragment = null;
        }

        // Show UI elements again
        basicInfoCard.setVisibility(View.VISIBLE);
        locationCard.setVisibility(View.VISIBLE);
        scionsCard.setVisibility(View.VISIBLE);
        photosCard.setVisibility(View.VISIBLE);
        notesCard.setVisibility(View.VISIBLE);
        findViewById(R.id.action_buttons_scroll).setVisibility(View.VISIBLE);
        
        // Scroll to top
        NestedScrollView scrollView = findViewById(R.id.scroll_view);
        scrollView.scrollTo(0, 0);
    }

    /**
     * Called when "Cancel" (Plant Now button now says "Cancel") is pressed.
     */
    private void finishPlantingProcess() {
        if (locationFragment != null) {
            locationFragment.stopLocationUpdates();
        }
        restoreUIfromLocation();
    }

    /**
     * Save the tree to the database using the averaged location.
     * The datePlanted is set to the current date and time.
     */
    private void saveTreeToDatabase(double latitude, double longitude) {
        int collectionId = tree.getCollectionId(); // Use current tree's collection
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dateUpdated = LocalDateTime.now().format(dtf);
        
        // Create a location record for the tree
        com.john.TreeApp.beans.Location location = new com.john.TreeApp.beans.Location();
        location.setLatitude(latitude);
        location.setLongitude(longitude);
 
        String message = treeService.updateTreeWithLocationAndLabel(
                tree.getTreeId(),
                collectionId,
                location,
                tree.getLabel()); // Use current label
        
        Toast.makeText(EditTreeActivity.this, message, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Tree location updated: ID=" + tree.getTreeId() + " on " + dateUpdated);
        
        restoreUIfromLocation();
    }

    @Override
    public void onLocationAveraged(Location averagedLocation) {
        if (averagedLocation != null) {
            double latitude = averagedLocation.getLatitude();
            double longitude = averagedLocation.getLongitude();
            saveTreeToDatabase(latitude, longitude);
        } else {
            Toast.makeText(EditTreeActivity.this, "Not enough accurate GPS readings. Try again.", Toast.LENGTH_SHORT)
                    .show();
            restoreUIfromLocation();
        }
    }

    @Override
    public void onLocationCancelled() {
        restoreUIfromLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationFragment != null) {
            locationFragment.stopLocationUpdates();
        }
    }

    private void populateTreeData() {
        if (tree != null) {
            // Collection info
            CollectionDAO collectionDAO = new CollectionDAOImpl();
            Collection treeCollection = collectionDAO.getCollection(tree.getCollectionId());
            if (treeCollection != null) {
                collectionNameView.setText(treeCollection.getName());
            }

            labelView.setText(tree.getLabel() != null ? tree.getLabel() : "No label");
            englishNameView.setText(tree.getEnglishName());
            latinNameView.setText(tree.getLatinName());
            rootstockView.setText(tree.getRootstock() != null ? tree.getRootstock() : "Not specified");
            varietyView.setText(tree.getVariety() != null ? tree.getVariety() : "Not specified");

            // Color-code label background based on status
            String status = tree.getStatus() != null ? tree.getStatus() : "unverified";
            switch (status) {
                case "verified":
                    labelView.setBackgroundColor(0xFFC8E6C9); // green
                    break;
                case "lost":
                    labelView.setBackgroundColor(0xFFE0E0E0); // grey
                    break;
                default: // unverified
                    labelView.setBackgroundColor(0xFFFFFFFF); // white
                    break;
            }

            // Set up status spinner
            String[] statusOptions = { "verified", "unverified", "lost" };
            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, statusOptions);
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            statusSpinner.setAdapter(statusAdapter);

            // Pre-select current status
            for (int i = 0; i < statusOptions.length; i++) {
                if (statusOptions[i].equals(status)) {
                    statusSpinnerInitialized = false;
                    statusSpinner.setSelection(i);
                    break;
                }
            }

            statusSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (!statusSpinnerInitialized) {
                        statusSpinnerInitialized = true;
                        return;
                    }
                    String newStatus = statusOptions[position];
                    tree.setStatus(newStatus);
                    treeDAO.updateTreeStatus(tree.getTreeId(), newStatus);
                    // Update label background color
                    switch (newStatus) {
                        case "verified":
                            labelView.setBackgroundColor(0xFFC8E6C9);
                            break;
                        case "lost":
                            labelView.setBackgroundColor(0xFFE0E0E0);
                            break;
                        default:
                            labelView.setBackgroundColor(0xFFFFFFFF);
                            break;
                    }
                    Toast.makeText(EditTreeActivity.this, "Status set to " + newStatus, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
            TextView datePlantedView = findViewById(R.id.tv_date_planted);
            datePlantedView.setText(
                    tree.getDatePlanted() != null ? dateFormat.format(tree.getDatePlanted()) : "Not specified");

            TextView fruitingInfoView = findViewById(R.id.tv_fruiting_info);
            String fruitingInfo = "Not specified";

            // 1. Check attached scions
            List<com.john.TreeApp.beans.Scion> attachedScions = treeScionDAO.getScionsForTree(tree.getTreeId());
            for (com.john.TreeApp.beans.Scion scion : attachedScions) {
                if (scion.getFruitingDescription() != null && !scion.getFruitingDescription().isEmpty()) {
                    fruitingInfo = scion.getFruitingDescription();
                    break;
                }
            }

            // 2. Lookup in Scion Library (attached = 0)
            if (fruitingInfo.equals("Not specified") && tree.getVariety() != null && !tree.getVariety().isEmpty()) {
                List<com.john.TreeApp.beans.Scion> libraryScions = scionDAO.getScionsBySpecies(tree.getLatinName());
                for (com.john.TreeApp.beans.Scion scion : libraryScions) {
                    if (!scion.isAttached() && tree.getVariety().equalsIgnoreCase(scion.getVariety())) {
                        if (scion.getFruitingDescription() != null && !scion.getFruitingDescription().isEmpty()) {
                            fruitingInfo = scion.getFruitingDescription();
                            break;
                        }
                    }
                }
            }

            // 3. Species Default
            if (fruitingInfo.equals("Not specified")) {
                db.TreeSpeciesDAO speciesDAO = new db.TreeSpeciesDAOImpl();
                com.john.TreeApp.beans.TreeSpecies species = speciesDAO.findTreesSpecies_Latin(tree.getLatinName());
                if (species != null && species.getFruitingDescription() != null
                        && !species.getFruitingDescription().isEmpty()) {
                    fruitingInfo = species.getFruitingDescription();
                }
            }

            fruitingInfoView.setText(fruitingInfo);

            // Load photos and notes
            setupRecyclerViews();
        }
    }

    private void verifyTree() {
        if (tree != null && !"verified".equals(tree.getStatus())) {
            tree.setStatus("verified");
            treeDAO.updateTreeStatus(tree.getTreeId(), "verified");
            populateTreeData(); // Refresh UI including color-coding and spinner
        }
    }

    private void dispatchTakePictureIntent() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.CAMERA }, REQUEST_CAMERA_PERMISSION);
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.john.TreeApp.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name using our agreed naming convention
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "tree_" + treeId + "_" + timeStamp;

        File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Trees");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Save the image to the database
            if (currentPhotoPath != null) {
                // Get just the filename from the full path
                String fileName = new File(currentPhotoPath).getName();
                // Convert java.util.Date to java.sql.Date and int to long
                java.util.Date utilDate = new Date();
                java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
                imageDAO.addImage((long) treeId, sqlDate, fileName);

                // Refresh the photos list
                List<Image> updatedImages = imageDAO.getAllImages(treeId);
                photoAdapter.updatePhotos(updatedImages);

                // Auto-verify tree
                verifyTree();

                Toast.makeText(this, "Photo saved successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openMapViewWithTree() {
        // Create intent to open MapViewActivity
        Intent intent = new Intent(this, MapViewActivity.class);

        // Create ArrayList with current tree ID
        ArrayList<Integer> selectedTreeIds = new ArrayList<>();
        selectedTreeIds.add(tree.getTreeId());

        // Add the selected tree IDs to the intent
        intent.putIntegerArrayListExtra("SELECTED_TREE_IDS", selectedTreeIds);

        // Add the collection ID to ensure consistency
        intent.putExtra("collectionId", tree.getCollectionId());

        // Start MapViewActivity
        startActivity(intent);
    }
}
