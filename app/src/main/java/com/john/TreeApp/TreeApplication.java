package com.john.TreeApp;

import android.app.Application;
import android.util.Log;

public class TreeApplication extends Application {
    private static final String TAG = "TreeApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        // Enable all logging
        Log.e(TAG, "Application started - ERROR level");
        Log.w(TAG, "Application started - WARN level");
        Log.i(TAG, "Application started - INFO level");
        Log.d(TAG, "Application started - DEBUG level");
        Log.v(TAG, "Application started - VERBOSE level");
    }
}
