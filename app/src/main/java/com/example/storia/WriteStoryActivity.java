package com.example.storia;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.storia.databinding.ActivityWriteStoryBinding;
import com.example.storia.utils.HelperClass;
import com.example.storia.utils.SharedPrefHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WriteStoryActivity extends AppCompatActivity {
    ActivityWriteStoryBinding binding;
    FirebaseAuth auth;
    DatabaseReference dbRefUsers;
    StorageReference storageReference;
    ProgressDialog progressDialog;
    String title, description, imageUri = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWriteStoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.app_name));
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        auth = FirebaseAuth.getInstance();
        dbRefUsers = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference("Pictures");
        Log.d("CheckUserData", "onCreate: "+ HelperClass.users);

        if (getIntent().getExtras() != null) {
            binding.etTitle.setText(HelperClass.users.getPostTitle());
            binding.etDetails.setText(HelperClass.users.getPostDescription());
            imageUri = HelperClass.users.getPostImage();
            binding.cvRemove.setVisibility(View.VISIBLE);
            Glide.with(binding.ivStory).load(HelperClass.users.getPostImage()).into(binding.ivStory);
        }

        binding.ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.ivSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
                try {
                    startActivityForResult(intent, 1);
                } catch (Exception e) {
                    Toast.makeText(WriteStoryActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.ivStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                startActivityForResult(Intent.createChooser(intent, "Select Story Image"), 123);
            }
        });

        binding.cvRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.ivStory.setImageResource(R.drawable.baseline_add_24);
                binding.cvRemove.setVisibility(View.GONE);
                imageUri = "";
            }
        });

        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                title = binding.etTitle.getText().toString();
                description = binding.etDetails.getText().toString();

                if (title.isEmpty()) {
                    Toast.makeText(WriteStoryActivity.this, "Please write title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (imageUri.isEmpty()) {
                    Toast.makeText(WriteStoryActivity.this, "Please choose story picture", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (description.isEmpty()) {
                    Toast.makeText(WriteStoryActivity.this, "Please write about your life", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressDialog.show();
                if (HelperClass.users.getPostImage().equals(imageUri)){
                    addStoryToFirebase(HelperClass.users.getPostImage());
                }else{
                    Uri uriImage = Uri.parse(imageUri);
                    StorageReference imageRef = storageReference.child(uriImage.getLastPathSegment());
                    imageRef.putFile(uriImage).addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUri1 = uri.toString();
                        addStoryToFirebase(downloadUri1);
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(WriteStoryActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    })).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(WriteStoryActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

            }
        });

    }

    void addStoryToFirebase(String imageUri){
        Map<String, Object> update = new HashMap<>();
        update.put("postTitle", title);
        update.put("postDescription", description);
        update.put("postImage", imageUri);
        if (HelperClass.users.getStatus() == null || HelperClass.users.getStatus().isEmpty()){
            update.put("status", "Private");
        }
        dbRefUsers.child(HelperClass.users.getUserId()).updateChildren(update)
                .addOnCompleteListener(task -> {
                    HelperClass.users.setPostTitle(title);
                    HelperClass.users.setPostDescription(description);
                    HelperClass.users.setPostImage(imageUri);
                    if (HelperClass.users.getStatus() == null || HelperClass.users.getStatus().isEmpty()){
                        HelperClass.users.setStatus("Private");
                    }
                    SharedPrefHelper.saveUser(WriteStoryActivity.this, HelperClass.users);
                    progressDialog.dismiss();
                    Toast.makeText(WriteStoryActivity.this, "Saved Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(WriteStoryActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 123) {
                imageUri = data.getData().toString();
                binding.ivStory.setImageURI(data.getData());
                binding.cvRemove.setVisibility(View.VISIBLE);
            } else if (requestCode == 1) {
                ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (text != null && !text.isEmpty()) {
                    String existingText = binding.etDetails.getText().toString().trim();
                    String newText = text.get(0);

                    if (!existingText.isEmpty()) {
                        binding.etDetails.setText(existingText + " " + newText);
                    } else {
                        binding.etDetails.setText(newText);
                    }
                }
            }
        }
    }

}