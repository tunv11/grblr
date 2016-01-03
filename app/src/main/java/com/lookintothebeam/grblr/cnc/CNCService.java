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
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;

import com.felhr.usbserial.*;

import de.greenrobot.event.EventBus;

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
    private UsbSerialInterface.UsbReadCallback mReadCallback;

    BroadcastReceiver mUsbAttachReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                //
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
            registerReceiver(mUsbDetachReceiver , filter);

            closeUSBDevice();
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

            mReadCallback = new UsbSerialInterface.UsbReadCallback() {
                @Override
                public void onReceivedData(byte[] arg0) {
                    Log.d(TAG, "FROM SERIAL: " + new String(arg0));
                }
            };

            if(mSerial != null) {
                if(mSerial.open()) {
                    mSerial.setBaudRate(115200);
                    mSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    mSerial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    mSerial.setParity(UsbSerialInterface.PARITY_NONE);
                    mSerial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    mSerial.read(mReadCallback);

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

    public boolean isUSBDeviceConnected() {
        return mSerial != null;
    }

    public void sendSerialData(String data) {
        String output = data + "\n";
        mSerial.write(output.getBytes());
        Log.d(TAG, "Sent: " + data);
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
