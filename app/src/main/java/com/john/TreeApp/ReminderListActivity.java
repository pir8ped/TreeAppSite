package com.john.TreeApp;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.john.TreeApp.adapters.ReminderAdapter;
import db.ReminderDAO;
import db.ReminderDAOImpl;

public class ReminderListActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private ReminderAdapter adapter;
    private ReminderDAO reminderDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_reminder_list);
        setActionBarTitle("Reminders");

        reminderDAO = new ReminderDAOImpl();
        recyclerView = findViewById(R.id.reminder_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ReminderAdapter(this, reminderDAO, this::loadReminders);
        recyclerView.setAdapter(adapter);

        loadReminders();
    }

    private void loadReminders() {
        adapter.setData(reminderDAO.getAllRemindersWithMetadata());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }
}
