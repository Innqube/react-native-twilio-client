package com.ngs.react.RNNotifications;

import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;
import com.google.firebase.messaging.RemoteMessage;
import com.ngs.react.Utils;

public class NotificationsModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "[IIMobile-Notif]";

    public NotificationsModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNNotificationsModule";
    }

    public void sendNotification(RemoteMessage remoteMessage) {
        Log.d(LOG_TAG, "Sending notification");
        WritableMap msg = Utils.convertRemoteMessageDataToMap(remoteMessage);
        Utils.sendEvent(getReactApplicationContext(),  "notificationReceived", msg);
    }

}
