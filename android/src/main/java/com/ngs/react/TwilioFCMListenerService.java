package com.ngs.react;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class TwilioFCMListenerService extends Service {

    private static final String LOG_TAG = "[Twi-Push]";
    private final IBinder binder = new TwilioFCMListenerBinder();
    private String token;

    public TwilioFCMListenerService() {
        Log.d(LOG_TAG, "TwilioFCMListenerService instantiated");
    }

    public class TwilioFCMListenerBinder extends Binder {
        public TwilioFCMListenerService getService() {
            return TwilioFCMListenerService.this;
        }
    }

    public class ListenerService extends FirebaseMessagingService {

        @Override
        public void onNewToken(String t) {
            Log.d(LOG_TAG, "On new token: " + token);

            super.onNewToken(t);
            token = t;
        }

        @Override
        public void onMessageReceived(RemoteMessage remoteMessage) {
            super.onMessageReceived(remoteMessage);

            Log.d(LOG_TAG, "Received push notification: " + remoteMessageToString(remoteMessage));
        }

        private String remoteMessageToString(RemoteMessage msg) {
            return "RemoteMessage[from:" + msg.getFrom() + ", " +
                    "messageId: " + msg.getMessageId() + ", " +
                    "messageType: " + msg.getMessageType() + ", " +
                    "to: " + msg.getTo() + ", " +
                    "data: " + (msg.getData() == null ? "null" : msg.getData().toString()) + "]";
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind called");
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate called");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int value = super.onStartCommand(intent, flags, startId);

        Log.d(LOG_TAG, "Starting FCM listener service");
        Intent fcmServiceIntent = new Intent(getApplicationContext(), ListenerService.class);
        startService(fcmServiceIntent);

        return value;
    }

    public String getToken() {
        return token;
    }

}
