package com.brainfriend.app.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class RoutineItem {
    private String id;
    private String title;
    private String userId;
    private String category;
    private int importance;
    private int dueHour;
    private int dueMinute;
    private boolean completed;

    public RoutineItem() {}

    public RoutineItem(String title, String userId, String category,
                       int importance, int dueHour, int dueMinute) {
        this.title = title;
        this.userId = userId;
        this.category = category;
        this.importance = importance;
        this.dueHour = dueHour;
        this.dueMinute = dueMinute;
        this.completed = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getImportance() { return importance; }
    public void setImportance(int importance) { this.importance = importance; }
    public int getDueHour() { return dueHour; }
    public void setDueHour(int dueHour) { this.dueHour = dueHour; }
    public int getDueMinute() { return dueMinute; }
    public void setDueMinute(int dueMinute) { this.dueMinute = dueMinute; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}