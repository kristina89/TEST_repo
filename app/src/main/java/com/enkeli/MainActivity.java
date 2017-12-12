package com.enkeli;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public class GPSServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Output the logs to ui
        }
    }

    private Intent locationService;
    private GPSServiceBroadcastReceiver gpsServiceBroadcastReceiver;

    private TextView notificationMessageTextView;
    private SwitchCompat enkeliSwitch;
    private SwitchCompat soundSwitch;
    private TextView timeValueTextView;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        statusTextView = (TextView) findViewById(R.id.statusTextView);

        timeValueTextView = (TextView) findViewById(R.id.timeValueTextView);
        notificationMessageTextView = (TextView) findViewById(R.id.notificationMessageTextView);
        enkeliSwitch = (SwitchCompat) findViewById(R.id.enkeliSwitch);
        soundSwitch = (SwitchCompat) findViewById(R.id.soundSwitch);

        setValues();
        checkStatusAndRunStopService();

        findViewById(R.id.notificationTimeContainer).setOnClickListener(this);
        findViewById(R.id.notificationMessageContainer).setOnClickListener(this);
        enkeliSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enable) {
                setAppState(enable);
                checkStatusAndRunStopService();
            }
        });
        soundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enable) {
                AppPreferences.setIsSound(enable, getApplicationContext());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkStatusAndRunStopService();
    }

    private void checkStatusAndRunStopService() {
        updateStatusView();
        runService();
    }

    private void setValues() {
        setSwitchState(AppPreferences.getIsEnabled(getApplicationContext()));
        soundSwitch.setChecked(AppPreferences.getIsSound(getApplicationContext()));
        if (AppPreferences.isNotificationTextDefined(getApplicationContext())) {
            notificationMessageTextView.setText(AppPreferences.getNotificationMessage(getApplicationContext()));
        }
        timeValueTextView.setText(NotificationDuration.findByValue(AppPreferences.getSendNotificationAfter(getApplicationContext())).getTitle());
    }

    private void updateStatusView() {
        boolean isEnabled = AppPreferences.getIsEnabled(getApplicationContext());
        boolean isNotificationTextDefined = AppPreferences.isNotificationTextDefined(getApplicationContext());
        if (isEnabled && isNotificationTextDefined) {
            setStatusViewStyle(false, R.string.message_app_is_active);
        } else if (!isEnabled) {
            setStatusViewStyle(true, R.string.message_turn_on);
        } else {
            setStatusViewStyle(true, R.string.message_specify_notification);
        }
    }

    private void runService() {
        boolean isEnabled = AppPreferences.getIsEnabled(getApplicationContext());
        if (isEnabled) {
            askPermissions();
        } else {
            stopGpsService();
        }
    }

    public void startGPSService() {
        if (locationService == null && gpsServiceBroadcastReceiver == null) {
            LogUtil.logInfo("MainActivity", "Registering service");
            locationService = new Intent(getApplicationContext(), LocationService.class);
            getApplicationContext().startService(locationService);
            gpsServiceBroadcastReceiver = new GPSServiceBroadcastReceiver();
            getApplicationContext().registerReceiver(gpsServiceBroadcastReceiver, new IntentFilter(ExtraConstants.GPS_BROADCAST_RECEIVER));
        }
    }

    public void stopGpsService() {
        if (locationService != null) {
            getApplicationContext().stopService(locationService);
            locationService = null;
        }
        if (gpsServiceBroadcastReceiver != null) {
            getApplicationContext().unregisterReceiver(gpsServiceBroadcastReceiver);
            gpsServiceBroadcastReceiver = null;
        }

        setAppState(false);
    }

    public void askPermissions() {

        //check the global location services
        if (!isLocationEnabled()) {
            setAppState(false);
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(R.string.location_service_not_enabled);
            dialog.setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    MainActivity.this.startActivity(myIntent);
                    paramDialogInterface.dismiss();
                }
            });
            dialog.setNegativeButton(R.string.cancel, null);
            dialog.show();
        } else {
            //check permissions for app
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        ExtraConstants.GET_PERMISSIONS_REQUEST_LOCATION);

            } else {
                startGPSService();
            }
        }
    }

    public boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case ExtraConstants.GET_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startGPSService();
                } else {
                    setAppState(false);
                }
                return;
            }
        }
    }

    private void setAppState(boolean enable) {
        AppPreferences.setIsEnabled(enable, getApplicationContext());
        setSwitchState(enable);
        setStatusViewStyle(true, R.string.message_turn_on);
    }

    private void setSwitchState(boolean active) {
        enkeliSwitch.setChecked(active);
        enkeliSwitch.setText(active ? getString(R.string.switch_enkeli_is_on)
                : getString(R.string.switch_enkeli_is_off));
    }

    private void setStatusViewStyle(boolean isWarning, int messageRes) {
        if (isWarning) {
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.red));
            statusTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0);
        } else {
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.dark_gray_text_color));
            statusTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ready, 0, 0, 0);
        }

        statusTextView.setText(getString(messageRes));
    }

    private void showEnterNotificationDialog(final String message) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_enter_text_view, null);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        final EditText notificationPatternEditText = view.findViewById(R.id.notificationPatternEditText);
        notificationPatternEditText.setText(message);

        builder.setTitle(R.string.label_notification_message)
                .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String newNotificationPattern = notificationPatternEditText.getText().toString();
                        AppPreferences.setNotificationMessage(newNotificationPattern, getApplicationContext());
                        if (newNotificationPattern.isEmpty()) {
                            notificationMessageTextView.setText(R.string.hint_customize_message);
                        } else {
                            notificationMessageTextView.setText(newNotificationPattern);
                        }
                        updateStatusView();
                    }
                })
                .setNegativeButton(R.string.cancel_button, null);
        builder.show();
    }

    private void showSelectDurationDialog(int value) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_send_notification_after);

        builder.setSingleChoiceItems(
                NotificationDuration.getTitles(),
                NotificationDuration.getItemPositionByValue(value),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NotificationDuration selectedItem = NotificationDuration.values()[which];

                        if (selectedItem != null) {
                            NotificationDuration notificationDuration = NotificationDuration.values()[which];
                            String selectedTitle = notificationDuration.getTitle();
                            AppPreferences.setSendNotificationAfter(notificationDuration.getValue(), getApplicationContext());
                            timeValueTextView.setText(selectedTitle);
                        }
                    }
                })
                .setPositiveButton(R.string.ok_button, null)
                .create()
                .show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.notificationMessageContainer:
                showEnterNotificationDialog(AppPreferences.getNotificationMessage(getApplicationContext()));
                break;
            case R.id.notificationTimeContainer:
                showSelectDurationDialog(AppPreferences.getSendNotificationAfter(getApplicationContext()));
                break;

        }
    }
}
