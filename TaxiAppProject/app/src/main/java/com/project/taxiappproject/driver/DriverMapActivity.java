package com.project.taxiappproject.driver;

import static android.widget.Toast.makeText;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.taxiappproject.R;
import com.project.taxiappproject.databinding.ActivityDriverMapBinding;
import com.project.taxiappproject.objects.CustomerObject;

import java.util.List;


public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    final int REQUEST_LOCATION_PERMISSION_CODE = 202210;
    LocationRequest mLocationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;

    private ActivityDriverMapBinding binding;
    Button mLogoutButton;
    Boolean isLoggingOut = false;
    Switch connectionSwitch;
    Boolean Busy = false;
    DatabaseReference assignedClientPickupLocationRef;
    ValueEventListener assignedClientPickupLocationRefListener;
    LinearLayout customerInfo;
    TextView customerName,customerPhone;
    Marker customerPickupLocation;
    CustomerObject currentCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest.Builder(2000)
                .setMinUpdateIntervalMillis(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY).build();
        mLogoutButton = (Button) findViewById(R.id.Logout);
        mLogoutButton.setOnClickListener(this);
        connectionSwitch = (Switch) findViewById(R.id.working_mode);
        connectionSwitch.setOnClickListener(this);
        customerInfo = (LinearLayout) findViewById(R.id.customerInfo);
        customerName = (TextView) findViewById(R.id.customerName);
        customerPhone = (TextView) findViewById(R.id.customerPhone);
    }

    LocationCallback mLocationCallBack = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
                GeoFire geoFireWorking = new GeoFire(refWorking);
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
                GeoFire geoFireAvailable = new GeoFire(refAvailable);
                if(Busy){
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), (key, error) -> {
                        if(key == null){
                            Log.d("Database error",error.toString());
                        }
                    });
                } else {
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), (key, error) -> {
                        if(key == null){
                            Log.d("Database error",error.toString());
                        }
                    });
                }
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
                        (dialogInterface, i) -> ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE)).create().show();
            } else {
                ActivityCompat.requestPermissions(DriverMapActivity.this,
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

                    mMap.setMyLocationEnabled(true);
                }
            } else {
                makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
            }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isLoggingOut){
            disconnectDriver();
        }
    }

    private void disconnectDriver(){
        if(!Busy) {
            if (customerPickupLocation != null)
                customerPickupLocation.remove();
            if (assignedClientPickupLocationRefListener != null)
                assignedClientPickupLocationRef.removeEventListener(assignedClientPickupLocationRefListener);
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallBack);
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversAvailable");
            GeoFire geoFireWorking = new GeoFire(refWorking);
            geoFireWorking.removeLocation(userId);
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.Logout){
            isLoggingOut = true;
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallBack);
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(DriverMapActivity.this, DriverLoginActivity.class);
            startActivity(intent);
            finish();
        } else if(v.getId()==R.id.working_mode){
            if(connectionSwitch.isChecked()) {
                fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                listenToAClient();
            } else
                if(!Busy){
                disconnectDriver();
                } else {
                    connectionSwitch.setChecked(true);
                    Toast.makeText(DriverMapActivity.this,"You can't disconnect while having an order"
                    ,Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void listenToAClient(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedClientRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Drivers").child(userId).child("currentCustomerId");
        assignedClientRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String currentCustomerId = snapshot.getValue().toString();
                    currentCustomer = new CustomerObject(currentCustomerId);
                    Busy = true;
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerInfo();
                } else {
                    Busy = false;
                    if(customerPickupLocation != null)
                        customerPickupLocation.remove();
                    if(assignedClientPickupLocationRefListener != null)
                        assignedClientPickupLocationRef.removeEventListener(assignedClientPickupLocationRefListener);
                    customerInfo.setVisibility(View.GONE);
                    customerName.setText("");
                    customerPhone.setText("");
                    currentCustomer = null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedCustomerPickupLocation(){
        assignedClientPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customersRequest")
                .child(currentCustomer.getId()).child("l");
       assignedClientPickupLocationRefListener =  assignedClientPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && Busy){
                    List<Object> map = (List<Object>) snapshot.getValue();
                    List<Object> list = (List<Object>)snapshot.getValue();
                    double LocationLat = 0 ,LocationLon = 0;
                    if(list.get(0) != null && list.get(0) != null) {
                        LocationLat = Double.parseDouble(list.get(0).toString());
                        LocationLon = Double.parseDouble(list.get(1).toString());
                    }
                    LatLng latLng = new LatLng(LocationLat,LocationLon);
                    customerPickupLocation = mMap.addMarker(new MarkerOptions().position(latLng).title("Pickup here"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("Database Error",error.toString());
            }
        });
    }

    private void getAssignedCustomerInfo() {
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Customers").child(currentCustomer.getId());
        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    String key = snapshot.getKey().toString();
                    Toast.makeText(DriverMapActivity.this, key, Toast.LENGTH_SHORT).show();
                    currentCustomer.parseData(snapshot);
                    customerName.setText(currentCustomer.getName());
                    customerPhone.setText(currentCustomer.getPhone());
                    customerInfo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}