package com.lookintothebeam.grblr.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.lookintothebeam.grblr.R;

import com.lookintothebeam.grblr.cnc.CNCService;
import com.lookintothebeam.grblr.cnc.CNCServiceEvent;

import java.util.Map;
import java.util.Set;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {

    // --- UI ---
    private RelativeLayout mRootLayoutView;
    private TabHost mTabHost;

    private TextView mMainStatusText;
    private TextView mSecondaryStatusText;
    private RadioButton mUSBStatusRadio;
    private ImageButton mUSBConnectButton;
    private ImageButton mSettingsButton;

    private GcodeVisualizerSurfaceView mVisualizerSurfaceView;

    // --- CNC Service ---
    private CNCService mCNCService;
    private boolean mCNCServiceBound = false;
    private ServiceConnection mCNCServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CNCService.LocalBinder binder = (CNCService.LocalBinder) service;
            mCNCService = binder.getService();
            mCNCServiceBound = true;

            EventBus.getDefault().registerSticky(MainActivity.this);

            toggleUSBButtion(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCNCServiceBound = false;

            EventBus.getDefault().unregister(MainActivity.this);

            toggleUSBButtion(false);
        }
    };

    // --- Service Events ---
    public void onEvent(CNCServiceEvent event) {
        switch(event.type) {
            case USB_DEVICE_CONNECTED:
                toggleUSBConnectionStatus(true);
                break;
            case USB_DEVICE_DISCONNECTED:
                toggleUSBConnectionStatus(false);
                break;
        }

    }

    // --- Activity Lifecycle ---
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, CNCService.class);
        bindService(intent, mCNCServiceConnection, Context.BIND_AUTO_CREATE);

        setupTabs();
        mRootLayoutView = (RelativeLayout) findViewById(R.id.rootLayoutView);

        //startService(new Intent(getBaseContext(), WebServerService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mCNCServiceConnection);
        mCNCServiceBound = false;
    }

    public void onSendClick(View v) {
        EditText serialEditText = (EditText) findViewById(R.id.serialEditText);
        mCNCService.sendSerialData(serialEditText.getText().toString());
        serialEditText.setText("");
    }

    public void onUSBClick(View v) {

        final Set<Map.Entry<String, UsbDevice>> devices = mCNCService.getUSBDevices().entrySet();
        String[] deviceStrings = new String[devices.size()];

        int i = 0;
        for(Map.Entry<String, UsbDevice> deviceEntry : devices) {
            UsbDevice device = deviceEntry.getValue();
            deviceStrings[i] = deviceEntry.getKey() + " - " + device.getProductName() + " (" + device.getManufacturerName() + ")";
            i++;
        }

        //TODO: No devices available
        //TODO: Disconnect menu item
        //TODO: Notify after permission request

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.select_usb_device);
        alert.setItems(deviceStrings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int i = 0;
                for (Map.Entry<String, UsbDevice> deviceEntry : devices) {
                    if (i == which) {
                        mCNCService.openUSBDevice(deviceEntry.getKey());
                        return;
                    }
                    i++;
                }
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Cancel
            }
        });

        alert.show();
    }

    private void setupTabs() {
        mTabHost = (TabHost) findViewById(R.id.tabHost);
        mTabHost.setup();

        TabHost.TabSpec ts1 = mTabHost.newTabSpec("manualControl");
        ts1.setContent(R.id.manualControlTabContent);
        ts1.setIndicator(getResources().getString(R.string.machine_control_tab_name));
        mTabHost.addTab(ts1);

        TabHost.TabSpec ts2 = mTabHost.newTabSpec("fileControl");
        ts2.setContent(R.id.fileControlTabContent);
        ts2.setIndicator(getResources().getString(R.string.file_control_tab_name));
        mTabHost.addTab(ts2);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // The following call pauses the rendering thread.
        // Consider de-allocating objects that consume significant memory here.
        mVisualizerSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize UI
        mMainStatusText = (TextView) findViewById(R.id.mainStatusText);
        mSecondaryStatusText = (TextView) findViewById(R.id.secondaryStatusText);
        mUSBStatusRadio = (RadioButton) findViewById(R.id.usbStatusButton);
        mUSBConnectButton = (ImageButton) findViewById(R.id.usbConnectButton);
        mSettingsButton = (ImageButton) findViewById(R.id.settingsButton);
        mVisualizerSurfaceView = (GcodeVisualizerSurfaceView) findViewById(R.id.gcodeVisualizerSurfaceView);

        // The following call resumes a paused rendering thread.
        // Re-allocate graphic objects from onPause()
        mVisualizerSurfaceView.onResume();

        hideSystemUI();
    }

    private void toggleUSBButtion(boolean enabled) {
        mUSBConnectButton.setEnabled(enabled);
        mUSBConnectButton.setClickable(enabled);
        mUSBConnectButton.setImageAlpha(enabled ? 255 : 127);
    }

    private void toggleUSBConnectionStatus(boolean enabled) {
        mUSBStatusRadio.setChecked(enabled);
        mUSBStatusRadio.setAlpha(enabled ? 1.0f : 0.5f);
    }

    public void onViewClick(View v) {
        hideSystemUI();
    }

    private void hideSystemUI() {
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mRootLayoutView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void showSystemUI() {
        mRootLayoutView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
