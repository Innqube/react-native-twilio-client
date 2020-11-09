package com.ngs.react.RNNotifications;

import android.content.Intent;
import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class NotificationsModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "[IIMobile-Notif]";

    public NotificationsModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNNotificationsModule";
    }

    @ReactMethod
    public void startService() {
        Log.d(LOG_TAG, "Starting FCM service");

        Intent tnsIntent = new Intent(this.getReactApplicationContext(), TwilioNotificationsService.class);
        this.getReactApplicationContext().startService(tnsIntent);
    }

}
