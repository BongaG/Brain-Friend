package com.brainfriend.app.reminders;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "reminders")
public class ReminderEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String description;
    public long triggerTimeMs;
    public boolean isActive;

    public ReminderEntity() {}

    public ReminderEntity(String title, long triggerTimeMs) {
        this.title = title;
        this.triggerTimeMs = triggerTimeMs;
        this.isActive = true;
    }
}
