package com.brainfriend.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.fragments.DashboardFragment;
import com.brainfriend.app.fragments.ExercisesFragment;
import com.brainfriend.app.fragments.RoutineFragment;
import com.brainfriend.app.fragments.SettingsFragment;
import com.brainfriend.app.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    // Session timeout — 5 minutes
    private static final long SESSION_TIMEOUT = 5 * 60 * 1000;
    private Handler sessionHandler = new Handler();
    private Runnable sessionRunnable;
    private boolean backPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Restore dark mode
        SharedPreferences prefs = getSharedPreferences("settings", 0);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(darkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.bottom_navigation);

        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) selectedFragment = new DashboardFragment();
            else if (id == R.id.nav_tasks) selectedFragment = new TasksFragment();
            else if (id == R.id.nav_routine) selectedFragment = new RoutineFragment();
            else if (id == R.id.nav_brain) selectedFragment = new ExercisesFragment();
            else if (id == R.id.nav_settings) selectedFragment = new SettingsFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }

        startSessionTimer();
    }

    // ─── Session timeout ───
    private void startSessionTimer() {
        sessionRunnable = () -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Toast.makeText(this, "Session expired. Please log in again.",
                    Toast.LENGTH_SHORT).show();
        };
        sessionHandler.postDelayed(sessionRunnable, SESSION_TIMEOUT);
    }

    // Reset timer on any user interaction
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        sessionHandler.removeCallbacks(sessionRunnable);
        sessionHandler.postDelayed(sessionRunnable, SESSION_TIMEOUT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sessionHandler.removeCallbacks(sessionRunnable);
    }

    // ─── Back button — double press to exit ───
    @Override
    public void onBackPressed() {
        // If fragment back stack has entries, pop them
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        // If on home tab already — double press to exit
        if (backPressedOnce) {
            super.onBackPressed();
            return;
        }

        backPressedOnce = true;
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> backPressedOnce = false, 2000);
    }
}