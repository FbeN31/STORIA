package com.example.storia;

import static com.example.storia.MainActivity.createInitialsBitmap;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.storia.databinding.ActivityMapBinding;
import com.example.storia.model.UserModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    ActivityMapBinding binding;
    FirebaseAuth auth;
    DatabaseReference dbRefUsers;
    ProgressDialog progressDialog;
    List<UserModel> listOfUserStories = new ArrayList<>();
    GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.ivBack.setOnClickListener(view -> finish());

        auth = FirebaseAuth.getInstance();
        dbRefUsers = FirebaseDatabase.getInstance().getReference("Users");

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.app_name));
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        fetchUserStories();
    }

    private void fetchUserStories() {
        dbRefUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listOfUserStories.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        try {
                            UserModel model = ds.getValue(UserModel.class);
                            listOfUserStories.add(model);
                        } catch (DatabaseException e) {
                            e.printStackTrace();
                        }
                    }

                    if (listOfUserStories.isEmpty()) {
                        Toast.makeText(MapActivity.this, "No Story Found", Toast.LENGTH_SHORT).show();
                    } else {
                        addMarkersToMap();
                    }
                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(MapActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int sizeInDp) {
        int sizeInPx = (int) (sizeInDp * getResources().getDisplayMetrics().density);
        return Bitmap.createScaledBitmap(bitmap, sizeInPx, sizeInPx, false);
    }

    // Called when the map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable clicking on markers
        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        // Check if users are already fetched
        if (!listOfUserStories.isEmpty()) {
            addMarkersToMap();
        }
    }

    private void addMarkersToMap() {
        if (mMap == null) return;

        for (UserModel userModel : listOfUserStories) {
            Bitmap initialsBitmap = createInitialsBitmap(userModel.getName(), 200);
            Bitmap resizedBitmap = resizeBitmap(initialsBitmap, 40);
            LatLng location = getLatLngFromAddress(userModel.getAddress());

            if (location != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(userModel.getName() + ", " + userModel.getAddress())
                        .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)));

                // Move camera to first marker
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10));
            }
        }
    }

    // Convert Address to LatLng (you can use Geocoder for this)
    private LatLng getLatLngFromAddress(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                return new LatLng(location.getLatitude(), location.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
