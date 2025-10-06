package com.taile.runner;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.taile.runner.TrackerService.LocalBinder;
import com.taile.runner.databinding.ActivityMainBinding;
import com.taile.runner.fragments.MapFragment;
import com.taile.runner.fragments.RecordedLogFragment;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean isTracking = false;
    private TrackerService trackerService;
    private boolean boundToService = false;

    // Current active fragment
    private Fragment currentFragment;

    // Formatters for display
    private final DecimalFormat distanceFormat = new DecimalFormat("0.00");
    private final DecimalFormat speedFormat = new DecimalFormat("0.0");

    // Service connection
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            trackerService = binder.getService();
            boundToService = true;

            // Start observing data from service
            setupObservers();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            boundToService = false;
        }
    };

    // Permission launcher
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    allGranted = allGranted && isGranted;
                }

                if (allGranted) {
                    startTracking();
                } else {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup RadioGroup for tab switching
        setupTabsRadioGroup();

        // Setup start/stop button
        binding.btnStartStop.setOnClickListener(v -> {
            if (isTracking) {
                stopTracking();
            } else {
                checkPermissionsAndStartTracking();
            }
        });

        // Initialize UI
        updateUIForTrackingState();

        // Show the default fragment (RecordedLog)
        showFragment(new RecordedLogFragment());
    }

    private void setupTabsRadioGroup() {
        binding.tagGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.tagLogRecord) {
                showFragment(new RecordedLogFragment());
            } else if (checkedId == R.id.tagMap) {
                showFragment(new MapFragment());
            } else if (checkedId == R.id.tagRanking) {
                // Implement Ranking fragment if needed
                Toast.makeText(this, "Ranking feature coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        // Set default selection
        binding.tagLogRecord.setChecked(true);
    }

    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Replace whatever is in the fragment container with the new fragment
        transaction.replace(R.id.cardShowContentTag, fragment);

        // Complete the changes
        transaction.commit();

        currentFragment = fragment;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to TrackerService
        Intent intent = new Intent(this, TrackerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from service
        if (boundToService) {
            unbindService(connection);
            boundToService = false;
        }
    }

    private void setupObservers() {
        if (trackerService != null) {
            // Observe step count
            trackerService.getStepCount().observe(this, steps -> {
                binding.tvSteps.setText(String.valueOf(steps));
            });

            // Observe distance
            trackerService.getTotalDistance().observe(this, distance -> {
                binding.tvDistance.setText(distanceFormat.format(distance));
            });

            // Observe speed
            trackerService.getCurrentSpeed().observe(this, speed -> {
                binding.tvSpeed.setText(speedFormat.format(speed));
            });

            // Observe current location (will be useful for MapFragment)
            trackerService.getCurrentLocation().observe(this, location -> {
                if (currentFragment instanceof MapFragment) {
                    // The MapFragment will handle this through its own observer
                }
            });
        }
    }

    private void checkPermissionsAndStartTracking() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Check activity recognition permission (for step counter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsNeeded.isEmpty()) {
            // Show rationale before requesting permissions
            showPermissionRationale(permissionsNeeded.toArray(new String[0]));
        } else {
            // All permissions granted, start tracking
            startTracking();
        }
    }

    private void showPermissionRationale(String[] permissions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_required)
                .setMessage(R.string.location_permission_message)
                .setPositiveButton(R.string.grant_permission, (dialog, which) ->
                        permissionLauncher.launch(permissions))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startTracking() {
        // Start the tracker service
        Intent serviceIntent = new Intent(this, TrackerService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // Update tracking state
        isTracking = true;
        updateUIForTrackingState();
    }

    private void stopTracking() {
        // Stop the tracker service
        if (boundToService) {
            Intent serviceIntent = new Intent(this, TrackerService.class);
            stopService(serviceIntent);
        }

        // Update tracking state
        isTracking = false;
        updateUIForTrackingState();

        // Refresh the RecordedLogFragment if it's visible
        if (currentFragment instanceof RecordedLogFragment) {
            showFragment(new RecordedLogFragment());
        }
    }

    private void updateUIForTrackingState() {
        // Update button text
        binding.btnStartStop.setText(isTracking ? R.string.stop : R.string.start);

        // Update button style
        if (isTracking) {
            binding.btnStartStop.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.stop_button));
        } else {
            binding.btnStartStop.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.start_button));
        }
    }

    /**
     * Method to provide access to the TrackerService for fragments
     * @return The current TrackerService instance or null if not bound
     */
    public TrackerService getTrackerService() {
        return boundToService ? trackerService : null;
    }
}
