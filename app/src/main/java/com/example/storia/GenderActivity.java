package com.example.storia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.storia.databinding.ActivityGenderBinding;
import com.example.storia.utils.HelperClass;

public class GenderActivity extends AppCompatActivity {
    ActivityGenderBinding binding;
    String dateOfBirth = "";
    String selectedGender = "";
    String from = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().getExtras() != null){
            dateOfBirth = getIntent().getStringExtra("dateOfBirth");
            from = getIntent().getStringExtra("from");
        }

        selectedGender = HelperClass.users.getGender();
        applyDefaultGenderSelection();
        binding.tvMale.setOnClickListener(view -> selectGender(binding.tvMale, "Male"));
        binding.tvFemale.setOnClickListener(view -> selectGender(binding.tvFemale, "Female"));
        binding.tvOther.setOnClickListener(view -> selectGender(binding.tvOther, "Other"));

        binding.btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedGender.isEmpty()){
                    Toast.makeText(GenderActivity.this, "Please select gender", Toast.LENGTH_SHORT).show();
                }else{
                    Intent intent = new Intent(GenderActivity.this, LocationActivity.class);
                    intent.putExtra("dateOfBirth", dateOfBirth);
                    intent.putExtra("gender", selectedGender);
                    intent.putExtra("from", from);
                    startActivity(intent);
                    finish();
                }
            }
        });

    }

    private void selectGender(TextView selectedTextView, String gender) {
        // Reset all backgrounds to default
        binding.tvMale.setBackgroundResource(R.drawable.bg_input);
        binding.tvFemale.setBackgroundResource(R.drawable.bg_input);
        binding.tvOther.setBackgroundResource(R.drawable.bg_input);

        // Set selected background
        selectedTextView.setBackgroundResource(R.drawable.borders);

        // Update selected gender
        selectedGender = gender;
    }

    private void applyDefaultGenderSelection() {
        if (selectedGender.equalsIgnoreCase("Male")) {
            selectGender(binding.tvMale, "Male");
        } else if (selectedGender.equalsIgnoreCase("Female")) {
            selectGender(binding.tvFemale, "Female");
        } else if (selectedGender.equalsIgnoreCase("Other")) {
            selectGender(binding.tvOther, "Other");
        }
    }

}