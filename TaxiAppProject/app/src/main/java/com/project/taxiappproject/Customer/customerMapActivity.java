package com.project.taxiappproject.Customer;

import static android.widget.Toast.makeText;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.taxiappproject.R;
import com.project.taxiappproject.databinding.ActivityCustomerMapBinding;

public class customerMapActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    final int REQUEST_LOCATION_PERMISSION_CODE = 202210;
    LocationRequest mLocationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private ActivityCustomerMapBinding binding;
    Button mLogoutButton,mRequestARideButton;
    Boolean isLoggingOut = false;
    LatLng pickUpLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustomerMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest.Builder(4000)
                .setMinUpdateIntervalMillis(2000)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY).build();
        mLogoutButton = (Button) findViewById(R.id.Logout);
        mLogoutButton.setOnClickListener(this);
        mRequestARideButton = (Button) findViewById(R.id.requestARide);
        mRequestARideButton.setOnClickListener(this);
    }

    LocationCallback mLocationCallBack = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {
            }
        }
    };

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
        } else {
            checkLocationPermission();
        }

    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale
                    (this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                new android.app.AlertDialog.Builder(this).setTitle("give permission").setMessage(
                        "give permission message").setPositiveButton("OK",
                        (dialogInterface, i) -> ActivityCompat.requestPermissions(customerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE)).create().show();
            } else {
                ActivityCompat.requestPermissions(customerMapActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CALL_PHONE}, REQUEST_LOCATION_PERMISSION_CODE);
            }
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_CODE)
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
            }
    }

    @Override
    protected void onStop() {
        super.onStop();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refRequest = FirebaseDatabase.getInstance().getReference("customersRequest");
        GeoFire geoFireRequest = new GeoFire(refRequest);
        geoFireRequest.removeLocation(userId);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.Logout) {
            isLoggingOut = true;
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(customerMapActivity.this, customerLoginActivity.class);
            startActivity(intent);
            finish();
        } else if (v.getId() == R.id.requestARide){
            Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
            locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refRequest = FirebaseDatabase.getInstance().getReference("customersRequest");
                    GeoFire geoFireRequest = new GeoFire(refRequest);
                    geoFireRequest.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), (key, error) -> {
                        Log.d("Success",key);
                    });
                    pickUpLocation = new LatLng(location.getLatitude(),location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("The driver will arrive here"));
                    mRequestARideButton.setText("Looking for a driver...");
                }
            });
            locationTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Failure",e.toString());
                }
            });
        }
    }
}