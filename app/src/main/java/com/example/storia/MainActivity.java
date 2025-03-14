package com.example.storia;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.storia.adapter.StoriesAdapter;
import com.example.storia.databinding.ActivityMainBinding;
import com.example.storia.model.CommentModel;
import com.example.storia.model.UserModel;
import com.example.storia.utils.HelperClass;
import com.example.storia.utils.SharedPrefHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    FirebaseAuth auth;
    ProgressDialog progressDialog;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private DatabaseReference dbRefUsers;
    private DatabaseReference dbRefComments;
    UserModel savedUser;
    private static final int LOCATION_REQUEST_CODE = 100;
    List<UserModel> listOfUserStories = new ArrayList<>();
    int count = 0;
    List<CommentModel> listOfComments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        dbRefUsers = FirebaseDatabase.getInstance().getReference("Users");
        dbRefComments = FirebaseDatabase.getInstance().getReference("Comments");

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.app_name));
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        FirebaseUser user = auth.getCurrentUser();
        savedUser = SharedPrefHelper.getUser(this);
        if (savedUser != null) {
            HelperClass.users = savedUser;
            Bitmap initialsBitmap = createInitialsBitmap(savedUser.getName(), 200);
            binding.ivUser.setImageBitmap(initialsBitmap);
            setMyStoryLayouts();
        } else if (user != null) {
            dbRefUsers.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        savedUser = snapshot.getValue(UserModel.class);
                        HelperClass.users = savedUser;
                        saveUserToPreferences(savedUser);
                        Bitmap initialsBitmap = createInitialsBitmap(savedUser.getName(), 200);
                        binding.ivUser.setImageBitmap(initialsBitmap);
                        setMyStoryLayouts();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        binding.ivUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (savedUser != null) {
                    showSignOutConfirmationDialog();
                } else {
                    signInWithGoogle();
                }
            }
        });

        binding.llStartStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (HelperClass.users != null) {
                    startActivity(new Intent(MainActivity.this, GuideActivity.class));
                } else {
                    Toast.makeText(MainActivity.this, "Please first login to write your story", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.llEditMyStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WriteStoryActivity.class);
                intent.putExtra("from", "edit");
                startActivity(intent);
            }
        });

        binding.llEditMyInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BirthActivity.class);
                intent.putExtra("from", "edit");
                startActivity(intent);
            }
        });

        binding.llPublishMyStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                Map<String, Object> update = new HashMap<>();
                if (HelperClass.users.getStatus().equals("Private")) {
                    update.put("status", "Public");
                } else {
                    update.put("status", "Private");
                }
                dbRefUsers.child(HelperClass.users.getUserId()).updateChildren(update)
                        .addOnCompleteListener(task -> {
                            if (HelperClass.users.getStatus().equals("Private")) {
                                HelperClass.users.setStatus("Public");
                                binding.tvPublish.setText("Change my story to private");
                            } else {
                                HelperClass.users.setStatus("Private");
                                binding.tvPublish.setText("Publish my story");
                            }
                            SharedPrefHelper.saveUser(MainActivity.this, HelperClass.users);
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Updated Successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        binding.llMyStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, StoryDetailsActivity.class);
                intent.putExtra("story", HelperClass.users);
                startActivity(intent);
            }
        });

        binding.llDeleteMyStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (HelperClass.users != null) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Story")
                            .setMessage("Are you sure you want to delete your story?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                deleteStory();
                            })
                            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                            .show();
                }
            }
        });

        binding.ivMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });


    }


    private void deleteStory() {
        progressDialog.show();
        Map<String, Object> update = new HashMap<>();
        update.put("postTitle", "");
        update.put("postDescription", "");
        update.put("postImage", "");
        update.put("status", "");
        dbRefUsers.child(HelperClass.users.getUserId()).updateChildren(update)
                .addOnCompleteListener(task -> {
                    HelperClass.users.setPostTitle("");
                    HelperClass.users.setPostDescription("");
                    HelperClass.users.setPostImage("");
                    HelperClass.users.setStatus("");
                    SharedPrefHelper.saveUser(MainActivity.this, HelperClass.users);
                    if (!listOfComments.isEmpty()) {
                        for (CommentModel commentModel : listOfComments) {
                            dbRefComments.child(commentModel.getId()).removeValue();
                        }
                        setMyStoryLayouts();
                        getUserStoriesFromDatabase();
                        getCommentsFromDatabase();
                        listOfComments.clear();
                    } else {
                        setMyStoryLayouts();
                        getUserStoriesFromDatabase();
                        getCommentsFromDatabase();
                    }
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Story deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    @Override
    protected void onResume() {
        super.onResume();
        setMyStoryLayouts();
        getUserStoriesFromDatabase();
        getCommentsFromDatabase();
    }

    private void showSignOutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", (dialog, which) -> signOut())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setMyStoryLayouts() {
        if (HelperClass.users != null && !HelperClass.users.getPostTitle().isEmpty()) {
            Glide.with(binding.ivMyStory).load(HelperClass.users.getPostImage()).into(binding.ivMyStory);
            binding.llStartStory.setVisibility(View.GONE);
            binding.llMyStory.setVisibility(View.VISIBLE);
            String title = HelperClass.users.getName() + ", " + calculateAge(HelperClass.users.getDateOfBirth()) + " years old, " + HelperClass.users.getCity() + " " + HelperClass.users.getCountry();
            binding.tvMyDetails.setText(title);
            if (HelperClass.users.getStatus().equals("Private")) {
                binding.tvPublish.setText("Publish my story");
            } else {
                binding.tvPublish.setText("Change my story to private");
            }
        } else {
            binding.llStartStory.setVisibility(View.VISIBLE);
            binding.llMyStory.setVisibility(View.GONE);
        }
    }

    void getUserStoriesFromDatabase() {
        progressDialog.show();
        listOfUserStories.clear();
        dbRefUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listOfUserStories.clear();
                    progressDialog.dismiss();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        try {
                            UserModel model = ds.getValue(UserModel.class);
                            if (model.getStatus().equals("Public")) {
                                if (HelperClass.users != null) {
                                    if (!HelperClass.users.getUserId().equals(model.getUserId())) {
                                        listOfUserStories.add(model);
                                    }
                                } else {
                                    listOfUserStories.add(model);
                                }
                            }
                        } catch (DatabaseException e) {
                            e.printStackTrace();
                        }
                    }

                    if (listOfUserStories.isEmpty()) {
                        binding.tvNoDataFound.setVisibility(View.VISIBLE);
                        binding.rvMemberStories.setVisibility(View.GONE);
                    } else {
                        setAdapter();
                        binding.tvNoDataFound.setVisibility(View.GONE);
                        binding.rvMemberStories.setVisibility(View.VISIBLE);
                    }
                } else {
                    progressDialog.dismiss();
                    binding.tvNoDataFound.setVisibility(View.VISIBLE);
                    binding.rvMemberStories.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAdapter() {
        Collections.reverse(listOfUserStories);
        StoriesAdapter storiesAdapter = new StoriesAdapter(this, listOfUserStories, pos -> {
            UserModel model = listOfUserStories.get(pos);
            Intent intent = new Intent(MainActivity.this, StoryDetailsActivity.class);
            intent.putExtra("story", model);
            startActivity(intent);
        });
        binding.rvMemberStories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMemberStories.setAdapter(storiesAdapter);
    }

    public static int calculateAge(String dateOfBirth) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date birthDate = sdf.parse(dateOfBirth);
            if (birthDate == null) return 0;

            Calendar birthCalendar = Calendar.getInstance();
            birthCalendar.setTime(birthDate);

            Calendar todayCalendar = Calendar.getInstance();

            int age = todayCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);

            // Check if birthday has occurred this year
            if (todayCalendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--; // Birthday hasn't happened yet this year
            }

            return age;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0; // Return 0 in case of an error
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication Failed!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(FirebaseUser user) {
        String userId = user.getUid();
        dbRefUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User already exists, just login
                    savedUser = snapshot.getValue(UserModel.class);
                    HelperClass.users = savedUser;
                    saveUserToPreferences(savedUser);
                    Toast.makeText(MainActivity.this, "Welcome back, " + savedUser.getName(), Toast.LENGTH_SHORT).show();
                    Bitmap initialsBitmap = createInitialsBitmap(savedUser.getName(), 200);
                    binding.ivUser.setImageBitmap(initialsBitmap);
                    setMyStoryLayouts();
                    getUserStoriesFromDatabase();
                } else {
                    // Save new user
                    UserModel userData = new UserModel(userId, user.getDisplayName(), user.getEmail(), user.getPhotoUrl().toString(), "", "", "", "", "", "", "", "", "", "");
                    dbRefUsers.child(userId).setValue(userData)
                            .addOnSuccessListener(aVoid -> {
                                HelperClass.users = userData;
                                saveUserToPreferences(userData);
                                Toast.makeText(MainActivity.this, "User data saved successfully!", Toast.LENGTH_SHORT).show();
                                Bitmap initialsBitmap = createInitialsBitmap(userData.getName(), 200);
                                binding.ivUser.setImageBitmap(initialsBitmap);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Failed to save user data!", Toast.LENGTH_SHORT).show();
                            });
                }
                getCommentsFromDatabase();
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void getCommentsFromDatabase() {
        listOfComments.clear();
        dbRefComments.addValueEventListener(new ValueEventListener() {
            @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                count = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        try {
                            CommentModel model = ds.getValue(CommentModel.class);
                            if (HelperClass.users != null) {
                                if (model.getStoryUserId().equals(HelperClass.users.getUserId())) {
                                    if (!model.isRead()) {
                                        count = count + 1;
                                    }
                                    listOfComments.add(model);
                                }
                            }
                        } catch (DatabaseException e) {
                            e.printStackTrace();
                        }
                    }

                    if (count > 0) {
                        binding.tvCount.setVisibility(View.VISIBLE);
                        binding.tvCount.setText(String.valueOf(count));
                    } else {
                        binding.tvCount.setVisibility(View.GONE);
                    }
                } else {
                    binding.tvCount.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserToPreferences(UserModel user) {
        SharedPrefHelper.saveUser(this, user);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            HelperClass.users = null;
            SharedPrefHelper.clearUser(this);
            Toast.makeText(this, "Signed out successfully!", Toast.LENGTH_SHORT).show();
            binding.ivUser.setImageResource(R.drawable.ic_google);
            startActivity(new Intent(MainActivity.this, SplashActivity.class));
            finishAffinity();
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("Google Sign-In", "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In Failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static Bitmap createInitialsBitmap(String name, int size) {
        // Extract initials
        String initials = getInitials(name);

        // Create bitmap and canvas
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Background paint
        Paint paintBg = new Paint();
        paintBg.setColor(Color.parseColor("#2E7D32")); // Dark Green
        paintBg.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2, size / 2, size / 2, paintBg);

        // Text paint
        Paint paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(size / 3);
        paintText.setTypeface(Typeface.DEFAULT_BOLD);
        paintText.setAntiAlias(true);

        // Measure text size
        Rect textBounds = new Rect();
        paintText.getTextBounds(initials, 0, initials.length(), textBounds);

        // Center the text
        float x = (size - textBounds.width()) / 2f;
        float y = (size + textBounds.height()) / 2f;
        canvas.drawText(initials, x, y, paintText);

        return bitmap;
    }

    public static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split(" ");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
    }


}