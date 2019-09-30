package com.ngs.react;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMListenerService extends FirebaseMessagingService {

    private TwilioChatTokenService tts;
    private static final String LOG_TAG = "[Twi-Push]";
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(LOG_TAG, "FCMListenerService -> TwilioTokenService connected");

            TwilioChatTokenService.TwilioFCMListenerBinder binder = (TwilioChatTokenService.TwilioFCMListenerBinder) iBinder;
            tts = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(LOG_TAG, "FCMListenerService -> TwilioTokenService disconnected");
            tts = null;
        }
    };

    public FCMListenerService() {
        Log.d(LOG_TAG, "FCM Listener service instantiated");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "Starting twilio chat token service");
        Intent ttsIntent = new Intent(getApplicationContext(), TwilioChatTokenService.class);
        startService(ttsIntent);

        Intent intent = new Intent(getApplicationContext(), TwilioChatTokenService.class);
        getApplicationContext().bindService(intent, this.serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(LOG_TAG, "On new token: " + token);
        tts.setToken(token);
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
