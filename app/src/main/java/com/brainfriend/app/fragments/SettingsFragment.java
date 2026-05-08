package com.brainfriend.app.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view;
        try {
            view = inflater.inflate(R.layout.fragment_settings,
                    container, false);
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

    private void setupDarkMode(View view) {
        SwitchMaterial sw = view.findViewById(R.id.switch_dark_mode);
        if (sw == null) return;
        sw.setChecked(prefs.getBoolean("dark_mode", false));
        sw.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("dark_mode", checked).apply();
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
            requireActivity().recreate();
        });
    }

    private void loadUserData(View view) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null) return;
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    TextView tvName =
                            view.findViewById(R.id.tv_settings_name);
                    TextView tvPhone =
                            view.findViewById(R.id.tv_settings_phone);
                    TextView tvAvatar =
                            view.findViewById(R.id.tv_avatar);
                    if (tvName != null && name != null)
                        tvName.setText(name);
                    if (tvPhone != null && phone != null)
                        tvPhone.setText(phone);
                    if (tvAvatar != null && name != null
                            && !name.isEmpty())
                        tvAvatar.setText(String.valueOf(
                                name.charAt(0)).toUpperCase());
                });
    }

    private void showEditNameDialog() {
        EditText et = new EditText(getContext());
        et.setHint("Enter new name");
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        et.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Name")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Name cannot be empty",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("users").document(userId)
                            .update("name", name)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(getContext(),
                                        "Name updated!",
                                        Toast.LENGTH_SHORT).show();
                                if (getView() != null)
                                    loadUserData(getView());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditPhoneDialog() {
        EditText et = new EditText(getContext());
        et.setHint("Enter 10 digit phone number");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setPadding(48, 32, 48, 32);
        // Enforce exactly 10 digits
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Phone Number")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String phone = et.getText().toString().trim();
                    if (phone.length() != 10) {
                        Toast.makeText(getContext(),
                                "Phone number must be exactly 10 digits",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("users").document(userId)
                            .update("phone", phone)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(getContext(),
                                        "Phone updated!",
                                        Toast.LENGTH_SHORT).show();
                                if (getView() != null)
                                    loadUserData(getView());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        // Clean well designed layout
        LinearLayout outer = new LinearLayout(getContext());
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(48, 32, 48, 16);
        outer.setBackgroundColor(0xFFFFFFFF);

        // Current password
        TextView lbl1 = new TextView(getContext());
        lbl1.setText("Current Password");
        lbl1.setTextSize(13f);
        lbl1.setTextColor(0xFF64748B);
        lbl1.setPadding(0, 0, 0, 8);
        outer.addView(lbl1);

        EditText etCurrent = new EditText(getContext());
        etCurrent.setHint("••••••••");
        etCurrent.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        etCurrent.setPadding(24, 16, 24, 16);
        outer.addView(etCurrent);

        // Divider
        View div1 = new View(getContext());
        div1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        div1.setBackgroundColor(0xFFE2E8F0);
        LinearLayout.LayoutParams divParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 2);
        divParams.setMargins(0, 16, 0, 16);
        div1.setLayoutParams(divParams);
        outer.addView(div1);

        // New password
        TextView lbl2 = new TextView(getContext());
        lbl2.setText("New Password");
        lbl2.setTextSize(13f);
        lbl2.setTextColor(0xFF64748B);
        lbl2.setPadding(0, 0, 0, 8);
        outer.addView(lbl2);

        EditText etNew = new EditText(getContext());
        etNew.setHint("Min 6 characters");
        etNew.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etNew.setPadding(24, 16, 24, 16);
        outer.addView(etNew);

        // Divider
        View div2 = new View(getContext());
        div2.setLayoutParams(divParams);
        div2.setBackgroundColor(0xFFE2E8F0);
        outer.addView(div2);

        // Confirm password
        TextView lbl3 = new TextView(getContext());
        lbl3.setText("Confirm New Password");
        lbl3.setTextSize(13f);
        lbl3.setTextColor(0xFF64748B);
        lbl3.setPadding(0, 0, 0, 8);
        outer.addView(lbl3);

        EditText etConfirm = new EditText(getContext());
        etConfirm.setHint("Repeat new password");
        etConfirm.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etConfirm.setPadding(24, 16, 24, 16);
        outer.addView(etConfirm);

        new AlertDialog.Builder(requireContext())
                .setTitle("🔒 Change Password")
                .setView(outer)
                .setPositiveButton("Update Password", (d, w) -> {
                    String current =
                            etCurrent.getText().toString().trim();
                    String newPass = etNew.getText().toString().trim();
                    String confirm =
                            etConfirm.getText().toString().trim();

                    if (current.isEmpty() || newPass.isEmpty()
                            || confirm.isEmpty()) {
                        Toast.makeText(getContext(),
                                "All fields are required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirm)) {
                        Toast.makeText(getContext(),
                                "New passwords do not match",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(getContext(),
                                "Password must be at least 6 characters",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null || userEmail == null) return;

                    AuthCredential cred =
                            EmailAuthProvider.getCredential(
                                    userEmail, current);
                    user.reauthenticate(cred)
                            .addOnSuccessListener(a ->
                                    user.updatePassword(newPass)
                                            .addOnSuccessListener(b ->
                                                    Toast.makeText(
                                                                    getContext(),
                                                                    "✅ Password changed successfully!",
                                                                    Toast.LENGTH_LONG)
                                                            .show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(
                                                                    getContext(),
                                                                    "Failed: "
                                                                            + e.getMessage(),
                                                                    Toast.LENGTH_SHORT)
                                                            .show()))
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "❌ Current password is incorrect",
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
            Class<?> cls = Class.forName(
                    "com.brainfriend.app.LoginActivity");
            Intent intent = new Intent(requireContext(), cls);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(getContext(), "Logged out",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteAccountDialog() {
        EditText et = new EditText(getContext());
        et.setHint("Enter your password to confirm");
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        et.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Delete Account")
                .setMessage("This permanently deletes your account "
                        + "and all data. Cannot be undone.")
                .setView(et)
                .setPositiveButton("Delete Forever", (d, w) -> {
                    String pass = et.getText().toString().trim();
                    if (pass.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Password required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null || userEmail == null) return;
                    AuthCredential cred =
                            EmailAuthProvider.getCredential(
                                    userEmail, pass);
                    user.reauthenticate(cred)
                            .addOnSuccessListener(a -> {
                                db.collection("users")
                                        .document(userId).delete();
                                db.collection("tasks")
                                        .whereEqualTo("userId", userId)
                                        .get()
                                        .addOnSuccessListener(snap -> {
                                            for (var doc
                                                    : snap.getDocuments())
                                                doc.getReference()
                                                        .delete();
                                        });
                                user.delete().addOnSuccessListener(
                                        b -> {
                                            Toast.makeText(getContext(),
                                                            "Account deleted",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                            performLogout();
                                        });
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Incorrect password",
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}