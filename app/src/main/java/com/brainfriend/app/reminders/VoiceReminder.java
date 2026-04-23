package com.brainfriend.app.reminders;



public class VoiceReminder {

    private long id;
    private String title;          // transcribed text from speech
    private String description;    // optional extra note
    private long triggerTimeMs;    // epoch millis when alarm fires
    private boolean isActive;      // false once dismissed/completed

    public VoiceReminder() {}

    public VoiceReminder(String title, long triggerTimeMs) {
        this.title = title;
        this.triggerTimeMs = triggerTimeMs;
        this.isActive = true;
    }


    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTriggerTimeMs() { return triggerTimeMs; }
    public void setTriggerTimeMs(long triggerTimeMs) { this.triggerTimeMs = triggerTimeMs; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
