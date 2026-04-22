package com.brainfriend.app.models;

import com.google.firebase.Timestamp;
import java.util.Date;

public class Task {
    private String id;
    private String title;
    private String details;
    private String userId;
    private boolean completed;
    private int importance;
    private Date dueDate;
    private Timestamp createdAt;

    public Task() {}

    public Task(String title, String details, String userId, boolean completed, int importance, Date dueDate) {
        this.title = title;
        this.details = details;
        this.userId = userId;
        this.completed = completed;
        this.importance = importance;
        this.dueDate = dueDate;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters ...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public String getDetails() { return details; }
    public int getImportance() { return importance; }
    public Date getDueDate() { return dueDate; }
    public boolean isCompleted() { return completed; }
}