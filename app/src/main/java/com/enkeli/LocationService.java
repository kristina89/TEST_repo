package com.enkeli;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Created by Kristina on 9/20/17.
 */

public class LocationService extends Service implements LocationListener {

    public class LocationServiceBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }

    private static String TAG = LocationService.class.getSimpleName();
    private static long MIN_TIME_LOCATION_UPDATE = 10000; // 10 seconds
    private static long FASTEST_INTERVAL = 5000; /* 5 sec */
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters
    private float MIN_SPEED = 5f;  // 5 km/h
    private static int COUNTER_MIDDLE = 3; //3 locations
    private static int CHECK_TEMPS = 2; // 2 temp

    private static int notificationId = 1;

    // Declaring a Location Manager
    protected LocationManager locationManager;
    private Context mContext;
    // flag for GPS status
    private boolean isGPSEnabled = false;
    // flag for network status
    private boolean isNetworkEnabled = false;
    // flag for GPS status
    private boolean canGetLocation = false;

    private double lastSpeedsSum = 0; //
    private int lastLocationCounter = 0;
    private boolean isRiding = false;
    private int tempsCounter = 0;
    private boolean runTimer = false;

    private Location lastLocation; // location
    private com.google.android.gms.location.LocationListener fusedLocationListener;

    private Handler timerHandler;
    private Runnable postDelayedRunnable;

    private GoogleApiClient locationUpdatesClient;

    private void startGps() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LogUtil.logInfo(TAG, "No PERMISSIONS");
            return;
        }

        try {
            locationUpdatesClient = buildLocationGoogleApiClient();
            locationUpdatesClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(TAG, "Failed to initialise getFusedLocationProviderClient");

        }

        if (locationUpdatesClient == null) {
            //This code do the same as the FusedLocationApi but FusedLocationApi can crash when
            // Google Services are updated. Better to use FusedLocationApi because it is fresh and provided by google.
            // The code above was wrote before FusedLocationApi was appeared and it uses deprecated methods.
            //IMPORTANT: It DOESN"T work on Nexus 5 (android 6.0.1)

            try {
                locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

                // getting GPS status
                isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                // getting network status
                isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!isGPSEnabled) {
                    LogUtil.logInfo(TAG, "GPS is not enabled");
                    showSettingsAlert();
                } else {
                    this.canGetLocation = true;

                    LogUtil.logInfo(TAG, "GPS Enabled >>>>>>");

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_LOCATION_UPDATE,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                }

//                if (isNetworkEnabled) {
//                    LogUtil.logInfo(TAG, "NETWORK listener started >>>>>>");
//
//                    locationManager.requestLocationUpdates(
//                            LocationManager.NETWORK_PROVIDER,
//                            MIN_TIME_LOCATION_UPDATE,
//                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
//                    LogUtil.logInfo("Network", "Network");
//                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected synchronized GoogleApiClient buildLocationGoogleApiClient() {
        LogUtil.logInfo(TAG, "Building GoogleApiClient");
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        LogUtil.logInfo(TAG, "GoogleApiClient is CONNECTED, status :" + locationUpdatesClient.isConnected());
                        writeToFileSeparator("GoogleApiClient is CONNECTED");
                        createLocationUpdateListener();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        LogUtil.logInfo(TAG, "GoogleApiClient is : Suspended");
                        writeToFileSeparator("GoogleApiClient is : Suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        LogUtil.logInfo(TAG, "GoogleApiClient is : FAILED");
                        writeToFileSeparator("GoogleApiClient is : FAILED");
                    }
                })
                .addApi(LocationServices.API)
                .build();

    }

    protected void createLocationUpdateListener() {

        if (locationUpdatesClient != null && locationUpdatesClient.isConnected()) {

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                LogUtil.logInfo(TAG, "No PERMISSIONS");
                writeToFileSeparator("No PERMISSIONS");
                return;
            }

            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(MIN_TIME_LOCATION_UPDATE);
            locationRequest.setFastestInterval(FASTEST_INTERVAL);

            fusedLocationListener = new com.google.android.gms.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    LocationService.this.onLocationChanged(location);
                }
            };

            LocationServices.FusedLocationApi.requestLocationUpdates(locationUpdatesClient, locationRequest,
                    fusedLocationListener);
        }
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(LocationService.this);
        }

        if (locationUpdatesClient != null && fusedLocationListener != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    locationUpdatesClient, fusedLocationListener);
            locationUpdatesClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopUsingGPS();
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocationServiceBinder();
    }

    @Override
    public void onLocationChanged(Location location) {
        double speedKH = 0;
        double distance2 = 0;

        if (lastLocation != null) {
            speedKH = countSpeed(location, lastLocation);
            distance2 = countDTM(location, lastLocation);
        }

        lastLocation = location;

        isRidingOrStopped(speedKH);

        writeToFile("speed (km/h)", speedKH);
        writeToFileSeparator("###########################################");

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("*****************");
        stringBuilder.append("\n");
        stringBuilder.append("SPEED: " + speedKH + " km/h");
        stringBuilder.append("\n");
        stringBuilder.append("DISTANCE: " + distance2 + " meters");
        stringBuilder.append("\n");
        stringBuilder.append("#######################");
        stringBuilder.append("\n");

        LogUtil.logInfo(TAG, "Location changed:");
        LogUtil.logInfo(TAG, stringBuilder.toString());

        Intent intent = new Intent(ExtraConstants.GPS_BROADCAST_RECEIVER);
        intent.putExtra(ExtraConstants.LOG_INFO, stringBuilder.toString());
        sendBroadcast(intent);
    }

    private void isRidingOrStopped(double speedKH) {

        lastSpeedsSum += speedKH;
        lastLocationCounter++;

        if (lastLocationCounter == COUNTER_MIDDLE) {
            lastLocationCounter = 0;
            double  middleSpeedValue = lastSpeedsSum / COUNTER_MIDDLE;
            lastSpeedsSum = 0;
            writeToFile("middleSpeedValue (km/h)", middleSpeedValue);

            if (isRiding) {
                // the car was riding
                if (middleSpeedValue < MIN_SPEED) {
                    // the car could be in STOPPED state, so we check it.
                    tempsCounter++;
                    writeToFileSeparator("RIDING TEMP " + tempsCounter);
                    if (tempsCounter == CHECK_TEMPS) {
                        // CAR stopped
//                        cleanTempCounters();
                        tempsCounter = 0;
                        isRiding = false;
                        runTimer = true;
                        writeToFileSeparator("TIMER IS RUN");
                        runNotificationTimer();
                        LogUtil.logInfo(TAG, "CAR STOPPED");
                    }
                } else {
                    // the car still is riding
                    tempsCounter = 0;
                    writeToFileSeparator("RIDING CLEANED TEMPS (<MIN_SPEED)");
                }
            } else {
                // the car is staying. We should get the moment when car will ride
                if (middleSpeedValue >= MIN_SPEED) {
                    tempsCounter++;
                    writeToFileSeparator("SOPPED TEMP " + tempsCounter);
                    if (tempsCounter == CHECK_TEMPS) {
                        // CAR is riding
                        cancelNotificationTimer();
                        tempsCounter = 0;
                        isRiding = true;
                        writeToFileSeparator("STARTED RIDING. WAITING FOR STOP.");
                        LogUtil.logInfo(TAG, "CAR STARTED riding");
                    }
                } else {
                    // the car still is stopped
                    tempsCounter = 0;
                    writeToFileSeparator("STOPPED CLEANED TEMPS (>=MIN_SPEED)");
                }
            }
        }
    }

    private void runNotificationTimer() {
//        int notifyTimePeriod = AppPreferences.getSendNotificationAfter(getApplicationContext()); // in minutes
        int notifyTimePeriod = 1; // in minutes
        long delayMilliseconds = notifyTimePeriod * 60000;
        timerHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                return false;
            }
        });

        postDelayedRunnable = new Runnable() {
            @Override
            public void run() {
                LogUtil.logInfo(TAG, "Is RUNNABLE");
                if (!isRiding) {
                    createNotification();
                }
            }
        };
        timerHandler.postDelayed(postDelayedRunnable, delayMilliseconds);
    }

    private void cancelNotificationTimer() {
        if (timerHandler != null && postDelayedRunnable != null) {
            timerHandler.removeCallbacks(postDelayedRunnable);
            timerHandler = null;
            postDelayedRunnable = null;
        }
    }

    private void createNotification() {
        int defaults = Notification.DEFAULT_VIBRATE;
        if (AppPreferences.getIsSound(getApplicationContext())) {
            defaults = Notification.DEFAULT_SOUND;
        }

        PendingIntent mainContentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setDefaults(defaults)
                        .setContentText(AppPreferences.getNotificationMessage(getApplicationContext()))
                        .setContentIntent(mainContentIntent);

                NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(++notificationId, mBuilder.build());
    }


    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    //    @IntDef(value = {Service.START_FLAG_REDELIVERY, Service.START_FLAG_RETRY}, flag = true)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.logInfo(TAG, "Service is stated");
        mContext = getApplicationContext();
//        String boatString = intent.getStringExtra(BOAT_UUID);
        startGps();
        return super.onStartCommand(intent, flags, startId);
    }

    // speed in km/h
    public static Double countSpeed(Location pos, Location prevPos) {
        float[] results = new float[1];
        Location.distanceBetween(prevPos.getLatitude(), prevPos.getLongitude(),
                pos.getLatitude(), pos.getLongitude(), results);
        float dist = results[0];
        float time = pos.getTime() - prevPos.getTime();

        return dist == 0 ? 0 : (double) (dist / (time / (1000 * 3600)) / 1000);
    }

    // in meters  (distance)
    public static double countDTM(Location location1, Location location2) {
        float[] resultsTarget = new float[1];
        Location.distanceBetween(location1.getLatitude(), location1.getLongitude(), location2.getLatitude(), location2.getLongitude(), resultsTarget);
        return resultsTarget[0];
    }

    public void writeToFile(String header, double speed) {
        String fileName = FileUtil.logsFileName;
        try {
            File file = FileUtil.createTempFile(getApplicationContext(), fileName);
            FileWriter writer = new FileWriter(file.getPath(), true);
            writer.append('\n');
            writer.append(new Date().toString());
            writer.append("   ");
            writer.append(header);
            writer.append(";");
            writer.append(String.valueOf(speed));
            writer.append('\n');
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.logError(TAG, "Failed to write location logs", e);
        }
    }

    private void writeToFileSeparator(String separator) {
        String fileName = FileUtil.logsFileName;
        try {
            File file = FileUtil.createTempFile(getApplicationContext(), fileName);
            FileWriter writer = new FileWriter(file.getPath(), true);
            writer.append(new Date().toString());
            writer.append("   ");
            writer.append(separator);
            writer.append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.logError(TAG, "Failed to write location logs", e);
        }
    }
}
