package com.ngs.react.RNNotifications;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.google.firebase.messaging.RemoteMessage;

import java.lang.reflect.InvocationTargetException;

public class TwilioNotificationsService extends Service {

    private static final String LOG_TAG = "[Twi-Push]";
    private final IBinder binder = new TwilioFCMListenerBinder();
    private String token;
    private MessageReceivedDelegate delegate;

    public TwilioNotificationsService() {
        Log.d(LOG_TAG, "TwilioNotificationsService instantiated");
    }

    public class TwilioFCMListenerBinder extends Binder {
        public TwilioNotificationsService getService() {
            return TwilioNotificationsService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int res = super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "TwilioNotificationsService started");

        Class<MessageReceivedDelegate> delegateClass = (Class<MessageReceivedDelegate>) intent.getExtras().getSerializable("delegate");
        try {
            delegate = delegateClass
                    .getConstructor(Context.class, NotificationManager.class)
                    .newInstance(getApplicationContext(), getSystemService(NotificationManager.class));
            delegate.createNotificationChannel();
            Log.e(LOG_TAG, "MessageReceivedDelegate: " + delegateClass.getName() + " instantiated");
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(LOG_TAG, "Could not instantiate MessageReceivedDelegate: " + delegateClass.getName());
        }

        return res;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (delegate != null) {
            delegate.onMessageReceived(remoteMessage);
        } else {
            Log.w(LOG_TAG, "No MessageReceivedDelegate found!");
        }
    }

}
