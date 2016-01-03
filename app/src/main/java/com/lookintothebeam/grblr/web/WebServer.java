package com.lookintothebeam.grblr.web;

import com.lookintothebeam.grblr.R;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {

    private static final String TAG = "WebServer";

    private Context context;

    public WebServer(Context context) throws IOException {
        super(8080);
        this.context = context;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        Log.i(TAG, "Web server running on port 8080.\n");
    }

    /*
    public static void main(String[] args) {
        try {
            new WebServer();
        } catch (IOException ioe) {
            Log.e(TAG, "Couldn't start server:\n" + ioe);
        }
    }
    */

    @Override
    public Response serve(IHTTPSession session) {

        String output = "";

        try {
            InputStream file = context.getResources().openRawResource(R.raw.index);

            byte[] b = new byte[file.available()];
            file.read(b);
            output += new String(b);
            return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML, output);

        } catch (Exception e) {
            Log.e(TAG, e.toString());
            output += "Not found.";
            return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML, output);
        }
                /*
        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, String> parms = session.getParms();
        if (parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }
        return newFixedLengthResponse(msg + "</body></html>\n");
        */
    }
}
