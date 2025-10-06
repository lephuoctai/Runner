package com.taile.runner.fragments;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.taile.runner.MainActivity;
import com.taile.runner.R;
import com.taile.runner.TrackerService;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean locationUpdateActive = false;
    private TrackerService trackerService;
    private boolean boundToService = false;
    private boolean mapReady = false;

    // Service connection
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackerService.LocalBinder binder = (TrackerService.LocalBinder) service;
            trackerService = binder.getService();
            boundToService = true;

            // Start observing location data if map is ready
            if (mapReady && trackerService != null) {
                observeLocationUpdates();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            boundToService = false;
            trackerService = null;
        }
    };

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Create location request
        locationRequest = new LocationRequest.Builder(2000) // Update every 2 seconds
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && googleMap != null) {
                    updateMapLocation(location);
                }
            }
        };

        // Try to get tracker service from activity
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            bindToTrackerService();
        }
    }

    private void bindToTrackerService() {
        // Connect to the service
        if (getActivity() != null) {
            // Check if service is already running
            if (trackerService == null) {
                try {
                    // Get service from the activity if possible
                    MainActivity activity = (MainActivity) getActivity();
                    trackerService = activity.getTrackerService();

                    if (trackerService != null) {
                        boundToService = true;
                        if (mapReady) {
                            observeLocationUpdates();
                        }
                    }
                } catch (Exception e) {
                    // Use direct location updates if we can't get the service
                }
            }
        }
    }

    private void observeLocationUpdates() {
        if (trackerService != null && mapReady) {
            trackerService.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
                if (location != null) {
                    updateMapLocation(location);
                }
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        mapReady = true;
        enableMyLocation();

        // If we already have the service bound, start observing location
        if (boundToService && trackerService != null) {
            observeLocationUpdates();
        }
    }

    private void enableMyLocation() {
        if (getActivity() != null && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            // Get current location and move camera there
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            updateMapLocation(location);
                        } else {
                            // If last location is null, start our own location updates
                            startLocationUpdates();
                        }
                    });
        } else {
            // If no permission, start location updates anyway (will be checked in startLocationUpdates)
            startLocationUpdates();
        }
    }

    private void updateMapLocation(Location location) {
        if (googleMap == null) return;

        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

        // Clear previous markers if needed
        googleMap.clear();

        // Add marker at current location
        googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));

        // Move camera to current location with zoom level 17 (street level)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 17f));
    }

    private void startLocationUpdates() {
        if (getActivity() != null && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
            );
            locationUpdateActive = true;
        }
    }

    private void stopLocationUpdates() {
        if (locationUpdateActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationUpdateActive = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        bindToTrackerService();
        if (googleMap != null) {
            enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
}
