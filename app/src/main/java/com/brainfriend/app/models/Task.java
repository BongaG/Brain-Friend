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
    private int importance; // 1: Low, 2: Med, 3: High
    private String category;
    private boolean recurring;
    private Date dueDate;

    public Task() {} // Required for Firestore

    public Task(String title, String details, String userId, boolean completed, int importance, String category, boolean recurring, Date dueDate) {
        this.title = title;
        this.details = details;
        this.userId = userId;
        this.completed = completed;
        this.importance = importance;
        this.category = category;
        this.recurring = recurring;
        this.dueDate = dueDate;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public String getDetails() { return details; }
    public String getUserId() { return userId; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public int getImportance() { return importance; }
    public String getCategory() { return category; }
    public boolean isRecurring() { return recurring; }
    public Date getDueDate() { return dueDate != null ? dueDate : new Date(); }
}