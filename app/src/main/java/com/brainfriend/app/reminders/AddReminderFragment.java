package com.brainfriend.app.reminders;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.brainfriend.app.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;


public class AddReminderFragment extends Fragment {

    // UI refs
    private TextView tvTranscribed;
    private TextView tvScheduledTime;
    private Button  btnMic;
    private Button  btnPickTime;
    private Button  btnSave;

    // State
    private String transcribedText = "";
    private Calendar scheduledTime  = null;

    // Permission launcher
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchSpeechRecogniser();
                else Toast.makeText(getContext(),
                        "Microphone permission is needed to record your reminder", Toast.LENGTH_LONG).show();
            });

    // Speech recogniser result launcher
    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK
                        && result.getData() != null) {
                    ArrayList<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        transcribedText = matches.get(0);
                        tvTranscribed.setText(transcribedText);
                        tvTranscribed.setVisibility(View.VISIBLE);
                    }
                }
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate your fragment_add_reminder.xml (see companion XML file)
        return inflater.inflate(R.layout.fragment_add_reminder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTranscribed   = view.findViewById(R.id.tv_transcribed);
        tvScheduledTime = view.findViewById(R.id.tv_scheduled_time);
        btnMic          = view.findViewById(R.id.btn_mic);
        btnPickTime     = view.findViewById(R.id.btn_pick_time);
        btnSave         = view.findViewById(R.id.btn_save_reminder);

        btnMic.setOnClickListener(v -> checkMicAndLaunch());
        btnPickTime.setOnClickListener(v -> showDateTimePicker());
        btnSave.setOnClickListener(v -> saveReminder());
    }

    // -------------------------------------------------------------------------
    // Speech recognition
    // -------------------------------------------------------------------------

    private void checkMicAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchSpeechRecogniser();
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void launchSpeechRecogniser() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your reminder…");
        speechLauncher.launch(intent);
    }


    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
            new TimePickerDialog(requireContext(), (tp, hour, minute) -> {
                scheduledTime = Calendar.getInstance();
                scheduledTime.set(year, month, day, hour, minute, 0);
                scheduledTime.set(Calendar.MILLISECOND, 0);

                String formatted = String.format(Locale.getDefault(),
                        "%02d/%02d/%d at %02d:%02d", day, month + 1, year, hour, minute);
                tvScheduledTime.setText("⏰ " + formatted);
                tvScheduledTime.setVisibility(View.VISIBLE);
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }


    private void saveReminder() {
        if (transcribedText.isEmpty()) {
            Toast.makeText(getContext(), "Please record your reminder first 🎙", Toast.LENGTH_SHORT).show();
            return;
        }
        if (scheduledTime == null) {
            Toast.makeText(getContext(), "Please pick a time for your reminder ⏰", Toast.LENGTH_SHORT).show();
            return;
        }
        if (scheduledTime.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(getContext(), "Please pick a future time", Toast.LENGTH_SHORT).show();
            return;
        }

        ReminderEntity entity = new ReminderEntity(transcribedText, scheduledTime.getTimeInMillis());

        // Insert on background thread, then schedule alarm on main thread
        new Thread(() -> {
            long newId = AppDatabase.getInstance(requireContext())
                    .reminderDao().insert(entity);
            requireActivity().runOnUiThread(() -> {
                ReminderScheduler.schedule(requireContext(), newId,
                        transcribedText, scheduledTime.getTimeInMillis());
                Toast.makeText(getContext(), "Reminder saved! 🧠✅", Toast.LENGTH_SHORT).show();
                clearForm();
            });
        }).start();
    }

    private void clearForm() {
        transcribedText = "";
        scheduledTime   = null;
        tvTranscribed.setVisibility(View.GONE);
        tvTranscribed.setText("");
        tvScheduledTime.setVisibility(View.GONE);
    }
}
