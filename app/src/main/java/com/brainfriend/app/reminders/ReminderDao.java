package com.brainfriend.app.reminders;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;


@Dao
public interface ReminderDao {

    @Insert
    long insert(ReminderEntity reminder);

    @Update
    void update(ReminderEntity reminder);

    @Delete
    void delete(ReminderEntity reminder);

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY triggerTimeMs ASC")
    LiveData<List<ReminderEntity>> getActiveReminders();

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    ReminderEntity getById(long id);

    @Query("UPDATE reminders SET isActive = 0 WHERE id = :id")
    void markDone(long id);

    @Query("DELETE FROM reminders WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM reminders ORDER BY triggerTimeMs ASC")
    LiveData<List<ReminderEntity>> getAllReminders();
}
