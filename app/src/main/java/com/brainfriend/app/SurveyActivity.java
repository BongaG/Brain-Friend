package com.brainfriend.app;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SurveyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Make sure this matches your XML filename
        setContentView(R.layout.activity_survey);

        // Skip button
        Button btnSkip = findViewById(R.id.btnSkip);
        btnSkip.setOnClickListener(v -> {
            // TODO: Replace with navigation to main screen
            finish();
        });
    }
}