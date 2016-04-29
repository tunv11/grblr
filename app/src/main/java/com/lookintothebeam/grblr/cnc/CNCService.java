package com.lookintothebeam.grblr.cnc;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import org.greenrobot.eventbus.EventBus;

public class CNCService extends Service {

    // --- Constants ---
    private static final String TAG = "CNCService";
    private static final String ACTION_USB_PERMISSION = "com.lookintothebeam.grblr.USB_PERMISSION";

    // --- Service Binding ---
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public CNCService getService() {
            return CNCService.this; // Return this instance of CNCService so clients can call public methods
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        init();
        return mBinder;
    }

    // --- USB ---
    private static boolean mInitialized = false; //TODO: Better lifecycle method?
    private static UsbManager mUsbManager = null;
    private static UsbDevice mUsbDevice = null;
    private static UsbDeviceConnection mUsbConnection = null;
    private static UsbSerialDevice mSerial = null;

    private static GrblController mGrblController;

    // --- File ---
    private File mFile;
    private List<GcodeCommand> mFileCommandQueue;
    private boolean mFileCommandQueueStarted;
    private int mFileCommandPosition = 0;

    public File getFile() { return mFile; }
    public List<GcodeCommand> getCommandQueue() { return mFileCommandQueue; }
    public boolean getFileCommandQueueStarted() { return mFileCommandQueueStarted; }
    public int getFileCommandPosition() { return mFileCommandPosition; }

    BroadcastReceiver mUsbAttachReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                //TODO: Automatically attach or bring up menu?
            }
        }
    };

    BroadcastReceiver mUsbDetachReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) { // Clean up and closes communication with the device
                    closeUSBDevice();
                }
            }
        }
    };

    //TODO: Disconnection errors

    public HashMap<String, UsbDevice> getUSBDevices() {
        return mUsbManager.getDeviceList();
    }

    private void init() {
        if(!mInitialized) {
            mInitialized = true;
            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            registerReceiver(mUsbAttachReceiver , filter);
            filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
            registerReceiver(mUsbDetachReceiver, filter);

            closeUSBDevice();

            mFileCommandQueue = new ArrayList<GcodeCommand>();
        }
    }

    public void openUSBDevice(String deviceName) {

        closeUSBDevice();

        UsbDevice selectedDevice = (UsbDevice) mUsbManager.getDeviceList().get(deviceName);

        if(selectedDevice != null) {
            if (!mUsbManager.hasPermission(selectedDevice)) {
                PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                mUsbManager.requestPermission(selectedDevice, mPermissionIntent);
                return;
            }

            mUsbDevice = selectedDevice;
            mUsbConnection = mUsbManager.openDevice(mUsbDevice);
            mSerial = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, mUsbConnection);
            //new ConnectionThread().run();

            if(mSerial != null) {
                if(mSerial.open()) {
                    mSerial.setBaudRate(115200);
                    mSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    mSerial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    mSerial.setParity(UsbSerialInterface.PARITY_NONE);
                    mSerial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                    mGrblController = new GrblController(mSerial);

                    EventBus.getDefault().postSticky(new CNCServiceEvent(CNCServiceEvent.EventType.USB_DEVICE_CONNECTED));
                } else {
                    //TODO: Alerts for these errors
                    Log.e(TAG, "Serial port could not be opened, maybe an I/O error or incorrect driver.");
                    closeUSBDevice();
                }
            } else {
                Log.e(TAG, "No driver for given device, even generic CDC driver could not be loaded.");
                closeUSBDevice();
            }
        } else {
            Log.e(TAG, "USB device not available.");
            closeUSBDevice();
        }
    }

    public void closeUSBDevice() {

        if(mSerial != null) {
            mSerial.close();
            mSerial = null;
        }

        if(mUsbConnection != null) {
            mUsbConnection.close();
            mUsbConnection = null;
        }
        mUsbDevice = null;

        EventBus.getDefault().postSticky(new CNCServiceEvent(CNCServiceEvent.EventType.USB_DEVICE_DISCONNECTED));
    }

    /*
    private class ConnectionThread extends Thread
    {
        @Override
        public void run() {

            mSerial = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, mUsbConnection);

            if(mSerial != null) {
                if(mSerial.open()) {
                    mSerial.setBaudRate(115200);
                    mSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    mSerial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    mSerial.setParity(UsbSerialInterface.PARITY_NONE);
                    mSerial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                    mGrblController = new GrblController(mSerial);

                    EventBus.getDefault().postSticky(new CNCServiceEvent(CNCServiceEvent.EventType.USB_DEVICE_CONNECTED));

                    // Everything went as expected. Send an intent to MainActivity
                    //Intent intent = new Intent(ACTION_USB_READY);
                    //context.sendBroadcast(intent);
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if(mSerial instanceof CDCSerialDevice) {
                        //Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        //context.sendBroadcast(intent);
                    } else {
                        //Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        //context.sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                //Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                //context.sendBroadcast(intent);
            }
        }
    }
    */

    public boolean isUSBDeviceConnected() {
        return mSerial != null;
    }

    public void sendSerialData(String data) {
        String output = data;
        mSerial.write(data.getBytes());
        Log.d(TAG, "Sent: " + data);
    }

    public void openFile(String filepath) {
        try {
            mFileCommandQueue = new ArrayList<GcodeCommand>();
            mFile = new File(filepath);
            mFileCommandPosition = 0;

            BufferedReader reader = new BufferedReader(new FileReader(mFile));

            String line;
            while((line = reader.readLine()) != null) {
                //TODO: if(line.length() > MAX_GRBL_LINE_LENGTH) DO SOMETHING
                mFileCommandQueue.add(new GcodeCommand(line));
            }
            reader.close();

            mFileCommandQueueStarted = false;

            EventBus.getDefault().post(new CNCServiceEvent(CNCServiceEvent.EventType.FILE_OPEN_SUCCESS)); //TODO: Receive this

        } catch(IOException e) {
            EventBus.getDefault().post(new CNCServiceEvent(CNCServiceEvent.EventType.FILE_OPEN_FAILURE)); //TODO: Receive this
        }
    }


    public void cycleStart() {
        if(mFile == null || mFileCommandQueue.size() <= 0) return;

        mCycleHandler.post(mCycleLoop);
    }

    private static final int CYCLE_DELAY = 100;
    private Handler mCycleHandler = new Handler();
    private Runnable mCycleLoop = new Runnable() {
        @Override
        public void run() {
            if(mFileCommandPosition > 0) mFileCommandQueue.get(mFileCommandPosition-1).setStatus(GcodeCommand.Status.COMPLETE);
            if(mFileCommandPosition < mFileCommandQueue.size()) mFileCommandQueue.get(mFileCommandPosition).setStatus(GcodeCommand.Status.RUNNING);

            mFileCommandPosition++;
            EventBus.getDefault().post(new CNCServiceEvent(CNCServiceEvent.EventType.FILE_COMMAND_STATUS_UPDATED));

            if(mFileCommandPosition < mFileCommandQueue.size()) {
                mCycleHandler.postDelayed(this, CYCLE_DELAY);
            }
        }
    };

    public GrblController getGrblController() {
        return mGrblController;
    }


    // --- Lifecycle ---
    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mUsbDetachReceiver);
        unregisterReceiver(mUsbAttachReceiver);

        closeUSBDevice();
    }
}
