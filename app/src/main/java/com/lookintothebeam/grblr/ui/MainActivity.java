package com.lookintothebeam.grblr.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.lookintothebeam.grblr.R;

import com.lookintothebeam.grblr.cnc.CNCService;
import com.lookintothebeam.grblr.cnc.CNCServiceEvent;
import com.lookintothebeam.grblr.cnc.GcodeCommand;
import com.lookintothebeam.grblr.cnc.GrblController;
import com.lookintothebeam.grblr.cnc.GrblControllerEvent;
import com.lookintothebeam.grblr.ui.visualizer.GcodeVisualizerSurfaceView;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.util.Map;
import java.util.Set;

import org.greenrobot.eventbus.EventBus;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public static final int FILE_PICK_CODE = 1;

    // --- UI ---
    private RelativeLayout mRootLayoutView;
    private TabHost mTabHost;

    private TextView mMainStatusText;
    private TextView mSecondaryStatusText;
    private TextView mPositionTextView;

    private RadioButton mUSBStatusRadio;
    private ImageButton mUSBConnectButton;
    private ImageButton mSettingsButton;

    private Button mXMinusJogButton;
    private Button mXPlusJogButton;
    private Button mYMinusJogButton;
    private Button mYPlusJogButton;
    private Button mZMinusJogButton;
    private Button mZPlusJogButton;
    private Button mHomingSequenceButton;
    private Button mReturnToZeroButton;
    private Button mSetZeroXYButton;
    private Button mSetZeroZButton;

    private ListView mFileCodeLineListView;
    private TextView mFileNameTextView;
    private android.support.design.widget.FloatingActionButton mCycleStartButton;
    private GcodeFileListAdapter mFileListViewAdapter;

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

            //EventBus.getDefault().registerSticky(MainActivity.this);

            toggleUSBButton(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCNCServiceBound = false;

            EventBus.getDefault().unregister(MainActivity.this);

            toggleUSBButton(false);
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
            case FILE_OPEN_SUCCESS:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initFileContents();
                    }
                });
                break;
            case FILE_COMMAND_STATUS_UPDATED:
                refreshFileContents();
                break;
        }
    }

    public void onEvent(GrblControllerEvent event) {
        switch(event.type) {
            case MACHINE_STATUS_UPDATED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateMachineStatus();
                    }
                });
                break;
        }
    }

    // --- Activity Lifecycle ---
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI
        mMainStatusText = (TextView) findViewById(R.id.mainStatusText);
        mSecondaryStatusText = (TextView) findViewById(R.id.secondaryStatusText);
        mUSBStatusRadio = (RadioButton) findViewById(R.id.usbStatusButton);
        mUSBConnectButton = (ImageButton) findViewById(R.id.usbConnectButton);
        mSettingsButton = (ImageButton) findViewById(R.id.settingsButton);
        mVisualizerSurfaceView = (GcodeVisualizerSurfaceView) findViewById(R.id.gcodeVisualizerSurfaceView);
        mPositionTextView = (TextView) findViewById(R.id.positionTextView);

        // JOG BUTTONS
        mXMinusJogButton = (Button) findViewById(R.id.xMinusJogButton); mXMinusJogButton.setOnTouchListener(mJogButtonTouchListener);
        mXPlusJogButton = (Button) findViewById(R.id.xPlusJogButton); mXPlusJogButton.setOnTouchListener(mJogButtonTouchListener);
        mYMinusJogButton = (Button) findViewById(R.id.yMinusJogButton); mYMinusJogButton.setOnTouchListener(mJogButtonTouchListener);
        mYPlusJogButton = (Button) findViewById(R.id.yPlusJogButton); mYPlusJogButton.setOnTouchListener(mJogButtonTouchListener);
        mZMinusJogButton = (Button) findViewById(R.id.zMinusJogButton); mZMinusJogButton.setOnTouchListener(mJogButtonTouchListener);
        mZPlusJogButton = (Button) findViewById(R.id.zPlusJogButton); mZPlusJogButton.setOnTouchListener(mJogButtonTouchListener);

        mSetZeroXYButton = (Button) findViewById(R.id.zeroXYButton);
        mSetZeroZButton = (Button) findViewById(R.id.zeroZButton);

        mFileCodeLineListView = (ListView) findViewById(R.id.fileCodeLineListView);
        mFileNameTextView = (TextView) findViewById(R.id.fileNameTextView);
        mCycleStartButton = (android.support.design.widget.FloatingActionButton) findViewById(R.id.cycleStartButton);

        mRootLayoutView = (RelativeLayout) findViewById(R.id.rootLayoutView);
        setupTabs();

        Intent intent = new Intent(this, CNCService.class);
        bindService(intent, mCNCServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // The following call resumes a paused rendering thread.
        // Re-allocate graphic objects from onPause()
        mVisualizerSurfaceView.onResume();

        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // The following call pauses the rendering thread.
        // Consider de-allocating objects that consume significant memory here.
        mVisualizerSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mCNCServiceConnection);
        mCNCServiceBound = false;
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

        TabHost.TabSpec ts3 = mTabHost.newTabSpec("gcodeControl");
        ts3.setContent(R.id.gcodeControlTabContent);
        ts3.setIndicator(getResources().getString(R.string.gcode_control_tab_name));
        mTabHost.addTab(ts3);
    }

    // --- Listeners ---
    private View.OnTouchListener mJogButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            GrblController grblController = mCNCService.getGrblController();
            if(grblController == null) return true;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
//                    if(v == mXMinusJogButton) grblController.jogX(-1);
//                    if(v == mXPlusJogButton) grblController.jogX(1);
//                    if(v == mYMinusJogButton) grblController.jogY(-1);
//                    if(v == mYPlusJogButton) grblController.jogY(1);
//                    if(v == mZMinusJogButton) grblController.jogZ(-1);
//                    if(v == mZPlusJogButton) grblController.jogZ(1);
                    break;
                case MotionEvent.ACTION_UP:
//                    grblController.stopJogging();
                    break;
            }
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // File picker code
        if(requestCode == FILE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            if(data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {

                ClipData clip = data.getClipData();

                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri uri = clip.getItemAt(i).getUri();
                        // Do something with the URI
                    }
                }

            } else {
                Uri uri = data.getData();
                mCNCService.openFile(uri.getPath());
            }
        }
    }

    // --- Actions ---

    public void onSendClick(View v) {
        EditText serialEditText = (EditText) findViewById(R.id.serialEditText);
        mCNCService.sendSerialData(serialEditText.getText().toString());
        serialEditText.setText("");
    }

    public void onCycleStartClick(View v) {
        mCNCService.cycleStart();
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
        //TODO: Disconnect menu item?
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

    public void onFileOpenClick(View v) {

        if(mCNCService.getFileCommandQueueStarted()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.confirm_open_file);

            alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Cancel
                }
            });

            alert.setPositiveButton(R.string.confirm_open_file, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Confirm
                    openFilePicker();
                }
            });

            alert.show();
        } else {
            openFilePicker();
        }
    }

    private void openFilePicker() {
        Intent i = new Intent(this, FilePickerActivity.class);

        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, FILE_PICK_CODE);
    }

    private void toggleUSBButton(boolean enabled) {
        mUSBConnectButton.setEnabled(enabled);
        mUSBConnectButton.setClickable(enabled);
        mUSBConnectButton.setImageAlpha(enabled ? 255 : 127);
    }

    private void toggleUSBConnectionStatus(boolean enabled) {
        mUSBStatusRadio.setChecked(enabled);
        mUSBStatusRadio.setAlpha(enabled ? 1.0f : 0.5f);

        mXPlusJogButton.setEnabled(enabled);
        mXMinusJogButton.setEnabled(enabled);
        mYPlusJogButton.setEnabled(enabled);
        mYMinusJogButton.setEnabled(enabled);
        mZPlusJogButton.setEnabled(enabled);
        mZMinusJogButton.setEnabled(enabled);

        mSetZeroXYButton.setEnabled(enabled);
        mSetZeroZButton.setEnabled(enabled);

        //mCycleStartButton.setEnabled(enabled);
    }

    private void updateMachineStatus() {
        GrblController grblController = mCNCService.getGrblController();

        float[] machinePos = grblController.getMachinePos();
        mPositionTextView.setText("X: " + Float.toString(machinePos[0]) + "\nY: " + Float.toString(machinePos[1]) + "\nZ: " + Float.toString(machinePos[2]));

        mMainStatusText.setText(grblController.getMachineStatus().toString());
    }

    private void initFileContents() {
        mFileNameTextView.setText(mCNCService.getFile().getName());
        mFileListViewAdapter = new GcodeFileListAdapter(this, mCNCService.getCommandQueue().toArray(new GcodeCommand[mCNCService.getCommandQueue().size()]));
        mFileCodeLineListView.setAdapter(mFileListViewAdapter);
    }
    private void refreshFileContents() {
        mFileListViewAdapter.notifyDataSetChanged();

        // Hopefully scroll to middle
        mFileCodeLineListView.smoothScrollToPosition(mCNCService.getFileCommandPosition() - 5);
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
