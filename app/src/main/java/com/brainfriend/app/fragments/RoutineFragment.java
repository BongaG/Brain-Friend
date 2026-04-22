package com.brainfriend.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import com.google.firebase.auth.FirebaseAuth;

public class RoutineFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine, container, false);

        // Header Logout Button
        View logoutBtn = view.findViewById(R.id.btn_logout_routine);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                try {
                    Class<?> loginClass = Class.forName("com.brainfriend.app.LoginActivity");
                    Intent intent = new Intent(requireContext(), loginClass);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } catch (ClassNotFoundException e) {
                    Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }
}