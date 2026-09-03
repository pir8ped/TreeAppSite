package com.john.TreeApp.adapters;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.john.TreeApp.R;
import com.john.TreeApp.beans.Reminder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> items = new ArrayList<>();
    private db.ReminderDAO reminderDAO;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private Runnable onDataChanged;

    public ReminderAdapter(Context context, db.ReminderDAO reminderDAO, Runnable onDataChanged) {
        this.context = context;
        this.reminderDAO = reminderDAO;
        this.onDataChanged = onDataChanged;
    }

    public void setData(List<Reminder> reminders) {
        items.clear();
        String currentCollection = null;
        for (Reminder r : reminders) {
            String col = r.getCollectionName();
            if (col == null) col = "Uncategorized";
            if (!col.equals(currentCollection)) {
                items.add(col);
                currentCollection = col;
            }
            items.add(r);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else {
            ((ItemViewHolder) holder).bind((Reminder) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        HeaderViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.header_collection_name);
        }

        void bind(String collectionName) {
            headerText.setText(collectionName);
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView urgentIndicator;
        TextView dateText;
        TextView descriptionText;
        TextView treeLabelText;
        View contentContainer;

        ItemViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.reminder_checkbox);
            urgentIndicator = itemView.findViewById(R.id.reminder_urgent_indicator);
            dateText = itemView.findViewById(R.id.reminder_date);
            descriptionText = itemView.findViewById(R.id.reminder_description);
            treeLabelText = itemView.findViewById(R.id.reminder_tree_label);
            contentContainer = itemView.findViewById(R.id.reminder_content_container);
        }

        void bind(Reminder reminder) {
            descriptionText.setText(reminder.getDescription());
            dateText.setText(reminder.getReminderDate() != null ? dateFormat.format(reminder.getReminderDate()) : "No date");
            treeLabelText.setText("Tree: " + (reminder.getTreeLabel() != null ? reminder.getTreeLabel() : "Unknown"));
            urgentIndicator.setVisibility(reminder.isUrgent() ? View.VISIBLE : View.GONE);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(false);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    reminderDAO.deleteReminder(reminder.getReminderId());
                    Toast.makeText(context, "Reminder dismissed", Toast.LENGTH_SHORT).show();
                    onDataChanged.run();
                }
            });

            View.OnClickListener editListener = v -> showEditDialog(reminder);
            contentContainer.setOnClickListener(editListener);
        }
    }

    private void showEditDialog(Reminder reminder) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_reminder, null);
        EditText descriptionInput = dialogView.findViewById(R.id.input_reminder_description);
        EditText dateInput = dialogView.findViewById(R.id.input_reminder_date);
        CheckBox urgentCheckbox = dialogView.findViewById(R.id.checkbox_urgent);

        descriptionInput.setText(reminder.getDescription());
        if (reminder.getReminderDate() != null) {
            dateInput.setText(dateFormat.format(reminder.getReminderDate()));
        }
        urgentCheckbox.setChecked(reminder.isUrgent());

        final Calendar calendar = Calendar.getInstance();
        if (reminder.getReminderDate() != null) {
            calendar.setTime(reminder.getReminderDate());
        }

        dateInput.setOnClickListener(v -> {
            new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                dateInput.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(context)
                .setTitle("Edit Reminder")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
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

                        reminder.setDescription(description);
                        reminder.setReminderDate(reminderDate);
                        reminder.setUrgent(isUrgent);
                        reminderDAO.updateReminder(reminder);
                        Toast.makeText(context, "Reminder updated", Toast.LENGTH_SHORT).show();
                        onDataChanged.run();
                    } else {
                        Toast.makeText(context, "Description cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
