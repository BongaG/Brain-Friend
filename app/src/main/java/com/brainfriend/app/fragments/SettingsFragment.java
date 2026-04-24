package com.brainfriend.app.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private String userEmail;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view;
        try {
            view = inflater.inflate(R.layout.fragment_settings, container, false);
        } catch (Exception e) {
            return new View(getContext());
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = requireActivity().getSharedPreferences("settings", 0);

        if (mAuth.getCurrentUser() == null) return view;

        userId = mAuth.getCurrentUser().getUid();
        userEmail = mAuth.getCurrentUser().getEmail();

        TextView tvEmail = view.findViewById(R.id.tv_settings_email);
        if (tvEmail != null) tvEmail.setText(userEmail);

        loadUserData(view);
        setupDarkMode(view);

        view.findViewById(R.id.btn_edit_profile)
                .setOnClickListener(v -> showEditNameDialog());
        view.findViewById(R.id.btn_edit_phone)
                .setOnClickListener(v -> showEditPhoneDialog());
        view.findViewById(R.id.btn_change_password)
                .setOnClickListener(v -> showChangePasswordDialog());
        view.findViewById(R.id.btn_logout_full)
                .setOnClickListener(v -> showLogoutDialog());
        view.findViewById(R.id.btn_delete_account)
                .setOnClickListener(v -> showDeleteAccountDialog());

        return view;
    }

    // ─── Dark Mode ───
    private void setupDarkMode(View view) {
        SwitchMaterial switchDark = view.findViewById(R.id.switch_dark_mode);
        if (switchDark == null) return;

        // Read saved preference — NOT from AppCompatDelegate
        boolean isDark = prefs.getBoolean("dark_mode", false);
        switchDark.setChecked(isDark);

        switchDark.setOnCheckedChangeListener((btn, checked) -> {
            // Save first
            prefs.edit().putBoolean("dark_mode", checked).apply();

            // Apply mode
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);

            // Recreate to apply
            requireActivity().recreate();
        });
    }

    private void loadUserData(View view) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null) return;
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");

                    TextView tvName = view.findViewById(R.id.tv_settings_name);
                    TextView tvPhone = view.findViewById(R.id.tv_settings_phone);
                    TextView tvAvatar = view.findViewById(R.id.tv_avatar);

                    if (tvName != null && name != null) tvName.setText(name);
                    if (tvPhone != null && phone != null) tvPhone.setText(phone);
                    if (tvAvatar != null && name != null && !name.isEmpty()) {
                        tvAvatar.setText(
                                String.valueOf(name.charAt(0)).toUpperCase());
                    }
                });
    }

    private void showEditNameDialog() {
        EditText et = new EditText(getContext());
        et.setHint("Enter new name");
        et.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Name")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = et.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(), "Name cannot be empty",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("users").document(userId)
                            .update("name", newName)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(getContext(), "Name updated!",
                                        Toast.LENGTH_SHORT).show();
                                if (getView() != null) loadUserData(getView());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditPhoneDialog() {
        EditText et = new EditText(getContext());
        et.setHint("Enter new phone number");
        et.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        et.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Phone Number")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String newPhone = et.getText().toString().trim();
                    if (newPhone.isEmpty()) {
                        Toast.makeText(getContext(), "Phone cannot be empty",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("users").document(userId)
                            .update("phone", newPhone)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(getContext(), "Phone updated!",
                                        Toast.LENGTH_SHORT).show();
                                if (getView() != null) loadUserData(getView());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText etCurrent = new EditText(getContext());
        etCurrent.setHint("Current password");
        etCurrent.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etCurrent);

        EditText etNew = new EditText(getContext());
        etNew.setHint("New password");
        etNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etNew.setPadding(0, 16, 0, 0);
        layout.addView(etNew);

        EditText etConfirm = new EditText(getContext());
        etConfirm.setHint("Confirm new password");
        etConfirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etConfirm.setPadding(0, 16, 0, 0);
        layout.addView(etConfirm);

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", (d, w) -> {
                    String current = etCurrent.getText().toString().trim();
                    String newPass = etNew.getText().toString().trim();
                    String confirm = etConfirm.getText().toString().trim();

                    if (current.isEmpty() || newPass.isEmpty()
                            || confirm.isEmpty()) {
                        Toast.makeText(getContext(), "All fields required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirm)) {
                        Toast.makeText(getContext(), "Passwords do not match",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(getContext(),
                                "Password must be 6+ characters",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null || userEmail == null) return;

                    AuthCredential credential = EmailAuthProvider
                            .getCredential(userEmail, current);

                    user.reauthenticate(credential)
                            .addOnSuccessListener(a ->
                                    user.updatePassword(newPass)
                                            .addOnSuccessListener(b ->
                                                    Toast.makeText(getContext(),
                                                            "Password changed!",
                                                            Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(getContext(),
                                                            "Failed: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show()))
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Current password incorrect",
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (d, w) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        mAuth.signOut();
        try {
            Class<?> loginClass = Class.forName("com.brainfriend.app.LoginActivity");
            Intent intent = new Intent(requireContext(), loginClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteAccountDialog() {
        EditText etPassword = new EditText(getContext());
        etPassword.setHint("Enter your password to confirm");
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Delete Account")
                .setMessage("This will permanently delete your account " +
                        "and all your data. This cannot be undone.")
                .setView(etPassword)
                .setPositiveButton("Delete Forever", (d, w) -> {
                    String password = etPassword.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(getContext(), "Password required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null || userEmail == null) return;

                    AuthCredential credential = EmailAuthProvider
                            .getCredential(userEmail, password);

                    user.reauthenticate(credential).addOnSuccessListener(a -> {
                        db.collection("users").document(userId).delete();
                        db.collection("tasks")
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnSuccessListener(snap -> {
                                    for (var doc : snap.getDocuments()) {
                                        doc.getReference().delete();
                                    }
                                });
                        user.delete().addOnSuccessListener(b -> {
                            Toast.makeText(getContext(), "Account deleted",
                                    Toast.LENGTH_SHORT).show();
                            performLogout();
                        });
                    }).addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Incorrect password",
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}