package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.john.TreeApp.beans.Reminder;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReminderDAOImpl extends DAOBase implements ReminderDAO {

    public ReminderDAOImpl() {
        super();
    }

    @Override
    public List<Reminder> getAllReminders() {
        return queryReminders(null, null);
    }

    @Override
    public List<Reminder> getAllRemindersUpToADate(Date date) {
        String selection = "reminderDate <= ?";
        String[] selectionArgs = { new Timestamp(date.getTime()).toString() };
        return queryReminders(selection, selectionArgs);
    }

    @Override
    public List<Reminder> getAllRemindersForTheNextDay() {
        return getRemindersForNextPeriod(Calendar.DAY_OF_MONTH, 1);
    }

    @Override
    public List<Reminder> getAllRemindersForTheNextWeek() {
        return getRemindersForNextPeriod(Calendar.WEEK_OF_YEAR, 1);
    }

    @Override
    public List<Reminder> getAllRemindersForTheNextMonth() {
        return getRemindersForNextPeriod(Calendar.MONTH, 1);
    }

    @Override
    public long addReminder(Reminder reminder) {
        if (!isDatabaseReady()) {
            return -1;
        }
        final long[] reminderId = { -1 };
        executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("treeId", reminder.gettreeId());
            values.put("dateWritten", reminder.getDateWritten() != null ? new SimpleDateFormat("yyyy-MM-dd").format(reminder.getDateWritten()) : null);
            values.put("reminderDate", reminder.getReminderDate() != null ? new Timestamp(reminder.getReminderDate().getTime()).toString() : null);
            values.put("description", reminder.getDescription());
            values.put("isUrgent", reminder.isUrgent() ? 1 : 0);

            reminderId[0] = getDatabase().insert("Reminder", null, values);
        });
        return reminderId[0];
    }

    @Override
    public boolean updateReminder(Reminder reminder) {
        if (!isDatabaseReady()) {
            return false;
        }
        final boolean[] success = { false };
        executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("reminderDate", reminder.getReminderDate() != null ? new Timestamp(reminder.getReminderDate().getTime()).toString() : null);
            values.put("description", reminder.getDescription());
            values.put("isUrgent", reminder.isUrgent() ? 1 : 0);

            int rows = getDatabase().update("Reminder", values, "reminderId = ?", new String[] { String.valueOf(reminder.getReminderId()) });
            success[0] = rows > 0;
        });
        return success[0];
    }

    @Override
    public List<Reminder> getAllRemindersWithMetadata() {
        if (!isDatabaseReady()) {
            return new ArrayList<>();
        }
        List<Reminder> reminders = new ArrayList<>();
        String query = "SELECT r.*, c.name as collectionName, t.label as treeLabel " +
                "FROM Reminder r " +
                "JOIN Tree t ON r.treeId = t.treeId " +
                "JOIN Collection c ON t.collectionId = c.id " +
                "ORDER BY c.name ASC, r.isUrgent DESC, r.reminderDate DESC, r.dateWritten DESC";

        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, null);
            while (cursor.moveToNext()) {
                reminders.add(cursorToReminder(cursor));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return reminders;
    }

    @Override
    public void deleteReminder(int reminderId) {
        try {
            String whereClause = "reminderId = ?";
            String[] whereArgs = { String.valueOf(reminderId) };

            int rowsAffected = getDatabase().delete("Reminder", whereClause, whereArgs);

            if (rowsAffected > 0) {
                Log.d("ReminderDAOImpl", "Reminder deleted successfully.");
            } else {
                Log.d("ReminderDAOImpl", "No reminder found with the given ID.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ReminderDAOImpl", "Error deleting reminder", e);
        }
    }

    private List<Reminder> queryReminders(String selection, String[] selectionArgs) {
        List<Reminder> reminders = new ArrayList<>();
        Cursor cursor = null;

        try {
            String[] projection = {
                    "reminderId",
                    "treeId",
                    "dateWritten",
                    "reminderDate",
                    "description",
                    "isUrgent"
            };

            cursor = getDatabase().query(
                    "Reminder",
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null);

            while (cursor.moveToNext()) {
                reminders.add(cursorToReminder(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return reminders;
    }

    private Reminder cursorToReminder(Cursor cursor) {
        // Ensure the cursor is at the correct position
        if (cursor == null || cursor.isClosed()) {
            throw new IllegalStateException("Cursor is null or closed");
        }

        // Retrieve column indices
        int indexReminderId = cursor.getColumnIndexOrThrow("reminderId");
        int indextreeId = cursor.getColumnIndexOrThrow("treeId");
        int indexDateWritten = cursor.getColumnIndex("dateWritten");
        int indexReminderDate = cursor.getColumnIndex("reminderDate");
        int indexDescription = cursor.getColumnIndex("description");
        int indexIsUrgent = cursor.getColumnIndex("isUrgent");
        int indexCollectionName = cursor.getColumnIndex("collectionName");
        int indexTreeLabel = cursor.getColumnIndex("treeLabel");

        // Retrieve values from the cursor
        int reminderId = cursor.getInt(indexReminderId);
        int treeId = cursor.getInt(indextreeId);

        // Handle dateWritten as java.sql.Date
        String dateWrittenStr = cursor.getString(indexDateWritten);
        java.util.Date dateWritten = null;
        if (dateWrittenStr != null && indexDateWritten != -1) {
            try {
                dateWritten = new SimpleDateFormat("yyyy-MM-dd").parse(dateWrittenStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Handle reminderDate as Timestamp/Date
        String reminderDateStr = cursor.getString(indexReminderDate);
        java.util.Date reminderDate = null;
        if (reminderDateStr != null && indexReminderDate != -1) {
            try {
                if (reminderDateStr.contains(":")) {
                    reminderDate = Timestamp.valueOf(reminderDateStr);
                } else {
                    reminderDate = new SimpleDateFormat("yyyy-MM-dd").parse(reminderDateStr);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String description = indexDescription != -1 ? cursor.getString(indexDescription) : null;
        boolean isUrgent = indexIsUrgent != -1 && cursor.getInt(indexIsUrgent) == 1;
        String collectionName = indexCollectionName != -1 ? cursor.getString(indexCollectionName) : null;
        String treeLabel = indexTreeLabel != -1 ? cursor.getString(indexTreeLabel) : null;

        // Create and return a Reminder object
        return new Reminder(reminderId, treeId, dateWritten, reminderDate, description, isUrgent, collectionName, treeLabel);
    }

    private List<Reminder> getRemindersForNextPeriod(int calendarField, int amount) {
        Calendar calendar = Calendar.getInstance();
        Date startDate = (Date) calendar.getTime();
        calendar.add(calendarField, amount);
        Date endDate = (Date) calendar.getTime();

        String selection = "reminderDate BETWEEN ? AND ?";
        String[] selectionArgs = {
                new Timestamp(startDate.getTime()).toString(),
                new Timestamp(endDate.getTime()).toString()
        };

        return queryReminders(selection, selectionArgs);
    }
}
