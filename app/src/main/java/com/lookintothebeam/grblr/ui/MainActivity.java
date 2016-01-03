package com.lookintothebeam.grblr.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.media.Image;
import android.opengl.GLSurfaceView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.lookintothebeam.grblr.R;
import com.lookintothebeam.grblr.web.WebServerService;

import com.lookintothebeam.grblr.cnc.CNCService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // UI
    private RelativeLayout rootLayoutView;
    private TabHost tabHost;

    private TextView mainStatusText;
    private TextView secondaryStatusText;
    private RadioButton usbStatusRadio;
    private ImageButton usbConnectButton;
    private ImageButton settingsButton;

    private GcodeVisualizerSurfaceView visualizerSurfaceView;

    // State

    // CNC Service
    public CNCService cncService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupTabs();
        rootLayoutView = (RelativeLayout) findViewById(R.id.rootLayoutView);

        //startService(new Intent(getBaseContext(), WebServerService.class));

        //TODO: Wait for service to started
        startService(new Intent(getBaseContext(), CNCService.class));

        Log.d("MainActivity", "Service should be started.");
        cncService = CNCService.sInstance;
    }

    public void onSendClick(View v) {
        EditText serialEditText = (EditText) findViewById(R.id.serialEditText);
        cncService.sendSerialData(serialEditText.getText().toString());
        serialEditText.setText("");
    }

    public void onUSBClick(View v) {

        cncService = CNCService.sInstance;

        final Set<Map.Entry<String, UsbDevice>> devices = cncService.getUSBDevices().entrySet();
        String[] deviceStrings = new String[devices.size()];

        int i = 0;
        for(Map.Entry<String, UsbDevice> deviceEntry : devices) {
            UsbDevice device = deviceEntry.getValue();
            deviceStrings[i] = deviceEntry.getKey() + " - " + device.getProductName() + " (" + device.getManufacturerName() + ")";
            i++;
        }

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.select_usb_device);
        alert.setItems(deviceStrings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int i = 0;
                for(Map.Entry<String, UsbDevice> deviceEntry : devices) {
                    if(i == which) {
                        cncService.openUSBDevice(deviceEntry.getKey());
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
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec ts1 = tabHost.newTabSpec("manualControl");
        ts1.setContent(R.id.manualControlTabContent);
        ts1.setIndicator(getResources().getString(R.string.machine_control_tab_name));
        tabHost.addTab(ts1);

        TabHost.TabSpec ts2 = tabHost.newTabSpec("fileControl");
        ts2.setContent(R.id.fileControlTabContent);
        ts2.setIndicator(getResources().getString(R.string.file_control_tab_name));
        tabHost.addTab(ts2);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        visualizerSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize UI
        mainStatusText = (TextView) findViewById(R.id.mainStatusText);
        secondaryStatusText = (TextView) findViewById(R.id.secondaryStatusText);
        usbStatusRadio = (RadioButton) findViewById(R.id.usbStatusButton);
        usbConnectButton = (ImageButton) findViewById(R.id.usbConnectButton);
        settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        visualizerSurfaceView = (GcodeVisualizerSurfaceView) findViewById(R.id.gcodeVisualizerSurfaceView);

        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        visualizerSurfaceView.onResume();

        hideSystemUI();
    }

    public void onViewClick(View v) {
        hideSystemUI();
    }

    private void hideSystemUI() {
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        rootLayoutView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void showSystemUI() {
        rootLayoutView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
