package com.taile.runner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class TrackerService extends LifecycleService implements SensorEventListener {

    private static final String CHANNEL_ID = "tracker_notification_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final float STEP_LENGTH = 0.75f; // Average step length in meters
    private static final float MAX_VALID_SPEED = 12.0f; // Maximum valid speed in m/s

    // LiveData for UI updates
    private final MutableLiveData<Integer> stepCount = new MutableLiveData<>(0);
    private final MutableLiveData<Float> totalDistance = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> currentSpeed = new MutableLiveData<>(0f);

    // Location tracking
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private long lastLocationTime;

    // Step counting
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int initialSteps = -1;
    private int currentSteps = 0;

    // Binder
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public TrackerService getService() {
            return TrackerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create location request
        locationRequest = new LocationRequest.Builder(2000) // Update every 2 seconds
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(1000)
                .build();

        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        // Initialize step sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // Start foreground service with notification
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Start location updates
        startLocationUpdates();

        // Start step counting
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Stop location updates
        stopLocationUpdates();

        // Stop step counting
        if (stepSensor != null) {
            sensorManager.unregisterListener(this);
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }

    // Location updates handling
    private void startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void onNewLocation(Location location) {
        if (location != null) {
            if (lastLocation != null) {
                // Calculate distance between current and last location
                float distance = location.distanceTo(lastLocation);

                // Calculate time difference in seconds
                long currentTime = System.currentTimeMillis();
                float timeDifference = (currentTime - lastLocationTime) / 1000f;

                // Calculate speed in m/s
                float speed = distance / timeDifference;

                // Check if speed is reasonable (not GPS error)
                if (speed <= MAX_VALID_SPEED) {
                    // Update total distance (convert to kilometers)
                    float currentTotalDistance = totalDistance.getValue() != null ?
                            totalDistance.getValue() : 0f;
                    totalDistance.postValue(currentTotalDistance + (distance / 1000f));

                    // Update current speed
                    currentSpeed.postValue(speed);
                } else {
                    // GPS error, show warning toast
                    Toast.makeText(
                            this,
                            R.string.gps_signal_unstable,
                            Toast.LENGTH_SHORT
                    ).show();

                    // Keep the last valid speed
                    float lastSpeed = currentSpeed.getValue() != null ?
                            currentSpeed.getValue() : 0f;
                    currentSpeed.postValue(lastSpeed);
                }
            }

            // Update last location
            lastLocation = location;
            lastLocationTime = System.currentTimeMillis();
        }
    }

    // Step counter sensor handling
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int steps = (int) event.values[0];

            // Initialize baseline on first reading
            if (initialSteps == -1) {
                initialSteps = steps;
            }

            // Calculate steps since start
            currentSteps = steps - initialSteps;
            stepCount.postValue(currentSteps);

            // Add steps to distance calculation (convert step distance to km)
            float stepDistanceKm = (currentSteps * STEP_LENGTH) / 1000f;

            // We're not directly adding this to totalDistance to avoid double counting
            // The UI can use both values for display if needed
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    // LiveData getters for UI observation
    public MutableLiveData<Integer> getStepCount() {
        return stepCount;
    }

    public MutableLiveData<Float> getTotalDistance() {
        return totalDistance;
    }

    public MutableLiveData<Float> getCurrentSpeed() {
        return currentSpeed;
    }

    // Notification management
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Tracker Notification Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.tracker_notification_title))
                .setContentText(getString(R.string.tracker_notification_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
