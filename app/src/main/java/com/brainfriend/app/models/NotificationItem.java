package com.brainfriend.app.models;

public class NotificationItem {
    public static final int TYPE_ALERT = 0;   // orange
    public static final int TYPE_OVERDUE = 1; // red
    public static final int TYPE_DONE = 2;    // green

    private String title;
    private String body;
    private int type;

    public NotificationItem(String title, String body, int type) {
        this.title = title;
        this.body = body;
        this.type = type;
    }

    public String getTitle() { return title; }
    public String getBody() { return body; }
    public int getType() { return type; }
}