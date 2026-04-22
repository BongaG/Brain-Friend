package com.brainfriend.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvName, tvEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        tvName = view.findViewById(R.id.tv_settings_name);
        tvEmail = view.findViewById(R.id.tv_settings_email);

        if (mAuth.getCurrentUser() != null) {
            tvEmail.setText(mAuth.getCurrentUser().getEmail());
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
                if (isAdded() && doc.exists()) {
                    String name = doc.getString("name");
                    if (name != null) tvName.setText(name);
                }
            });
        }

        view.findViewById(R.id.btn_logout_settings).setOnClickListener(v -> performLogout());
        return view;
    }

    private void performLogout() {
        mAuth.signOut();
        try {
            Class<?> loginClass = Class.forName("com.brainfriend.app.LoginActivity");
            Intent intent = new Intent(requireContext(), loginClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
        }
    }
}