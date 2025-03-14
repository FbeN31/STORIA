package com.example.storia;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.storia.databinding.ActivityLocationBinding;
import com.example.storia.utils.HelperClass;
import com.example.storia.utils.SharedPrefHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationActivity extends AppCompatActivity {
    ActivityLocationBinding binding;
    FirebaseAuth auth;
    DatabaseReference dbRefUsers;
    ProgressDialog progressDialog;
    String dateOfBirth = "";
    String gender = "";
    String from = "";
    private static final int LOCATION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().getExtras() != null) {
            dateOfBirth = getIntent().getStringExtra("dateOfBirth");
            gender = getIntent().getStringExtra("gender");
            from = getIntent().getStringExtra("from");
        }

        binding.etCountry.setText(HelperClass.users.getCountry());
        binding.etState.setText(HelperClass.users.getState());
        binding.etCity.setText(HelperClass.users.getCity());
        binding.etAddress.setText(HelperClass.users.getAddress());

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.app_name));
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        auth = FirebaseAuth.getInstance();
        dbRefUsers = FirebaseDatabase.getInstance().getReference("Users");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Places.initialize(this, getString(R.string.google_maps_key));

        if (HelperClass.users.getAddress().isEmpty()){
            requestLocationPermission();
        }

        binding.etAddress.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this);
            startActivityForResult(intent, 101);
        });

        binding.btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String country = binding.etCountry.getText().toString();
                String state = binding.etState.getText().toString();
                String city = binding.etCity.getText().toString();
                String address = binding.etAddress.getText().toString();

                if (country.isEmpty() || state.isEmpty() || city.isEmpty() || address.isEmpty()) {
                    Toast.makeText(LocationActivity.this, "All the fields are required", Toast.LENGTH_SHORT).show();
                } else {
                    progressDialog.show();
                    Map<String, Object> update = new HashMap<>();
                    update.put("dateOfBirth", dateOfBirth);
                    update.put("gender", gender);
                    update.put("country", country);
                    update.put("state", state);
                    update.put("city", city);
                    update.put("address", address);
                    dbRefUsers.child(HelperClass.users.getUserId()).updateChildren(update)
                            .addOnCompleteListener(task -> {
                                HelperClass.users.setDateOfBirth(dateOfBirth);
                                HelperClass.users.setGender(gender);
                                HelperClass.users.setCountry(country);
                                HelperClass.users.setState(state);
                                HelperClass.users.setCity(city);
                                HelperClass.users.setAddress(address);
                                SharedPrefHelper.saveUser(LocationActivity.this, HelperClass.users);
                                progressDialog.dismiss();
                                if (!from.equals("edit")){
                                    startActivity(new Intent(LocationActivity.this, WriteStoryActivity.class));
                                }
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(LocationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }

            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 101) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                LatLng latLng = place.getLatLng();
                if (latLng != null) {
                    getAddressFromLocation(latLng.latitude, latLng.longitude);
                }
            }
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    progressDialog.dismiss();
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        getAddressFromLocation(latitude, longitude);
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                });
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                String country = address.getCountryName(); // Get country
                String state = address.getAdminArea(); // Get state
                String city = address.getLocality(); // Get city
                String addressLine = address.getAddressLine(0); // Full address

                // Set values to EditText fields
                binding.etCountry.setText(country);
                binding.etState.setText(state);
                binding.etCity.setText(city);
                binding.etAddress.setText(addressLine); // Ensure etAddress matches full address
            } else {
                Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to fetch address", Toast.LENGTH_SHORT).show();
        }
    }


}