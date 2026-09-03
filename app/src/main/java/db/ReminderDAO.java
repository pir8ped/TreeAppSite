package db;

import com.john.TreeApp.beans.Reminder;

import java.sql.Date;
import java.util.List;

public interface ReminderDAO {
    List<Reminder> getAllReminders();

    List<Reminder> getAllRemindersUpToADate(Date date);

    List<Reminder> getAllRemindersForTheNextDay();

    List<Reminder> getAllRemindersForTheNextWeek();

    List<Reminder> getAllRemindersForTheNextMonth();

    long addReminder(Reminder reminder);

    boolean updateReminder(Reminder reminder);

    List<Reminder> getAllRemindersWithMetadata();

    void deleteReminder(int reminderId);
}
