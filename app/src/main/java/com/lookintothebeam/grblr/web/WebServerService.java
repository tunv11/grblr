package com.lookintothebeam.grblr.web;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class WebServerService extends Service {

    private static final String TAG = "WebServerService";

    WebServer webServer;

    public WebServerService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            webServer = new WebServer(getApplicationContext());

            //TODO: Replace this with generic network manager?
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

            Toast.makeText(this, "Web server started: http://" + ip + ":8080", Toast.LENGTH_SHORT).show();

        } catch(IOException ioe) {
            Toast.makeText(this, "Error starting web server:\n" + ioe, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Couldn't start server:\n" + ioe);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        webServer.stop();
        Toast.makeText(this, "Service started.", Toast.LENGTH_LONG).show();
    }
}
