package com.example.storia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.storia.databinding.ActivityGuideBinding;
import com.example.storia.utils.HelperClass;

public class GuideActivity extends AppCompatActivity {
    ActivityGuideBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGuideBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.btnStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (HelperClass.users != null) {
                    if (HelperClass.users.getDateOfBirth().isEmpty()) {
                        startActivity(new Intent(GuideActivity.this, BirthActivity.class));
                    } else {
                        startActivity(new Intent(GuideActivity.this, WriteStoryActivity.class));
                    }
                    finish();
                }
            }
        });

    }
}