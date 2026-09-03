package com.john.TreeApp.beans;

import java.util.Date;

public class Reminder {
    private int reminderId;
    private int treeId;
    private Date dateWritten;
    private Date reminderDate;
    private String description;
    private boolean isUrgent;
    private String collectionName; // Joined field
    private String treeLabel;      // Joined field

    // Constructor
    public Reminder(int reminderId, int treeId, Date dateWritten, Date reminderDate, String description, boolean isUrgent) {
        this.reminderId = reminderId;
        this.treeId = treeId;
        this.dateWritten = dateWritten;
        this.reminderDate = reminderDate;
        this.description = description;
        this.isUrgent = isUrgent;
    }

    public Reminder(int reminderId, int treeId, Date dateWritten, Date reminderDate, String description, boolean isUrgent, String collectionName, String treeLabel) {
        this(reminderId, treeId, dateWritten, reminderDate, description, isUrgent);
        this.collectionName = collectionName;
        this.treeLabel = treeLabel;
    }

    // Default Constructor
    public Reminder() {
    }

    // Getters and Setters
    public int getReminderId() {
        return reminderId;
    }

    public void setReminderId(int reminderId) {
        this.reminderId = reminderId;
    }

    public int gettreeId() {
        return treeId;
    }

    public void settreeId(int treeId) {
        this.treeId = treeId;
    }

    public Date getDateWritten() {
        return dateWritten;
    }

    public void setDateWritten(Date dateWritten) {
        this.dateWritten = dateWritten;
    }

    public Date getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(Date reminderDate) {
        this.reminderDate = reminderDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isUrgent() {
        return isUrgent;
    }

    public void setUrgent(boolean urgent) {
        isUrgent = urgent;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getTreeLabel() {
        return treeLabel;
    }

    public void setTreeLabel(String treeLabel) {
        this.treeLabel = treeLabel;
    }

    // Optionally, override toString, equals, and hashCode methods
    @Override
    public String toString() {
        return "Reminder{" +
                "reminderId=" + reminderId +
                ", treeId=" + treeId +
                ", dateWritten=" + dateWritten +
                ", reminderDate=" + reminderDate +
                ", description='" + description + '\'' +
                ", isUrgent=" + isUrgent +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reminder reminder = (Reminder) o;

        return reminderId == reminder.reminderId;
    }

    @Override
    public int hashCode() {
        return (int) (reminderId ^ (reminderId >>> 32));
    }
}
