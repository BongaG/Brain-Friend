package com.brainfriend.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.fragments.DashboardFragment;
import com.brainfriend.app.fragments.ExercisesFragment;
import com.brainfriend.app.fragments.RoutineFragment;
import com.brainfriend.app.fragments.SettingsFragment;
import com.brainfriend.app.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ MUST be before super.onCreate and setContentView
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

            if (id == R.id.nav_home) {
                selectedFragment = new DashboardFragment();
            } else if (id == R.id.nav_tasks) {
                selectedFragment = new TasksFragment();
            } else if (id == R.id.nav_routine) {
                selectedFragment = new RoutineFragment();
            } else if (id == R.id.nav_brain) {
                selectedFragment = new ExercisesFragment();
            } else if (id == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

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
    }
}