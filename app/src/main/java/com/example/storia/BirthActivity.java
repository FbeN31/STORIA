package com.example.storia;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.storia.databinding.ActivityBirthBinding;
import com.example.storia.utils.HelperClass;

import java.util.Calendar;

public class BirthActivity extends AppCompatActivity {
    ActivityBirthBinding binding;
    String from = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBirthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().getExtras() != null){
            from = getIntent().getStringExtra("from");
        }

        binding.etDateOfBirth.setText(HelperClass.users.getDateOfBirth());

        binding.etDateOfBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });

        binding.btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String dateOfBirth = binding.etDateOfBirth.getText().toString();
                if (dateOfBirth.isEmpty()){
                    Toast.makeText(BirthActivity.this, "Please select date of birth", Toast.LENGTH_SHORT).show();
                }else{
                    Intent intent = new Intent(BirthActivity.this, GenderActivity.class);
                    intent.putExtra("dateOfBirth", dateOfBirth);
                    intent.putExtra("from", from);
                    startActivity(intent);
                    finish();
                }
            }
        });

    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            String selectedDate = String.format("%02d/%02d/%d", dayOfMonth, month1 + 1, year1);
            binding.etDateOfBirth.setText(selectedDate);
        }, year, month, day);

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

}