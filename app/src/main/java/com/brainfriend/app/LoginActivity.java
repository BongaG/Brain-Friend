package com.brainfriend.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark mode before UI loads
        SharedPreferences prefs = getSharedPreferences("settings", 0);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(darkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        TextInputEditText etEmail = findViewById(R.id.et_email);
        TextInputEditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.tv_go_to_register);
        TextView tvForgot = findViewById(R.id.tv_forgot_password);

        // Login
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null
                    ? etEmail.getText().toString().trim() : "";
            String pass = etPassword.getText() != null
                    ? etPassword.getText().toString().trim() : "";

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email and password",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            checkIfSurveyNeeded(); // ← just the call, nothing else
                        } else {
                            Toast.makeText(this,
                                    "Login failed. Check your credentials.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Register
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Forgot Password
        tvForgot.setOnClickListener(v -> showForgotPasswordDialog());
    }

    // ── Survey check ────────────────────────────────────────────────────────
    private void checkIfSurveyNeeded() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("userPreferences")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    boolean surveyDone = doc.exists()
                            && Boolean.TRUE.equals(doc.getBoolean("surveyDone"));
                    if (surveyDone) {
                        startActivity(new Intent(this, MainActivity.class));
                    } else {
                        startActivity(new Intent(this, SurveyActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    // If Firestore fails, go to main app anyway
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }

    // ── Forgot password ─────────────────────────────────────────────────────
    private void showForgotPasswordDialog() {
        TextInputEditText etResetEmail = new TextInputEditText(this);
        etResetEmail.setHint("Enter your email address");
        etResetEmail.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etResetEmail.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("We will send a password reset link to your email.")
                .setView(etResetEmail)
                .setPositiveButton("Send Reset Email", (dialog, which) -> {
                    String email = etResetEmail.getText() != null
                            ? etResetEmail.getText().toString().trim() : "";

                    if (email.isEmpty()) {
                        Toast.makeText(this,
                                "Please enter your email address",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS
                            .matcher(email).matches()) {
                        Toast.makeText(this,
                                "Please enter a valid email address",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(a -> {
                                new AlertDialog.Builder(this)
                                        .setTitle("✅ Email Sent!")
                                        .setMessage(
                                                "A password reset link has been "
                                                        + "sent to:\n\n" + email
                                                        + "\n\nCheck your inbox and "
                                                        + "follow the link to reset "
                                                        + "your password.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                String errorMsg = e.getMessage();
                                if (errorMsg != null && errorMsg.contains(
                                        "no user record")) {
                                    Toast.makeText(this,
                                            "No account found with that email",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this,
                                            "Error: " + errorMsg,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}