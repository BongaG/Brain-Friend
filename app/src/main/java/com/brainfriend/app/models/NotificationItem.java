package com.brainfriend.app.models;

public class NotificationItem {
    public static final int TYPE_ALERT = 0;
    public static final int TYPE_OVERDUE = 1;
    public static final int TYPE_DONE = 2;

    private String title;
    private String body;
    private String aiMessage;
    private int type;
    private String taskId;
    private String taskTitle;
    private int importance;
    private String category;
    private boolean aiLoaded = false;

    public NotificationItem(String title, String body, int type,
                            String taskId, String taskTitle,
                            int importance, String category) {
        this.title = title;
        this.body = body;
        this.type = type;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.importance = importance;
        this.category = category != null ? category : "Personal";
        this.aiMessage = body;
    }

    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getAiMessage() { return aiMessage; }
    public void setAiMessage(String msg) { this.aiMessage = msg; }
    public int getType() { return type; }
    public String getTaskId() { return taskId; }
    public String getTaskTitle() { return taskTitle; }
    public int getImportance() { return importance; }
    public String getCategory() { return category; }
    public boolean isAiLoaded() { return aiLoaded; }
    public void setAiLoaded(boolean loaded) { this.aiLoaded = loaded; }
}