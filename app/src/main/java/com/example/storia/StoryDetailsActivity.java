package com.example.storia;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.storia.adapter.CommentsAdapter;
import com.example.storia.databinding.ActivityStoryDetailsBinding;
import com.example.storia.model.CommentModel;
import com.example.storia.model.UserModel;
import com.example.storia.utils.HelperClass;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StoryDetailsActivity extends AppCompatActivity {
    ActivityStoryDetailsBinding binding;
    FirebaseAuth auth;
    ProgressDialog progressDialog;
    DatabaseReference dbRefComments;
    DatabaseReference dbRefUsers;
    UserModel userModel;
    List<CommentModel> listOfComments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoryDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().getExtras() != null) {
            userModel = (UserModel) getIntent().getSerializableExtra("story");
        }

        auth = FirebaseAuth.getInstance();
        dbRefUsers = FirebaseDatabase.getInstance().getReference("Users");
        dbRefComments = FirebaseDatabase.getInstance().getReference("Comments");

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.app_name));
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        if (userModel != null) {
            binding.tvTitle.setText("“" + userModel.getPostTitle() + "”");
            binding.tvName.setText("By " + userModel.getName());
            binding.tvBirth.setText(userModel.getName() + ", born on " + formatBirthDate(userModel.getDateOfBirth()) + ",");
            binding.tvStoryDetails.setText(userModel.getPostDescription());
            Glide.with(binding.ivStory).load(userModel.getPostImage()).into(binding.ivStory);
        }

        binding.ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.btnPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (HelperClass.users != null) {
                    String comment = binding.etComment.getText().toString();
                    if (comment.isEmpty()) {
                        Toast.makeText(StoryDetailsActivity.this, "Please write comment", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    progressDialog.show();
                    String commentId = dbRefComments.push().getKey();
                    Boolean isRead = false;
                    if (userModel.getUserId().equals(HelperClass.users.getUserId())){
                        isRead = true;
                    }
                    CommentModel commentModel = new CommentModel(commentId, userModel.getUserId(), HelperClass.users.getUserId(), getCurrentDate(), comment);
                    commentModel.setRead(isRead);
                    dbRefComments.child(commentId).setValue(commentModel)
                            .addOnSuccessListener(aVoid -> {
                                binding.etComment.setText("");
                                Toast.makeText(StoryDetailsActivity.this, "Comment added successfully!", Toast.LENGTH_SHORT).show();
                                getCommentsFromDatabase();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(StoryDetailsActivity.this, "Failed to add comment: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(StoryDetailsActivity.this, "Please login first to post comment", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH);
        return sdf.format(new Date());
    }

    public String formatBirthDate(String dateOfBirth) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

        try {
            Date date = inputFormat.parse(dateOfBirth);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCommentsFromDatabase();
    }

    private void getCommentsFromDatabase() {
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
        listOfComments.clear();
        dbRefComments.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listOfComments.clear();
                    progressDialog.dismiss();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        try {
                            CommentModel model = ds.getValue(CommentModel.class);
                            if (model.getStoryUserId().equals(userModel.getUserId())) {
                                listOfComments.add(model);
                                if (HelperClass.users != null){
                                    if (model.getStoryUserId().equals(HelperClass.users.getUserId()) && !model.isRead()) {
                                        Map<String, Object> update = new HashMap<>();
                                        update.put("read", true);
                                        dbRefComments.child(model.getId()).updateChildren(update)
                                                .addOnSuccessListener(aVoid -> {
                                                    model.setRead(true);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(StoryDetailsActivity.this, "Failed to update read status", Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                }
                            }

                        } catch (DatabaseException e) {
                            e.printStackTrace();
                        }
                    }

                    if (listOfComments.isEmpty()) {
                        binding.tvNoDataFound.setVisibility(View.VISIBLE);
                        binding.rvComments.setVisibility(View.GONE);
                    } else {
                        setAdapter();
                        binding.tvNoDataFound.setVisibility(View.GONE);
                        binding.rvComments.setVisibility(View.VISIBLE);
                    }
                } else {
                    progressDialog.dismiss();
                    binding.tvNoDataFound.setVisibility(View.VISIBLE);
                    binding.rvComments.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(StoryDetailsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAdapter() {
        Collections.reverse(listOfComments);
        CommentsAdapter commentsAdapter = new CommentsAdapter(this, listOfComments, dbRefUsers);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComments.setAdapter(commentsAdapter);
    }

}