package com.brainfriend.app.models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Date;

@IgnoreExtraProperties
public class Task {
    private String id;
    private String title;
    private String details;
    private String userId;
    private boolean completed;
    private int importance; // 1=Low, 2=Med, 3=High
    private String category;
    private boolean recurring;
    private Date dueDate;
    private int dueHour;
    private int dueMinute;
    private boolean alertEnabled;

    public Task() {}

    public Task(String title, String details, String userId, boolean completed,
                int importance, String category, boolean recurring, Date dueDate,
                int dueHour, int dueMinute, boolean alertEnabled) {
        this.title = title;
        this.details = details;
        this.userId = userId;
        this.completed = completed;
        this.importance = importance;
        this.category = category;
        this.recurring = recurring;
        this.dueDate = dueDate;
        this.dueHour = dueHour;
        this.dueMinute = dueMinute;
        this.alertEnabled = alertEnabled;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public int getImportance() { return importance; }
    public void setImportance(int importance) { this.importance = importance; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public Date getDueDate() { return dueDate != null ? dueDate : new Date(); }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public int getDueHour() { return dueHour; }
    public void setDueHour(int dueHour) { this.dueHour = dueHour; }
    public int getDueMinute() { return dueMinute; }
    public void setDueMinute(int dueMinute) { this.dueMinute = dueMinute; }
    public boolean isAlertEnabled() { return alertEnabled; }
    public void setAlertEnabled(boolean alertEnabled) { this.alertEnabled = alertEnabled; }
}