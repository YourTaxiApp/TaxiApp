package com.project.taxiappproject.customer;

import static android.widget.Toast.makeText;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
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
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.project.taxiappproject.R;
import com.project.taxiappproject.databinding.ActivityCustomerMapBinding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    final int REQUEST_LOCATION_PERMISSION_CODE = 202210;
    FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private ActivityCustomerMapBinding binding;
    Button mLogoutButton,mRequestARideButton,cancelRideButton;
    Boolean isLoggingOut = false;
    LatLng pickUpLocation;
    GeoQuery geoQuery;
    double radius;
    Boolean driverFound = false;
    Boolean requestBol = false;
    String driverFoundId;
    DatabaseReference driverLocationRef;
    ValueEventListener driverLocationRefListener;
    Marker driverMarker,pickupMarker;

    GoogleMap.OnCameraIdleListener onCameraIdleListener;

    AutocompleteSupportFragment pickupAutocompleteFragment,destinationAutocompleteFragment;
    PlacesClient mPlaces;

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
        mLogoutButton = (Button) findViewById(R.id.Logout);
        mLogoutButton.setOnClickListener(this);
        mRequestARideButton = (Button) findViewById(R.id.requestARide);
        mRequestARideButton.setOnClickListener(this);
        cancelRideButton = (Button) findViewById(R.id.cancelARide);
        cancelRideButton.setOnClickListener(this);
        radius = 1;
        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(),getResources().getString(R.string.google_key));
        }
        mPlaces = Places.createClient(this);
        instanceAutocompletePickup();
        instanceAutocompleteDestination();
        onCameraMove();
    }

    private void onCameraMove(){
        onCameraIdleListener = new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                try {
                    Geocoder geocoder = new Geocoder(CustomerMapActivity.this);
                    LatLng latLng = mMap.getCameraPosition().target;
                    List<Address> addressList = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
                    String address = addressList.get(0).getAddressLine(0).toString();
                }catch (Exception e){
                    Log.d("Error",e.toString());
                }
            }
        };
    }

    private void instanceAutocompletePickup(){
        pickupAutocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_pickup);
        pickupAutocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG));
        pickupAutocompleteFragment.setHint("Pickup");
        pickupAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                Log.d("Error",status.toString());
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.d("Location",place.getName());
            }
        });
    }

    private void instanceAutocompleteDestination(){
        destinationAutocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_destination);
        destinationAutocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG));
        destinationAutocompleteFragment.setHint("Destination");
        destinationAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                Log.d("Error",status.toString());
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Toast.makeText(getApplicationContext(),place.getName().toString(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    private void limitSearch(){
        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                LatLng latLng = new LatLng(56.3269,44.0059);
                LatLng northSide = SphericalUtil.computeOffset(latLng, 460000,0);
                LatLng southSide = SphericalUtil.computeOffset(latLng, 460000,180);
                pickupAutocompleteFragment.setLocationBias(RectangularBounds.newInstance(northSide,southSide));
                destinationAutocompleteFragment.setLocationBias(RectangularBounds.newInstance(northSide,southSide));
            }
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnCameraIdleListener(onCameraIdleListener);
            //limitSearch();
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
                        (dialogInterface, i) -> ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE)).create().show();
            } else {
                ActivityCompat.requestPermissions(CustomerMapActivity.this,
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

    private void disconnect(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refRequest = FirebaseDatabase.getInstance().getReference("customersRequest");
        GeoFire geoFireRequest = new GeoFire(refRequest);
        geoFireRequest.removeLocation(userId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isLoggingOut){
            disconnect();
        }
    }


    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.Logout:
                isLoggingOut = true;
                disconnect();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, CustomerLoginActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.requestARide:
                requestBol = true;
                Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
                locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference refRequest = FirebaseDatabase.getInstance().getReference("customersRequest");
                        GeoFire geoFireRequest = new GeoFire(refRequest);
                        geoFireRequest.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), (key, error) -> {
                            if(key != null)
                                Log.d("Success",key);
                            else
                                Log.d("Database error",error.toString());
                        });
                        pickUpLocation = new LatLng(location.getLatitude(),location.getLongitude());

                        pickupMarker =  mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("The driver will arrive here"));
                        mRequestARideButton.setText("Looking for a driver...");
                        findADriver(location);
                    }
                });
                locationTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Failure",e.toString());
                    }
                });
                break;
            default:
                if (driverFound) {
                    if(geoQuery != null)
                        geoQuery.removeAllListeners();
                    if(driverLocationRefListener != null)
                        driverLocationRef.removeEventListener(driverLocationRefListener);
                    requestBol = false;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refRequest = FirebaseDatabase.getInstance().getReference("customersRequest");
                    GeoFire geoFireRequest = new GeoFire(refRequest);
                    geoFireRequest.removeLocation(userId);
                    if(driverFoundId != null){
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers")
                            .child(driverFoundId).child("currentCustomerId");
                        driverRef.setValue(null);
                        driverFoundId =  null;
                    }
                    driverFound = false;
                    radius = 1;
                    if(pickupMarker != null) pickupMarker.remove();
                    if(driverMarker != null) driverMarker.remove();
                }
                cancelRideButton.setVisibility(View.GONE);
                mRequestARideButton.setText(R.string.find_driver);
                break;
        }
    }

    private void findADriver(Location location){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire =  new GeoFire(ref);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(),location.getLongitude()),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBol) {
                    driverFound = true;
                    driverFoundId = key;
                }

                DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers")
                        .child(driverFoundId);
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                HashMap map = new HashMap();
                map.put("currentCustomerId",userId);
                driverRef.updateChildren(map);
                getDriverLocation();
                mRequestARideButton.setText("Loading driver location...");
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound){
                    radius++;
                    findADriver(location);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.d("Error",error.toString());
            }
        });
    }

    private void getDriverLocation(){
        cancelRideButton.setVisibility(View.VISIBLE);
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId)
                .child("l");
        driverLocationRefListener =  driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && requestBol){
                    List<Object> list = (List<Object>)snapshot.getValue();
                    double LocationLat = 0 ,LocationLon = 0;
                    mRequestARideButton.setText("The driver on his way to you");
                    if(list.get(0) != null && list.get(0) != null) {
                        LocationLat = Double.parseDouble(list.get(0).toString());
                        LocationLon = Double.parseDouble(list.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(LocationLat,LocationLon);
                    double distance = distance(pickUpLocation,driverLatLng);
                    mRequestARideButton.setText("Driver on his way " + String.valueOf(distance/1000) + " km");
                    if(distance <= 50){
                        mRequestARideButton.setText("Your driver arrived");
                    }
                    if(driverMarker != null) driverMarker.remove();
                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your driver"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private double distance(LatLng latLng1, LatLng latLng2) {

        final int R = 6371;

        double latDistance = Math.toRadians(latLng2.latitude - latLng1.latitude);
        double lonDistance = Math.toRadians(latLng2.longitude - latLng1.longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latLng1.latitude)) * Math.cos(Math.toRadians(latLng2.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters


        distance = Math.pow(distance, 2) + Math.pow(0.0, 2);

        return Math.sqrt(distance);
    }

}