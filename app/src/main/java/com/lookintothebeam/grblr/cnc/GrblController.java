package com.lookintothebeam.grblr.cnc;

import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class GrblController {

    private static final String TAG = "GrblController";
    private static final int RECEIVE_BUFFER_SIZE = 128;
    public static final int MAX_GRBL_LINE_LENGTH = 80;

    // --- USB Serial ---
    private UsbSerialDevice mSerial;
    private UsbSerialInterface.UsbReadCallback mReadCallback;
    private ByteBuffer mReceiveBuffer;

    // --- Status Checker Loop --
    public static final int STATUS_CHECK_DELAY = 250;
    private Handler mStatusCheckHandler = new Handler();
    private Runnable mStatusCheckLoop = new Runnable() {
        @Override
        public void run() {
            grblRequestStatus();
            mStatusCheckHandler.postDelayed(this, STATUS_CHECK_DELAY);
        }
    };

    // --- State ---
    private boolean mConnected;

    //private enum Unit { INCH, MM };
    //private Unit mGrblUnits; // Units

    public static enum GrblStatus { IDLE, ALARM, CHECK_MODE, HOMING, CYCLE, HOLD, SAFETY_DOOR };

    private GrblStatus mGrblStatus; // Machine status
    private float[] mGrblMachinePos; // Machine Position
    private float[] mGrblWorkPos; // Work Position
    private float mGrblMotionBuf; // Number of motions queued in Grbl's planner buffer
    private float mGrblRxBuf; // Number of characters queued in Grbl's serial RX receive buffer

    public GrblStatus getMachineStatus() { return mGrblStatus; }
    public float[] getMachinePos() { return mGrblMachinePos; }
    public float[] getWorkPos() { return mGrblWorkPos; }

    public GrblController(UsbSerialDevice serial) {
        mSerial = serial;
        mReceiveBuffer = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE);

        mReadCallback = new UsbSerialInterface.UsbReadCallback() {
            @Override
            public void onReceivedData(byte[] arg0) {

                for(int i = 0; i < arg0.length; i++) {
                    mReceiveBuffer.put((byte) arg0[i]);

                    // Check for newline character
                    if(arg0[i] == (byte) 0x0A) {

                        // Read the buffer contents
                        mReceiveBuffer.flip();
                        byte[] outputArray = new byte[mReceiveBuffer.limit()];
                        mReceiveBuffer.get(outputArray);

                        // Convert to string and process
                        processResponse(new String(outputArray).trim());

                        //Reset buffer
                        mReceiveBuffer.clear();
                    }
                }
            }
        };

        serial.read(mReadCallback);

        // Initialize state
        mConnected = false;
        mGrblMachinePos = new float[3];
        mGrblWorkPos = new float[3];
    }

    private void startStatusChecker() {
        mStatusCheckHandler.post(mStatusCheckLoop);
    }

    private void stopStatusChecker() {
        mStatusCheckHandler.removeCallbacks(mStatusCheckLoop);
    }

    // --- Commands Sent to grbl ---
    private void grblRequestStatus() {
        mSerial.write("?\n".getBytes());
    }
    private void grblCycleStart() {
        mSerial.write("~\n".getBytes());
    }
    private void grblFeedHold() {
        mSerial.write("!\n".getBytes());
    }
    private void grblHomingCycle() {
        mSerial.write("$H\n".getBytes());
    }
    private void grblKillAlarmLock() {
        mSerial.write("$X\n".getBytes());
    }

    // --- Actions ---
    public void stop() {
        grblFeedHold();
    }
    public void resume() {
        grblCycleStart();
    }
    public void homingSequence() {
        grblHomingCycle();
    }
    public void killAlarmLock() {
        grblKillAlarmLock();
    }

    // --- Receiving commands from grbl ---
    private void processResponse(String response) {
        Log.d(TAG, "grbl: " + response);

        if(response.matches("^<.*>$")) { handleStatusResponse(response); return; }
        if(response.matches("^ok$")) { handleOKResponse(); return; }
        /*
        if(response.matches("^error:")) { handleErrorResponse(response.split(":")[1]); return; }
        if(response.matches("^ALARM:")) { handleAlarmResponse(response.split(":")[1]); return; }
        if(response.matches("^\\[.*\\]$")) { handleFeedbackResponse(response); return; }

        handleUnknownResponse(response);
        */
    }

    private void handleStatusResponse(String res) {
        // Trim the brackets
        String response = res.substring(1, res.length()-1);

        String[] parts = response.split(",");

        for(int i = 0; i < parts.length; i++) {
            if(parts[i].contains("Idle"))   { mGrblStatus = GrblStatus.IDLE; continue; }
            if(parts[i].contains("Alarm"))  { mGrblStatus = GrblStatus.ALARM; continue; }
            if(parts[i].contains("Check"))  { mGrblStatus = GrblStatus.CHECK_MODE; continue; }
            if(parts[i].contains("Home"))   { mGrblStatus = GrblStatus.HOMING; continue; }
            if(parts[i].contains("Run"))    { mGrblStatus = GrblStatus.CYCLE; continue; }
            if(parts[i].contains("Hold"))   { mGrblStatus = GrblStatus.HOLD; continue; }
            if(parts[i].contains("Door"))   { mGrblStatus = GrblStatus.SAFETY_DOOR; continue; }

            if(parts[i].contains("Buf")) { mGrblMotionBuf = Integer.parseInt(parts[i].split(":")[1]); }
            if(parts[i].contains("RX")) { mGrblRxBuf = Integer.parseInt(parts[i].split(":")[1]); }

            if(parts[i].contains("MPos")) {
                mGrblMachinePos[0] = Float.parseFloat(parts[i].split(":")[1]);
                mGrblMachinePos[1] = Float.parseFloat(parts[i+1]);
                mGrblMachinePos[2] = Float.parseFloat(parts[i+2]);
            }

            if(parts[i].contains("WPos")) {
                mGrblWorkPos[0] = Float.parseFloat(parts[i].split(":")[1]);
                mGrblWorkPos[1] = Float.parseFloat(parts[i+1]);
                mGrblWorkPos[2] = Float.parseFloat(parts[i+2]);
            }
        }

        EventBus.getDefault().postSticky(new GrblControllerEvent(GrblControllerEvent.EventType.MACHINE_STATUS_UPDATED));
    }

    private void handleOKResponse() {

        if(!mConnected) {
            startStatusChecker();
            mConnected = true;
        }
    }


}
