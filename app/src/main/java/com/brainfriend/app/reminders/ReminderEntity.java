package com.brainfriend.app.reminders;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class ReminderEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String description;
    public long triggerTimeMs;
    public boolean isActive;

    // Empty constructor – not used by Room, so ignore it
    @Ignore
    public ReminderEntity() {}

    // Main constructor for Room
    public ReminderEntity(String title, long triggerTimeMs) {
        this.title = title;
        this.triggerTimeMs = triggerTimeMs;
        this.isActive = true;
    }
}