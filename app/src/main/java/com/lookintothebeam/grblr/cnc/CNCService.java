package com.lookintothebeam.grblr.cnc;

import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.felhr.usbserial.*;

public class CNCService extends Service {

    private static final String TAG = "CNCService";
    private static final String ACTION_USB_PERMISSION = "com.lookintothebeam.grblr.USB_PERMISSION";

    public static CNCService sInstance;

    private static UsbManager usbManager;
    private static UsbDevice usbDevice;
    private static UsbDeviceConnection usbConnection;
    private static UsbSerialDevice serial;
    private UsbSerialInterface.UsbReadCallback readCallback;

    //TODO: Disconnection errors

    public CNCService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sInstance = this;
        Log.d(TAG, "Service started.");

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        return START_STICKY;
    }

    public static HashMap<String, UsbDevice> getUSBDevices() {

        HashMap usbDevices = usbManager.getDeviceList();
        return usbDevices;

        /*
        UsbDevice device;

        for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()){

            device = entry.getValue();
            int deviceVID = device.getVendorId();
            int devicePID = device.getProductId();

            if(deviceVID != 0x1d6b || (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003)) {
                //Build a map of these devices
            }

        }
        */
    }

    public void openUSBDevice(String deviceName) {

        HashMap usbDevices = usbManager.getDeviceList();

        UsbDevice selectedDevice = (UsbDevice) usbDevices.get(deviceName);

        if(selectedDevice != null) {

            if (!usbManager.hasPermission(selectedDevice)) {
                PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(selectedDevice, mPermissionIntent);
                return;
            }

            usbDevice = selectedDevice;
            usbConnection = usbManager.openDevice(usbDevice);
            serial = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbConnection);

            readCallback = new UsbSerialInterface.UsbReadCallback() {
                @Override
                public void onReceivedData(byte[] arg0) {
                    Log.d(TAG, "FROM SERIAL: " + new String(arg0));
                }
            };

            if(serial != null) {
                if(serial.open()) {
                    serial.setBaudRate(115200);
                    serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serial.setParity(UsbSerialInterface.PARITY_NONE);
                    serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serial.read(readCallback);
                } else {
                    //TODO: Alerts for these errors
                    Log.e(TAG, "Serial port could not be opened, maybe an I/O error or it CDC driver was chosen it does not really fit.");
                }
            } else {
                Log.e(TAG, "No driver for given device, even generic CDC driver could not be loaded.");
            }
        } else {
            Log.e(TAG, "USB Device not available");
        }

    }

    public void closeUSBDevice() {
        if(usbConnection != null) {

        }
    }

    public void sendSerialData(String data) {
        String output = data + "\n";
        serial.write(output.getBytes());
        Log.d(TAG, "Sent: " + data);
    }



    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
