package com.ngs.react.RNLogHelper;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class LogHelperModule extends ReactContextBaseJavaModule {

    public LogHelperModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNLogHelper";
    }

    @ReactMethod
    public void log(String message) {

        if (message != null) {
            Log.i("[IIMobile-RNLogHelper]", message);
        }

    }

}
