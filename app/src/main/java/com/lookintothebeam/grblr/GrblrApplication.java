package com.lookintothebeam.grblr;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;

import com.lookintothebeam.grblr.cnc.CNCService;

public class GrblrApplication extends Application {

    // --- Static instance ---
    private static GrblrApplication sInstance;

    public GrblrApplication getsInstance() {
        return sInstance;
    }

    // --- CNC Service ---
    private CNCService mCNCService;
    private boolean mCNCServiceBound = false;
    private ServiceConnection mCNCServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CNCService.LocalBinder binder = (CNCService.LocalBinder) service;
            mCNCService = binder.getService();
            mCNCServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCNCServiceBound = false;
        }
    };

    // --- Application Lifecycle ---
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        Intent intent = new Intent(this, CNCService.class);
        bindService(intent, mCNCServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        unbindService(mCNCServiceConnection);
        mCNCServiceBound = false;
    }

    /*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    */
}
